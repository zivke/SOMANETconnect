package SOMANETconnect;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class SomanetConnectSystemTray {
    public SomanetConnectSystemTray() throws IOException {
        //Check the SystemTray is supported
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }

        final PopupMenu popup = new PopupMenu();

        //Add components to pop-up menu
        Menu devicesMenu = new Menu("Devices");
        popup.add(devicesMenu);
        MenuItem dummyDevice = new MenuItem("Dummy SOMANET device");
        devicesMenu.add(dummyDevice);

        CheckboxMenuItem startOnBootItem = new CheckboxMenuItem("Start on boot");
        startOnBootItem.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                int startOnBoot = e.getStateChange();
                if (startOnBoot == ItemEvent.SELECTED) {
                    JOptionPane.showMessageDialog(null, "Start on boot: Yes", "Dummy message", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Start on boot: No", "Dummy message", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
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
