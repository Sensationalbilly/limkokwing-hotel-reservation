package com.hotel.ui.components;

import com.hotel.ui.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Two-column form (label | field) built on GridBagLayout. Every label is linked to its
 * field with setLabelFor, so screen readers announce the label and Alt+mnemonic jumps
 * straight to the field.
 */
public class FormPanel extends JPanel {

    private int row;

    public FormPanel() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    public JLabel addField(String text, int mnemonic, JComponent field) {
        JLabel label = new JLabel(text);
        label.setForeground(Theme.text());
        label.setLabelFor(field);
        if (mnemonic != 0) {
            label.setDisplayedMnemonic(mnemonic);
        }

        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0;
        lc.gridy = row;
        lc.anchor = GridBagConstraints.LINE_START;
        lc.insets = new Insets(5, 0, 5, 12);
        add(label, lc);

        GridBagConstraints fc = new GridBagConstraints();
        fc.gridx = 1;
        fc.gridy = row;
        fc.weightx = 1;
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.insets = new Insets(5, 0, 5, 0);
        add(field, fc);

        row++;
        return label;
    }

    public void addFullWidth(JComponent component) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 0, 5, 0);
        add(component, c);
    }

    /** Pushes the rows to the top when the panel is taller than needed. */
    public void addFiller() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.weighty = 1;
        add(Box.createGlue(), c);
    }
}
