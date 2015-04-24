package SOMANETconnect;

import SOMANETconnect.command.ListCommand;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

public class SomanetConnectSystemTray {
    private static final Logger logger = Logger.getLogger(SomanetConnectSystemTray.class.getName());

    public SomanetConnectSystemTray() throws IOException {
        //Check the SystemTray is supported
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }

        final PopupMenu popup = new PopupMenu();

        //Add components to pop-up menu
        MenuItem devicesItem = new MenuItem("Devices");
        devicesItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ListCommand listCommand;
                try {
                    listCommand = new ListCommand();
                } catch (IOException e2) {
                    logger.error(e2.getMessage());
                    return;
                }
                String devicesList = "";
                java.util.List devices = listCommand.getDeviceList();
                if (devices.isEmpty()) {
                    devicesList = "No available devices";
                } else {
                    for (Object deviceObject : devices) {
                        Map device = (Map) deviceObject;
                        devicesList += device.get(Constants.ID) + "  " + device.get(Constants.NAME) + "  "
                                + device.get(Constants.ADAPTER_ID) + "  " + device.get(Constants.DEVICES) + "\n";
                    }
                }
                JOptionPane.showMessageDialog(null, devicesList, "Device List", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        popup.add(devicesItem);

        CheckboxMenuItem startOnBootItem = new CheckboxMenuItem("Start on boot");
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
        popup.add(startOnBootItem);

        popup.addSeparator();
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "Synapticon SOMANETconnect v1.0", "About", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        popup.add(aboutItem);

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        popup.add(exitItem);

        BufferedImage bufferedImage = ImageIO.read(SomanetConnect.class.getResourceAsStream("/synapticon_tray_icon.png"));
        final SystemTray tray = SystemTray.getSystemTray();
        Dimension trayIconSize = tray.getTrayIconSize();
        Image image = bufferedImage.getScaledInstance(trayIconSize.width - 2, trayIconSize.height, Image.SCALE_SMOOTH);
        TrayIcon trayIcon = new TrayIcon(image);
        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
        }
    }
}
