package com.hotel.ui.components;

import com.hotel.ui.Theme;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.function.IntConsumer;

/**
 * Five hand-drawn stars. Editable mode: hover previews, click sets the rating (clicking the same
 * star again clears it); keyboard: Left/Right arrows or the keys 0-5.
 */
public class StarRating extends JComponent {

    private static final Color GOLD = new Color(0xF5A524);

    private final boolean editable;
    private int value;
    private int hover = -1;
    private IntConsumer onChange = v -> { };

    public StarRating(boolean editable) {
        this.editable = editable;
        int size = Theme.scaled(editable ? 26 : 16);
        setPreferredSize(new Dimension(size * 5 + 4 * Theme.scaled(4), size + 2));
        if (!editable) return;

        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("Click a star to rate (1 = poor, 5 = excellent)");
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hover = starAt(e.getX());
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isEnabled()) return;
                int clicked = starAt(e.getX()) + 1;
                change(clicked == value ? 0 : clicked);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isEnabled()) return;
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_UP) change(Math.min(5, value + 1));
                else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_DOWN) change(Math.max(0, value - 1));
                else if (key >= KeyEvent.VK_0 && key <= KeyEvent.VK_5) change(key - KeyEvent.VK_0);
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

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = Math.max(0, Math.min(5, value));
        repaint();
    }

    /** Called with the new value (0 = cleared) when the user changes the rating. */
    public void setOnChange(IntConsumer onChange) {
        this.onChange = onChange;
    }

    private void change(int newValue) {
        if (newValue == value) return;
        setValue(newValue);
        onChange.accept(newValue);
    }

    private int starSize() {
        return getHeight() - 2;
    }

    private int starAt(int x) {
        int step = starSize() + Theme.scaled(4);
        return Math.max(0, Math.min(4, x / step));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (isOpaque()) {
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        int size = starSize();
        int shown = hover >= 0 && isEnabled() ? hover + 1 : value;
        for (int i = 0; i < 5; i++) {
            Shape star = star(i * (size + Theme.scaled(4)) + size / 2.0, getHeight() / 2.0, size / 2.0);
            if (i < shown) {
                g2.setColor(isEnabled() ? GOLD : Theme.mutedText());
                g2.fill(star);
            } else {
                g2.setColor(Theme.border());
                g2.setStroke(new BasicStroke(1.3f));
                g2.draw(star);
            }
        }
        if (editable && isFocusOwner()) {
            g2.setColor(Theme.focusRing());
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        }
        g2.dispose();
    }

    private static Shape star(double cx, double cy, double outer) {
        double inner = outer * 0.45;
        Path2D path = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? outer : inner;
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            double x = cx + r * Math.cos(angle);
            double y = cy + r * Math.sin(angle);
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.closePath();
        return path;
    }

    /** Shows a star rating inside a table cell. */
    public static final class CellRenderer extends StarRating implements TableCellRenderer {
        public CellRenderer() {
            super(false);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            setValue(value instanceof Integer stars ? stars : 0);
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }
}
