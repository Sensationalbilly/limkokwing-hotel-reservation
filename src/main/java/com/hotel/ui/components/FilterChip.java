package com.hotel.ui.components;

import com.hotel.ui.Theme;

import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Rounded toggle used as a filter (put several in a ButtonGroup). Keyboard: Tab + Space. */
public class FilterChip extends JToggleButton {

    private boolean hovered;

    public FilterChip(String text) {
        super(text);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFont(Theme.font(Font.BOLD, 12.5f));
        setBorder(new EmptyBorder(6, 14, 6, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
        addItemListener(e -> refreshColors());
        refreshColors();
    }

    private void refreshColors() {
        setForeground(isSelected() ? Theme.onAccent() : Theme.text());
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth() - 1;
        int h = getHeight() - 1;
        if (isSelected()) {
            g2.setColor(Theme.primary());
            g2.fillRoundRect(0, 0, w, h, h, h);
        } else {
            g2.setColor(hovered ? Theme.secondary() : Theme.surface());
            g2.fillRoundRect(0, 0, w, h, h, h);
            g2.setColor(Theme.border());
            g2.drawRoundRect(0, 0, w, h, h, h);
        }
        if (isFocusOwner()) {
            g2.setColor(Theme.focusRing());
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, h - 2, h - 2);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
