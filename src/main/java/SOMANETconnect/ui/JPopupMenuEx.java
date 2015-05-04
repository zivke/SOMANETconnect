package SOMANETconnect.ui;

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
    public void mouseEntered(MouseEvent e) {
        hideTimer.stop();
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
}
