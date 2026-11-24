package com.hotel.ui;

import com.hotel.controller.BookingController;
import com.hotel.controller.EventBus;
import com.hotel.controller.Session;
import com.hotel.model.Booking;
import com.hotel.model.BookingStatus;
import com.hotel.ui.components.BaseButton;
import com.hotel.ui.components.DangerButton;
import com.hotel.ui.components.ListTableModel;
import com.hotel.ui.components.PrimaryButton;
import com.hotel.util.Formats;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

/**
 * All bookings in a sortable, searchable table. Staff check guests in and out, cancel or
 * delete bookings. Double-click or Enter opens details; right-click opens a context menu.
 */
public class BookingsPanel extends BasePanel {

    private static final String ALL_STATUSES = "All statuses";

    private final BookingController controller = new BookingController();

    private final ListTableModel<Booking> model = new ListTableModel<>(
            new String[]{"Ref", "Guest", "Phone", "Room", "Type", "Check-in", "Check-out", "Nights", "Guests",
                    "Total", "Status"},
            new Class<?>[]{String.class, String.class, String.class, String.class, String.class, LocalDate.class,
                    LocalDate.class, Long.class, Integer.class, BigDecimal.class, BookingStatus.class}) {
        @Override
        protected Object valueAt(Booking b, int column) {
            return switch (column) {
                case 0 -> b.getReference();
                case 1 -> b.getCustomer().getFullName();
                case 2 -> b.getCustomer().getPhone();
                case 3 -> b.getRoom().getRoomNumber();
                case 4 -> b.getRoom().getDisplayName();
                case 5 -> b.getCheckIn();
                case 6 -> b.getCheckOut();
                case 7 -> b.getNights();
                case 8 -> b.getGuests();
                case 9 -> b.getTotalAmount();
                default -> b.getStatus();
            };
        }
    };

    private final JTable table = new JTable(model);
    private final TableRowSorter<ListTableModel<Booking>> sorter = new TableRowSorter<>(model);
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<Object> statusFilter = new JComboBox<>();
    private final JLabel countLabel = Ui.muted(" ");

    private final PrimaryButton checkInButton = new PrimaryButton("Check in");
    private final PrimaryButton checkOutButton = new PrimaryButton("Check out");
    private final DangerButton cancelButton = new DangerButton("Cancel booking");
    private final DangerButton deleteButton = new DangerButton("Delete");
    private final BaseButton detailsButton = new BaseButton("Details");
    private final BaseButton refreshButton = new BaseButton("Refresh");

    private final JMenuItem checkInItem = new JMenuItem("Check in");
    private final JMenuItem checkOutItem = new JMenuItem("Check out");
    private final JMenuItem cancelItem = new JMenuItem("Cancel booking");
    private final JMenuItem detailsItem = new JMenuItem("Details");
    private final JPopupMenu popup = new JPopupMenu();

    public BookingsPanel() {
        statusFilter.addItem(ALL_STATUSES);
        for (BookingStatus status : BookingStatus.values()) {
            statusFilter.addItem(status);
        }

        JLabel searchLabel = Ui.label("Search:");
        searchLabel.setDisplayedMnemonic(KeyEvent.VK_E);
        searchLabel.setLabelFor(searchField);
        Ui.describe(searchField, "Type a reference, guest name, phone number or room number");
        Ui.describe(statusFilter, "Show only bookings with this status");
        headerTools.add(searchLabel);
        headerTools.add(searchField);
        headerTools.add(statusFilter);
        headerTools.add(refreshButton);

        Ui.styleTable(table, "Bookings");
        table.setRowSorter(sorter);
        sorter.toggleSortOrder(5);  // check-in date
        sorter.toggleSortOrder(5);  // newest first

        checkInButton.setMnemonic(KeyEvent.VK_I);
        checkOutButton.setMnemonic(KeyEvent.VK_O);
        cancelButton.setMnemonic(KeyEvent.VK_N);
        detailsButton.setMnemonic(KeyEvent.VK_D);
        deleteButton.setVisible(Session.isAdmin());

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.add(countLabel, BorderLayout.WEST);
        actions.add(Ui.flow(FlowLayout.RIGHT, detailsButton, checkInButton, checkOutButton, cancelButton, deleteButton),
                BorderLayout.EAST);

        popup.add(detailsItem);
        popup.addSeparator();
        popup.add(checkInItem);
        popup.add(checkOutItem);
        popup.add(cancelItem);

        add(Ui.scroll(table), BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);

        wireEvents();
        updateButtons();
        EventBus.subscribe(EventBus.Topic.BOOKINGS_CHANGED, this::refresh);
    }

    @Override
    public String getTitle() {
        return "Bookings";
    }

    @Override
    public String getSubtitle() {
        return "Check guests in and out, or cancel reservations";
    }

    @Override
    public void refresh() {
        attempt(() -> {
            Booking previous = selected();
            model.setRows(controller.getAll());
            if (previous != null) {
                Ui.selectRow(table, model, b -> b.getId() == previous.getId());
            }
            applyFilter();
        });
    }

    private void wireEvents() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        statusFilter.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) applyFilter();
        });
        refreshButton.addActionListener(e -> refresh());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateButtons();
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && selected() != null) {
                    showDetails();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }
        });

        // Enter opens details instead of moving to the next row.
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "showDetails");
        table.getActionMap().put("showDetails", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selected() != null) showDetails();
            }
        });

        detailsButton.addActionListener(e -> showDetails());
        checkInButton.addActionListener(e -> checkIn());
        checkOutButton.addActionListener(e -> checkOut());
        cancelButton.addActionListener(e -> cancel());
        deleteButton.addActionListener(e -> delete());
        detailsItem.addActionListener(e -> showDetails());
        checkInItem.addActionListener(e -> checkIn());
        checkOutItem.addActionListener(e -> checkOut());
        cancelItem.addActionListener(e -> cancel());
    }

    private void maybeShowPopup(MouseEvent e) {
        if (!e.isPopupTrigger()) return;
        int row = table.rowAtPoint(e.getPoint());
        if (row < 0) return;
        table.setRowSelectionInterval(row, row);
        updateButtons();
        popup.show(table, e.getX(), e.getY());
    }

    private void applyFilter() {
        String text = searchField.getText().trim().toLowerCase(Locale.ROOT);
        Object status = statusFilter.getSelectedItem();
        sorter.setRowFilter(new RowFilter<ListTableModel<Booking>, Integer>() {
            @Override
            public boolean include(Entry<? extends ListTableModel<Booking>, ? extends Integer> entry) {
                Booking b = model.getRow(entry.getIdentifier());
                if (status instanceof BookingStatus wanted && b.getStatus() != wanted) {
                    return false;
                }
                return text.isEmpty()
                        || b.getReference().toLowerCase(Locale.ROOT).contains(text)
                        || b.getCustomer().getFullName().toLowerCase(Locale.ROOT).contains(text)
                        || b.getCustomer().getPhone().contains(text)
                        || b.getRoom().getRoomNumber().toLowerCase(Locale.ROOT).contains(text);
            }
        });
        countLabel.setText("Showing " + table.getRowCount() + " of " + model.getRowCount() + " bookings");
        updateButtons();
    }

    private Booking selected() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? null : model.getRow(table.convertRowIndexToModel(viewRow));
    }

    private void updateButtons() {
        Booking b = selected();
        boolean confirmed = b != null && b.getStatus() == BookingStatus.CONFIRMED;
        boolean checkedIn = b != null && b.getStatus() == BookingStatus.CHECKED_IN;
        detailsButton.setEnabled(b != null);
        checkInButton.setEnabled(confirmed);
        checkOutButton.setEnabled(checkedIn);
        cancelButton.setEnabled(confirmed);
        deleteButton.setEnabled(b != null && !checkedIn);
        detailsItem.setEnabled(b != null);
        checkInItem.setEnabled(confirmed);
        checkOutItem.setEnabled(checkedIn);
        cancelItem.setEnabled(confirmed);
    }

    // ---- actions ----

    private void checkIn() {
        Booking b = selected();
        if (b == null) return;
        if (attempt(() -> controller.checkIn(b))) {
            Dialogs.success(this, "Guest checked in", String.join("\n",
                    b.getCustomer().getFullName() + " is now checked in. Room "
                            + b.getRoom().getRoomNumber() + " is marked as occupied.",
                    "",
                    "Room:  " + b.getRoom(),
                    "Check-out:  " + Formats.date(b.getCheckOut())));
        }
    }

    private void checkOut() {
        Booking b = selected();
        if (b == null) return;
        if (Dialogs.confirm(this, "Check out guest?", String.join("\n",
                b.getCustomer().getFullName() + " will be checked out and the room becomes available again.",
                "",
                "Room:  " + b.getRoom(),
                "Nights:  " + b.getNights(),
                "Amount for stay:  " + Formats.money(b.getTotalAmount())), "Check out", false)) {
            attempt(() -> controller.checkOut(b));
        }
    }

    private void cancel() {
        Booking b = selected();
        if (b == null) return;
        if (Dialogs.confirm(this, "Cancel this booking?", String.join("\n",
                "The room will become free for these dates again.",
                "",
                "Booking:  " + b.getReference(),
                "Guest:  " + b.getCustomer().getFullName(),
                "Dates:  " + Formats.date(b.getCheckIn()) + " to " + Formats.date(b.getCheckOut())),
                "Yes, cancel it", true)) {
            attempt(() -> controller.cancel(b));
        }
    }

    private void delete() {
        Booking b = selected();
        if (b == null) return;
        if (Dialogs.confirm(this, "Delete booking " + b.getReference() + "?",
                "This permanently removes the booking and cannot be undone. Cancelling is usually better, "
                        + "because it keeps the record.", "Delete booking", true)) {
            attempt(() -> controller.delete(b));
        }
    }

    private void showDetails() {
        Booking b = selected();
        if (b == null) return;
        Dialogs.info(this, "Booking " + b.getReference(), String.join("\n",
                "Guest:  " + b.getCustomer().getFullName(),
                "Phone:  " + b.getCustomer().getPhone(),
                "Email:  " + (b.getCustomer().getEmail().isEmpty() ? "-" : b.getCustomer().getEmail()),
                "",
                "Room:  " + b.getRoom(),
                "Check-in:  " + Formats.date(b.getCheckIn()),
                "Check-out:  " + Formats.date(b.getCheckOut()),
                "Nights:  " + b.getNights(),
                "Guests:  " + b.getGuests(),
                "Breakfast:  " + (b.isBreakfast() ? "Yes" : "No"),
                "Airport pickup:  " + (b.isAirportPickup() ? "Yes" : "No"),
                "",
                "Payment:  " + b.getPaymentMethod(),
                "Total:  " + Formats.money(b.getTotalAmount()),
                "Status:  " + b.getStatus(),
                "Notes:  " + (b.getNotes().isBlank() ? "-" : b.getNotes()),
                "Booked:  " + Formats.dateTime(b.getCreatedAt())));
    }
}
