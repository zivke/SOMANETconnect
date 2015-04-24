package SOMANETconnect;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public final class Util {
    private final static Logger logger = Logger.getLogger(Util.class.getName());

    public static void sendWebSocketResultResponse(RemoteEndpoint remoteEndpoint, Object value, Object requestId) {
        try {
            JSONRPC2Response response = new JSONRPC2Response(value, requestId);
            remoteEndpoint.sendString(response.toString());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public static void sendWebSocketErrorResponse(RemoteEndpoint remoteEndpoint, JSONRPC2Error error, Object requestId) {
        try {
            JSONRPC2Response response = new JSONRPC2Response(error, requestId);
            remoteEndpoint.sendString(response.toString());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public static void killProcess(Process process) {
        if (process == null) {
            return;
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            if (process.getClass().getName().equals("java.lang.Win32Process") ||
                    process.getClass().getName().equals("java.lang.ProcessImpl")) {
                // Determine the pid on windows platforms
                try {
                    Field field = process.getClass().getDeclaredField("handle");
                    field.setAccessible(true);
                    long handleId = field.getLong(process);

                    Kernel32 kernel = Kernel32.INSTANCE;
                    WinNT.HANDLE handle = new WinNT.HANDLE();
                    handle.setPointer(Pointer.createConstant(handleId));
                    int pid = kernel.GetProcessId(handle);
                    ArrayList<String> command = new ArrayList<>();
                    command.add("taskkill");
                    command.add("/f");
                    command.add("/t");
                    command.add("/pid");
                    command.add(String.valueOf(pid));
                    new SystemProcess(command);
                } catch (Throwable e) {
                    logger.error(e.getMessage());
                }
            }
        } else {
            process.destroy();
        }
    }

    /**
     * Kill any residual processes (that the main process may have started) by the unique name of the temporary file
     * used in the original command (the temporary file contains the ID of the request that was used to start the
     * process in its name).
     *
     * @param requestId ID of the request used to start the process (that needs cleaning up after)
     * @throws IOException
     */
    public static void linuxProcessCleanup(String requestId) throws IOException {
        // Kill any residual processes (that the main process may have started) by the unique name of
        // the temporary file used in the original command
        ArrayList<String> command = new ArrayList<>();
        command.add("pkill");
        command.add("-f");
        command.add(requestId);
        new SystemProcess(command);
    }

    public static void startOnBoot(boolean startOnBoot) throws IOException {
        if (SystemUtils.IS_OS_LINUX) {
            Path desktopFile = Paths.get(System.getenv("HOME"), "/.config/autostart/SOMANETconnect.desktop");
            if (startOnBoot) {
                String applicationJarPath = System.getProperty("user.dir") + "/SOMANETconnect.jar";
                String libPath = System.getProperty("user.dir") + "/lib";
                String command = "java -Djava.library.path=" + libPath + " -cp " + applicationJarPath
                        + " SOMANETconnect.SomanetConnect";
                String desktopFileContent = "[Desktop Entry]\nType=Application\nName=SOMANETconnect\nPath="
                        + System.getProperty("user.dir") + "\nExec=" + command + "\n";
                Files.write(desktopFile, desktopFileContent.getBytes());
            } else {
                Files.deleteIfExists(desktopFile);
            }
        }
    }

    public static boolean isStartOnBootEnabled() {
        if (SystemUtils.IS_OS_LINUX) {
            Path desktopFile = Paths.get(System.getenv("HOME"), "/.config/autostart/SOMANETconnect.desktop");
            return Files.exists(desktopFile);
        }
        return false;
    }
}
