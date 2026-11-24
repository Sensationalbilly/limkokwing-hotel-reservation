package com.hotel.ui;

import com.hotel.ui.components.BaseButton;
import com.hotel.ui.components.ModalCard;
import com.hotel.ui.components.PrimaryButton;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.sql.SQLException;

/** All pop-ups in the app: animated cards that dim the window behind them. */
public final class Dialogs {

    public enum Kind { INFO, SUCCESS, WARNING, ERROR, QUESTION }

    private Dialogs() { }

    public static void info(Component parent, String title, String message) {
        new MessageCard(owner(parent), Kind.INFO, title, message, null, false).open();
    }

    public static void success(Component parent, String title, String message) {
        new MessageCard(owner(parent), Kind.SUCCESS, title, message, null, false).open();
    }

    public static void warn(Component parent, String message) {
        new MessageCard(owner(parent), Kind.WARNING, "Please check", message, null, false).open();
    }

    public static void error(Component parent, Exception ex) {
        ex.printStackTrace();
        String message = ex instanceof SQLException
                ? "A database error occurred:\n" + ex.getMessage()
                  + "\n\nCheck that MySQL is running and config/db.properties is correct."
                : "Something went wrong:\n" + ex.getMessage();
        new MessageCard(owner(parent), Kind.ERROR, "Error", message, null, false).open();
    }

    public static boolean confirm(Component parent, String title, String message) {
        return confirm(parent, title, message, "Confirm", false);
    }

    /**
     * @param confirmLabel text of the confirm button, e.g. "Sign out"
     * @param destructive  red button, and Enter defaults to Cancel
     */
    public static boolean confirm(Component parent, String title, String message,
                                  String confirmLabel, boolean destructive) {
        Kind kind = destructive ? Kind.WARNING : Kind.QUESTION;
        return new MessageCard(owner(parent), kind, title, message, confirmLabel, destructive).open();
    }

    /** Shows a small form in a card. @return true if the user pressed the OK button */
    public static boolean form(Component parent, String title, String subtitle, JComponent form, String okLabel) {
        return new FormCard(owner(parent), title, subtitle, form, okLabel).open();
    }

    private static Window owner(Component parent) {
        if (parent == null) return null;
        return parent instanceof Window window ? window : SwingUtilities.getWindowAncestor(parent);
    }

    private static final class FormCard extends ModalCard {
        private static final Object OK = new Object();

        FormCard(Window owner, String title, String subtitle, JComponent form, String okLabel) {
            super(owner, title, subtitle);
            setBody(form);
            addAction(new BaseButton("Cancel"), null, false);
            setDefaultAction(addAction(new PrimaryButton(okLabel), OK, false));
        }

        boolean open() {
            return showCard() == OK;
        }
    }
}
