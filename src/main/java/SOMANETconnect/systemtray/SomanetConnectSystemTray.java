package SOMANETconnect.systemtray;

import SOMANETconnect.device.DeviceManager;
import SOMANETconnect.miscellaneous.Constants;
import SOMANETconnect.miscellaneous.Util;
import SOMANETconnect.systemtray.ui.JPopupMenuEx;
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
import java.util.Observable;
import java.util.Observer;

// Singleton
public class SomanetConnectSystemTray implements Observer {
    private static SomanetConnectSystemTray somanetConnectSystemTray = new SomanetConnectSystemTray();

    private static final Logger logger = Logger.getLogger(SomanetConnectSystemTray.class.getName());

    private TrayIcon trayIcon;
    private JPopupMenuEx popupMenu;
    private ArrayList<JComponent> currentDeviceMenuItems = new ArrayList<>();
    private java.util.List<Map<String, String>> devices;
    private JLabel oblacConnectionStatus;
    private Point lastMouseClickPosition;

    /**
     * A worker class that finds all available devices and puts them into the popup menu
     */
    private class Worker implements Runnable {
        @Override
        public void run() {
            showLoading();

            clearDeviceList();

            if (devices.isEmpty()) {
                JPanel devicePanel = new JPanel();
                JLabel noAvailableDevicesMenuItem = new JLabel("No available devices");
                devicePanel.add(noAvailableDevicesMenuItem);
                popupMenu.insert(devicePanel, 2);

                currentDeviceMenuItems.add(devicePanel);
            } else {
                for (Map<String, String> device : devices) {
                    addDeviceToList(device);
                }
            }

            if (popupMenu.isVisible()) {
                // Resize the ancestor window of the popup menu to the required size. The popupMenu.pack() is not used
                // because it causes the popup menu to flicker.
                Window window = SwingUtilities.getWindowAncestor(popupMenu);
                if (window != null) {
                    window.pack();
                    window.validate();
                }
                // Fix the popup menu location according to its new size
                popupMenu.setLocation(lastMouseClickPosition);
            }
            popupMenu.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private SomanetConnectSystemTray() {
        assertSystemTrayAvailable();

        // Set the default styling of the application to match that of the system's
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }

        initPopupMenu();
        initTrayIcon();

        // Set the OBLAC disconnected label as default
        setDefaultOblacStatusLabel();
    }

    public static SomanetConnectSystemTray getInstance() {
        return somanetConnectSystemTray;
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
                JOptionPane jOptionPane = new JOptionPane();
                JDialog dialog = jOptionPane.createDialog("About");
                jOptionPane.setMessage("Synapticon SOMANETconnect v1.0");
                jOptionPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
                dialog.setSize(400, 150);
                dialog.setAlwaysOnTop(true);
                dialog.setModal(false);
                dialog.setVisible(true);
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
        oblacPanel.add(Box.createRigidArea(new Dimension(4, 25)));
        JLabel oblacLabel = new JLabel("OBLAC IDE", oblacIcon, SwingConstants.LEFT);
        oblacConnectionStatus = new JLabel("DISCONNECTED");
        oblacPanel.add(oblacLabel);
        oblacPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        oblacPanel.add(Box.createHorizontalGlue());
        oblacPanel.add(oblacConnectionStatus);
        oblacPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        return oblacPanel;
    }

    private void addDeviceToList(Map<String, String> device) {
        String tileString = device.get(Constants.DEVICES);
        JPanel devicePanel = new JPanel();
        devicePanel.setLayout(new BoxLayout(devicePanel, BoxLayout.X_AXIS));
        // Add a left "margin" and set the default height of the panel
        if (SystemUtils.IS_OS_LINUX) {
            devicePanel.add(Box.createRigidArea(new Dimension(21, 25)));
        } else {
            devicePanel.add(Box.createRigidArea(new Dimension(28, 25)));
        }
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

    private void setDefaultOblacStatusLabel() {
        oblacConnectionStatus.setText("DISCONNECTED");
        Font defaultFont = oblacConnectionStatus.getFont();
        int fontStyle = Font.BOLD;
        // Do not use italic font in Linux because of bad font anti-aliasing
        if (!SystemUtils.IS_OS_LINUX) {
            fontStyle |= Font.ITALIC;
        }
        oblacConnectionStatus.setFont(defaultFont.deriveFont(fontStyle));
        oblacConnectionStatus.setForeground(new Color(0xdc0000));
    }

    public void oblacConnected(boolean connected) {
        if (connected) {
            oblacConnectionStatus.setText("CONNECTED");
            oblacConnectionStatus.setForeground(new Color(0x9ade00));
        } else {
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
        if (SystemUtils.IS_OS_LINUX) {
            loadingPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        } else {
            loadingPanel.add(Box.createRigidArea(new Dimension(17, 0)));
        }
        JLabel loadingLabel = new JLabel("Loading...");
        loadingPanel.add(loadingLabel);
        return loadingPanel;
    }

    /**
     * Check if SystemTray is available several times, because it is possible that it has not yet been initialized after
     * system boot. If SystemTray is not available after multiple checks, assume that it is not supported, show an error
     * message and exit the application.
     */
    private static void assertSystemTrayAvailable() {
        boolean systemTraySupported = SystemTray.isSupported();
        for (int i = 0; !systemTraySupported && i < 10; i++) {
            try {
                Thread.sleep(1000);
                systemTraySupported = SystemTray.isSupported();
            } catch (InterruptedException e) {
                // NO-OP
            }
        }

        if (!systemTraySupported) {
            JOptionPane.showMessageDialog(null, "SystemTray is not supported. SOMANETconnect will now exit.",
                    "SOMANETconnect", JOptionPane.ERROR_MESSAGE);
            logger.fatal("SystemTray is not supported");
            System.exit(1);
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof DeviceManager) {
            devices = ((DeviceManager) o).getDevices();
            SwingUtilities.invokeLater(new Worker());
        }
    }
}
