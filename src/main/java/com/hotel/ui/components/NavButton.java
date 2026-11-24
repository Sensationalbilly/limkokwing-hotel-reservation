package com.hotel.ui.components;

import com.hotel.ui.Theme;

import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Color;

/** Sidebar navigation button; highlighted when its page is showing. */
public class NavButton extends BaseButton {

    private boolean active;

    public NavButton(String text) {
        super(text);
        setHorizontalAlignment(SwingConstants.LEFT);
        setBorder(new EmptyBorder(11, 18, 11, 18));
    }

    public void setActive(boolean active) {
        this.active = active;
        getAccessibleContext().setAccessibleDescription(active ? "Current page" : null);
        refreshColors();
    }

    @Override
    protected Color getBaseColor() {
        return active ? Theme.primary() : Theme.sidebar();
    }

    @Override
    protected Color getTextColor() {
        return active ? Theme.onAccent() : Theme.onSidebar();
    }

    private int badge;

    /** Small count bubble on the right, e.g. waiting password requests. 0 hides it. */
    public void setBadge(int count) {
        badge = count;
        getAccessibleContext().setAccessibleDescription(count > 0 ? count + " waiting" : null);
        repaint();
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        if (badge <= 0) return;
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(Theme.font(java.awt.Font.BOLD, 11.5f));
        java.awt.FontMetrics fm = g2.getFontMetrics();
        String text = badge > 99 ? "99+" : String.valueOf(badge);
        int h = fm.getHeight() + 2;
        int w = Math.max(h, fm.stringWidth(text) + 12);
        int x = getWidth() - w - 12;
        int y = (getHeight() - h) / 2;
        g2.setColor(Theme.danger());
        g2.fillRoundRect(x, y, w, h, h, h);
        g2.setColor(Color.WHITE);
        g2.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }

    /** Sidebar is dark, so hover lightens instead of darkening. */
    @Override
    protected Color hoverColor(Color base) {
        return active ? base : Theme.shade(base, 0.12f);
    }
}
