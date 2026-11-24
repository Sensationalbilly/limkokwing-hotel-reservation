package com.hotel.controller;

import com.hotel.dao.ActivityDAO;
import com.hotel.dao.AttendanceDAO;
import com.hotel.dao.UserDAO;
import com.hotel.model.Attendance;
import com.hotel.model.StaffPerformance;
import com.hotel.model.User;
import com.hotel.util.Formats;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Staff attendance and performance.
 * <ul>
 *   <li>The first sign-in of the day is the clock-in. Its time comes from the database server's clock
 *       in Freetown time, so it cannot be typed in or changed by staff.</li>
 *   <li>Work starts at 8:00; anyone signing in after 8:30 is late. Lateness is always based on the
 *       recorded time, never on the time the staff member types.</li>
 *   <li>Staff also type the time they say they arrived; the administrator sees both.</li>
 * </ul>
 */
public class AttendanceController {

    public static final ZoneId FREETOWN = ZoneId.of("Africa/Freetown");
    public static final LocalTime WORK_START = LocalTime.of(8, 0);
    public static final LocalTime LATE_AFTER = LocalTime.of(8, 30);
    /** Typed times within this many minutes of the recorded time count as matching. */
    public static final int MATCH_TOLERANCE_MINUTES = 2;

    /** Result of signing in: today's record and whether the staff member still has to confirm it. */
    public record ClockIn(Attendance attendance, boolean needsConfirmation) { }

    private final AttendanceDAO attendance = new AttendanceDAO();
    private final ActivityDAO activity = new ActivityDAO();
    private final UserDAO users = new UserDAO();

    /** Attendance is taken for receptionists/staff, not for administrators. */
    public static boolean isTracked(User user) {
        return user != null && !user.isAdmin();
    }

    /** Current Freetown date and time from the database server's clock. */
    public LocalDateTime now() throws SQLException {
        return attendance.nowUtc().atZone(ZoneOffset.UTC).withZoneSameInstant(FREETOWN).toLocalDateTime();
    }

    /** Minutes after 8:30, or 0 if on time. */
    public static int lateMinutes(LocalTime arrival) {
        LocalTime minute = arrival.truncatedTo(ChronoUnit.MINUTES);
        return minute.isAfter(LATE_AFTER) ? (int) ChronoUnit.MINUTES.between(LATE_AFTER, minute) : 0;
    }

    /** Records today's clock-in at the first sign-in. Later sign-ins the same day keep the first time. */
    public ClockIn clockIn(User user) throws SQLException {
        if (!isTracked(user)) return null;
        LocalDateTime now = now().truncatedTo(ChronoUnit.SECONDS);
        LocalDate today = now.toLocalDate();
        Attendance existing = attendance.findForDay(user.getId(), today);
        if (existing != null) {
            return new ClockIn(existing, existing.getReportedTime() == null);
        }
        attendance.insert(user.getId(), today, now, lateMinutes(now.toLocalTime()));
        return new ClockIn(attendance.findForDay(user.getId(), today), true);
    }

    /** Saves the arrival time the staff member typed. Being late is still decided by the recorded time. */
    public void confirm(Attendance record, LocalTime reportedTime, String note) throws ValidationException, SQLException {
        String text = note == null ? "" : note.trim();
        if (reportedTime == null) {
            throw new ValidationException("Please enter the time you arrived.");
        }
        LocalTime recorded = record.getClockIn().toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        if (reportedTime.isAfter(recorded)) {
            throw new ValidationException("You can't have arrived after you signed in (" + Formats.time(recorded)
                    + "). Please check the time.");
        }
        if (record.isLate() && text.isEmpty()) {
            throw new ValidationException("You signed in after " + Formats.time(LATE_AFTER)
                    + ". Please give a short reason for being late.");
        }
        if (text.length() > 255) {
            throw new ValidationException("Please keep the note under 255 characters.");
        }
        attendance.saveReport(record.getId(), reportedTime, text);
        record.setReportedTime(reportedTime);
        record.setNote(text);
    }

    /** Records the time of leaving when a staff member signs out or closes the app. */
    public void clockOut(User user) throws SQLException {
        if (!isTracked(user)) return;
        LocalDateTime now = now().truncatedTo(ChronoUnit.SECONDS);
        attendance.clockOut(user.getId(), now.toLocalDate(), now);
    }

    // ---- administrator reports ----

    public List<Attendance> today() throws ValidationException, SQLException {
        requireAdmin();
        return attendance.findDay(now().toLocalDate());
    }

    public List<Attendance> history(int days) throws ValidationException, SQLException {
        requireAdmin();
        LocalDate today = now().toLocalDate();
        return attendance.findBetween(today.minusDays(days - 1L), today);
    }

    /**
     * Score out of 100 for each staff member over the last {@code days} days:
     * 40% punctuality (on-time sign-ins), 30% attendance (days present out of days the hotel had
     * staff in) and 30% work done (bookings, check-ins, check-outs and new guests, compared with
     * the busiest staff member).
     */
    public List<StaffPerformance> performance(int days) throws ValidationException, SQLException {
        requireAdmin();
        LocalDate today = now().toLocalDate();
        LocalDate from = today.minusDays(days - 1L);
        List<Attendance> records = attendance.findBetween(from, today);
        List<ActivityDAO.Count> counts = activity.countsSince(from.atStartOfDay());
        long openDays = records.stream().map(Attendance::getWorkDate).distinct().count();

        List<User> staff = users.findAll().stream().filter(AttendanceController::isTracked).toList();
        int busiest = 0;
        for (User u : staff) {
            busiest = Math.max(busiest, (int) (count(counts, u, "BOOKING_CREATED") + count(counts, u, "CHECK_IN")
                    + count(counts, u, "CHECK_OUT") + count(counts, u, "GUEST_ADDED")));
        }

        List<StaffPerformance> result = new ArrayList<>();
        for (User u : staff) {
            List<Attendance> mine = records.stream().filter(a -> a.getUserId() == u.getId()).toList();
            int present = mine.size();
            int late = (int) mine.stream().filter(Attendance::isLate).count();
            int onTime = present - late;
            LocalTime average = present == 0 ? null : LocalTime.ofSecondOfDay(Math.round(mine.stream()
                    .mapToInt(a -> a.getClockIn().toLocalTime().toSecondOfDay()).average().orElse(0)));

            int bookings = (int) count(counts, u, "BOOKING_CREATED");
            int checkIns = (int) count(counts, u, "CHECK_IN");
            int checkOuts = (int) count(counts, u, "CHECK_OUT");
            int cancellations = (int) count(counts, u, "BOOKING_CANCELLED");
            int guests = (int) count(counts, u, "GUEST_ADDED");
            BigDecimal revenue = counts.stream()
                    .filter(c -> c.userId() == u.getId() && c.action().equals("BOOKING_CREATED"))
                    .map(ActivityDAO.Count::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

            double punctuality = present == 0 ? 0 : onTime / (double) present;
            double attendanceRate = openDays == 0 ? 0 : Math.min(1, present / (double) openDays);
            double work = busiest == 0 ? 0 : (bookings + checkIns + checkOuts + guests) / (double) busiest;
            int score = (int) Math.round(100 * (0.4 * punctuality + 0.3 * attendanceRate + 0.3 * work));

            result.add(new StaffPerformance(u, present, onTime, late, average, bookings, checkIns, checkOuts,
                    cancellations, guests, revenue, score));
        }
        result.sort(Comparator.comparingInt(StaffPerformance::score).reversed());
        return result;
    }

    /** Administrator's own 1-5 star rating for a staff member (null clears it). */
    public void rate(User staffMember, Integer stars) throws ValidationException, SQLException {
        requireAdmin();
        if (stars != null && (stars < 1 || stars > 5)) {
            throw new ValidationException("Ratings go from 1 to 5 stars.");
        }
        users.updateRating(staffMember.getId(), stars);
        staffMember.setRating(stars);
    }

    private static long count(List<ActivityDAO.Count> counts, User user, String action) {
        return counts.stream()
                .filter(c -> c.userId() == user.getId() && c.action().equals(action))
                .mapToLong(ActivityDAO.Count::times).sum();
    }

    private static void requireAdmin() throws ValidationException {
        if (!Session.isAdmin()) {
            throw new ValidationException("Only administrators can view attendance and performance.");
        }
    }
}
