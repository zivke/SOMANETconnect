package SOMANETconnect;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
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
            Path autoStartDir = Paths.get(System.getenv("HOME"), ".config", "autostart");
            if (!Files.exists(autoStartDir)) {
                Files.createDirectories(autoStartDir);
            }
            Path desktopFile = Paths.get(autoStartDir.toString(), "SOMANETconnect.desktop");
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
        } else if (SystemUtils.IS_OS_WINDOWS) {
            RegKeyManager rkm = new RegKeyManager();
            try {
                if (startOnBoot) {
                    rkm.add("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "SOMANETconnect", "REG_SZ",
                            System.getProperty("user.dir") + "\\start.vbs");
                } else {
                    rkm.delete("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "SOMANETconnect");
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }

    public static boolean isStartOnBootEnabled() {
        if (SystemUtils.IS_OS_LINUX) {
            Path desktopFile = Paths.get(System.getenv("HOME"), "/.config/autostart/SOMANETconnect.desktop");
            return Files.exists(desktopFile);
        } else if (SystemUtils.IS_OS_WINDOWS) {
            RegKeyManager rkm = new RegKeyManager();
            try {
                rkm.query("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "SOMANETconnect");
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }

    public static void setColors(JComponent component) {
        if (SystemUtils.IS_OS_LINUX) {
            component.setBackground(new Color(87, 85, 79));
            component.setForeground(Color.WHITE);
            component.setOpaque(true);
            if (component instanceof JPanel) {
                for (Component innerComponent : component.getComponents()) {
                    if (innerComponent instanceof JLabel) {
                        innerComponent.setForeground(Color.WHITE);
                    }
                }
            }
        }
    }

    public static BufferedImage getImageFromResource(String name) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(SomanetConnect.class.getResourceAsStream("/" + name));
        } catch (IOException e) {
            logger.error("Failed to read the icon image");
        }
        return image;
    }

    public static Point getPopupMenuPosition(JPopupMenu popupMenu, Point point) {
        Rectangle bounds = getSafeScreenBounds(point);

        int x = point.x;
        int y = point.y;
        if (y < bounds.y) {
            y = bounds.y;
        } else if (y > bounds.y + bounds.height) {
            y = bounds.y + bounds.height;
        }
        if (x < bounds.x) {
            x = bounds.x;
        } else if (x > bounds.x + bounds.width) {
            x = bounds.x + bounds.width;
        }

        if (x + popupMenu.getPreferredSize().width > bounds.x + bounds.width) {
            x = (bounds.x + bounds.width) - popupMenu.getPreferredSize().width;
        }
        if (y + popupMenu.getPreferredSize().height > bounds.y + bounds.height) {
            y = (bounds.y + bounds.height) - popupMenu.getPreferredSize().height;
        }
        return new Point(x, y);
    }


    private static Rectangle getSafeScreenBounds(Point pos) {
        Rectangle bounds = getScreenBoundsAt(pos);
        Insets insets = getScreenInsetsAt(pos);

        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= (insets.left + insets.right);
        bounds.height -= (insets.top + insets.bottom);

        return bounds;
    }

    private static Insets getScreenInsetsAt(Point pos) {
        GraphicsDevice gd = getGraphicsDeviceAt(pos);
        Insets insets = null;
        if (gd != null) {
            insets = Toolkit.getDefaultToolkit().getScreenInsets(gd.getDefaultConfiguration());
        }
        return insets;
    }

    private static Rectangle getScreenBoundsAt(Point pos) {
        GraphicsDevice gd = getGraphicsDeviceAt(pos);
        Rectangle bounds = null;
        if (gd != null) {
            bounds = gd.getDefaultConfiguration().getBounds();
        }
        return bounds;
    }

    private static GraphicsDevice getGraphicsDeviceAt(Point pos) {
        GraphicsDevice device;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice lstGDs[] = ge.getScreenDevices();

        ArrayList<GraphicsDevice> lstDevices = new ArrayList<>(lstGDs.length);

        for (GraphicsDevice gd : lstGDs) {

            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            Rectangle screenBounds = gc.getBounds();

            if (screenBounds.contains(pos)) {
                lstDevices.add(gd);
            }
        }

        if (lstDevices.size() > 0) {
            device = lstDevices.get(0);
        } else {
            device = ge.getDefaultScreenDevice();
        }

        return device;
    }
}
