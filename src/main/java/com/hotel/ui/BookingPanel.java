package com.hotel.ui;

import com.hotel.controller.BookingController;
import com.hotel.controller.CustomerController;
import com.hotel.controller.EventBus;
import com.hotel.model.Booking;
import com.hotel.model.Customer;
import com.hotel.model.PaymentMethod;
import com.hotel.model.Room;
import com.hotel.ui.components.BaseButton;
import com.hotel.ui.components.DateField;
import com.hotel.ui.components.FormPanel;
import com.hotel.ui.components.PrimaryButton;
import com.hotel.ui.components.ScrollablePanel;
import com.hotel.ui.components.SuccessButton;
import com.hotel.util.Formats;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * New booking form. Every input is wired to a listener so the price summary updates live:
 * ItemListener (radio buttons, check boxes), ChangeListener (dates, guests),
 * ListSelectionListener (room list) and ActionListener (buttons, combo box).
 */
public class BookingPanel extends BasePanel {

    private static final String NEW_CARD = "new";
    private static final String EXISTING_CARD = "existing";
    private static final String[] TYPE_OPTIONS = {"Any type", "Standard", "Deluxe", "Suite"};
    private static final String START_HINT = "Choose dates and press Search to see available rooms.";

    private final BookingController controller = new BookingController();
    private final CustomerController customerController = new CustomerController();

    // 1. guest
    private final JRadioButton newGuest = Ui.radio("New guest", KeyEvent.VK_W);
    private final JRadioButton existingGuest = Ui.radio("Returning guest", KeyEvent.VK_X);
    private final CardLayout guestCards = new CardLayout();
    private final JPanel guestCardPanel = new JPanel(guestCards);
    private final JComboBox<Customer> customerCombo = new JComboBox<>();
    private final JTextField nameField = new JTextField(18);
    private final JTextField phoneField = new JTextField(18);
    private final JTextField emailField = new JTextField(18);
    private final JTextField idField = new JTextField(18);
    private final JComboBox<String> nationalityCombo = new JComboBox<>(Customer.COMMON_NATIONALITIES);

    // 2. stay
    private final DateField checkInField = new DateField(LocalDate.now());
    private final DateField checkOutField = new DateField(LocalDate.now().plusDays(1));
    private final JSpinner guestsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
    private final JComboBox<String> typeCombo = new JComboBox<>(TYPE_OPTIONS);
    private final PrimaryButton searchButton = new PrimaryButton("Search available rooms");

    // 3. room
    private final DefaultListModel<Room> roomModel = new DefaultListModel<>();
    private final JList<Room> roomList = new JList<>(roomModel);
    private final JLabel resultsLabel = Ui.muted(START_HINT);

    // 4. extras
    private final String breakfastText = "Breakfast (" + Formats.money(BookingController.BREAKFAST_RATE)
            + " per guest per night)";
    private final JCheckBox breakfastBox = Ui.checkBox(breakfastText, KeyEvent.VK_B);
    private final JCheckBox pickupBox = Ui.checkBox("Airport pickup from Lungi ("
            + Formats.money(BookingController.AIRPORT_PICKUP_FEE) + ")", KeyEvent.VK_A);
    private final Map<PaymentMethod, JRadioButton> paymentButtons = new EnumMap<>(PaymentMethod.class);
    private final JTextField notesField = new JTextField(18);

    // 5. summary
    private final JLabel nightsValue = valueLabel();
    private final JLabel roomValue = valueLabel();
    private final JLabel extrasValue = valueLabel();
    private final JLabel totalValue = valueLabel();
    private final BaseButton clearButton = new BaseButton("Clear form");
    private final SuccessButton confirmButton = new SuccessButton("Confirm booking");

    public BookingPanel() {
        ScrollablePanel columns = new ScrollablePanel(new GridLayout(1, 2, 16, 0));
        columns.setBorder(new EmptyBorder(0, 0, 0, 8));  // keep clear of the scroll bar
        columns.add(Ui.stack(16, buildGuestSection(), buildStaySection()));
        columns.add(Ui.stack(16, buildRoomSection(), buildExtrasSection(), buildSummarySection()));

        // Each section keeps its natural height; the page scrolls when the window is too short.
        JScrollPane scroll = new JScrollPane(columns);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        add(scroll, BorderLayout.CENTER);

        wireEvents();
        updateSummary();
        EventBus.subscribe(EventBus.Topic.CUSTOMERS_CHANGED, this::loadCustomers);
    }

    @Override
    public String getTitle() {
        return "New booking";
    }

    @Override
    public String getSubtitle() {
        return "Find a free room and book it in five steps";
    }

    @Override
    public void refresh() {
        loadCustomers();
    }

    /** Called from the overview: pre-selects a room type and, if free, the room itself. */
    public void prefill(Room room) {
        typeCombo.setSelectedIndex(1 + Arrays.asList(Room.TYPES).indexOf(room.getType()));
        searchRooms();
        if (roomModel.contains(room)) {
            roomList.setSelectedValue(room, true);
            roomList.requestFocusInWindow();
        } else {
            resultsLabel.setText("Room " + room.getRoomNumber() + " is not free for these dates. "
                    + "Other " + room.getDisplayName() + " rooms are listed instead.");
        }
    }

    // ---- sections ----

    private JComponent buildGuestSection() {
        ButtonGroup group = new ButtonGroup();
        group.add(newGuest);
        group.add(existingGuest);
        newGuest.setSelected(true);

        FormPanel fresh = new FormPanel();
        fresh.addField("Full name", KeyEvent.VK_N, nameField);
        fresh.addField("Phone", KeyEvent.VK_P, phoneField);
        fresh.addField("Email (optional)", KeyEvent.VK_E, emailField);
        fresh.addField("ID / passport no.", KeyEvent.VK_I, idField);
        fresh.addField("Nationality", KeyEvent.VK_T, nationalityCombo);
        nationalityCombo.setEditable(true);
        Ui.describe(phoneField, "Guest phone number, for example +232 76 123456");

        FormPanel existing = new FormPanel();
        existing.addField("Guest", KeyEvent.VK_U, customerCombo);
        existing.addFullWidth(Ui.muted("Pick a guest who has stayed before."));
        existing.addFiller();

        guestCardPanel.setOpaque(false);
        guestCardPanel.add(fresh, NEW_CARD);
        guestCardPanel.add(existing, EXISTING_CARD);

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(Ui.flow(FlowLayout.LEFT, newGuest, existingGuest), BorderLayout.NORTH);
        body.add(guestCardPanel, BorderLayout.CENTER);
        return Ui.section("1. Guest", body);
    }

    private JComponent buildStaySection() {
        searchButton.setMnemonic(KeyEvent.VK_S);
        Ui.describe(searchButton, "Find rooms that are free for the chosen dates and number of guests");
        Ui.describe(checkInField, "Arrival date. Use the arrow keys to change the day.");
        Ui.describe(checkOutField, "Departure date. Must be after check-in.");

        FormPanel form = new FormPanel();
        form.addField("Check-in", KeyEvent.VK_K, checkInField);
        form.addField("Check-out", KeyEvent.VK_O, checkOutField);
        form.addField("Guests", KeyEvent.VK_G, guestsSpinner);
        form.addField("Room type", KeyEvent.VK_R, typeCombo);
        form.addFullWidth(searchButton);
        return Ui.section("2. Stay details", form);
    }

    private JComponent buildRoomSection() {
        roomList.setCellRenderer(new RoomRenderer());
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomList.setVisibleRowCount(4);
        roomList.setBackground(Theme.surface());
        roomList.setForeground(Theme.text());
        roomList.setSelectionBackground(Theme.primary());
        roomList.setSelectionForeground(Theme.onAccent());
        roomList.getAccessibleContext().setAccessibleName("Available rooms");

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(resultsLabel, BorderLayout.NORTH);
        JScrollPane listScroll = Ui.scroll(roomList);
        listScroll.setPreferredSize(new Dimension(0, Theme.scaled(190)));  // room for ~4 rooms, even when empty
        body.add(listScroll, BorderLayout.CENTER);
        return Ui.section("3. Choose a room", body);
    }

    private JComponent buildExtrasSection() {
        ButtonGroup group = new ButtonGroup();
        JPanel payments = Ui.flow(FlowLayout.LEFT);
        for (PaymentMethod method : PaymentMethod.values()) {
            JRadioButton button = Ui.radio(method.getLabel(), 0);
            group.add(button);
            paymentButtons.put(method, button);
            payments.add(button);
        }
        paymentButtons.get(PaymentMethod.CASH).setSelected(true);
        payments.getAccessibleContext().setAccessibleName("Payment method");

        FormPanel form = new FormPanel();
        form.addFullWidth(breakfastBox);
        form.addFullWidth(pickupBox);
        form.addFullWidth(Ui.label("Payment method"));
        form.addFullWidth(payments);
        form.addField("Notes", KeyEvent.VK_D, notesField);
        return Ui.section("4. Extras & payment", form);
    }

    private JComponent buildSummarySection() {
        JPanel grid = new JPanel(new GridLayout(0, 2, 8, 6));
        grid.setOpaque(false);
        addSummaryRow(grid, "Nights", nightsValue);
        addSummaryRow(grid, "Room", roomValue);
        addSummaryRow(grid, "Extras", extrasValue);
        JLabel totalLabel = addSummaryRow(grid, "Total", totalValue);
        totalLabel.setFont(Theme.font(Font.BOLD, 15f));
        totalValue.setFont(Theme.font(Font.BOLD, 15f));

        clearButton.setMnemonic(KeyEvent.VK_L);
        confirmButton.setMnemonic(KeyEvent.VK_C);
        Ui.describe(confirmButton, "Save this booking (Ctrl+Enter)");

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(grid, BorderLayout.CENTER);
        body.add(Ui.flow(FlowLayout.RIGHT, clearButton, confirmButton), BorderLayout.SOUTH);
        return Ui.section("5. Summary", body);
    }

    private static JLabel addSummaryRow(JPanel grid, String name, JLabel value) {
        JLabel label = Ui.label(name);
        label.setLabelFor(value);
        grid.add(label);
        grid.add(value);
        return label;
    }

    private static JLabel valueLabel() {
        JLabel label = Ui.label("-");
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    // ---- events ----

    private void wireEvents() {
        ItemListener guestToggle = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                guestCards.show(guestCardPanel, existingGuest.isSelected() ? EXISTING_CARD : NEW_CARD);
            }
        };
        newGuest.addItemListener(guestToggle);
        existingGuest.addItemListener(guestToggle);

        checkInField.addChangeListener(e -> {
            if (!checkOutField.getDate().isAfter(checkInField.getDate())) {
                checkOutField.setDate(checkInField.getDate().plusDays(1));
            }
            searchCriteriaChanged("Dates changed. Press Search to see available rooms.");
        });
        checkOutField.addChangeListener(e -> searchCriteriaChanged("Dates changed. Press Search to see available rooms."));
        guestsSpinner.addChangeListener(e -> searchCriteriaChanged("Number of guests changed. Press Search again."));
        typeCombo.addActionListener(e -> searchCriteriaChanged("Room type changed. Press Search again."));
        searchButton.addActionListener(e -> searchRooms());

        roomList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateSummary();
        });
        breakfastBox.addItemListener(e -> updateSummary());
        pickupBox.addItemListener(e -> updateSummary());

        clearButton.addActionListener(e -> {
            if (Dialogs.confirm(this, "Clear the form?", "Everything you have entered on this booking will be removed.",
                    "Clear form", true)) {
                resetForm();
            }
        });
        confirmButton.addActionListener(e -> confirmBooking());

        // Ctrl+Enter confirms from anywhere on the page.
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "confirmBooking");
        getActionMap().put("confirmBooking", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (confirmButton.isEnabled()) confirmBooking();
            }
        });
    }

    private void searchCriteriaChanged(String message) {
        roomModel.clear();
        resultsLabel.setText(message);
        updateSummary();
    }

    private void searchRooms() {
        attempt(() -> {
            List<Room> found = controller.findAvailableRooms(checkInField.getDate(), checkOutField.getDate(),
                    selectedType(), guests());
            roomModel.clear();
            found.forEach(roomModel::addElement);
            if (found.isEmpty()) {
                resultsLabel.setText("No rooms are free for those dates. Try other dates or another room type.");
            } else {
                resultsLabel.setText(found.size() + " room(s) available. Select one to see the price.");
                roomList.requestFocusInWindow();
            }
        });
        updateSummary();
    }

    private void updateSummary() {
        Room room = roomList.getSelectedValue();
        boolean included = room != null && room.includesBreakfast();
        breakfastBox.setEnabled(!included);
        breakfastBox.setText(included ? "Breakfast (included with this room)" : breakfastText);

        BookingController.Quote quote = controller.quote(room, checkInField.getDate(), checkOutField.getDate(),
                guests(), breakfastBox.isSelected(), pickupBox.isSelected());
        nightsValue.setText(String.valueOf(quote.nights()));
        roomValue.setText(room == null ? "Select a room" : Formats.money(quote.roomCost()));
        extrasValue.setText(Formats.money(quote.extrasCost()));
        totalValue.setText(Formats.money(quote.total()));
        confirmButton.setEnabled(room != null);
    }

    private void confirmBooking() {
        Room room = roomList.getSelectedValue();
        attempt(() -> {
            Customer guest = existingGuest.isSelected() ? (Customer) customerCombo.getSelectedItem() : newCustomer();
            Booking booking = controller.create(guest, room, checkInField.getDate(), checkOutField.getDate(),
                    guests(), breakfastBox.isSelected(), pickupBox.isSelected(), selectedPayment(),
                    notesField.getText());
            Dialogs.success(this, "Booking confirmed", receipt(booking));
            resetForm();
        });
    }

    // ---- helpers ----

    private void loadCustomers() {
        attempt(() -> {
            Object selected = customerCombo.getSelectedItem();
            List<Customer> all = customerController.getAll();
            customerCombo.setModel(new DefaultComboBoxModel<>(all.toArray(new Customer[0])));
            if (selected != null) customerCombo.setSelectedItem(selected);
            existingGuest.setEnabled(!all.isEmpty());
        });
    }

    private void resetForm() {
        nameField.setText("");
        phoneField.setText("");
        emailField.setText("");
        idField.setText("");
        nationalityCombo.setSelectedIndex(0);
        newGuest.setSelected(true);
        checkInField.setDate(LocalDate.now());
        checkOutField.setDate(LocalDate.now().plusDays(1));
        guestsSpinner.setValue(1);
        typeCombo.setSelectedIndex(0);
        breakfastBox.setSelected(false);
        pickupBox.setSelected(false);
        paymentButtons.get(PaymentMethod.CASH).setSelected(true);
        notesField.setText("");
        searchCriteriaChanged(START_HINT);
        nameField.requestFocusInWindow();
    }

    private Customer newCustomer() {
        Customer c = new Customer();
        c.setFullName(nameField.getText());
        c.setPhone(phoneField.getText());
        c.setEmail(emailField.getText());
        c.setIdNumber(idField.getText());
        c.setNationality(Objects.toString(nationalityCombo.getSelectedItem(), ""));
        return c;
    }

    private String selectedType() {
        int index = typeCombo.getSelectedIndex();
        return index <= 0 ? null : Room.TYPES[index - 1];
    }

    private int guests() {
        return (Integer) guestsSpinner.getValue();
    }

    private PaymentMethod selectedPayment() {
        return paymentButtons.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(PaymentMethod.CASH);
    }

    private static String receipt(Booking b) {
        return String.join("\n",
                "Reference:  " + b.getReference(),
                "Guest:  " + b.getCustomer().getFullName(),
                "Room:  " + b.getRoom(),
                "Stay:  " + Formats.date(b.getCheckIn()) + "  to  " + Formats.date(b.getCheckOut())
                        + "  (" + b.getNights() + " night" + (b.getNights() == 1 ? "" : "s") + ")",
                "Guests:  " + b.getGuests(),
                "Payment:  " + b.getPaymentMethod(),
                "",
                "Total:  " + Formats.money(b.getTotalAmount()));
    }

    /** Two-line list entry: room number and type, then price and amenities. */
    private static final class RoomRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Room room) {
                String details = Formats.money(room.getPricePerNight()) + " / night  |  "
                        + String.join(", ", room.getAmenities());
                String detailColor = Theme.hex(isSelected ? Theme.onAccent() : Theme.mutedText());
                setText("<html><b>Room " + room.getRoomNumber() + "</b>  " + room.getDisplayName()
                        + ", sleeps " + room.getCapacity()
                        + "<br><font color='" + detailColor + "'>" + details + "</font></html>");
                getAccessibleContext().setAccessibleName("Room " + room.getRoomNumber() + ", "
                        + room.getDisplayName() + ", sleeps " + room.getCapacity() + ", " + details);
            }
            setBorder(new EmptyBorder(6, 10, 6, 10));
            return this;
        }
    }
}
