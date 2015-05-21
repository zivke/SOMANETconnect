package SOMANETconnect.device;

import SOMANETconnect.systemprocess.SystemProcess;
import org.apache.log4j.Logger;
import org.usb4java.*;

import java.io.IOException;
import java.util.*;

// Singleton
public class DeviceManager extends Observable {
    private static DeviceManager deviceManager = new DeviceManager();

    private static final Logger logger = Logger.getLogger(DeviceManager.class.getName());

    private static final int XMOS_VENDOR_ID = 0x20b1;

    /**
     * The hotplug callback handler
     */
    class UsbHotplugCallback implements HotplugCallback {
        private final Logger logger = Logger.getLogger(UsbHotplugCallback.class.getName());

        @Override
        public int processEvent(Context context, Device device, int event, Object userData) {
            DeviceDescriptor descriptor = new DeviceDescriptor();
            int result = LibUsb.getDeviceDescriptor(device, descriptor);
            if (result != LibUsb.SUCCESS) {
                logger.error("Unable to read device descriptor");
                return result;
            }
            String status;
            if (event == LibUsb.HOTPLUG_EVENT_DEVICE_ARRIVED) {
                status = "Connected";
                if (++xmosDeviceCount == 1) {
                    devicePollingTimerTask = new TimerTask() {
                        @Override
                        public void run() {
                            timerTaskRunnable.run();
                        }
                    };
                    devicePollingTimer.schedule(devicePollingTimerTask, 0, 2000);
                }
            } else {
                status = "Disconnected";
                if (--xmosDeviceCount == 0) {
                    devicePollingTimerTask.cancel();
                    // Needs to run one more time to register that are no more devices connected
                    timerTaskRunnable.run();
                }
            }
            logger.info(status + ": " + Integer.toHexString(descriptor.idVendor()) + ":"
                    + Integer.toHexString(descriptor.idProduct()));
            return 0;
        }
    }

    // usb4java context
    private Context usbContext;
    private UsbEventHandlingThread usbEventHandlingThread;
    private HotplugCallbackHandle callbackHandle;

    private DeviceList deviceList;
    private int xmosDeviceCount = 0;
    private Timer devicePollingTimer = new Timer();
    private Runnable timerTaskRunnable = new Runnable() {
        @Override
        public void run() {
            ArrayList<String> command = new ArrayList<>();
            // Use the xrun command instead of xflash because of the bug in XMOS tools (starting from version 13.2.0)
            // where xflash always returns 1 instead of 0
            command.add(System.getProperty("user.dir") + "/bin/xrun");
            command.add("-l");
            SystemProcess listSystemProcess;
            try {
                listSystemProcess = new SystemProcess(command);
                if (listSystemProcess.getResult() != 0) {
                    throw new IOException(listSystemProcess.getError());
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
                DeviceManager.this.setDeviceList(new DeviceList());
                return;
            }

            // Sometimes when a device is plugged/unplugged just at the right time, the process output and error streams
            // are empty and the result is 0
            if (listSystemProcess.getOutput().isEmpty() && listSystemProcess.getError().isEmpty()
                    && listSystemProcess.getResult() == 0) {
                logger.error("Error detecting devices");
                return;
            }

            DeviceList newDeviceList = new DeviceList(listSystemProcess.getOutput());
            if (!DeviceManager.this.deviceList.equals(newDeviceList)) {
                DeviceManager.this.setDeviceList(newDeviceList);
            }
        }
    };
    private TimerTask devicePollingTimerTask = new TimerTask() {
        @Override
        public void run() {
            timerTaskRunnable.run();
        }
    };

    private DeviceManager() {
        this.deviceList = new DeviceList();

        // Create the libusb context
        this.usbContext = new Context();

        // Initialize the libusb context
        int result = LibUsb.init(usbContext);
        if (result == LibUsb.SUCCESS) {
            // Check if hotplug is available
            if (LibUsb.hasCapability(LibUsb.CAP_HAS_HOTPLUG)) {
                // Start the event handling thread
                usbEventHandlingThread = new UsbEventHandlingThread();
                usbEventHandlingThread.start();

                // Register the hotplug callback
                callbackHandle = new HotplugCallbackHandle();
                result = LibUsb.hotplugRegisterCallback(null,
                        LibUsb.HOTPLUG_EVENT_DEVICE_ARRIVED | LibUsb.HOTPLUG_EVENT_DEVICE_LEFT,
                        LibUsb.HOTPLUG_ENUMERATE,
                        XMOS_VENDOR_ID,
                        LibUsb.HOTPLUG_MATCH_ANY,
                        LibUsb.HOTPLUG_MATCH_ANY,
                        new UsbHotplugCallback(), null, callbackHandle);
                if (result != LibUsb.SUCCESS) {
                    logger.error("Unable to register hotplug callback");
                    xmosDeviceCount = -1;
                }
            } else {
                logger.error("libusb doesn't support hotplug on this system");
                xmosDeviceCount = -1;
            }
        } else {
            logger.error("Unable to initialize libusb");
            xmosDeviceCount = -1;
        }

        if (xmosDeviceCount == -1) {
            devicePollingTimer.schedule(devicePollingTimerTask, 0, 2000);
        }
    }

    public static DeviceManager getInstance() {
        return deviceManager;
    }

    public List<Map<String, String>> getDevices() {
        return deviceList.getDevices();
    }

    public DeviceList getDeviceList() {
        return deviceList;
    }

    public void setDeviceList(DeviceList deviceList) {
        this.deviceList = deviceList;
        setChanged();
        notifyObservers();
    }

    /**
     * Override the addObserver() method so that it notifies every newly registered observer of the current state
     */
    @Override
    public synchronized void addObserver(Observer o) {
        super.addObserver(o);
        setChanged();
        notifyObservers();
    }

    private int countXmosDevices() {
        int count = 0;
        org.usb4java.DeviceList list = new org.usb4java.DeviceList();
        int result = LibUsb.getDeviceList(usbContext, list);
        if (result < 0) {
            logger.error("Unable to get device list");
            return -1;
        }

        try {
            for (Device device : list) {
                DeviceDescriptor descriptor = new DeviceDescriptor();
                result = LibUsb.getDeviceDescriptor(device, descriptor);
                if (result != LibUsb.SUCCESS) {
                    logger.error("Unable to read device descriptor");
                } else {
                    if (descriptor.idVendor() == XMOS_VENDOR_ID) {
                        count++;
                    }
                }
            }
        } finally {
            LibUsb.freeDeviceList(list, true);
        }
        return count;
    }

    public void close() {
        if (callbackHandle != null) {
            // Unregister the hotplug callback and stop the event handling thread
            LibUsb.hotplugDeregisterCallback(null, callbackHandle);
        }
        if (usbEventHandlingThread != null) {
            try {
                usbEventHandlingThread.join();
            } catch (InterruptedException e) {
                // NO-OP
            }
        }
        if (usbContext != null) {
            LibUsb.exit(usbContext);
        }
    }
}
