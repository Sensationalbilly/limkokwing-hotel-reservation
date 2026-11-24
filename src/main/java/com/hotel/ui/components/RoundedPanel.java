package com.hotel.ui.components;

import com.hotel.ui.Theme;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * White card with rounded corners and a soft drop shadow. Subclasses (StatCard, RoomCard)
 * override {@link #borderColor()} or {@link #paintDecorations(Graphics2D, Shape)} to add
 * their own look on top of the same card.
 */
public class RoundedPanel extends JPanel {

    protected static final int SHADOW = 4;

    private final int arc;

    public RoundedPanel(LayoutManager layout, int arc) {
        super(layout);
        this.arc = arc;
        setOpaque(false);
    }

    /** Inner padding; space for the shadow is added automatically. */
    public void setPadding(int vertical, int horizontal) {
        setBorder(new EmptyBorder(vertical + 1, horizontal + 2, vertical + SHADOW + 1, horizontal + 2));
    }

    protected Color fillColor() {
        return Theme.surface();
    }

    protected Color borderColor() {
        return Theme.border();
    }

    protected float borderWidth() {
        return 1f;
    }

    /** Extra painting between the card fill and its border. */
    protected void paintDecorations(Graphics2D g2, Shape card) { }

    protected Shape cardShape() {
        return new RoundRectangle2D.Float(2, 1, getWidth() - 5, getHeight() - SHADOW - 2, arc, arc);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth() - 5;
        int h = getHeight() - SHADOW - 2;

        if (!Theme.isHighContrast()) {
            for (int i = SHADOW; i >= 1; i--) {
                g2.setColor(new Color(16, 24, 40, 4 + (SHADOW - i) * 3));
                g2.fill(new RoundRectangle2D.Float(2, 1 + i, w, h, arc + i, arc + i));
            }
        }

        Shape card = cardShape();
        g2.setColor(fillColor());
        g2.fill(card);
        paintDecorations(g2, card);
        g2.setColor(borderColor());
        g2.setStroke(new BasicStroke(borderWidth()));
        g2.draw(card);
        g2.dispose();
    }
}
