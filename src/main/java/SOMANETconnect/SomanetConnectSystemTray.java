package SOMANETconnect;

import SOMANETconnect.command.ListCommand;
import SOMANETconnect.ui.JPopupMenuEx;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class SomanetConnectSystemTray {
    private static final Logger logger = Logger.getLogger(SomanetConnectSystemTray.class.getName());

    private TrayIcon trayIcon;
    private JPopupMenuEx popupMenu;
    private ArrayList<JComponent> currentDeviceMenuItems = new ArrayList<>();
    private JLabel oblacConnectionStatus;

    /**
     * A worker class that finds all available devices and puts them into the popup menu
     */
    private class Worker implements Runnable {
        @Override
        public void run() {
            showLoading();

            ListCommand listCommand;
            try {
                listCommand = new ListCommand();
            } catch (IOException e2) {
                logger.error(e2.getMessage());
                return;
            }

            java.util.List devices = listCommand.getDeviceList();
            clearDeviceList();

            if (devices.isEmpty()) {
                JMenuItem noAvailableDevicesMenuItem = new JMenuItem("No available devices");
                popupMenu.insert(noAvailableDevicesMenuItem, 2);

                currentDeviceMenuItems.add(noAvailableDevicesMenuItem);
            } else {
                for (Object deviceObject : devices) {
                    Map device = (Map) deviceObject;
                    addDeviceToList(device);
                }
            }
            // Resize the ancestor window of the popup menu to the required size. The popupMenu.pack() is not used
            // because it causes the popup menu to flicker.
            Window window = SwingUtilities.getWindowAncestor(popupMenu);
            if (window != null) {
                window.pack();
                window.validate();
            }
            popupMenu.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    public SomanetConnectSystemTray() {
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

        // Set the OBLAC disconnected label as default
        oblacConnectionStatus.setText("DISCONNECTED");
        oblacConnectionStatus.setForeground(Color.RED);
    }

    private void initPopupMenu() {
        popupMenu = new JPopupMenuEx();

        popupMenu.add(setupOblacStatusPanel());
        popupMenu.addSeparator();
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

        popupMenu.setBorder(BorderFactory.createEmptyBorder());
    }

    private void initTrayIcon() {
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

    private JPanel setupOblacStatusPanel() {
        Icon oblacIcon;
        if (SystemUtils.IS_OS_LINUX) {
            oblacIcon = new ImageIcon(getImageFromResource("oblac_light.png"));
        } else {
            oblacIcon = new ImageIcon(getImageFromResource("oblac_dark.png"));
        }
        JPanel oblacPanel = new JPanel();
        oblacPanel.setLayout(new BoxLayout(oblacPanel, BoxLayout.X_AXIS));
        // Add a left "margin" and set the default height of the panel
        oblacPanel.add(Box.createRigidArea(new Dimension(10, 25)));
        JLabel oblacLabel = new JLabel("OBLAC IDE", oblacIcon, SwingConstants.LEFT);
        oblacConnectionStatus = new JLabel("DISCONNECTED");
        oblacPanel.add(oblacLabel);
        oblacPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        oblacPanel.add(Box.createHorizontalGlue());
        oblacPanel.add(oblacConnectionStatus);
        oblacPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        return oblacPanel;
    }

    private void addDeviceToList(Map device) {
        String tileString = (String) device.get(Constants.DEVICES);
        JPanel devicePanel = new JPanel();
        devicePanel.setLayout(new BoxLayout(devicePanel, BoxLayout.X_AXIS));
        // Add a left "margin" and set the default height of the panel
        devicePanel.add(Box.createRigidArea(new Dimension(34, 25)));
        JLabel deviceTilesLabel;
        if (tileString.equalsIgnoreCase("in use")) {
            deviceTilesLabel = new JLabel("Device in use");
        } else {
            tileString = tileString.substring(5, tileString.length() - 1);
            int tileNumber = Integer.valueOf(tileString) + 1;
            deviceTilesLabel = new JLabel(tileNumber + " tile device");
        }
        devicePanel.add(deviceTilesLabel);
        devicePanel.add(Box.createRigidArea(new Dimension(30, 0)));
        devicePanel.add(Box.createHorizontalGlue());
        JLabel adapterIdLabel = new JLabel("(Adapter: " + device.get(Constants.ADAPTER_ID) + ")");
        devicePanel.add(adapterIdLabel);
        devicePanel.add(Box.createRigidArea(new Dimension(10, 0)));
        popupMenu.insert(devicePanel, currentDeviceMenuItems.size() + 2);

        currentDeviceMenuItems.add(devicePanel);
    }

    /**
     * Remove all device that are currently in the device list inside the popup menu
     */
    private void clearDeviceList() {
        for (JComponent deviceMenuItem : currentDeviceMenuItems) {
            popupMenu.remove(deviceMenuItem);
        }
        currentDeviceMenuItems.clear();
    }

    private void showLoading() {
        clearDeviceList();
        popupMenu.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        JMenuItem loadingMenuItem = new JMenuItem("Loading...");
        popupMenu.insert(loadingMenuItem, 2);
        popupMenu.pack();
        currentDeviceMenuItems.add(loadingMenuItem);
    }

    public void showError(String message) {
        trayIcon.displayMessage("Error", message, MessageType.ERROR);
    }

    public void showInfo(String message) {
        trayIcon.displayMessage("Info", message, MessageType.INFO);
    }

    public void oblacConnected(boolean connected) {
        if (connected) {
            showInfo("OBLAC connected successfully");
            oblacConnectionStatus.setText("CONNECTED");
            oblacConnectionStatus.setForeground(Color.GREEN);
        } else {
            showInfo("OBLAC disconnected");
            oblacConnectionStatus.setText("DISCONNECTED");
            oblacConnectionStatus.setForeground(Color.RED);
        }
    }

    private static BufferedImage getImageFromResource(String name) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(SomanetConnect.class.getResourceAsStream("/" + name));
        } catch (IOException e) {
            logger.error("Failed to read the icon image");
        }
        return image;
    }

    private static Image getIconImage() {
        BufferedImage bufferedImage;
        // The system tray icon cannot be transparent in Linux
        if (SystemUtils.IS_OS_LINUX) {
            bufferedImage = getImageFromResource("synapticon_tray_icon.png");
        } else {
            bufferedImage = getImageFromResource("synapticon_tray_icon_transparent.png");
        }
        Dimension trayIconSize = SystemTray.getSystemTray().getTrayIconSize();
        return bufferedImage.getScaledInstance(trayIconSize.width - 2, trayIconSize.height, Image.SCALE_SMOOTH);
    }
}
