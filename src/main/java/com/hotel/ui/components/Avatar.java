package com.hotel.ui.components;

import com.hotel.ui.Theme;

import javax.swing.JComponent;
import java.awt.*;

/** Round badge with a person's initials, shown next to their name in the header. */
public class Avatar extends JComponent {

    private final String initials;

    public Avatar(String fullName) {
        StringBuilder sb = new StringBuilder();
        for (String part : fullName.trim().split("\\s+")) {
            if (!part.isEmpty() && sb.length() < 2) sb.append(Character.toUpperCase(part.charAt(0)));
        }
        initials = sb.length() == 0 ? "?" : sb.toString();
        int size = Theme.scaled(36);
        setPreferredSize(new Dimension(size, size));
        setFont(Theme.font(Font.BOLD, 13f));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int size = Math.min(getWidth(), getHeight());
        g2.setColor(Theme.primary());
        g2.fillOval(0, 0, size - 1, size - 1);
        g2.setColor(Theme.onAccent());
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(initials, (size - fm.stringWidth(initials)) / 2, (size - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
