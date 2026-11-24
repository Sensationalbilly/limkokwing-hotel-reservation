package com.hotel.ui;

import com.hotel.controller.AttendanceController;
import com.hotel.controller.AuthController;
import com.hotel.controller.EventBus;
import com.hotel.controller.PasswordRequestController;
import com.hotel.model.Room;
import com.hotel.model.User;
import com.hotel.ui.components.Avatar;
import com.hotel.ui.components.BaseButton;
import com.hotel.ui.components.NavButton;
import com.hotel.util.Formats;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main window: sidebar navigation (west), page header (north), pages in a CardLayout
 * (center) and a status bar (south). Pages are swapped without creating new windows.
 */
public class DashboardFrame extends JFrame implements Navigator {

    public static final String OVERVIEW = "overview";
    public static final String NEW_BOOKING = "new-booking";
    public static final String BOOKINGS = "bookings";
    public static final String GUESTS = "guests";
    public static final String ROOMS = "rooms";
    public static final String STAFF = "staff";
    public static final String TEAM = "team";

    private final User user;
    private final AuthController auth = new AuthController();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final JPanel nav = new JPanel(new GridLayout(0, 1, 0, 4));
    private final Map<String, BasePanel> pages = new LinkedHashMap<>();
    private final Map<String, NavButton> navButtons = new HashMap<>();
    private final JLabel statusBar = Ui.muted(" ");
    private final BookingPanel bookingPanel = new BookingPanel();
    private String currentPage = OVERVIEW;
    private final PasswordRequestController requestController = new PasswordRequestController();
    private Timer requestTimer;
    private long pendingRequests;

    public DashboardFrame(User user) {
        this(user, OVERVIEW);
    }

    public DashboardFrame(User user, String startPage) {
        super("Limkokwing Hotel Reservation");
        this.user = user;
        setIconImages(Images.windowIcons());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        addPage(OVERVIEW, "Overview", new OverviewPanel(this));
        addPage(NEW_BOOKING, "New booking", bookingPanel);
        addPage(BOOKINGS, "Bookings", new BookingsPanel());
        addPage(GUESTS, "Guests", new CustomersPanel());
        addPage(ROOMS, "Rooms", new RoomsPanel());
        if (user.isAdmin()) {
            addPage(TEAM, "Team", new TeamPanel());     // attendance and performance
            addPage(STAFF, "Staff", new StaffPanel());  // receptionists never see staff accounts
        }

        JPanel main = new JPanel(new BorderLayout());
        main.add(buildHeader(), BorderLayout.NORTH);
        main.add(cards, BorderLayout.CENTER);
        main.add(buildStatusBar(), BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(main, BorderLayout.CENTER);
        setContentPane(root);
        setJMenuBar(buildMenuBar());

        setMinimumSize(new Dimension(1100, 700));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        showPage(startPage);

        if (user.isAdmin()) {
            watchPasswordRequests();
        }
    }

    /**
     * Admins: checks for "forgot password" requests every 15 seconds (a Swing Timer event), shows
     * the count on the Staff button, and offers to open the Staff page when requests are waiting.
     */
    private void watchPasswordRequests() {
        requestTimer = new Timer(15_000, e -> checkPasswordRequests(false));
        requestTimer.setInitialDelay(15_000);
        requestTimer.start();
        EventBus.subscribe(EventBus.Topic.PASSWORD_REQUESTS_CHANGED, () -> checkPasswordRequests(false));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> checkPasswordRequests(true));
            }
        });
    }

    private void checkPasswordRequests(boolean announce) {
        long count;
        try {
            count = requestController.countPending();
        } catch (Exception ex) {
            return;  // database briefly unavailable: try again on the next tick
        }
        NavButton staff = navButtons.get(STAFF);
        if (staff != null) staff.setBadge((int) count);

        if (count > pendingRequests && !announce) {
            statusBar.setText("New password reset request received. Open the Staff page to handle it.");
            if (STAFF.equals(currentPage)) pages.get(STAFF).refresh();
        }
        if (announce && count > 0 && Dialogs.confirm(this, "Password reset requests",
                count + (count == 1 ? " staff member has" : " staff members have")
                        + " asked for a password reset. You can set a temporary password for them on the Staff page.",
                "Open Staff page", false)) {
            showPage(STAFF);
        }
        pendingRequests = count;
    }

    @Override
    public void dispose() {
        if (requestTimer != null) requestTimer.stop();
        super.dispose();
    }

    // ---- Navigator ----

    @Override
    public void showPage(String pageId) {
        String id = pages.containsKey(pageId) ? pageId : OVERVIEW;
        currentPage = id;
        cardLayout.show(cards, id);
        navButtons.forEach((key, button) -> button.setActive(key.equals(id)));
        BasePanel page = pages.get(id);
        page.refresh();
        setTitle(page.getTitle() + " - Limkokwing Hotel Reservation");
        statusBar.setText(page.getTitle() + "   |   Signed in as " + user.getFullName() + " (" + user.getRole()
                + ")   |   Ctrl+1-" + pages.size() + " switch pages, F5 refresh, F1 help");
    }

    @Override
    public void bookRoom(Room room) {
        showPage(NEW_BOOKING);
        bookingPanel.prefill(room);
    }

    // ---- layout ----

    private void addPage(String id, String label, BasePanel page) {
        pages.put(id, page);
        cards.add(page, id);
        NavButton button = new NavButton(label);
        Ui.describe(button, "Go to " + page.getTitle());
        button.addActionListener(e -> showPage(id));
        navButtons.put(id, button);
        nav.add(button);
    }

    private JComponent buildSidebar() {
        JComponent brand = Images.logoCard(Theme.scaled(170), 18f);
        nav.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout(0, 28));
        top.setOpaque(false);
        top.add(brand, BorderLayout.NORTH);
        top.add(nav, BorderLayout.CENTER);

        JLabel footer = new JLabel("v1.0.0  |  MIT License");
        footer.setFont(Theme.font(Font.PLAIN, 11f));
        footer.setForeground(Theme.mix(Theme.onSidebar(), Theme.sidebar(), 0.3f));
        footer.setBorder(new EmptyBorder(0, 8, 0, 0));

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.sidebar());
        sidebar.setBorder(new EmptyBorder(22, 12, 16, 12));
        sidebar.setPreferredSize(new Dimension(Theme.scaled(210), 0));
        sidebar.add(top, BorderLayout.NORTH);
        sidebar.add(footer, BorderLayout.SOUTH);
        sidebar.getAccessibleContext().setAccessibleName("Main navigation");
        return sidebar;
    }

    private JComponent buildHeader() {
        JLabel today = Ui.label(Formats.longDate(LocalDate.now()));
        today.setFont(Theme.font(Font.BOLD, 14f));

        JLabel name = Ui.label(user.getFullName());
        name.setFont(Theme.font(Font.BOLD, 13f));
        JPanel who = new JPanel(new GridLayout(0, 1));
        who.setOpaque(false);
        who.add(name);
        who.add(Ui.muted(user.getRole().getLabel()));
        who.getAccessibleContext().setAccessibleName("Signed in as " + user.getFullName() + ", " + user.getRole());
        BaseButton signOut = new BaseButton("Sign out");
        Ui.describe(signOut, "Sign out and return to the login screen");
        signOut.addActionListener(e -> signOut());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.surface());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border()),
                new EmptyBorder(10, 24, 10, 16)));
        header.add(today, BorderLayout.WEST);
        JPanel account = Ui.flow(FlowLayout.RIGHT, new Avatar(user.getFullName()), who, (JComponent) Box.createHorizontalStrut(6), signOut);
        header.add(account, BorderLayout.EAST);
        return header;
    }

    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.surface());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.border()),
                new EmptyBorder(5, 16, 5, 16)));
        bar.add(statusBar, BorderLayout.WEST);
        return bar;
    }

    private JMenuBar buildMenuBar() {
        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        file.add(menuItem("New booking", KeyEvent.VK_N,
                KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), e -> showPage(NEW_BOOKING)));
        file.add(menuItem("Refresh", KeyEvent.VK_R,
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), e -> pages.get(currentPage).refresh()));
        file.addSeparator();
        file.add(menuItem("Sign out", KeyEvent.VK_S, null, e -> signOut()));
        file.add(menuItem("Exit", KeyEvent.VK_X, null, e -> exitApplication()));

        JMenu view = new JMenu("View");
        view.setMnemonic(KeyEvent.VK_V);
        int index = 1;
        for (Map.Entry<String, BasePanel> page : pages.entrySet()) {
            String id = page.getKey();
            view.add(menuItem(page.getValue().getTitle(), 0,
                    KeyStroke.getKeyStroke(KeyEvent.VK_0 + index, InputEvent.CTRL_DOWN_MASK), e -> showPage(id)));
            index++;
        }
        view.addSeparator();

        JCheckBoxMenuItem contrast = new JCheckBoxMenuItem("High contrast", Theme.isHighContrast());
        contrast.setMnemonic(KeyEvent.VK_H);
        contrast.addActionListener(e -> {
            Theme.setHighContrast(contrast.isSelected());
            SwingUtilities.invokeLater(this::rebuild);
        });
        view.add(contrast);

        JMenu textSize = new JMenu("Text size");
        textSize.setMnemonic(KeyEvent.VK_T);
        String[] names = {"Normal", "Large", "Extra large"};
        float[] scales = {1.0f, 1.15f, 1.3f};
        ButtonGroup sizes = new ButtonGroup();
        for (int i = 0; i < names.length; i++) {
            float scale = scales[i];
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(names[i],
                    Math.abs(Theme.getFontScale() - scale) < 0.01f);
            item.addActionListener(e -> {
                if (Math.abs(Theme.getFontScale() - scale) >= 0.01f) {
                    Theme.setFontScale(scale);
                    SwingUtilities.invokeLater(this::rebuild);
                }
            });
            sizes.add(item);
            textSize.add(item);
        }
        view.add(textSize);

        JMenu help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        help.add(menuItem("Keyboard shortcuts", KeyEvent.VK_K,
                KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), e -> showShortcuts()));
        help.add(menuItem("About", KeyEvent.VK_A, null, e -> showAbout()));

        JMenuBar bar = new JMenuBar();
        bar.add(file);
        bar.add(view);
        bar.add(help);
        return bar;
    }

    private JMenuItem menuItem(String text, int mnemonic, KeyStroke accelerator, ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        if (mnemonic != 0) item.setMnemonic(mnemonic);
        if (accelerator != null) item.setAccelerator(accelerator);
        item.addActionListener(action);
        return item;
    }

    // ---- actions ----

    /** Rebuilds the window so theme and text-size changes apply everywhere. */
    private void rebuild() {
        EventBus.clear();
        Rectangle bounds = getBounds();
        int state = getExtendedState();
        dispose();
        DashboardFrame fresh = new DashboardFrame(user, currentPage);
        fresh.setBounds(bounds);
        fresh.setExtendedState(state);
        fresh.setVisible(true);
    }

    private void signOut() {
        if (Dialogs.confirm(this, "Sign out?", "You will return to the sign-in screen. Any unsaved form "
                + "entries will be lost.", "Sign out", false)) {
            recordSignOut();
            EventBus.clear();
            auth.logout();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    private void exitApplication() {
        if (Dialogs.confirm(this, "Close the app?", "Limkokwing Hotel Reservation will close. "
                + "All saved bookings stay in the database.", "Close app", false)) {
            recordSignOut();
            dispose();
            System.exit(0);
        }
    }

    /** Staff only: saves the time they left (the last sign-out of the day counts). */
    private void recordSignOut() {
        try {
            new AttendanceController().clockOut(user);
        } catch (Exception e) {
            System.err.println("Could not record sign-out time: " + e.getMessage());
        }
    }

    private void showShortcuts() {
        Dialogs.info(this, "Keyboard shortcuts", String.join("\n",
                "Ctrl+1 ... Ctrl+" + pages.size() + "   Switch page (in sidebar order)",
                "Ctrl+N                    New booking",
                "F5                           Refresh current page",
                "Ctrl+Enter              Confirm booking (on the New booking page)",
                "Enter                       Open the selected room card or booking",
                "Alt + underlined letter   Jump to that field or button",
                "Tab / Shift+Tab        Move between controls",
                "",
                "View > High contrast and View > Text size change the display for low-vision users."));
    }

    private void showAbout() {
        Dialogs.info(this, "About", String.join("\n",
                "Hotel Reservation System  v1.0.0",
                "PROG209 Event Driven Programming",
                "Limkokwing University of Creative Technology, Sierra Leone",
                "",
                "Java Swing + MySQL. Open source under the MIT License."));
    }
}
