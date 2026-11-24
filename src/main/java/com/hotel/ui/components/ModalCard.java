package com.hotel.ui.components;

import com.hotel.ui.Theme;
import com.hotel.ui.Ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Modern pop-up: a rounded card that fades and slides in over a dimmed window, and fades
 * out when closed. Subclasses add a body and action buttons; {@link #showCard()} blocks
 * until the user picks an action and returns that action's result (null for Close/Esc).
 */
public class ModalCard extends JDialog {

    private static final int OPEN_MS = 200;
    private static final int CLOSE_MS = 130;
    private static final float DIM_ALPHA = 0.38f;

    private final RoundedPanel card = new RoundedPanel(new BorderLayout(0, 20), 18);
    private final JPanel headerBadges = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    private final JPanel footerLeft = Ui.flow(FlowLayout.LEFT);
    private final JPanel footerRight = Ui.flow(FlowLayout.RIGHT);
    private final boolean translucent;

    private Object result;
    private boolean closing;
    private Point target;
    private Timer timer;
    private DimLayer dim;
    private Component previousGlassPane;

    public ModalCard(Window owner, String title, String subtitle) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        GraphicsDevice screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        translucent = screen.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)
                && screen.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
        if (translucent) {
            setBackground(new Color(0, 0, 0, 0));  // lets the rounded corners and shadow show
        }

        JLabel titleLabel = Ui.heading(title, 20f);
        headerBadges.setOpaque(false);
        JPanel titleRow = new JPanel(new BorderLayout(12, 0));
        titleRow.setOpaque(false);
        titleRow.add(titleLabel, BorderLayout.WEST);
        titleRow.add(headerBadges, BorderLayout.CENTER);

        JLabel subtitleLabel = Ui.muted(subtitle);
        subtitleLabel.setFont(Theme.font(Font.PLAIN, 13f));
        JPanel titles = new JPanel(new BorderLayout(0, 4));
        titles.setOpaque(false);
        titles.add(titleRow, BorderLayout.NORTH);
        if (subtitle != null && !subtitle.isEmpty()) {
            titles.add(subtitleLabel, BorderLayout.CENTER);
        }

        // Drawn by hand: not every Windows font has a "x" symbol, which showed up as an empty box.
        BaseButton closeButton = new BaseButton("");
        closeButton.setIcon(new CloseIcon());
        closeButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        closeButton.getAccessibleContext().setAccessibleName("Close");
        Ui.describe(closeButton, "Close (Esc)");
        closeButton.addActionListener(e -> close(null));
        JPanel closeHolder = new JPanel(new BorderLayout());
        closeHolder.setOpaque(false);
        closeHolder.add(closeButton, BorderLayout.NORTH);

        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.add(titles, BorderLayout.CENTER);
        header.add(closeHolder, BorderLayout.EAST);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(footerLeft, BorderLayout.WEST);
        footer.add(footerRight, BorderLayout.EAST);

        card.setPadding(22, 24);
        card.add(header, BorderLayout.NORTH);
        card.add(footer, BorderLayout.SOUTH);
        setContentPane(card);
        getAccessibleContext().setAccessibleName(subtitle == null || subtitle.isEmpty() ? title : title + ". " + subtitle);

        getRootPane().registerKeyboardAction(e -> close(null),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close(null);
            }
        });
    }

    protected void setBody(JComponent body) {
        card.add(body, BorderLayout.CENTER);
    }

    protected void addHeaderBadge(JComponent badge) {
        headerBadges.add(badge);
    }

    /** Adds a footer button that closes the card with the given result. */
    protected <B extends BaseButton> B addAction(B button, Object actionResult, boolean leftSide) {
        button.addActionListener(e -> close(actionResult));
        (leftSide ? footerLeft : footerRight).add(button);
        return button;
    }

    /** Adds a footer button that does NOT close the card; the caller adds its own listener. */
    protected <B extends BaseButton> B addButton(B button, boolean leftSide) {
        (leftSide ? footerLeft : footerRight).add(button);
        return button;
    }

    protected void setDefaultAction(JButton button) {
        getRootPane().setDefaultButton(button);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                button.requestFocusInWindow();
            }
        });
    }

    /** Shows the card with its entrance animation and waits for the user. */
    public Object showCard() {
        pack();
        setLocationRelativeTo(getOwner());
        target = getLocation();
        installDim();
        apply(0);
        animate(true, null);
        setVisible(true);  // blocks until close() disposes the dialog
        return result;
    }

    protected void close(Object actionResult) {
        if (closing) return;
        closing = true;
        result = actionResult;
        animate(false, () -> {
            removeDim();
            dispose();
        });
    }

    private void animate(boolean opening, Runnable done) {
        if (timer != null) timer.stop();
        long start = System.nanoTime();
        int duration = opening ? OPEN_MS : CLOSE_MS;
        timer = new Timer(15, null);
        timer.addActionListener(e -> {
            double t = Math.min(1, (System.nanoTime() - start) / 1_000_000.0 / duration);
            double eased = 1 - Math.pow(1 - t, 3);  // ease-out cubic
            apply(opening ? eased : 1 - eased);
            if (t >= 1) {
                timer.stop();
                if (done != null) done.run();
            }
        });
        timer.start();
    }

    /** progress 0 = hidden, 1 = fully shown. */
    private void apply(double progress) {
        if (translucent) {
            try {
                setOpacity((float) Math.max(0.01, progress));
            } catch (RuntimeException ignored) {
                // translucency refused at runtime: show without fading
            }
        }
        if (target != null) {
            setLocation(target.x, target.y + (int) Math.round((1 - progress) * Theme.scaled(18)));
        }
        if (dim != null) {
            dim.alpha = (float) (DIM_ALPHA * progress);
            dim.repaint();
        }
    }

    private void installDim() {
        if (getOwner() instanceof RootPaneContainer container) {
            previousGlassPane = container.getGlassPane();
            dim = new DimLayer();
            container.setGlassPane(dim);
            dim.setVisible(true);
        }
    }

    private void removeDim() {
        if (dim != null && getOwner() instanceof RootPaneContainer container) {
            container.setGlassPane(previousGlassPane);
            previousGlassPane.setVisible(false);
            dim = null;
        }
    }

    /** Two crossed lines in the button's text colour. */
    private static final class CloseIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.getForeground());
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int s = getIconWidth() - 1;
            g2.drawLine(x, y, x + s, y + s);
            g2.drawLine(x + s, y, x, y + s);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return Theme.scaled(11);
        }

        @Override
        public int getIconHeight() {
            return Theme.scaled(11);
        }
    }

    /** Darkens the window behind the card. */
    private static final class DimLayer extends JComponent {
        float alpha;

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(10, 16, 30, Math.round(255 * alpha)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}
