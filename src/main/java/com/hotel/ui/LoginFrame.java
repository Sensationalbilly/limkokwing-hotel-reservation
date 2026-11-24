package com.hotel.ui;

import com.hotel.controller.AttendanceController;
import com.hotel.controller.AuthController;
import com.hotel.controller.Session;
import com.hotel.controller.UserController;
import com.hotel.controller.ValidationException;
import com.hotel.model.Role;
import com.hotel.model.User;
import com.hotel.ui.components.BaseButton;
import com.hotel.ui.components.FormPanel;
import com.hotel.ui.components.PrimaryButton;
import com.hotel.ui.components.RoleCard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutionException;

/**
 * Sign-in window in two steps: first "Who are you?" (Administrator or Receptionist), then the
 * username and password form for that role. Database work runs in SwingWorkers so the window
 * never freezes.
 */
public class LoginFrame extends JFrame {

    private static final String ROLE_STEP = "role";
    private static final String CREDENTIALS_STEP = "credentials";

    private final AuthController auth = new AuthController();

    private final CardLayout steps = new CardLayout();
    private final JPanel stepPanel = new JPanel(steps);
    private final RoleCard adminCard = new RoleCard(RoleCard.Icon.ADMIN, "Administrator",
            "Manage rooms, prices and staff accounts, plus everything the front desk can do.",
            () -> chooseRole(Role.ADMIN));
    private final RoleCard staffCard = new RoleCard(RoleCard.Icon.STAFF, "Receptionist / Staff",
            "Make bookings, check guests in and out, and look after guest records.",
            () -> chooseRole(Role.RECEPTIONIST));

    private final JLabel formTitle = Ui.heading(" ", 24f);
    private final JLabel formSubtitle = Ui.muted(" ");
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final char defaultEchoChar = passwordField.getEchoChar();
    private final JCheckBox showPasswordBox = Ui.checkBox("Show password", KeyEvent.VK_H);
    private final PrimaryButton signInButton = new PrimaryButton("Sign in");
    private final BaseButton retryButton = new BaseButton("Retry connection");
    private final JLabel statusLabel = Ui.muted(" ");
    private final JLabel capsLockWarning = Ui.label("Caps Lock is on");

    private Role selectedRole = Role.RECEPTIONIST;
    private boolean connected;
    private Timer slideTimer;

    public LoginFrame() {
        super("Sign in - Limkokwing Hotel Reservation");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImages(Images.windowIcons());
        setContentPane(buildContent());
        wireEvents();
        pack();
        // Resizable and maximizable; the packed size is the smallest it can go.
        setMinimumSize(getSize());
        setSize(Math.max(getWidth(), 1000), Math.max(getHeight(), 600));
        setLocationRelativeTo(null);
        showRoleStep();
        connect();
    }

    // ---- layout ----

    private JPanel buildContent() {
        JComponent logo = Images.logoCard(Theme.scaled(250), 28f);
        JLabel tagline = new JLabel("<html>Rooms, guests and bookings<br>in one place.</html>");
        tagline.setFont(Theme.font(Font.PLAIN, 13f));
        tagline.setForeground(Theme.mix(Theme.onSidebar(), Theme.sidebar(), 0.2f));

        JPanel brandText = new JPanel(new BorderLayout(0, 20));
        brandText.setOpaque(false);
        brandText.add(logo, BorderLayout.NORTH);
        brandText.add(tagline, BorderLayout.CENTER);

        JPanel brand = new JPanel(new GridBagLayout());
        brand.setBackground(Theme.sidebar());
        brand.setBorder(new EmptyBorder(40, 40, 40, 40));
        brand.add(brandText);

        stepPanel.setOpaque(false);
        stepPanel.add(buildRoleStep(), ROLE_STEP);
        stepPanel.add(buildCredentialsStep(), CREDENTIALS_STEP);

        retryButton.setVisible(false);
        JPanel column = new JPanel(new BorderLayout(0, 18));
        column.setOpaque(false);
        column.add(stepPanel, BorderLayout.CENTER);
        column.add(Ui.stack(8, retryButton, statusLabel), BorderLayout.SOUTH);

        // GridBagLayout keeps the content centred at its natural width when the window is enlarged.
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Theme.surface());
        right.setBorder(new EmptyBorder(40, 40, 32, 40));
        right.add(column);

        JPanel root = new JPanel(new BorderLayout());
        root.add(brand, BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);
        return root;
    }

    private JComponent buildRoleStep() {
        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 6));
        heading.setOpaque(false);
        heading.add(Ui.heading("Who are you?", 26f));
        JLabel hint = Ui.muted("Choose your role to continue to the right sign-in.");
        hint.setFont(Theme.font(Font.PLAIN, 13.5f));
        heading.add(hint);

        JPanel step = new JPanel(new BorderLayout(0, 24));
        step.setOpaque(false);
        step.add(heading, BorderLayout.NORTH);
        step.add(Ui.stack(14, adminCard, staffCard), BorderLayout.CENTER);
        return step;
    }

    private JComponent buildCredentialsStep() {
        JButton back = linkButton("←  Back to role selection");
        back.setMnemonic(KeyEvent.VK_B);
        back.getAccessibleContext().setAccessibleDescription("Choose a different role (Esc)");
        back.addActionListener(e -> showRoleStep());

        usernameField.getAccessibleContext().setAccessibleDescription("Your staff username");
        passwordField.getAccessibleContext().setAccessibleDescription("Your password. Press Enter to sign in.");
        signInButton.setMnemonic(KeyEvent.VK_I);

        FormPanel form = new FormPanel();
        form.addField("Username", KeyEvent.VK_U, usernameField);
        form.addField("Password", KeyEvent.VK_P, passwordField);
        capsLockWarning.setForeground(Theme.warning());
        capsLockWarning.setFont(Theme.font(Font.BOLD, 12f));
        capsLockWarning.setVisible(false);
        form.addFullWidth(capsLockWarning);
        form.addFullWidth(showPasswordBox);

        JButton forgot = linkButton("Forgot password?");
        forgot.setMnemonic(KeyEvent.VK_F);
        forgot.getAccessibleContext().setAccessibleDescription("Ask an administrator to reset your password");
        forgot.addActionListener(e -> new ForgotPasswordDialog(this, usernameField.getText()).open());
        form.addFullWidth(forgot);

        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 6));
        heading.setOpaque(false);
        heading.add(formTitle);
        formSubtitle.setFont(Theme.font(Font.PLAIN, 13.5f));
        heading.add(formSubtitle);

        JPanel top = new JPanel(new BorderLayout(0, 18));
        top.setOpaque(false);
        top.add(back, BorderLayout.NORTH);
        top.add(heading, BorderLayout.CENTER);

        JPanel step = new JPanel(new BorderLayout(0, 18));
        step.setOpaque(false);
        step.add(top, BorderLayout.NORTH);
        step.add(form, BorderLayout.CENTER);
        step.add(signInButton, BorderLayout.SOUTH);
        return step;
    }

    /** Text-only button that underlines on hover, used for "Back" and "Forgot password?". */
    private static JButton linkButton(String text) {
        JButton link = new JButton(text);
        link.setBorderPainted(false);
        link.setContentAreaFilled(false);
        link.setFocusPainted(false);
        link.setForeground(Theme.primary());
        link.setFont(Theme.font(Font.BOLD, 12.5f));
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setHorizontalAlignment(SwingConstants.LEFT);
        link.setBorder(new EmptyBorder(2, 0, 2, 0));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                link.setText("<html><u>" + text + "</u></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                link.setText(text);
            }
        });
        return link;
    }

    // ---- events ----

    private void wireEvents() {
        signInButton.addActionListener(e -> signIn());
        retryButton.addActionListener(e -> connect());
        // Enter in the username box moves on; Enter in the password box triggers the default button.
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        showPasswordBox.addItemListener(e ->
                passwordField.setEchoChar(showPasswordBox.isSelected() ? (char) 0 : defaultEchoChar));

        // Caps Lock warning: checked whenever a key is pressed or released in the password box,
        // and when the box gains or loses focus (KeyListener + FocusListener).
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                updateCapsLockWarning();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                updateCapsLockWarning();
            }
        });
        passwordField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                updateCapsLockWarning();
            }

            @Override
            public void focusLost(FocusEvent e) {
                capsLockWarning.setVisible(false);
            }
        });

        // Esc on the sign-in step goes back to "Who are you?"
        getRootPane().registerKeyboardAction(e -> {
            if (getRootPane().getDefaultButton() != null) showRoleStep();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Shows "Caps Lock is on" while typing a password with Caps Lock on, a common cause of wrong passwords. */
    private void updateCapsLockWarning() {
        boolean on;
        try {
            on = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
        } catch (UnsupportedOperationException e) {
            on = false;  // some systems cannot report Caps Lock; just don't warn
        }
        if (capsLockWarning.isVisible() != on) {
            capsLockWarning.setVisible(on);
            passwordField.getAccessibleContext().setAccessibleDescription(on
                    ? "Caps Lock is on. Your password. Press Enter to sign in."
                    : "Your password. Press Enter to sign in.");
            capsLockWarning.revalidate();
        }
    }

    private void chooseRole(Role role) {
        selectedRole = role;
        boolean admin = role == Role.ADMIN;
        formTitle.setText(admin ? "Administrator sign in" : "Staff sign in");
        formSubtitle.setText(admin ? "Sign in with your administrator account."
                : "Sign in with your receptionist or staff account.");
        passwordField.setText("");
        showPasswordBox.setSelected(false);
        steps.show(stepPanel, CREDENTIALS_STEP);
        getRootPane().setDefaultButton(signInButton);
        signInButton.setEnabled(connected);
        slideIn();
        usernameField.requestFocusInWindow();
    }

    private void showRoleStep() {
        steps.show(stepPanel, ROLE_STEP);
        getRootPane().setDefaultButton(null);  // Enter on a role card selects that card instead
        slideIn();
        (selectedRole == Role.ADMIN ? adminCard : staffCard).requestFocusInWindow();
    }

    /** Short slide-in from the right when switching steps. */
    private void slideIn() {
        if (slideTimer != null) slideTimer.stop();
        long start = System.nanoTime();
        int distance = Theme.scaled(28);
        slideTimer = new Timer(15, null);
        slideTimer.addActionListener(e -> {
            double t = Math.min(1, (System.nanoTime() - start) / 1_000_000.0 / 220);
            int offset = (int) Math.round(distance * Math.pow(1 - t, 3));
            stepPanel.setBorder(new EmptyBorder(0, offset, 0, 0));
            stepPanel.revalidate();
            if (t >= 1) slideTimer.stop();
        });
        slideTimer.start();
    }

    // ---- database ----

    /** Tests the database and creates default accounts on first run, off the UI thread. */
    private void connect() {
        connected = false;
        signInButton.setEnabled(false);
        retryButton.setVisible(false);
        setStatus("Connecting to the database...", Theme.mutedText());

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return auth.initialise();
            }

            @Override
            protected void done() {
                try {
                    boolean createdDefaults = get();
                    connected = true;
                    signInButton.setEnabled(true);
                    setStatus(createdDefaults
                            ? "Database ready. Default staff accounts were created (see README)."
                            : "Connected to the database.", Theme.success());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    e.getCause().printStackTrace();
                    retryButton.setVisible(true);
                    setStatus("Cannot connect to MySQL: " + firstLine(e.getCause().getMessage())
                            + ". Is MySQL running, and is config/db.properties correct?", Theme.danger());
                }
            }
        }.execute();
    }

    private void signIn() {
        String username = usernameField.getText();
        char[] password = passwordField.getPassword();
        Role role = selectedRole;
        signInButton.setEnabled(false);
        setStatus("Signing in...", Theme.mutedText());

        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                return auth.login(username, password, role);
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    if (user.isMustChangePassword() && !chooseNewPassword()) {
                        auth.logout();
                        signInButton.setEnabled(true);
                        passwordField.setText("");
                        setStatus("You need to choose a new password before you can continue.", Theme.warning());
                        return;
                    }
                    takeAttendance(Session.currentUser());
                    dispose();
                    new DashboardFrame(Session.currentUser()).setVisible(true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    signInButton.setEnabled(true);
                    passwordField.setText("");
                    passwordField.requestFocusInWindow();
                    setStatus(cause instanceof ValidationException
                            ? cause.getMessage()
                            : "Sign-in failed: " + firstLine(cause.getMessage()), Theme.danger());
                }
            }
        }.execute();
    }

    /**
     * Records the staff member's clock-in (system time) and, on the first sign-in of the day,
     * asks them to confirm the time they arrived. Problems here never stop them signing in.
     */
    private void takeAttendance(User user) {
        if (!AttendanceController.isTracked(user)) return;
        try {
            AttendanceController.ClockIn clockIn = new AttendanceController().clockIn(user);
            if (clockIn != null && clockIn.needsConfirmation()) {
                new AttendanceDialog(this, user, clockIn.attendance()).open();
            }
        } catch (Exception e) {
            System.err.println("Could not record attendance: " + e.getMessage());
        }
    }

    /**
     * Shown after signing in with a temporary password from an administrator.
     *
     * @return true once the user has saved a new password; false if they cancelled
     */
    private boolean chooseNewPassword() {
        while (true) {
            JPasswordField first = new JPasswordField(18);
            JPasswordField second = new JPasswordField(18);
            FormPanel form = new FormPanel();
            form.addFullWidth(Ui.label("<html><div style='width:" + Theme.scaled(380) + "px'>"
                    + "An administrator reset your password. Choose a new one that only you know. "
                    + "You will use it from now on.</div></html>"));
            form.addField("New password", KeyEvent.VK_N, first);
            form.addField("Confirm", KeyEvent.VK_C, second);
            form.addFullWidth(Ui.muted("At least " + UserController.MIN_PASSWORD_LENGTH + " characters."));

            if (!Dialogs.form(this, "Choose a new password", "Welcome back, " + Session.currentUser().getFullName(),
                    form, "Save and continue")) {
                return false;
            }
            try {
                auth.changeOwnPassword(first.getPassword(), second.getPassword());
                return true;
            } catch (ValidationException e) {
                Dialogs.warn(this, e.getMessage());
            } catch (Exception e) {
                Dialogs.error(this, e);
                return false;
            }
        }
    }

    private void setStatus(String message, Color color) {
        statusLabel.setForeground(color);
        statusLabel.setText("<html><div style='width:" + Theme.scaled(300) + "px'>"
                + escape(message) + "</div></html>");
        statusLabel.getAccessibleContext().setAccessibleName(message);
        getContentPane().revalidate();
        // Only grow (never shrink) if a long message no longer fits; leave a maximized window alone.
        if (getExtendedState() == NORMAL) {
            Dimension needed = getPreferredSize();
            if (needed.width > getWidth() || needed.height > getHeight()) {
                setSize(Math.max(getWidth(), needed.width), Math.max(getHeight(), needed.height));
            }
        }
    }

    private static String firstLine(String text) {
        if (text == null) return "unknown error";
        int newline = text.indexOf('\n');
        return newline < 0 ? text : text.substring(0, newline);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
