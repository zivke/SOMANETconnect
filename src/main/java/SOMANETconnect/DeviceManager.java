package SOMANETconnect;

import SOMANETconnect.miscellaneous.DeviceList;
import SOMANETconnect.systemprocess.SystemProcess;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;

// Singleton
public class DeviceManager extends Observable {
    private static DeviceManager deviceManager = new DeviceManager();

    private static final Logger logger = Logger.getLogger(DeviceManager.class.getName());

    private DeviceList deviceList;
    private Timer devicePollingTimer = new Timer();
    private TimerTask devicePollingTimerTask = new TimerTask() {
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

    private DeviceManager() {
        this.deviceList = new DeviceList();
        devicePollingTimer.schedule(devicePollingTimerTask, 0, 2000);
    }

    public static DeviceManager getInstance() {
        return deviceManager;
    }

    public List<Map<String, String>> getDevices() {
        return deviceList.getDevices();
    }

    private void setDeviceList(DeviceList deviceList) {
        this.deviceList = deviceList;
        setChanged();
        notifyObservers();
    }
}
