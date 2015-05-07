package SOMANETconnect.command;

import SOMANETconnect.miscellaneous.Constants;
import SOMANETconnect.systemprocess.SystemProcess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListCommand extends SystemProcess {
    public ListCommand() throws IOException {
        super(prepareCommand());

        if (getResult() != 0) {
            throw new IOException(getError());
        }
    }

    public static List<String> prepareCommand() {
        ArrayList<String> command = new ArrayList<>();
        // Use the xrun command instead of xflash because of the bug in XMOS tools (starting from version 13.2.0) where
        // xflash always returns 1 instead of 0
        command.add(System.getProperty("user.dir") + "/bin/xrun");
        command.add("-l");
        return command;
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
                String[] properties = line.trim().split("\\t");
                Map<String, String> device = new HashMap<>();
                device.put(Constants.ID, properties[0].trim());
                device.put(Constants.NAME, properties[1].trim());
                device.put(Constants.ADAPTER_ID, properties[2].trim());
                device.put(Constants.DEVICES, properties[3].trim());

                deviceList.add(device);
            }
        }
        return deviceList;
    }
}
