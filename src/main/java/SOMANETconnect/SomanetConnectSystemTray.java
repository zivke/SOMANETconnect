package SOMANETconnect;

import SOMANETconnect.command.ListCommand;
import SOMANETconnect.ui.JPopupMenuEx;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;

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
    private Point lastMouseClickPosition;

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
                JPanel devicePanel = new JPanel();
                JLabel noAvailableDevicesMenuItem = new JLabel("No available devices");
                devicePanel.add(noAvailableDevicesMenuItem);
                popupMenu.insert(devicePanel, 2);

                currentDeviceMenuItems.add(devicePanel);
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
            // Fix the popup menu location according to its new size
            popupMenu.setLocation(lastMouseClickPosition);
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
        Font defaultFont = oblacConnectionStatus.getFont();
        oblacConnectionStatus.setFont(defaultFont.deriveFont(Font.ITALIC | Font.BOLD));
        oblacConnectionStatus.setForeground(new Color(0xdc0000));
    }

    private void initPopupMenu() {
        popupMenu = new JPopupMenuEx();

        popupMenu.add(setupOblacStatusPanel());
        popupMenu.addSeparator();
        JPanel loadingPanel = createLoadingPanel();
        popupMenu.add(loadingPanel);
        popupMenu.pack();
        currentDeviceMenuItems.add(loadingPanel);
        popupMenu.addSeparator();

        JCheckBoxMenuItem startOnBootItem = new JCheckBoxMenuItem("Start on boot");
        startOnBootItem.setState(Util.isStartOnBootEnabled());
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
        trayIcon = new TrayIcon(getTrayIconImage());

        trayIcon.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3) {
                    lastMouseClickPosition = e.getPoint();
                    popupMenu.setLocation(e.getPoint());
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
            oblacIcon = new ImageIcon(Util.getImageFromResource("oblac_light.png"));
        } else {
            oblacIcon = new ImageIcon(Util.getImageFromResource("oblac_dark.png"));
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
        } else if (tileString.equalsIgnoreCase("none")) {
            deviceTilesLabel = new JLabel("No device");
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

        // Change the color of the "(Adapter:...)" label font
        setSecondaryLabelLook(adapterIdLabel);

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
        JPanel loadingPanel = createLoadingPanel();
        popupMenu.insert(loadingPanel, 2);
        popupMenu.pack();
        currentDeviceMenuItems.add(loadingPanel);
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
            oblacConnectionStatus.setForeground(new Color(0x9ade00));
        } else {
            showInfo("OBLAC disconnected");
            oblacConnectionStatus.setText("DISCONNECTED");
            oblacConnectionStatus.setForeground(new Color(0xdc0000));
        }
    }

    private static Image getTrayIconImage() {
        BufferedImage bufferedImage;
        // The system tray icon cannot be transparent in Linux
        if (SystemUtils.IS_OS_LINUX) {
            bufferedImage = Util.getImageFromResource("synapticon_tray_icon.png");
        } else {
            bufferedImage = Util.getImageFromResource("synapticon_tray_icon_transparent.png");
        }
        Dimension trayIconSize = SystemTray.getSystemTray().getTrayIconSize();
        return bufferedImage.getScaledInstance(trayIconSize.width - 2, trayIconSize.height, Image.SCALE_SMOOTH);
    }

    private void setSecondaryLabelLook(JLabel label) {
        if (SystemUtils.IS_OS_LINUX) {
            label.setForeground(new Color(0xcccccc));
        } else {
            label.setForeground(new Color(0x777777));
        }
        label.setFont(label.getFont().deriveFont(Font.ITALIC));
    }

    private JPanel createLoadingPanel() {
        JPanel loadingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        loadingPanel.add(Box.createRigidArea(new Dimension(23, 0)));
        JLabel loadingLabel = new JLabel("Loading...");
        loadingPanel.add(loadingLabel);
        return loadingPanel;
    }
}
