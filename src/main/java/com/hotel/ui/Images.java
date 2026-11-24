package com.hotel.ui;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Loads the hotel logo from the classpath (src/main/resources/images/logo.png). */
public final class Images {

    private static final String LOGO_PATH = "/images/logo.png";
    private static final BufferedImage LOGO = load(LOGO_PATH);

    private Images() { }

    /** The logo scaled to the given width, or null if the image file is missing. */
    public static ImageIcon logo(int width) {
        if (LOGO == null) return null;
        int height = Math.round(LOGO.getHeight() * (width / (float) LOGO.getWidth()));
        return new ImageIcon(scale(LOGO, width, height));
    }

    /**
     * The logo on a white rounded card, for dark backgrounds. Falls back to plain text
     * (at the given font size) if the image file is missing.
     */
    public static JComponent logoCard(int width, float fallbackFontSize) {
        ImageIcon logo = logo(width);
        if (logo == null) {
            JLabel text = new JLabel("<html>Limkokwing<br>Hotel<br>Reservation</html>");
            text.setFont(Theme.font(Font.BOLD, fallbackFontSize));
            text.setForeground(Theme.onSidebar());
            return text;
        }
        JLabel image = new JLabel(logo);
        image.getAccessibleContext().setAccessibleName("Limkokwing Hotel Reservation, Sierra Leone");
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(8, 8, 8, 8));
        card.add(image, BorderLayout.CENTER);
        return card;
    }

    /** Window/taskbar icons in a few sizes; Windows picks the best fit. */
    public static List<Image> windowIcons() {
        List<Image> icons = new ArrayList<>();
        if (LOGO == null) return icons;
        // The black box (without the white margin and "Sierra Leone") reads better at icon size.
        BufferedImage box = LOGO.getSubimage(
                Math.round(LOGO.getWidth() * 0.047f), Math.round(LOGO.getHeight() * 0.058f),
                Math.round(LOGO.getWidth() * 0.906f), Math.round(LOGO.getHeight() * 0.712f));
        for (int size : new int[]{16, 32, 64, 128}) {
            icons.add(scale(box, size, Math.round(size * box.getHeight() / (float) box.getWidth())));
        }
        return icons;
    }

    /** High-quality downscale in steps, so the thin lettering stays sharp. */
    private static BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage current = source;
        int w = source.getWidth();
        int h = source.getHeight();
        do {
            w = Math.max(width, w / 2);
            h = Math.max(height, h / 2);
            BufferedImage next = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = next.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(current, 0, 0, w, h, null);
            g.dispose();
            current = next;
        } while (w != width || h != height);
        return current;
    }

    private static BufferedImage load(String path) {
        try (InputStream in = Images.class.getResourceAsStream(path)) {
            if (in == null) {
                System.err.println("Logo not found on classpath: " + path);
                return null;
            }
            return ImageIO.read(in);
        } catch (IOException e) {
            System.err.println("Could not load logo: " + e.getMessage());
            return null;
        }
    }
}
