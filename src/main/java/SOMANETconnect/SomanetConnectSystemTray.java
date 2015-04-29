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

    private ArrayList<JMenuItem> currentDeviceMenuItems = new ArrayList<>();

    public SomanetConnectSystemTray() throws IOException {
        //Check the SystemTray is supported
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        BufferedImage bufferedImage;
        // The system tray icon cannot be transparent in Linux
        if (SystemUtils.IS_OS_LINUX) {
            bufferedImage = ImageIO.read(SomanetConnect.class.getResourceAsStream("/synapticon_tray_icon.png"));
        } else {
            bufferedImage =
                    ImageIO.read(SomanetConnect.class.getResourceAsStream("/synapticon_tray_icon_transparent.png"));
        }
        final SystemTray systemTray = SystemTray.getSystemTray();
        Dimension trayIconSize = systemTray.getTrayIconSize();
        Image image = bufferedImage.getScaledInstance(trayIconSize.width - 2, trayIconSize.height, Image.SCALE_SMOOTH);
        final TrayIcon trayIcon = new TrayIcon(image);

        final JPopupMenuEx jPopupMenu = new JPopupMenuEx();

        trayIcon.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3) {
                    jPopupMenu.setLocation(e.getX(), e.getY());
                    jPopupMenu.setInvoker(jPopupMenu);
                    jPopupMenu.setVisible(true);

                    // Run the listing process in a separate thread, so that the context menu doesn't lag
                    (new Thread(new Runnable() {
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
                                jPopupMenu.remove(deviceMenuItem);
                            }
                            currentDeviceMenuItems.clear();

                            if (devices.isEmpty()) {
                                JMenuItem noAvailableDevicesMenuItem = new JMenuItem("No available devices");
                                jPopupMenu.insert(noAvailableDevicesMenuItem, currentDeviceMenuItems.size());
                                currentDeviceMenuItems.add(noAvailableDevicesMenuItem);
                            } else {
                                for (Object deviceObject : devices) {
                                    Map device = (Map) deviceObject;
                                    JMenuItem deviceMenuItem = new JMenuItem(device.get(Constants.ID) + "  " + device.get(Constants.NAME) + "  "
                                            + device.get(Constants.ADAPTER_ID) + "  " + device.get(Constants.DEVICES) + "\n");
                                    jPopupMenu.insert(deviceMenuItem, currentDeviceMenuItems.size());
                                    currentDeviceMenuItems.add(deviceMenuItem);
                                }
                            }
                            jPopupMenu.updateUI();
                        }
                    })).start();
                }
            }
        });

        jPopupMenu.addSeparator();
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
        jPopupMenu.add(startOnBootItem);

        JMenuItem aboutItem = new JMenuItem("About SOMANETconnect");
        aboutItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "Synapticon SOMANETconnect v1.0", "About", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        jPopupMenu.add(aboutItem);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SystemTray.getSystemTray().remove(trayIcon);
                System.exit(0);
            }
        });
        jPopupMenu.add(exitItem);

        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
        }
    }
}
