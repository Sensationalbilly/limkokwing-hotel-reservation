package com.hotel.ui;

import com.hotel.controller.PasswordRequestController;
import com.hotel.controller.ValidationException;
import com.hotel.model.PasswordRequest;
import com.hotel.ui.components.BaseButton;
import com.hotel.ui.components.FormPanel;
import com.hotel.ui.components.ModalCard;
import com.hotel.ui.components.PrimaryButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Lets staff ask an administrator for a password reset, and check what happened to the request.
 * The administrator sets a temporary password and gives it to them in person.
 */
public class ForgotPasswordDialog extends ModalCard {

    private final PasswordRequestController controller = new PasswordRequestController();
    private final JTextField usernameField = new JTextField(20);
    private final JTextArea noteArea = new JTextArea(3, 20);
    private final JLabel feedback = new JLabel(" ");
    private final PrimaryButton sendButton;
    private final BaseButton closeButton;

    public ForgotPasswordDialog(Window owner, String username) {
        super(owner, "Forgot your password?", "Send a reset request to the administrator");

        usernameField.setText(username == null ? "" : username.trim());
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setFont(Theme.font(Font.PLAIN, 13f));
        noteArea.getAccessibleContext().setAccessibleDescription("Optional short message for the administrator");
        // Tab moves to the next control instead of typing a tab character.
        noteArea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        noteArea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        JScrollPane noteScroll = new JScrollPane(noteArea);
        noteScroll.setBorder(BorderFactory.createLineBorder(Theme.border()));

        JLabel steps = Ui.label("<html><div style='width:" + Theme.scaled(420) + "px'>"
                + "1. Send a request below.<br>"
                + "2. An administrator sets a <b>temporary password</b> and gives it to you in person.<br>"
                + "3. Sign in with it. You will then choose your own new password.</div></html>");
        steps.setFont(Theme.font(Font.PLAIN, 13f));

        FormPanel form = new FormPanel();
        form.addField("Username", KeyEvent.VK_U, usernameField);
        form.addField("Note (optional)", KeyEvent.VK_N, noteScroll);

        feedback.setFont(Theme.font(Font.PLAIN, 13f));
        feedback.setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel body = Ui.stack(16, steps, form, feedback);
        body.setPreferredSize(new Dimension(Theme.scaled(480), body.getPreferredSize().height + Theme.scaled(40)));
        setBody(body);

        BaseButton statusButton = addButton(new BaseButton("Check status"), true);
        statusButton.setMnemonic(KeyEvent.VK_C);
        statusButton.addActionListener(e -> checkStatus());
        closeButton = addAction(new BaseButton("Cancel"), null, false);
        sendButton = addButton(new PrimaryButton("Send request"), false);
        sendButton.setMnemonic(KeyEvent.VK_S);
        sendButton.addActionListener(e -> send());
        setDefaultAction(sendButton);
    }

    public void open() {
        showCard();
    }

    private void send() {
        try {
            controller.submit(usernameField.getText(), noteArea.getText());
            showFeedback("Request sent. An administrator will see it on their Staff page. "
                    + "Use \"Check status\" later to see when your password has been reset.", Theme.success());
            sendButton.setEnabled(false);
            closeButton.setText("Done");
            closeButton.requestFocusInWindow();
        } catch (ValidationException e) {
            showFeedback(e.getMessage(), Theme.danger());
        } catch (Exception e) {
            showFeedback("Could not send the request: " + e.getMessage(), Theme.danger());
        }
    }

    private void checkStatus() {
        try {
            PasswordRequest latest = controller.latestFor(usernameField.getText());
            if (latest == null) {
                showFeedback("You have not sent a request yet.", Theme.mutedText());
                return;
            }
            Color color = switch (latest.getStatus()) {
                case PENDING -> Theme.warning();
                case RESOLVED -> Theme.success();
                case DISMISSED -> Theme.danger();
            };
            showFeedback(PasswordRequestController.describe(latest), color);
        } catch (ValidationException e) {
            showFeedback(e.getMessage(), Theme.danger());
        } catch (Exception e) {
            showFeedback("Could not check the status: " + e.getMessage(), Theme.danger());
        }
    }

    private void showFeedback(String message, Color color) {
        feedback.setForeground(color);
        feedback.setText("<html><div style='width:" + Theme.scaled(420) + "px'>"
                + message.replace("&", "&amp;").replace("<", "&lt;") + "</div></html>");
        feedback.getAccessibleContext().setAccessibleName(message);
        pack();
    }
}
