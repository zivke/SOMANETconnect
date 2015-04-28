package SOMANETconnect.ui;

import javax.swing.*;
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
        add(new JSeparatorEx());
    }

    @Override
    public JMenuItem add(JMenuItem menuItem) {
        menuItem.addMouseListener(this);
        return super.add(menuItem);
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
}