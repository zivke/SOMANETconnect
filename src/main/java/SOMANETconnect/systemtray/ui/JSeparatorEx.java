package SOMANETconnect.systemtray.ui;

import javax.swing.*;
import java.awt.*;

public class JSeparatorEx extends JSeparator {
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = 2;

        return d;
    }
}