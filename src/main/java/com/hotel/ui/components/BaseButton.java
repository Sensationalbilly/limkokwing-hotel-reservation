package com.hotel.ui.components;

import com.hotel.ui.Theme;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Rounded, flat button drawn by hand. Subclasses only override the colour methods
 * ({@link #getBaseColor()}, {@link #getTextColor()}, {@link #hoverColor(Color)}) to get a
 * different look, which is how Primary, Success, Danger and Nav buttons are made.
 * Shows a visible focus ring for keyboard users.
 */
public class BaseButton extends JButton {

    private static final int ARC = 10;

    private boolean hovered;
    private boolean pressed;

    public BaseButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFont(Theme.font(Font.BOLD, 13f));
        setForeground(getTextColor());
        setBorder(new EmptyBorder(9, 18, 9, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getAccessibleContext().setAccessibleName(text);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                pressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
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
    }

    protected Color getBaseColor() {
        return Theme.secondary();
    }

    protected Color getTextColor() {
        return Theme.text();
    }

    protected Color hoverColor(Color base) {
        return Theme.shade(base, -0.08f);
    }

    protected Color pressedColor(Color base) {
        return Theme.shade(base, -0.18f);
    }

    /** Re-reads the text colour after a subclass changes state. */
    protected void refreshColors() {
        setForeground(isEnabled() ? getTextColor() : Theme.mutedText());
        repaint();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        refreshColors();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color base = getBaseColor();
        Color fill;
        if (!isEnabled()) {
            fill = Theme.mix(base, Theme.background(), 0.6f);
        } else if (pressed) {
            fill = pressedColor(base);
        } else if (hovered) {
            fill = hoverColor(base);
        } else {
            fill = base;
        }
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);

        if (isFocusOwner()) {
            g2.setColor(Theme.focusRing());
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, ARC, ARC);
        }
        g2.dispose();

        super.paintComponent(g);  // draws the text and mnemonic underline
    }
}
