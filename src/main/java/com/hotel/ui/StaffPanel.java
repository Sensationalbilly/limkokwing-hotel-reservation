package com.hotel.ui;

import com.hotel.controller.EventBus;
import com.hotel.controller.PasswordRequestController;
import com.hotel.controller.Session;
import com.hotel.controller.UserController;
import com.hotel.model.PasswordRequest;
import com.hotel.model.Role;
import com.hotel.model.User;
import com.hotel.ui.components.BaseButton;
import com.hotel.ui.components.DangerButton;
import com.hotel.ui.components.FormPanel;
import com.hotel.ui.components.ListTableModel;
import com.hotel.ui.components.PrimaryButton;
import com.hotel.util.Formats;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

/**
 * Staff accounts (administrators only). Admins add staff, change usernames, names and
 * roles, and reset forgotten passwords for any account.
 */
public class StaffPanel extends BasePanel {

    private final UserController controller = new UserController();
    private final PasswordRequestController requestController = new PasswordRequestController();
    private final JPanel requestsList = new JPanel(new GridLayout(0, 1));
    private final JPanel requestsBox;

    private final ListTableModel<User> model = new ListTableModel<>(
            new String[]{"Username", "Full name", "Role"},
            new Class<?>[]{String.class, String.class, String.class}) {
        @Override
        protected Object valueAt(User u, int column) {
            return switch (column) {
                case 0 -> u.getUsername();
                case 1 -> u.getFullName() + (isMe(u) ? "  (you)" : "");
                default -> u.getRole().getLabel();
            };
        }
    };

    private final JTable table = new JTable(model);

    private final JLabel formTitle = Ui.heading("New staff account", 15f);
    private final JTextField usernameField = new JTextField(16);
    private final JTextField fullNameField = new JTextField(16);
    private final JComboBox<Role> roleCombo = new JComboBox<>(Role.values());
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JPasswordField confirmField = new JPasswordField(16);
    private final JLabel passwordLabel;
    private final JLabel confirmLabel;
    private final JLabel feedback = Ui.muted(" ");
    private final BaseButton newButton = new BaseButton("New");
    private final BaseButton resetButton = new BaseButton("Reset password...");
    private final DangerButton deleteButton = new DangerButton("Delete");
    private final PrimaryButton saveButton = new PrimaryButton("Save");

    private User editing;

    public StaffPanel() {
        Ui.styleTable(table, "Staff accounts");
        table.setAutoCreateRowSorter(true);

        FormPanel form = new FormPanel();
        form.addField("Username", KeyEvent.VK_U, usernameField);
        form.addField("Full name", KeyEvent.VK_N, fullNameField);
        form.addField("Role", KeyEvent.VK_R, roleCombo);
        passwordLabel = form.addField("Password", KeyEvent.VK_P, passwordField);
        confirmLabel = form.addField("Confirm password", KeyEvent.VK_F, confirmField);
        form.addFullWidth(Ui.muted("<html><div style='width:" + Theme.scaled(250) + "px'>Passwords need at least "
                + UserController.MIN_PASSWORD_LENGTH
                + " characters. To change an existing password, use Reset password.</div></html>"));
        form.addFullWidth(feedback);
        form.addFiller();

        Ui.describe(usernameField, "The name this person types to sign in");
        Ui.describe(roleCombo, "Administrators can manage rooms and staff; receptionists handle bookings and guests");
        Ui.describe(resetButton, "Set a new password for the selected account, e.g. if it was forgotten");
        newButton.setMnemonic(KeyEvent.VK_W);
        resetButton.setMnemonic(KeyEvent.VK_T);
        deleteButton.setMnemonic(KeyEvent.VK_D);
        saveButton.setMnemonic(KeyEvent.VK_S);

        JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
        buttons.setOpaque(false);
        buttons.add(newButton);
        buttons.add(resetButton);
        buttons.add(deleteButton);
        buttons.add(saveButton);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(form, BorderLayout.CENTER);
        body.add(buttons, BorderLayout.SOUTH);
        JPanel editor = Ui.section(formTitle, body);
        
        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setOpaque(false);
        content.add(Ui.scroll(table), BorderLayout.CENTER);
        content.add(Ui.fixedWidth(editor, Theme.scaled(400)), BorderLayout.EAST);

        JLabel requestsTitle = Ui.heading("Password reset requests", 15f);
        requestsBox = Ui.section(requestsTitle, requestsList);
        requestsList.setOpaque(false);
        requestsBox.setVisible(false);

        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setOpaque(false);
        page.add(requestsBox, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        setScrollableContent(page);

        wireEvents();
        startNew();
        EventBus.subscribe(EventBus.Topic.PASSWORD_REQUESTS_CHANGED, this::refresh);
    }

    /** One row per waiting request, with Reset password / Dismiss buttons. */
    private void showRequests(List<PasswordRequest> pending) {
        requestsList.removeAll();
        for (PasswordRequest r : pending) {
            String note = r.getNote().isBlank() ? "No note" : "“" + r.getNote() + "”";
            JLabel text = new JLabel("<html><b>" + escape(r.getFullName()) + "</b>&nbsp;&nbsp;"
                    + "<font color='" + Theme.hex(Theme.mutedText()) + "'>" + escape(r.getUsername())
                    + "  ·  sent " + Formats.dateTime(r.getCreatedAt()) + "</font><br>" + escape(note) + "</html>");
            text.setForeground(Theme.text());

            BaseButton dismiss = new BaseButton("Dismiss");
            dismiss.addActionListener(e -> dismissRequest(r));
            PrimaryButton reset = new PrimaryButton("Set temporary password");
            reset.addActionListener(e -> resolveRequest(r));

            JPanel row = new JPanel(new BorderLayout(16, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border()), new EmptyBorder(8, 0, 8, 0)));
            row.add(text, BorderLayout.CENTER);
            row.add(Ui.flow(FlowLayout.RIGHT, dismiss, reset), BorderLayout.EAST);
            row.getAccessibleContext().setAccessibleName("Password request from " + r.getFullName() + ". " + note);
            requestsList.add(row);
        }
        requestsBox.setVisible(!pending.isEmpty());
        revalidate();
        repaint();
    }

    private void resolveRequest(PasswordRequest r) {
        char[][] passwords = askTemporaryPassword(r.getFullName(), r.getUsername());
        if (passwords == null) return;
        if (attempt(() -> requestController.resolve(r, passwords[0], passwords[1]))) {
            Dialogs.success(this, "Temporary password set", String.join("\n",
                    "Give the temporary password to " + r.getFullName() + " in person or by phone. "
                            + "When they sign in with it, they will be asked to choose their own new password.",
                    "",
                    "Username:  " + r.getUsername()));
        }
    }

    private void dismissRequest(PasswordRequest r) {
        if (Dialogs.confirm(this, "Dismiss this request?", r.getFullName() + " will see that the request was "
                + "closed and asked to speak to you. Their password stays the same.", "Dismiss", true)) {
            attempt(() -> requestController.dismiss(r));
        }
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public String getTitle() {
        return "Staff";
    }

    @Override
    public String getSubtitle() {
        return "Add staff, change usernames and roles, and reset forgotten passwords";
    }

    @Override
    public void refresh() {
        attempt(() -> {
            showRequests(requestController.pending());
            model.setRows(controller.getAll());
            if (editing != null) {
                int id = editing.getId();
                Ui.selectRow(table, model, u -> u.getId() == id);
            }
        });
    }

    private void wireEvents() {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row >= 0) {
                load(model.getRow(table.convertRowIndexToModel(row)));
            }
        });
        newButton.addActionListener(e -> startNew());
        saveButton.addActionListener(e -> save());
        resetButton.addActionListener(e -> resetPassword());
        deleteButton.addActionListener(e -> delete());
    }

    private void load(User u) {
        editing = u;
        formTitle.setText("Editing: " + u.getUsername());
        usernameField.setText(u.getUsername());
        fullNameField.setText(u.getFullName());
        roleCombo.setSelectedItem(u.getRole());
        setPasswordFieldsVisible(false);
        resetButton.setEnabled(true);
        deleteButton.setEnabled(!isMe(u));
        feedback.setText(" ");
    }

    private void startNew() {
        editing = null;
        table.clearSelection();
        formTitle.setText("New staff account");
        usernameField.setText("");
        fullNameField.setText("");
        roleCombo.setSelectedItem(Role.RECEPTIONIST);
        passwordField.setText("");
        confirmField.setText("");
        setPasswordFieldsVisible(true);
        resetButton.setEnabled(false);
        deleteButton.setEnabled(false);
        feedback.setText(" ");
        usernameField.requestFocusInWindow();
    }

    /** Passwords are typed when creating an account; existing accounts use Reset password. */
    private void setPasswordFieldsVisible(boolean visible) {
        passwordLabel.setVisible(visible);
        passwordField.setVisible(visible);
        confirmLabel.setVisible(visible);
        confirmField.setVisible(visible);
    }

    private void save() {
        User u = new User();
        u.setId(editing == null ? 0 : editing.getId());
        u.setUsername(usernameField.getText());
        u.setFullName(fullNameField.getText());
        u.setRole((Role) roleCombo.getSelectedItem());
        boolean isNew = u.getId() == 0;

        if (attempt(() -> controller.save(u, passwordField.getPassword(), confirmField.getPassword()))) {
            passwordField.setText("");
            confirmField.setText("");
            refresh();
            Ui.selectRow(table, model, row -> row.getId() == u.getId());
            feedback.setForeground(Theme.success());
            feedback.setText(isNew ? "Account " + u.getUsername() + " created." : "Changes saved.");
            if (isMe(u)) {
                feedback.setText("Changes saved. Your new details show in the header after you sign in again.");
            }
        }
    }

    private void resetPassword() {
        if (editing == null) return;
        User target = editing;
        char[][] passwords = askTemporaryPassword(target.getFullName(), target.getUsername());
        if (passwords == null) return;

        if (attempt(() -> controller.resetPassword(target, passwords[0], passwords[1]))) {
            feedback.setForeground(Theme.success());
            feedback.setText("Temporary password set for " + target.getUsername() + ".");
            Dialogs.success(this, "Temporary password set", String.join("\n",
                    "Give the temporary password to " + target.getFullName() + " in person or by phone. "
                            + "When they sign in with it, they will be asked to choose their own new password.",
                    "",
                    "Username:  " + target.getUsername()));
        }
        Arrays.fill(passwords[0], '\0');
        Arrays.fill(passwords[1], '\0');
    }

    /** @return {password, confirmation}, or null if the admin cancelled */
    private char[][] askTemporaryPassword(String fullName, String username) {
        JPasswordField first = new JPasswordField(16);
        JPasswordField second = new JPasswordField(16);
        FormPanel form = new FormPanel();
        form.addFullWidth(Ui.label("<html><div style='width:" + Theme.scaled(360) + "px'>"
                + "Set a temporary password. The staff member must choose their own new password "
                + "the next time they sign in.</div></html>"));
        form.addField("Temporary password", KeyEvent.VK_T, first);
        form.addField("Confirm", KeyEvent.VK_C, second);
        form.addFullWidth(Ui.muted("At least " + UserController.MIN_PASSWORD_LENGTH + " characters."));
        form.setPreferredSize(new Dimension(Theme.scaled(420), form.getPreferredSize().height));

        if (!Dialogs.form(this, "Reset password", fullName + "  ·  " + username, form, "Set temporary password")) {
            return null;
        }
        return new char[][]{first.getPassword(), second.getPassword()};
    }

    private void delete() {
        if (editing == null) return;
        User target = editing;
        if (Dialogs.confirm(this, "Delete " + target.getFullName() + "'s account?",
                "\"" + target.getUsername() + "\" will no longer be able to sign in. This cannot be undone.",
                "Delete account", true)) {
            if (attempt(() -> controller.delete(target))) {
                startNew();
                refresh();
            }
        }
    }

    private static boolean isMe(User u) {
        User me = Session.currentUser();
        return me != null && me.getId() == u.getId();
    }
}
