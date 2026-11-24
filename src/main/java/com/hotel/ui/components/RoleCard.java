package com.hotel.ui.components;

import com.hotel.ui.Theme;
import com.hotel.ui.Ui;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Large clickable choice card on the "Who are you?" screen: icon, title, description and
 * an arrow. Responds to mouse click and to Enter/Space when focused with the keyboard.
 */
public class RoleCard extends RoundedPanel {

    public enum Icon { ADMIN, STAFF }

    private final JLabel arrow = new JLabel("›");
    private boolean hovered;

    public RoleCard(Icon icon, String title, String description, Runnable onSelect) {
        super(new BorderLayout(18, 0), 16);
        setPadding(18, 20);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel titleLabel = Ui.heading(title, 16f);
        JLabel descriptionLabel = Ui.muted("<html><div style='width:" + Theme.scaled(250) + "px'>"
                + description + "</div></html>");
        descriptionLabel.setFont(Theme.font(Font.PLAIN, 12.5f));
        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);
        text.add(titleLabel, BorderLayout.NORTH);
        text.add(descriptionLabel, BorderLayout.CENTER);

        arrow.setFont(Theme.font(Font.BOLD, 30f));
        arrow.setForeground(Theme.mutedText());

        JPanel iconHolder = new JPanel(new GridBagLayout());
        iconHolder.setOpaque(false);
        iconHolder.add(new RoleIcon(icon));

        add(iconHolder, BorderLayout.WEST);
        add(text, BorderLayout.CENTER);
        add(arrow, BorderLayout.EAST);

        getAccessibleContext().setAccessibleName(title);
        getAccessibleContext().setAccessibleDescription(description + ". Press Enter to continue.");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setHovered(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHovered(false);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) onSelect.run();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
                    e.consume();
                    onSelect.run();
                }
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

    private void setHovered(boolean hovered) {
        this.hovered = hovered;
        arrow.setForeground(hovered ? Theme.primary() : Theme.mutedText());
        repaint();
    }

    @Override
    protected Color fillColor() {
        return hovered ? Theme.mix(Theme.surface(), Theme.primary(), 0.04f) : Theme.surface();
    }

    @Override
    protected Color borderColor() {
        if (isFocusOwner()) return Theme.focusRing();
        return hovered ? Theme.primary() : Theme.border();
    }

    @Override
    protected float borderWidth() {
        return isFocusOwner() ? 2.5f : hovered ? 1.5f : 1f;
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJPanel() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.PUSH_BUTTON;
                }
            };
        }
        return accessibleContext;
    }

    /** Hand-drawn icon: a shield for administrators, a person for staff. */
    private static final class RoleIcon extends JComponent {
        private final Icon icon;

        RoleIcon(Icon icon) {
            this.icon = icon;
            int size = Theme.scaled(52);
            setPreferredSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float s = Math.min(getWidth(), getHeight());
            Color color = icon == Icon.ADMIN ? Theme.primary() : Theme.success();

            g2.setColor(Theme.tint(color));
            g2.fill(new RoundRectangle2D.Float(0, 0, s - 1, s - 1, s * 0.32f, s * 0.32f));
            g2.setColor(color);

            if (icon == Icon.ADMIN) {
                // Shield with a check mark
                float cx = s / 2f;
                Path2D shield = new Path2D.Float();
                shield.moveTo(cx, s * 0.20f);
                shield.lineTo(s * 0.76f, s * 0.30f);
                shield.curveTo(s * 0.76f, s * 0.58f, s * 0.66f, s * 0.72f, cx, s * 0.82f);
                shield.curveTo(s * 0.34f, s * 0.72f, s * 0.24f, s * 0.58f, s * 0.24f, s * 0.30f);
                shield.closePath();
                g2.fill(shield);
                g2.setColor(Theme.onAccent());
                g2.setStroke(new BasicStroke(s * 0.06f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D tick = new Path2D.Float();
                tick.moveTo(s * 0.39f, s * 0.50f);
                tick.lineTo(s * 0.47f, s * 0.58f);
                tick.lineTo(s * 0.62f, s * 0.42f);
                g2.draw(tick);
            } else {
                // Person: head and shoulders
                float head = s * 0.22f;
                g2.fill(new Ellipse2D.Float((s - head) / 2f, s * 0.22f, head, head));
                Shape oldClip = g2.getClip();
                g2.clip(new java.awt.Rectangle(0, 0, (int) s, (int) (s * 0.80f)));
                g2.fill(new Ellipse2D.Float(s * 0.27f, s * 0.50f, s * 0.46f, s * 0.46f));
                g2.setClip(oldClip);
            }
            g2.dispose();
        }
    }
}
