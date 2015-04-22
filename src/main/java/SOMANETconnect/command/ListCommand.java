package SOMANETconnect.command;

import SOMANETconnect.Constants;
import SOMANETconnect.SystemProcess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListCommand extends SystemProcess {
    public ListCommand() throws IOException {
        super(System.getProperty("user.dir") + "/bin/xflash -l");

        if (getResult() != 0) {
            throw new IOException(getError());
        }
    }

    public List getDeviceList() {
        List<Map> deviceList = new ArrayList<>();
        if (!getOutput().contains("No Available Devices Found")) {
            String output = getOutput();
            int marker = output.indexOf("Available XMOS Devices");
            marker = output.indexOf("  0 ", marker);
            output = output.substring(marker).trim();
            String[] lines = output.split(System.getProperty("line.separator"));
            for (String line : lines) {
                String[] properties = line.trim().split("\\s+");
                Map<String, String> device = new HashMap<>();
                device.put(Constants.ID, properties[0]);
                device.put(Constants.NAME, properties[1]);
                device.put(Constants.ADAPTER_ID, properties[2]);
                device.put(Constants.DEVICES, properties[3]);

                deviceList.add(device);
            }
        }
        return deviceList;
    }
}
