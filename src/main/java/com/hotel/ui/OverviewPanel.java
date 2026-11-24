package com.hotel.ui;

import com.hotel.controller.BookingController;
import com.hotel.controller.EventBus;
import com.hotel.controller.RoomController;
import com.hotel.controller.Session;
import com.hotel.model.Booking;
import com.hotel.model.BookingStatus;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.model.User;
import com.hotel.ui.components.BaseButton;
import com.hotel.ui.components.FilterChip;
import com.hotel.ui.components.PrimaryButton;
import com.hotel.ui.components.RoomCard;
import com.hotel.ui.components.RoundedPanel;
import com.hotel.ui.components.ScrollablePanel;
import com.hotel.ui.components.StatCard;
import com.hotel.ui.components.StatusBadge;
import com.hotel.util.Formats;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Dashboard home: key numbers, a filterable room board grouped by floor, and a
 * "Today" panel listing arrivals and departures.
 */
public class OverviewPanel extends BasePanel {

    private static final int GRID_GAP = 14;
    private static final int MAX_LIST_ROWS = 4;

    private final Navigator navigator;
    private final RoomController roomController = new RoomController();
    private final BookingController bookingController = new BookingController();

    private final StatCard occupancyCard = new StatCard("Occupancy", Theme.primary());
    private final StatCard availableCard = new StatCard("Available rooms", Theme.success());
    private final StatCard arrivalsCard = new StatCard("Arrivals today", Theme.warning());
    private final StatCard departuresCard = new StatCard("Departures today", Theme.danger());
    // Money figures are for administrators only; receptionists see guests in house instead.
    private final boolean showRevenue = Session.isAdmin();
    private final StatCard revenueCard = new StatCard(showRevenue ? "Booked revenue" : "Guests in house", Theme.primary());

    private final FilterChip allChip = new FilterChip("All");
    private final FilterChip availableChip = new FilterChip("Available");
    private final FilterChip occupiedChip = new FilterChip("Occupied");
    private final FilterChip maintenanceChip = new FilterChip("Maintenance");

    private final ScrollablePanel roomsContent = new ScrollablePanel(new GridBagLayout());
    private final JScrollPane roomsScroll = new JScrollPane(roomsContent);

    private final JPanel arrivalsList = listPanel();
    private final JPanel departuresList = listPanel();
    private final StatusBadge arrivalsCount = new StatusBadge("0", Theme.warning());
    private final StatusBadge departuresCount = new StatusBadge("0", Theme.danger());

    private List<Room> rooms = List.of();
    private List<Booking> bookings = List.of();
    private int columns = 3;

    public OverviewPanel(Navigator navigator) {
        this.navigator = navigator;

        PrimaryButton newBooking = new PrimaryButton("+  New booking");
        newBooking.setMnemonic(KeyEvent.VK_N);
        Ui.describe(newBooking, "Start a new booking (Ctrl+N)");
        newBooking.addActionListener(e -> navigator.showPage(DashboardFrame.NEW_BOOKING));
        headerTools.add(newBooking);

        JPanel stats = new JPanel(new GridLayout(1, 0, 16, 0));
        stats.setOpaque(false);
        stats.add(occupancyCard);
        stats.add(availableCard);
        stats.add(arrivalsCard);
        stats.add(departuresCard);
        stats.add(revenueCard);

        JPanel main = new JPanel(new BorderLayout(20, 0));
        main.setOpaque(false);
        main.add(buildRoomsArea(), BorderLayout.CENTER);
        main.add(buildTodayPanel(), BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(0, 20));
        body.setOpaque(false);
        body.add(stats, BorderLayout.NORTH);
        body.add(main, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        EventBus.subscribe(EventBus.Topic.ROOMS_CHANGED, this::refresh);
        EventBus.subscribe(EventBus.Topic.BOOKINGS_CHANGED, this::refresh);
    }

    @Override
    public String getTitle() {
        return "Overview";
    }

    @Override
    public String getSubtitle() {
        int hour = LocalTime.now().getHour();
        String greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
        User me = Session.currentUser();
        return greeting + (me == null ? "" : ", " + me.getFullName()) + ". Here is the hotel at a glance.";
    }

    // ---- layout ----

    private JComponent buildRoomsArea() {
        ButtonGroup group = new ButtonGroup();
        for (FilterChip chip : new FilterChip[]{allChip, availableChip, occupiedChip, maintenanceChip}) {
            group.add(chip);
            chip.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) renderRooms();
            });
        }
        allChip.setSelected(true);
        Ui.describe(allChip, "Show all rooms");
        Ui.describe(availableChip, "Show only available rooms");
        Ui.describe(occupiedChip, "Show only occupied rooms");
        Ui.describe(maintenanceChip, "Show only rooms under maintenance");

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Ui.heading("Rooms", 16f), BorderLayout.WEST);
        header.add(Ui.flow(FlowLayout.RIGHT, allChip, availableChip, occupiedChip, maintenanceChip), BorderLayout.EAST);

        roomsContent.getAccessibleContext().setAccessibleName("Rooms by floor");
        roomsScroll.setBorder(null);
        roomsScroll.setOpaque(false);
        roomsScroll.getViewport().setOpaque(false);
        roomsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        roomsScroll.getVerticalScrollBar().setUnitIncrement(20);
        // Fit as many card columns as the width allows (ComponentListener).
        roomsScroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int fit = Math.max(1, (e.getComponent().getWidth() + GRID_GAP) / (Theme.scaled(215) + GRID_GAP));
                if (fit != columns) {
                    columns = fit;
                    renderRooms();
                }
            }
        });

        JPanel area = new JPanel(new BorderLayout(0, 12));
        area.setOpaque(false);
        area.add(header, BorderLayout.NORTH);
        area.add(roomsScroll, BorderLayout.CENTER);
        return area;
    }

    private JComponent buildTodayPanel() {
        JPanel titles = new JPanel(new GridLayout(0, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(Ui.heading("Today", 16f));
        titles.add(Ui.muted(Formats.longDate(LocalDate.now())));

        JSeparator divider = new JSeparator();
        divider.setForeground(Theme.border());

        JPanel lists = Ui.stack(10,
                listHeader("Arrivals", arrivalsCount), arrivalsList,
                divider,
                listHeader("Departures", departuresCount), departuresList);

        BaseButton viewAll = new BaseButton("View all bookings");
        viewAll.setMnemonic(KeyEvent.VK_V);
        viewAll.addActionListener(e -> navigator.showPage(DashboardFrame.BOOKINGS));

        RoundedPanel panel = new RoundedPanel(new BorderLayout(0, 16), 14);
        panel.setPadding(16, 18);
        panel.add(titles, BorderLayout.NORTH);
        panel.add(lists, BorderLayout.CENTER);
        panel.add(viewAll, BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(Theme.scaled(300), 0));
        panel.getAccessibleContext().setAccessibleName("Today's arrivals and departures");
        return panel;
    }

    private static JPanel listHeader(String title, StatusBadge count) {
        JLabel label = Ui.label(title);
        label.setFont(Theme.font(Font.BOLD, 13f));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(label, BorderLayout.WEST);
        header.add(count, BorderLayout.EAST);
        return header;
    }

    private static JPanel listPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 2));
        panel.setOpaque(false);
        return panel;
    }

    // ---- data ----

    @Override
    public void refresh() {
        attempt(() -> {
            rooms = roomController.getAll();
            bookings = bookingController.getAll();
            LocalDate today = LocalDate.now();

            int total = rooms.size();
            long available = count(RoomStatus.AVAILABLE);
            long occupied = count(RoomStatus.OCCUPIED);
            long maintenance = count(RoomStatus.MAINTENANCE);

            List<Booking> arrivals = bookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED && b.getCheckIn().equals(today))
                    .toList();
            List<Booking> departures = bookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CHECKED_IN && !b.getCheckOut().isAfter(today))
                    .toList();
            BigDecimal revenue = bookings.stream()
                    .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                    .map(Booking::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long active = bookings.stream().filter(b -> b.getStatus().isActive()).count();

            occupancyCard.setValue(total == 0 ? "0%" : Math.round(occupied * 100.0 / total) + "%");
            occupancyCard.setCaption(occupied + " of " + total + " rooms occupied");
            occupancyCard.setProgress(total == 0 ? 0 : occupied / (double) total);
            availableCard.setValue(String.valueOf(available));
            availableCard.setCaption(maintenance == 0 ? "All rooms in service" : maintenance + " in maintenance");
            arrivalsCard.setValue(String.valueOf(arrivals.size()));
            arrivalsCard.setCaption(arrivals.isEmpty() ? "No guests expected" : "Waiting to check in");
            departuresCard.setValue(String.valueOf(departures.size()));
            departuresCard.setCaption(departures.isEmpty() ? "No check-outs due" : "Due to check out");
            if (showRevenue) {
                revenueCard.setValue(Formats.moneyCompact(revenue));
                revenueCard.setCaption(active + " active booking" + (active == 1 ? "" : "s"));
            } else {
                int inHouse = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CHECKED_IN)
                        .mapToInt(Booking::getGuests).sum();
                revenueCard.setValue(String.valueOf(inHouse));
                revenueCard.setCaption(active + " active booking" + (active == 1 ? "" : "s"));
            }

            allChip.setText("All  " + total);
            availableChip.setText("Available  " + available);
            occupiedChip.setText("Occupied  " + occupied);
            maintenanceChip.setText("Maintenance  " + maintenance);

            arrivalsCount.setText(String.valueOf(arrivals.size()));
            departuresCount.setText(String.valueOf(departures.size()));
            fillList(arrivalsList, arrivals, b -> "Room " + b.getRoom().getRoomNumber() + "  ·  "
                    + b.getGuests() + " guest" + (b.getGuests() == 1 ? "" : "s") + "  ·  "
                    + b.getNights() + " night" + (b.getNights() == 1 ? "" : "s"), "No arrivals today");
            fillList(departuresList, departures, b -> "Room " + b.getRoom().getRoomNumber() + "  ·  "
                    + (b.getCheckOut().isBefore(today) ? "overdue since " + Formats.shortDate(b.getCheckOut())
                    : "checking out today"), "No departures today");

            renderRooms();
        });
    }

    private long count(RoomStatus status) {
        return rooms.stream().filter(r -> r.getStatus() == status).count();
    }

    private void fillList(JPanel list, List<Booking> items, Function<Booking, String> detail, String emptyText) {
        list.removeAll();
        if (items.isEmpty()) {
            JLabel empty = Ui.muted(emptyText);
            empty.setBorder(new EmptyBorder(4, 0, 4, 0));
            list.add(empty);
        }
        for (int i = 0; i < Math.min(items.size(), MAX_LIST_ROWS); i++) {
            Booking b = items.get(i);
            JLabel row = new JLabel("<html><b>" + escape(b.getCustomer().getFullName()) + "</b><br>"
                    + "<font color='" + Theme.hex(Theme.mutedText()) + "'>" + escape(detail.apply(b))
                    + "</font></html>");
            row.setForeground(Theme.text());
            row.setBorder(new EmptyBorder(5, 0, 5, 0));
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            row.setToolTipText("Open bookings");
            row.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    navigator.showPage(DashboardFrame.BOOKINGS);
                }
            });
            list.add(row);
        }
        if (items.size() > MAX_LIST_ROWS) {
            list.add(Ui.muted("+ " + (items.size() - MAX_LIST_ROWS) + " more"));
        }
        list.revalidate();
        list.repaint();
    }

    private void renderRooms() {
        roomsContent.removeAll();
        RoomStatus wanted = availableChip.isSelected() ? RoomStatus.AVAILABLE
                : occupiedChip.isSelected() ? RoomStatus.OCCUPIED
                : maintenanceChip.isSelected() ? RoomStatus.MAINTENANCE
                : null;

        Map<Integer, List<Room>> byFloor = new TreeMap<>();
        for (Room room : rooms) {
            if (wanted == null || room.getStatus() == wanted) {
                byFloor.computeIfAbsent(room.getFloor(), f -> new ArrayList<>()).add(room);
            }
        }

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        int row = 0;

        for (Map.Entry<Integer, List<Room>> floor : byFloor.entrySet()) {
            int n = floor.getValue().size();
            JLabel title = Ui.label((floor.getKey() == 0 ? "Ground floor" : "Floor " + floor.getKey())
                    + "   ");
            title.setFont(Theme.font(Font.BOLD, 13f));
            JPanel floorHeader = Ui.flow(FlowLayout.LEFT, title, Ui.muted(n + " room" + (n == 1 ? "" : "s")));
            ((FlowLayout) floorHeader.getLayout()).setHgap(0);
            c.gridy = row++;
            c.insets = new Insets(row == 1 ? 0 : 18, 0, 8, 0);
            roomsContent.add(floorHeader, c);

            JPanel grid = new JPanel(new GridLayout(0, columns, GRID_GAP, GRID_GAP));
            grid.setOpaque(false);
            for (Room room : floor.getValue()) {
                grid.add(new RoomCard(room, detailFor(room), this::openRoom));
            }
            c.gridy = row++;
            c.insets = new Insets(0, 0, 0, 0);
            roomsContent.add(grid, c);
        }

        if (byFloor.isEmpty()) {
            c.gridy = row++;
            c.insets = new Insets(8, 0, 0, 0);
            roomsContent.add(Ui.muted(rooms.isEmpty()
                    ? "No rooms yet. An administrator can add them on the Rooms page."
                    : "No rooms match this filter."), c);
        }

        c.gridy = row;
        c.weighty = 1;
        roomsContent.add(Box.createGlue(), c);
        roomsContent.revalidate();
        roomsContent.repaint();
    }

    /** One line of context for a room card: who is in it, who is arriving, or why it is closed. */
    private String detailFor(Room room) {
        LocalDate today = LocalDate.now();
        for (Booking b : bookings) {
            if (b.getRoom().getId() == room.getId() && b.getStatus() == BookingStatus.CHECKED_IN) {
                return b.getCustomer().getFullName() + "  ·  until " + Formats.shortDate(b.getCheckOut());
            }
        }
        for (Booking b : bookings) {
            if (b.getRoom().getId() == room.getId() && b.getStatus() == BookingStatus.CONFIRMED
                    && b.getCheckIn().equals(today)) {
                return "Arriving today: " + b.getCustomer().getFullName();
            }
        }
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            return room.getDescription().isBlank() ? "Out of service" : room.getDescription();
        }
        return "Free tonight";
    }

    private void openRoom(Room room) {
        boolean canChangeStatus = Session.isAdmin() && room.getStatus() != RoomStatus.OCCUPIED;
        RoomDetailsDialog.Action action = new RoomDetailsDialog(SwingUtilities.getWindowAncestor(this),
                room, detailFor(room), room.isBookable(), canChangeStatus).open();
        if (action == null) return;

        switch (action) {
            case BOOK -> navigator.bookRoom(room);
            case MAINTENANCE -> attempt(() -> roomController.setStatus(room, RoomStatus.MAINTENANCE));
            case AVAILABLE -> attempt(() -> roomController.setStatus(room, RoomStatus.AVAILABLE));
        }
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
