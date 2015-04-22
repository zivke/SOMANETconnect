package SOMANETconnect;

import org.apache.commons.lang3.SystemUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Constants {
    public final static String ERROR = "error";
    public final static String LIST = "list";
    public final static String FLASH = "flash";
    public final static String RUN = "run";
    public final static String INTERRUPT = "interrupt";
    public final static String XSCOPE = "xscope";
    public final static String XSCOPE_PROBE_REG = "xscope_probe_reg";
    public final static String START_XSCOPE = "start-xscope";
    public final static String STOP_XSCOPE = "stop-xscope";
    public final static String START_MOTOR = "start-motor";
    public final static String STOP_MOTOR = "stop-motor";
    public final static String SEND = "send";
    public final static String PARAMS = "params";
    public final static String EXEC_LIVE = "exec_live";
    public final static String EXEC_DONE = "exec_done";
    public final static String ERASE_FIRMWARE = "erase-firmware";
    public final static String CODE = "code";
    public final static String MESSAGE = "message";
    public final static String METHOD = "method";
    public final static String ID = "id";
    public final static String JSONRPC = "jsonrpc";
    public final static String JSONRPC_VERSION = "2.0";
    public final static String TYPE = "type";
    public final static String NUMBER = "number";
    public final static String NAME = "name";
    public final static String PROBE = "probe";
    public final static String DATA = "data";
    public final static String TIMESTAMP = "timestamp";
    public final static String ADAPTER_ID = "adapter_id";
    public final static String DEVICES = "devices";
    public final static String CONTENT = "content";
    public final static String RESULT = "result";
    public final static String MOTOR_PARAMETERS = "motor_parameters";
    public final static Map<String, String> environmentVariables;

    static {
        Map<String, String> tmpMap = new HashMap<>();
        String delimiter;
        String currentDir = System.getProperty("user.dir");
        String homeDir;
        if (SystemUtils.IS_OS_WINDOWS) {
            delimiter = ";";
            homeDir = getEnv("HOMEDRIVE") + getEnv("HOMEPATH");
        } else {
            delimiter = ":";
            homeDir = getEnv("HOME");
        }
        String pathEnvVar = getEnv("PATH");
        tmpMap.put("XMOS_TOOL_PATH", currentDir);
        tmpMap.put("installpath", currentDir);
        String xmosHome = homeDir + "/.xmos";
        tmpMap.put("XMOS_HOME", xmosHome);
        if (SystemUtils.IS_OS_WINDOWS) {
            tmpMap.put("Path", currentDir + "/bin" + delimiter + pathEnvVar);
        } else {
            tmpMap.put("PATH", currentDir + "/bin" + delimiter + pathEnvVar);
        }
        String ldLibraryPath = getEnv("LD_LIBRARY_PATH");
        tmpMap.put("LD_LIBRARY_PATH", currentDir + "/lib" + delimiter + ldLibraryPath);
        String xccCIncludePath = currentDir + "/target/include" + delimiter + currentDir + "/target/include/gcc";
        tmpMap.put("XCC_C_INCLUDE_PATH", xccCIncludePath);
        String xccXcIncludePath = currentDir + "/target/include/xc" + delimiter + xccCIncludePath;
        tmpMap.put("XCC_XC_INCLUDE_PATH", xccXcIncludePath);
        String xccCPlusIncludePath = xccXcIncludePath + delimiter + currentDir + "/target/include/c++/4.2.1" + delimiter
                + currentDir + "/target/include/c++/4.2.1/xcore-xmos-elf";
        tmpMap.put("XCC_CPLUS_INCLUDE_PATH", xccCPlusIncludePath);
        tmpMap.put("XCC_ASSEMBLER_INCLUDE_PATH", xccCIncludePath);
        tmpMap.put("XCC_LIBRARY_PATH", currentDir + "/target/lib");
        tmpMap.put("XCC_DEVICE_PATH", currentDir + "/configs" + delimiter + currentDir + "/configs/.deprecated");
        tmpMap.put("XCC_TARGET_PATH", xmosHome + "/targets" + delimiter + currentDir + "/targets" + delimiter
                + currentDir + "/targets/.deprecated");
        tmpMap.put("XCC_EXEC_PREFIX", currentDir + "/libexec/");
        tmpMap.put("PYTHON_HOME", currentDir + "/lib/jython");
        tmpMap.put("PYTHON_VERBOSE", "warning");
        tmpMap.put("XMOS_CACHE_PATH", xmosHome + "/cache");
        tmpMap.put("XMOS_REPO_PATH", xmosHome + "/repos");
        environmentVariables = Collections.unmodifiableMap(tmpMap);
    }

    private static String getEnv(String env) {
        String value = System.getenv(env);
        if (value == null) {
            value = "";
        }
        return value;
    }
}
