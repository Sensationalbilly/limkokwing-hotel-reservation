package com.hotel.ui;

import com.hotel.ui.components.BaseButton;
import com.hotel.ui.components.DangerButton;
import com.hotel.ui.components.ModalCard;
import com.hotel.ui.components.PrimaryButton;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Animated message / confirmation card used by {@link Dialogs}. Shows a coloured icon and
 * the message. Lines written as "Label:  value" (or "Keys   description") are laid out as
 * a neat two-column list, so receipts and details read like a form.
 */
public class MessageCard extends ModalCard {

    private static final Object CONFIRMED = new Object();
    private static final Pattern LABELLED = Pattern.compile("^([^:]{1,30}):\\s{2,}(.+)$");
    private static final Pattern COLUMNS = Pattern.compile("^(\\S.*?)\\s{3,}(\\S.*)$");

    public MessageCard(Window owner, Dialogs.Kind kind, String title, String message,
                       String confirmLabel, boolean destructive) {
        super(owner, title, "");

        JPanel iconHolder = new JPanel(new BorderLayout());
        iconHolder.setOpaque(false);
        iconHolder.add(new KindIcon(kind), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(18, 0));
        body.setOpaque(false);
        body.add(iconHolder, BorderLayout.WEST);
        body.add(content(message), BorderLayout.CENTER);
        setBody(body);
        getAccessibleContext().setAccessibleDescription(message);

        if (confirmLabel == null) {
            setDefaultAction(addAction(new PrimaryButton("OK"), null, false));
        } else {
            BaseButton cancel = addAction(new BaseButton("Cancel"), null, false);
            BaseButton confirm = addAction(destructive ? new DangerButton(confirmLabel) : new PrimaryButton(confirmLabel),
                    CONFIRMED, false);
            // For risky actions Enter means "Cancel", so nothing is lost by accident.
            setDefaultAction(destructive ? cancel : confirm);
        }
    }

    /** @return true if the confirm button was pressed */
    public boolean open() {
        return showCard() == CONFIRMED;
    }

    private static JComponent content(String message) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        int textWidth = Theme.scaled(380);
        int row = 0;
        boolean afterGap = false;

        for (String line : message.split("\n", -1)) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = row++;
            c.anchor = GridBagConstraints.LINE_START;
            c.insets = new Insets(row == 1 || afterGap ? 0 : 4, 0, 0, 0);
            afterGap = false;

            if (line.isBlank()) {
                c.gridx = 0;
                panel.add(Box.createVerticalStrut(Theme.scaled(10)), c);
                afterGap = true;
                continue;
            }

            Matcher m = LABELLED.matcher(line.trim());
            if (!m.matches()) m = COLUMNS.matcher(line.trim());
            if (m.matches()) {
                JLabel key = Ui.muted(m.group(1));
                key.setFont(Theme.font(Font.PLAIN, 13f));
                JLabel value = Ui.label(m.group(2));
                value.setFont(Theme.font(Font.BOLD, 13f));
                c.gridx = 0;
                c.insets = new Insets(c.insets.top, 0, 0, 24);
                panel.add(key, c);
                c.gridx = 1;
                c.weightx = 1;
                c.insets = new Insets(c.insets.top, 0, 0, 0);
                panel.add(value, c);
            } else {
                JLabel text = Ui.label("<html><div style='width:" + textWidth + "px'>" + escape(line) + "</div></html>");
                text.setFont(Theme.font(Font.PLAIN, 14f));
                c.gridx = 0;
                c.gridwidth = 2;
                c.weightx = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                panel.add(text, c);
            }
        }
        return panel;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Round tinted icon: i, check mark, !, x or ? depending on the message kind. */
    private static final class KindIcon extends JComponent {
        private final Color color;
        private final String glyph;

        KindIcon(Dialogs.Kind kind) {
            switch (kind) {
                case SUCCESS -> { color = Theme.success(); glyph = "✓"; }
                case WARNING -> { color = Theme.warning(); glyph = "!"; }
                case ERROR -> { color = Theme.danger(); glyph = "✕"; }
                case QUESTION -> { color = Theme.primary(); glyph = "?"; }
                default -> { color = Theme.primary(); glyph = "i"; }
            }
            int size = Theme.scaled(46);
            setPreferredSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight());
            g2.setColor(Theme.tint(color));
            g2.fillOval(0, 0, size - 1, size - 1);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(1, 1, size - 3, size - 3);
            // Tick and cross are drawn as lines, since some fonts lack those symbols.
            g2.setStroke(new BasicStroke(size * 0.07f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (glyph.equals("✓")) {
                java.awt.geom.Path2D tick = new java.awt.geom.Path2D.Float();
                tick.moveTo(size * 0.31, size * 0.52);
                tick.lineTo(size * 0.44, size * 0.65);
                tick.lineTo(size * 0.69, size * 0.38);
                g2.draw(tick);
            } else if (glyph.equals("✕")) {
                g2.drawLine(Math.round(size * 0.36f), Math.round(size * 0.36f), Math.round(size * 0.64f), Math.round(size * 0.64f));
                g2.drawLine(Math.round(size * 0.64f), Math.round(size * 0.36f), Math.round(size * 0.36f), Math.round(size * 0.64f));
            } else {
                g2.setFont(Theme.font(Font.BOLD, 20f));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(glyph, (size - fm.stringWidth(glyph)) / 2, (size - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }
}
