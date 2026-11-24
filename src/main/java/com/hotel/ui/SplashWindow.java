package com.hotel.ui;

import com.hotel.db.DBConnection;
import com.hotel.ui.components.RoundedPanel;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.util.List;

/**
 * Start-up splash screen: logo, app name and a progress bar that follows real start-up
 * steps (loading, database check). Fades in, then fades out and hands over to sign-in.
 * The slow work runs in a SwingWorker so the animation stays smooth.
 */
public class SplashWindow extends JWindow {

    private static final int FADE_MS = 260;

    private final JLabel statusLabel = Ui.muted("Starting...");
    private final ProgressBar progressBar = new ProgressBar();
    private final boolean translucent;

    private double shownProgress;
    private double targetProgress;
    private float opacity;
    private Timer timer;

    public SplashWindow() {
        GraphicsDevice screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        translucent = screen.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)
                && screen.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
        if (translucent) {
            setBackground(new Color(0, 0, 0, 0));
        }
        setIconImages(Images.windowIcons());
        // A splash has no taskbar button, so keep it above other windows (e.g. the code editor).
        setAlwaysOnTop(true);

        JComponent logo = Images.logoCard(Theme.scaled(230), 26f);
        JPanel logoHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        logoHolder.setOpaque(false);
        logoHolder.add(logo);

        JLabel title = Ui.heading("Limkokwing Hotel Reservation", 20f);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel tagline = Ui.muted("Front desk management  ·  Rooms, guests and bookings");
        tagline.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel titles = new JPanel(new GridLayout(0, 1, 0, 4));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(tagline);

        JLabel version = Ui.muted("v1.0.0");
        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setOpaque(false);
        statusRow.add(statusLabel, BorderLayout.WEST);
        statusRow.add(version, BorderLayout.EAST);

        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setOpaque(false);
        bottom.add(progressBar, BorderLayout.NORTH);
        bottom.add(statusRow, BorderLayout.SOUTH);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 22), 20);
        card.setPadding(30, 36);
        card.add(logoHolder, BorderLayout.NORTH);
        card.add(titles, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        setContentPane(card);

        getAccessibleContext().setAccessibleName("Limkokwing Hotel Reservation is starting");
        pack();
        setSize(Math.max(getWidth(), Theme.scaled(520)), getHeight());
        setLocationRelativeTo(null);
    }

    /** Shows the splash, runs the start-up steps, then calls {@code whenDone} on the UI thread. */
    public void start(Runnable whenDone) {
        setOpacitySafely(0f);
        setVisible(true);
        toFront();
        startAnimation();

        new SwingWorker<Void, Object[]>() {
            @Override
            protected Void doInBackground() throws Exception {
                step(0.35, "Loading interface...", 450);
                publish(new Object[]{0.55, "Connecting to database..."});
                boolean online;
                try (Connection ignored = DBConnection.getConnection()) {
                    online = true;
                } catch (Exception e) {
                    online = false;
                }
                step(0.85, online ? "Database connected" : "Database offline - you can retry at sign-in", online ? 350 : 1400);
                step(1.0, "Ready", 450);
                return null;
            }

            private void step(double progress, String text, long pauseMs) throws InterruptedException {
                publish(new Object[]{progress, text});
                Thread.sleep(pauseMs);
            }

            @Override
            protected void process(List<Object[]> updates) {
                Object[] latest = updates.get(updates.size() - 1);
                targetProgress = (Double) latest[0];
                statusLabel.setText((String) latest[1]);
                getAccessibleContext().setAccessibleDescription((String) latest[1]);
            }

            @Override
            protected void done() {
                fadeOut(whenDone);
            }
        }.execute();
    }

    /** One timer drives the fade-in and the smooth progress bar. */
    private void startAnimation() {
        long start = System.nanoTime();
        timer = new Timer(15, e -> {
            double t = Math.min(1, (System.nanoTime() - start) / 1_000_000.0 / FADE_MS);
            opacity = (float) (1 - Math.pow(1 - t, 3));
            setOpacitySafely(opacity);
            shownProgress += (targetProgress - shownProgress) * 0.12;  // ease towards the target
            progressBar.setValue(shownProgress);
        });
        timer.start();
    }

    private void fadeOut(Runnable whenDone) {
        if (timer != null) timer.stop();
        progressBar.setValue(1.0);
        long start = System.nanoTime();
        Timer fade = new Timer(15, null);
        fade.addActionListener(e -> {
            double t = Math.min(1, (System.nanoTime() - start) / 1_000_000.0 / FADE_MS);
            setOpacitySafely((float) (opacity * (1 - t)));
            if (t >= 1) {
                fade.stop();
                dispose();
                whenDone.run();
            }
        });
        fade.start();
    }

    private void setOpacitySafely(float value) {
        if (!translucent) return;
        try {
            setOpacity(Math.max(0f, Math.min(1f, value)));
        } catch (RuntimeException ignored) {
            // translucency refused at runtime: show without fading
        }
    }

    /** Thin rounded progress bar. */
    private static final class ProgressBar extends JComponent {
        private double value;

        ProgressBar() {
            setPreferredSize(new Dimension(10, Theme.scaled(6)));
            // Decorative only: the status text next to it is what screen readers announce.
        }

        void setValue(double value) {
            this.value = value;
            repaint();
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
                g2.setColor(Theme.primary());
                g2.fillRoundRect(0, 0, Math.max(filled, h), h, h, h);
            }
            g2.dispose();
        }
    }
}
