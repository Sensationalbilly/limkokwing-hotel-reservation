package com.hotel.ui.components;

import com.hotel.ui.Theme;
import com.hotel.ui.Ui;

import javax.swing.*;
import java.awt.*;

/** Dashboard tile: small title, big number, caption, and an optional progress bar. */
public class StatCard extends RoundedPanel {

    private final String title;
    private final Color accent;
    private final JLabel valueLabel = new JLabel("-");
    private final JLabel captionLabel = Ui.muted(" ");
    private final ProgressLine progressLine = new ProgressLine();

    public StatCard(String title, Color accent) {
        super(new BorderLayout(0, 4), 14);
        this.title = title;
        this.accent = accent;
        setPadding(14, 18);

        JLabel titleLabel = Ui.muted(title);
        titleLabel.setIcon(new DotIcon(accent));
        titleLabel.setIconTextGap(8);
        valueLabel.setFont(Theme.font(Font.BOLD, 24f));
        valueLabel.setForeground(Theme.text());

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.add(captionLabel, BorderLayout.NORTH);
        south.add(progressLine, BorderLayout.SOUTH);
        progressLine.setVisible(false);

        add(titleLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        getAccessibleContext().setAccessibleName(title);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
        updateDescription();
    }

    public void setCaption(String caption) {
        captionLabel.setText(caption);
        updateDescription();
    }

    /** 0.0 to 1.0; shows a thin bar under the caption. */
    public void setProgress(double fraction) {
        progressLine.value = Math.max(0, Math.min(1, fraction));
        progressLine.setVisible(true);
        progressLine.repaint();
    }

    private void updateDescription() {
        getAccessibleContext().setAccessibleDescription(title + ": " + valueLabel.getText() + ". " + captionLabel.getText());
    }

    private final class ProgressLine extends JComponent {
        double value;

        ProgressLine() {
            setPreferredSize(new Dimension(10, Theme.scaled(6)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight();
            g2.setColor(Theme.secondary());
            g2.fillRoundRect(0, 0, getWidth(), h, h, h);
            int filled = (int) Math.round(getWidth() * value);
            if (filled > 0) {
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, Math.max(filled, h), h, h, h);
            }
            g2.dispose();
        }
    }

    private record DotIcon(Color color) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y, getIconWidth(), getIconHeight());
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 8;
        }

        @Override
        public int getIconHeight() {
            return 8;
        }
    }
}
