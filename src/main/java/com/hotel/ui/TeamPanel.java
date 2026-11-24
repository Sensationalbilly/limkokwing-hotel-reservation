package com.hotel.ui;

import com.hotel.controller.AttendanceController;
import com.hotel.model.Attendance;
import com.hotel.model.StaffPerformance;
import com.hotel.ui.components.ListTableModel;
import com.hotel.ui.components.StarRating;
import com.hotel.ui.components.StatCard;
import com.hotel.util.Formats;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Administrator's view of the team: today's attendance, attendance history, and a performance
 * ranking with the admin's own star rating for each staff member.
 */
public class TeamPanel extends BasePanel {

    private static final int[] RANGE_DAYS = {7, 30, 90};
    private static final String[] RANGE_NAMES = {"Last 7 days", "Last 30 days", "Last 90 days"};

    private final AttendanceController controller = new AttendanceController();
    private final JTabbedPane tabs = new JTabbedPane();

    // Today
    private final StatCard presentCard = new StatCard("Signed in today", Theme.primary());
    private final StatCard onTimeCard = new StatCard("On time", Theme.success());
    private final StatCard lateCard = new StatCard("Late", Theme.danger());
    private final StatCard missingCard = new StatCard("Not in yet", Theme.warning());
    private final ListTableModel<Attendance> todayModel = attendanceModel(false);
    private final JTable todayTable = new JTable(todayModel);

    // History
    private final JComboBox<String> historyRange = new JComboBox<>(RANGE_NAMES);
    private final JComboBox<String> historyStaff = new JComboBox<>();
    private final ListTableModel<Attendance> historyModel = attendanceModel(true);
    private final JTable historyTable = new JTable(historyModel);
    private final TableRowSorter<ListTableModel<Attendance>> historySorter = new TableRowSorter<>(historyModel);

    // Performance
    private final JComboBox<String> performanceRange = new JComboBox<>(RANGE_NAMES);
    private final StatCard topCard = new StatCard("Top performer", Theme.success());
    private final StatCard punctualCard = new StatCard("Most punctual", Theme.primary());
    private final StatCard lateMostCard = new StatCard("Most late arrivals", Theme.danger());
    private final ListTableModel<StaffPerformance> performanceModel = new ListTableModel<>(
            new String[]{"Staff", "Score", "Days in", "On time", "Late days", "Avg. arrival", "Bookings",
                    "Check-ins", "Check-outs", "New guests", "Booking value", "Your rating"},
            new Class<?>[]{String.class, Integer.class, Integer.class, String.class, Integer.class, String.class,
                    Integer.class, Integer.class, Integer.class, Integer.class, BigDecimal.class, Integer.class}) {
        @Override
        protected Object valueAt(StaffPerformance p, int column) {
            return switch (column) {
                case 0 -> p.user().getFullName();
                case 1 -> p.score();
                case 2 -> p.daysPresent();
                case 3 -> p.daysPresent() == 0 ? "-" : p.onTimePercent() + "%";
                case 4 -> p.lateDays();
                case 5 -> p.averageArrival() == null ? "-" : Formats.time(p.averageArrival());
                case 6 -> p.bookings();
                case 7 -> p.checkIns();
                case 8 -> p.checkOuts();
                case 9 -> p.guestsAdded();
                case 10 -> p.revenue();
                default -> p.user().getRating();
            };
        }
    };
    private final JTable performanceTable = new JTable(performanceModel);
    private final StarRating ratingEditor = new StarRating(true);
    private final JLabel ratingLabel = Ui.label("Select a staff member to rate them.");

    public TeamPanel() {
        tabs.addTab("Today", buildTodayTab());
        tabs.addTab("Attendance history", buildHistoryTab());
        tabs.addTab("Performance", buildPerformanceTab());
        tabs.setMnemonicAt(0, KeyEvent.VK_T);
        tabs.setMnemonicAt(1, KeyEvent.VK_H);
        tabs.setMnemonicAt(2, KeyEvent.VK_P);
        tabs.setFont(Theme.font(Font.BOLD, 13f));
        add(tabs, BorderLayout.CENTER);
    }

    @Override
    public String getTitle() {
        return "Team";
    }

    @Override
    public String getSubtitle() {
        return "Attendance and performance  ·  work starts " + Formats.time(AttendanceController.WORK_START)
                + ", late after " + Formats.time(AttendanceController.LATE_AFTER) + " (Freetown time)";
    }

    @Override
    public void refresh() {
        attempt(() -> {
            loadToday();
            loadHistory();
            loadPerformance();
        });
    }

    // ---- Today ----

    private JComponent buildTodayTab() {
        JPanel stats = new JPanel(new GridLayout(1, 0, 14, 0));
        stats.setOpaque(false);
        stats.add(presentCard);
        stats.add(onTimeCard);
        stats.add(lateCard);
        stats.add(missingCard);

        styleAttendanceTable(todayTable, "Today's attendance");
        return tab(stats, Ui.scroll(todayTable), null);
    }

    private void loadToday() throws Exception {
        List<Attendance> today = controller.today();
        todayModel.setRows(today);
        long present = today.stream().filter(Attendance::isPresent).count();
        long late = today.stream().filter(Attendance::isLate).count();
        presentCard.setValue(String.valueOf(present));
        presentCard.setCaption("of " + today.size() + " staff");
        onTimeCard.setValue(String.valueOf(present - late));
        onTimeCard.setCaption("Signed in by " + Formats.time(AttendanceController.LATE_AFTER));
        lateCard.setValue(String.valueOf(late));
        lateCard.setCaption(late == 0 ? "Nobody late today" : "Signed in after " + Formats.time(AttendanceController.LATE_AFTER));
        missingCard.setValue(String.valueOf(today.size() - present));
        missingCard.setCaption("Have not signed in");
    }

    // ---- History ----

    private JComponent buildHistoryTab() {
        historyRange.setSelectedIndex(1);
        historyRange.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) attempt(this::loadHistory);
        });
        historyStaff.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) filterHistory();
        });
        historyRange.getAccessibleContext().setAccessibleName("Period");
        historyStaff.getAccessibleContext().setAccessibleName("Staff member");

        styleAttendanceTable(historyTable, "Attendance history");
        historyTable.setRowSorter(historySorter);

        JPanel filters = Ui.flow(FlowLayout.LEFT, Ui.label("Show:"), historyRange, Ui.label("  Staff:"), historyStaff);
        return tab(filters, Ui.scroll(historyTable), null);
    }

    private void loadHistory() throws Exception {
        List<Attendance> history = controller.history(RANGE_DAYS[historyRange.getSelectedIndex()]);
        historyModel.setRows(history);

        Object selected = historyStaff.getSelectedItem();
        historyStaff.removeAllItems();
        historyStaff.addItem("Everyone");
        history.stream().map(Attendance::getFullName).distinct().sorted().forEach(historyStaff::addItem);
        historyStaff.setSelectedItem(selected == null ? "Everyone" : selected);
        filterHistory();
    }

    private void filterHistory() {
        Object staff = historyStaff.getSelectedItem();
        if (staff == null || "Everyone".equals(staff)) {
            historySorter.setRowFilter(null);
        } else {
            historySorter.setRowFilter(new RowFilter<ListTableModel<Attendance>, Integer>() {
                @Override
                public boolean include(Entry<? extends ListTableModel<Attendance>, ? extends Integer> entry) {
                    return Objects.equals(historyModel.getRow(entry.getIdentifier()).getFullName(), staff);
                }
            });
        }
    }

    // ---- Performance ----

    private JComponent buildPerformanceTab() {
        performanceRange.setSelectedIndex(1);
        performanceRange.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) attempt(this::loadPerformance);
        });
        performanceRange.getAccessibleContext().setAccessibleName("Period");

        JPanel highlights = new JPanel(new GridLayout(1, 0, 14, 0));
        highlights.setOpaque(false);
        highlights.add(topCard);
        highlights.add(punctualCard);
        highlights.add(lateMostCard);

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(Ui.flow(FlowLayout.LEFT, Ui.label("Period:"), performanceRange,
                Ui.muted("   Score = 40% punctuality + 30% attendance + 30% work done (bookings, check-ins, "
                        + "check-outs, new guests)")), BorderLayout.NORTH);
        north.add(highlights, BorderLayout.CENTER);

        Ui.styleTable(performanceTable, "Staff performance");
        performanceTable.setAutoCreateRowSorter(true);
        performanceTable.getColumnModel().getColumn(11).setCellRenderer(new StarRating.CellRenderer());
        performanceTable.getColumnModel().getColumn(11).setPreferredWidth(Theme.scaled(110));
        performanceTable.getColumnModel().getColumn(1).setCellRenderer(new ScoreRenderer());
        performanceTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelectedRating();
        });

        ratingEditor.setEnabled(false);
        ratingEditor.setOnChange(stars -> {
            StaffPerformance p = selectedPerformance();
            if (p == null) return;
            if (attempt(() -> controller.rate(p.user(), stars == 0 ? null : stars))) {
                performanceModel.fireTableDataChanged();
                ratingLabel.setText(stars == 0 ? "Rating cleared for " + p.user().getFullName() + "."
                        : "You rated " + p.user().getFullName() + " " + stars + " out of 5.");
            }
        });
        JLabel rateCaption = Ui.label("Your rating:");
        rateCaption.setFont(Theme.font(Font.BOLD, 13f));
        JPanel ratingRow = Ui.flow(FlowLayout.LEFT, rateCaption, ratingEditor, ratingLabel);

        return tab(north, Ui.scroll(performanceTable), ratingRow);
    }

    private void loadPerformance() throws Exception {
        List<StaffPerformance> list = controller.performance(RANGE_DAYS[performanceRange.getSelectedIndex()]);
        performanceModel.setRows(list);

        StaffPerformance top = list.stream().filter(p -> p.daysPresent() > 0 || p.workDone() > 0).findFirst().orElse(null);
        topCard.setValue(top == null ? "-" : top.user().getFullName());
        topCard.setCaption(top == null ? "No activity yet" : "Score " + top.score() + " / 100");

        StaffPerformance punctual = list.stream().filter(p -> p.daysPresent() > 0)
                .max(Comparator.comparingInt(StaffPerformance::onTimePercent)
                        .thenComparingInt(StaffPerformance::daysPresent)).orElse(null);
        punctualCard.setValue(punctual == null ? "-" : punctual.user().getFullName());
        punctualCard.setCaption(punctual == null ? "No sign-ins yet"
                : punctual.onTimePercent() + "% on time over " + punctual.daysPresent() + " day(s)");

        StaffPerformance latest = list.stream().filter(p -> p.lateDays() > 0)
                .max(Comparator.comparingInt(StaffPerformance::lateDays)).orElse(null);
        lateMostCard.setValue(latest == null ? "Nobody" : latest.user().getFullName());
        lateMostCard.setCaption(latest == null ? "No late arrivals in this period"
                : latest.lateDays() + " late day(s)");

        ratingEditor.setEnabled(false);
        ratingEditor.setValue(0);
        ratingLabel.setText("Select a staff member to rate them.");
    }

    private StaffPerformance selectedPerformance() {
        int row = performanceTable.getSelectedRow();
        return row < 0 ? null : performanceModel.getRow(performanceTable.convertRowIndexToModel(row));
    }

    private void showSelectedRating() {
        StaffPerformance p = selectedPerformance();
        ratingEditor.setEnabled(p != null);
        ratingEditor.setValue(p == null || p.user().getRating() == null ? 0 : p.user().getRating());
        ratingLabel.setText(p == null ? "Select a staff member to rate them."
                : "Click the stars to rate " + p.user().getFullName() + " (click again to clear).");
    }

    // ---- shared ----

    private static JComponent tab(JComponent north, JComponent center, JComponent south) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(Theme.background());
        panel.setBorder(new EmptyBorder(16, 4, 4, 4));
        if (north != null) panel.add(north, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        if (south != null) panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private static ListTableModel<Attendance> attendanceModel(boolean withDate) {
        String[] columns = withDate
                ? new String[]{"Date", "Staff", "Signed in (system)", "Time they entered", "Difference", "Status", "Reason / note", "Signed out"}
                : new String[]{"Staff", "Signed in (system)", "Time they entered", "Difference", "Status", "Reason / note", "Signed out"};
        Class<?>[] types = new Class<?>[columns.length];
        java.util.Arrays.fill(types, String.class);
        if (withDate) types[0] = LocalDate.class;

        return new ListTableModel<>(columns, types) {
            @Override
            protected Object valueAt(Attendance a, int column) {
                int c = withDate ? column : column + 1;
                return switch (c) {
                    case 0 -> a.getWorkDate();
                    case 1 -> a.getFullName();
                    case 2 -> a.isPresent() ? Formats.time(a.getClockIn().toLocalTime()) : "-";
                    case 3 -> !a.isPresent() ? "-" : a.getReportedTime() == null ? "Not entered" : Formats.time(a.getReportedTime());
                    case 4 -> difference(a);
                    case 5 -> status(a);
                    case 6 -> a.getNote().isBlank() ? "" : a.getNote();
                    default -> a.getClockOut() == null ? "" : Formats.time(a.getClockOut().toLocalTime());
                };
            }
        };
    }

    private static String status(Attendance a) {
        if (!a.isPresent()) return "Not in yet";
        return a.isLate() ? "Late " + AttendanceDialog.duration(a.getLateMinutes()) : "On time";
    }

    private static String difference(Attendance a) {
        Integer d = a.reportedDifference();
        if (d == null) return "";
        if (Math.abs(d) <= AttendanceController.MATCH_TOLERANCE_MINUTES) return "Matches";
        return "Typed " + AttendanceDialog.duration(Math.abs(d)) + (d > 0 ? " earlier" : " later");
    }

    private static void styleAttendanceTable(JTable table, String name) {
        Ui.styleTable(table, name);
        StatusTextRenderer renderer = new StatusTextRenderer();
        table.setDefaultRenderer(String.class, renderer);
    }

    /** Colours words like "Late", "On time", "Typed ... earlier" so problems stand out. */
    private static final class StatusTextRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(table.getFont());
            if (isSelected || value == null) return this;
            String text = value.toString();
            if (text.startsWith("Late")) {
                setForeground(Theme.danger());
                setFont(getFont().deriveFont(Font.BOLD));
            } else if (text.equals("On time")) {
                setForeground(Theme.success());
                setFont(getFont().deriveFont(Font.BOLD));
            } else if (text.startsWith("Typed") || text.equals("Not entered")) {
                setForeground(Theme.warning());
                setFont(getFont().deriveFont(Font.BOLD));
            } else if (text.equals("Not in yet")) {
                setForeground(Theme.mutedText());
            } else {
                setForeground(table.getForeground());
            }
            return this;
        }
    }

    /** Score shown as "82 / 100" in a colour from red to green. */
    private static final class ScoreRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            int score = value instanceof Integer s ? s : 0;
            setText(score + " / 100");
            setFont(table.getFont().deriveFont(Font.BOLD));
            if (!isSelected) {
                setForeground(score >= 70 ? Theme.success() : score >= 40 ? Theme.warning() : Theme.danger());
            }
            return this;
        }
    }
}
