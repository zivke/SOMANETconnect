package SOMANETconnect.ui;

import org.apache.commons.lang3.SystemUtils;
import SOMANETconnect.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class JPopupMenuEx extends JPopupMenu implements MouseListener {
    // FIXME: Using a timer to hide the popup menu is not the correct solution
    Timer hideTimer = new Timer(500, new ActionListener() {
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
        return super.add(menuItem);
    }

    @Override
    public JMenuItem add(String text) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addMouseListener(this);
        Util.setColors(menuItem);
        return super.add(menuItem);
    }

    @Override
    public void insert(Component component, int index) {
        component.addMouseListener(this);
        super.insert(component, index);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        hideTimer.stop();
        ((JMenuItem) e.getSource()).setArmed(true);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        hideTimer.setRepeats(false);
        hideTimer.restart();
        ((JMenuItem) e.getSource()).setArmed(false);
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

    private static void setColors(JComponent component) {
        if (SystemUtils.IS_OS_LINUX) {
            component.setBackground(new Color(87, 85, 79));
            component.setForeground(Color.WHITE);
            component.setOpaque(true);
        }
    }
}