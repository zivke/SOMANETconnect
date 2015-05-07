package SOMANETconnect.miscellaneous;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RegKeyManager {

    private final String TYPES[] = {"SZ", "BINARY", "DWORD", "QWORD", "DWORD_LITTLE_ENDIAN", "QWORD_LITTLE_ENDIAN", "DWORD_BIG_ENDIAN", "EXPAND_SZ", "LINK", "MULTI_SZ", "NONE", "RESOURCE_LIST"};
    private String type = "", value = "", key = "";

    protected void query(String loc, String k) throws Exception {
        Process p = Runtime.getRuntime().exec("reg QUERY \"" + loc + "\" /v \"" + k + "\"");

        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String out = "";

        while ((out = in.readLine()) != null) {
            if (out.matches("(.*)\\s+REG_(.*)")) {
                break;
            }
        }
        in.close();

        String str[] = out.split(" ");
        int b = 0;
        for (int a = 0; a < str.length; a++) {
            if (str[a].matches("\\S+")) {
                switch (b) {
                    case 0:
                        key = str[a];
                        break;
                    case 1:
                        type = str[a];
                        break;
                    case 2:
                        value = str[a];
                        break;
                }
                b++;
            }
        }
    }

    protected String getKey() {
        return key;
    }

    protected String getType() {
        return type;
    }

    protected String getValue() {
        return value;
    }

    protected boolean add(String loc, String name, String dType, String value) throws Exception {
        boolean comp = false, valid = false;

        for (int a = 0; a < TYPES.length; a++) {
            if (dType.equalsIgnoreCase("REG_" + TYPES[a])) {
                valid = true;
                break;
            }
        }

        if (valid) {
            Process p = Runtime.getRuntime().exec("reg ADD \"" + loc + "\" /v \"" + name + "\" /t \"" + dType + "\" /d \"" + value + "\"");

            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String out = "";

            while ((out = in.readLine()) != null) {
                if (out.equalsIgnoreCase("The operation completed successfully.")) {
                    comp = true;
                }
            }
            in.close();
        }

        return comp;
    }

    protected boolean delete(String loc, String key) throws Exception {
        boolean comp = false;
        Process p = Runtime.getRuntime().exec("reg DELETE \"" + loc + "\" /v \"" + key + "\" /f");

        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String out = "";

        while ((out = in.readLine()) != null) {
            if (out.equalsIgnoreCase("The operation completed successfully.")) {
                comp = true;
            }
        }
        in.close();

        return comp;
    }
}