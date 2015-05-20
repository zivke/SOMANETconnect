package SOMANETconnect.miscellaneous;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceList {
    private List<Map<String, String>> devices;

    public DeviceList() {
        this.devices = new ArrayList<>();
    }

    public DeviceList(List<Map<String, String>> devices) {
        this.devices = devices;
    }

    public DeviceList(final String output) {
        devices = new ArrayList<>();
        if (!output.contains("No Available Devices Found")) {
            String tmpOutput = output;
            int marker = tmpOutput.indexOf("Available XMOS Devices");
            marker = tmpOutput.indexOf("  0 ", marker);
            tmpOutput = tmpOutput.substring(marker).trim();
            String[] lines = tmpOutput.split(System.getProperty("line.separator"));
            for (String line : lines) {
                String[] properties = line.trim().split("\\t");
                Map<String, String> device = new HashMap<>();
                device.put(Constants.ID, properties[0].trim());
                device.put(Constants.NAME, properties[1].trim());
                device.put(Constants.ADAPTER_ID, properties[2].trim());
                device.put(Constants.DEVICES, properties[3].trim());

                devices.add(device);
            }
        }
    }

    public List<Map<String, String>> getDevices() {
        return devices;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof DeviceList)) {
            return false;
        }
        DeviceList otherDeviceList = (DeviceList) object;
        if (otherDeviceList.getDevices().size() != this.getDevices().size()) {
            return false;
        }
        for (int i = 0; i < this.getDevices().size(); i++) {
            List<Map<String, String>> otherDevices = otherDeviceList.getDevices();
            if (!this.getDevices().get(i).values().containsAll(otherDevices.get(i).values())) {
                return false;
            }
        }
        return true;
    }
}
