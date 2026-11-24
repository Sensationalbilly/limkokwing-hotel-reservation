package com.hotel.ui.components;

import com.hotel.ui.Theme;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Pill-shaped label: coloured dot plus text on a light tint of the same colour. */
public class StatusBadge extends JLabel {

    private Color color;

    public StatusBadge(String text, Color color) {
        super(text);
        setFont(Theme.font(Font.BOLD, 11.5f));
        setBorder(new EmptyBorder(3, 20, 3, 10));
        setOpaque(false);
        setColor(color);
    }

    public final void setColor(Color color) {
        this.color = color;
        setForeground(color);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int h = getHeight();
        g2.setColor(Theme.tint(color));
        g2.fillRoundRect(0, 0, getWidth() - 1, h - 1, h, h);
        if (Theme.isHighContrast()) {
            g2.setColor(color);
            g2.drawRoundRect(0, 0, getWidth() - 1, h - 1, h, h);
        }
        int dot = Math.max(6, h / 3);
        g2.setColor(color);
        g2.fillOval(9, (h - dot) / 2, dot, dot);
        g2.dispose();
        super.paintComponent(g);
    }
}
