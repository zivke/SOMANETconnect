package SOMANETconnect.ui;

import SOMANETconnect.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

public class JPopupMenuEx extends JPopupMenu implements MouseListener {
    // FIXME: Using a timer to hide the popup menu is not the correct solution
    Timer hideTimer = new Timer(500, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JPopupMenuEx.this.setVisible(false);
        }
    });
    Timer forceHideTimer = new Timer(2000, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JPopupMenuEx.this.setVisible(false);
        }
    });

    @Override
    public void addSeparator() {
        JSeparatorEx separator = new JSeparatorEx();
        Util.setColors(separator);
        add(separator);
    }

    @Override
    public JMenuItem add(JMenuItem menuItem) {
        menuItem.addMouseListener(this);
        Util.setColors(menuItem);
        menuItem.setMargin(new Insets(0, 10, 0, 0));
        return super.add(menuItem);
    }

    @Override
    public JMenuItem add(String text) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addMouseListener(this);
        Util.setColors(menuItem);
        menuItem.setMargin(new Insets(0, 10, 0, 0));
        return super.add(menuItem);
    }

    public JPanel add(JPanel panel) {
        panel.addMouseListener(this);
        Util.setColors(panel);
        return (JPanel) super.add(panel);
    }

    public void insert(JMenuItem menuItem, int index) {
        menuItem.addMouseListener(this);
        Util.setColors(menuItem);
        menuItem.setMargin(new Insets(0, 10, 0, 0));
        super.insert(menuItem, index);
    }

    public void insert(JPanel panel, int index) {
        panel.addMouseListener(this);
        Util.setColors(panel);
        super.insert(panel, index);
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            forceHideTimer.setRepeats(false);
            forceHideTimer.restart();
        }
        super.setVisible(b);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        hideTimer.stop();
        forceHideTimer.stop();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        hideTimer.setRepeats(false);
        hideTimer.restart();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void setLocation(Point point) {
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

        if (x + getPreferredSize().width > bounds.x + bounds.width) {
            x = (bounds.x + bounds.width) - getPreferredSize().width;
        }
        if (y + getPreferredSize().height > bounds.y + bounds.height) {
            y = (bounds.y + bounds.height) - getPreferredSize().height;
        }
        super.setLocation(x, y);
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
