package SOMANETconnect;

import SOMANETconnect.command.ListCommand;
import SOMANETconnect.ui.JPopupMenuEx;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class SomanetConnectSystemTray {
    private static final Logger logger = Logger.getLogger(SomanetConnectSystemTray.class.getName());

    private TrayIcon trayIcon;
    private JPopupMenuEx popupMenu;
    private ArrayList<JMenuItem> currentDeviceMenuItems = new ArrayList<>();

    /**
     * A worker class that finds all available devices and puts them into the popup menu
     */
    private class Worker implements Runnable {
        @Override
        public void run() {
            ListCommand listCommand;
            try {
                listCommand = new ListCommand();
            } catch (IOException e2) {
                logger.error(e2.getMessage());
                return;
            }
            java.util.List devices = listCommand.getDeviceList();

            // Remove current device labels
            for (JMenuItem deviceMenuItem : currentDeviceMenuItems) {
                popupMenu.remove(deviceMenuItem);
            }
            currentDeviceMenuItems.clear();

            if (devices.isEmpty()) {
                JMenuItem noAvailableDevicesMenuItem = new JMenuItem("No available devices");
                popupMenu.insert(noAvailableDevicesMenuItem, currentDeviceMenuItems.size());

                currentDeviceMenuItems.add(noAvailableDevicesMenuItem);
            } else {
                for (Object deviceObject : devices) {
                    Map device = (Map) deviceObject;
                    String tileString = (String) device.get(Constants.DEVICES);
                    JMenuItem deviceMenuItem;
                    if (tileString.equalsIgnoreCase("in use")) {
                        deviceMenuItem = new JMenuItem(
                                "Device in use    (Adapter: " + device.get(Constants.ADAPTER_ID) + ")");
                    } else {
                        tileString = tileString.substring(5, tileString.length() - 1);
                        int tileNumber = Integer.valueOf(tileString) + 1;
                        deviceMenuItem = new JMenuItem(
                                tileNumber + " tile device    (Adapter: " + device.get(Constants.ADAPTER_ID) + ")");
                    }
                    popupMenu.insert(deviceMenuItem, currentDeviceMenuItems.size());

                    currentDeviceMenuItems.add(deviceMenuItem);
                }
            }
            // Resize the ancestor window of the popup menu to the required size
            Window window = SwingUtilities.getWindowAncestor(popupMenu);
            if (window != null) {
                window.pack();
                window.validate();
            }
        }
    }

    public SomanetConnectSystemTray() throws IOException {
        //Check the SystemTray is supported
        if (!SystemTray.isSupported()) {
            logger.error("SystemTray is not supported");
            return;
        }

        // Set the default styling of the application to match that of the system's
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }

        initPopupMenu();
        initTrayIcon();
    }

    private static Image getIconImage() throws IOException {
        BufferedImage bufferedImage;
        // The system tray icon cannot be transparent in Linux
        if (SystemUtils.IS_OS_LINUX) {
            bufferedImage = ImageIO.read(SomanetConnect.class.getResourceAsStream("/synapticon_tray_icon.png"));
        } else {
            bufferedImage =
                    ImageIO.read(SomanetConnect.class.getResourceAsStream("/synapticon_tray_icon_transparent.png"));
        }
        Dimension trayIconSize = SystemTray.getSystemTray().getTrayIconSize();
        return bufferedImage.getScaledInstance(trayIconSize.width - 2, trayIconSize.height, Image.SCALE_SMOOTH);
    }

    private void initPopupMenu() {
        popupMenu = new JPopupMenuEx();

        popupMenu.addSeparator();
        JCheckBoxMenuItem startOnBootItem = new JCheckBoxMenuItem("Start on boot");
        startOnBootItem.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                try {
                    Util.startOnBoot(event.getStateChange() == ItemEvent.SELECTED);
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        });
        startOnBootItem.setState(Util.isStartOnBootEnabled());
        popupMenu.add(startOnBootItem);

        JMenuItem aboutItem = new JMenuItem("About SOMANETconnect");
        aboutItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "Synapticon SOMANETconnect v1.0", "About", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        popupMenu.add(aboutItem);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SystemTray.getSystemTray().remove(trayIcon);
                System.exit(0);
            }
        });
        popupMenu.add(exitItem);
    }

    private void initTrayIcon() throws IOException {
        trayIcon = new TrayIcon(getIconImage());

        trayIcon.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3) {
                    popupMenu.setLocation(e.getX(), e.getY());
                    popupMenu.setInvoker(popupMenu);
                    popupMenu.setVisible(true);

                    // Run the listing process in a separate thread, so that the context menu doesn't lag
                    (new Thread(new Worker())).start();
                }
            }
        });

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            logger.error("TrayIcon could not be added.");
        }
    }
}
