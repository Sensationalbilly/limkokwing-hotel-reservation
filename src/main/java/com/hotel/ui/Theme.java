package com.hotel.ui;

import com.hotel.model.BookingStatus;
import com.hotel.model.RoomStatus;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;
import java.util.prefs.Preferences;

/**
 * Colours and fonts for the whole app. Supports a high-contrast palette and larger text
 * (accessibility); both choices are remembered between sessions.
 */
public final class Theme {

    private static final Preferences PREFS = Preferences.userNodeForPackage(Theme.class);
    private static final String FONT_FAMILY = "Segoe UI";

    private static boolean highContrast = PREFS.getBoolean("highContrast", false);
    private static float fontScale = PREFS.getFloat("fontScale", 1.0f);

    private Theme() { }

    public static boolean isHighContrast() {
        return highContrast;
    }

    public static void setHighContrast(boolean enabled) {
        highContrast = enabled;
        PREFS.putBoolean("highContrast", enabled);
        installDefaults();
    }

    public static float getFontScale() {
        return fontScale;
    }

    public static void setFontScale(float scale) {
        fontScale = scale;
        PREFS.putFloat("fontScale", scale);
        installDefaults();
    }

    // ---- palette (normal colours meet WCAG AA contrast on white) ----

    public static Color primary()    { return highContrast ? new Color(0xFFD700) : new Color(0x1E5AA8); }
    public static Color onAccent()   { return highContrast ? Color.BLACK : Color.WHITE; }
    public static Color background() { return highContrast ? Color.BLACK : new Color(0xF4F6FA); }
    public static Color surface()    { return highContrast ? new Color(0x101010) : Color.WHITE; }
    public static Color secondary()  { return highContrast ? new Color(0x333333) : new Color(0xE4E7EB); }
    public static Color text()       { return highContrast ? Color.WHITE : new Color(0x1F2933); }
    public static Color mutedText()  { return highContrast ? new Color(0xE0E0E0) : new Color(0x52606D); }
    public static Color border()     { return highContrast ? Color.WHITE : new Color(0xD9DEE7); }
    public static Color sidebar()    { return highContrast ? Color.BLACK : new Color(0x14213D); }
    public static Color onSidebar()  { return Color.WHITE; }
    public static Color success()    { return highContrast ? new Color(0x00FF7F) : new Color(0x2E7D32); }
    public static Color warning()    { return highContrast ? new Color(0xFFA500) : new Color(0x9A5B00); }
    public static Color danger()     { return highContrast ? new Color(0xFF7070) : new Color(0xC62828); }
    public static Color focusRing()  { return highContrast ? Color.CYAN : new Color(0xF5A524); }

    /** Status colours. The status is always written as text too, never shown by colour alone. */
    public static Color statusColor(Object status) {
        if (status instanceof RoomStatus roomStatus) {
            return switch (roomStatus) {
                case AVAILABLE -> success();
                case OCCUPIED -> danger();
                case MAINTENANCE -> warning();
            };
        }
        if (status instanceof BookingStatus bookingStatus) {
            return switch (bookingStatus) {
                case CONFIRMED -> primary();
                case CHECKED_IN -> success();
                case CHECKED_OUT -> mutedText();
                case CANCELLED -> danger();
            };
        }
        return text();
    }

    // ---- fonts ----

    public static Font font(int style, float size) {
        return new Font(FONT_FAMILY, style, Math.round(size * fontScale));
    }

    public static int scaled(int pixels) {
        return Math.round(pixels * fontScale);
    }

    /** Applies the scaled base font to all standard Swing components. */
    public static void installDefaults() {
        FontUIResource base = new FontUIResource(font(Font.PLAIN, 13f));
        String[] keys = {
                "Label.font", "Button.font", "TextField.font", "PasswordField.font", "FormattedTextField.font",
                "TextArea.font", "ComboBox.font", "CheckBox.font", "RadioButton.font", "List.font", "Table.font",
                "TableHeader.font", "Spinner.font", "Menu.font", "MenuItem.font", "CheckBoxMenuItem.font",
                "RadioButtonMenuItem.font", "PopupMenu.font", "MenuBar.font", "ToolTip.font", "TitledBorder.font",
                "OptionPane.messageFont", "OptionPane.buttonFont", "TabbedPane.font"
        };
        for (String key : keys) {
            UIManager.put(key, base);
        }
    }

    // ---- colour helpers ----

    /** Negative amount darkens, positive lightens. */
    public static Color shade(Color c, float amount) {
        if (amount < 0) {
            float f = 1 + amount;
            return new Color(clamp(c.getRed() * f), clamp(c.getGreen() * f), clamp(c.getBlue() * f));
        }
        return mix(c, Color.WHITE, amount);
    }

    public static Color mix(Color a, Color b, float t) {
        return new Color(
                clamp(a.getRed() + (b.getRed() - a.getRed()) * t),
                clamp(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                clamp(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** Very light version of a colour, used behind status pills. */
    public static Color tint(Color c) {
        return highContrast ? surface() : mix(c, Color.WHITE, 0.88f);
    }

    public static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static int clamp(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }
}
