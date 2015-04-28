package SOMANETconnect.ui;

import javax.swing.*;
import java.awt.*;

public class JSeparatorEx extends JSeparator {
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();

        if (d.height == 0)
            d.height = 4;

        return d;
    }
}