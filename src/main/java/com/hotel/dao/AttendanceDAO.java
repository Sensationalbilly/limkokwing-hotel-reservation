package com.hotel.dao;

import com.hotel.db.DBConnection;
import com.hotel.model.Attendance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class AttendanceDAO extends BaseDAO<Attendance> {

    private static final String COLUMNS =
            "a.id, u.id AS user_id, u.username, u.full_name, a.work_date, a.clock_in,"
            + " a.reported_time, a.late_minutes, a.note, a.clock_out";

    /** UTC time from the database server's clock, so staff cannot fake it from the form. */
    public LocalDateTime nowUtc() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT UTC_TIMESTAMP()");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            // Read as LocalDateTime so the driver does not shift it by the PC's time zone.
            return rs.getObject(1, LocalDateTime.class);
        }
    }

    public Attendance findForDay(int userId, LocalDate day) throws SQLException {
        return queryOne("SELECT " + COLUMNS + " FROM attendance a JOIN users u ON u.id = a.user_id"
                + " WHERE a.user_id = ? AND a.work_date = ?", ps -> {
            ps.setInt(1, userId);
            ps.setObject(2, day);
        });
    }

    /** Every receptionist for the day, including those who have not signed in (no attendance row). */
    public List<Attendance> findDay(LocalDate day) throws SQLException {
        List<Attendance> list = queryList("SELECT " + COLUMNS + " FROM users u"
                + " LEFT JOIN attendance a ON a.user_id = u.id AND a.work_date = ?"
                + " WHERE u.role = 'RECEPTIONIST'"
                + " ORDER BY a.clock_in IS NULL, a.clock_in, u.full_name", ps -> ps.setObject(1, day));
        list.forEach(a -> a.setWorkDate(day));
        return list;
    }

    public List<Attendance> findBetween(LocalDate from, LocalDate to) throws SQLException {
        return queryList("SELECT " + COLUMNS + " FROM attendance a JOIN users u ON u.id = a.user_id"
                + " WHERE a.work_date BETWEEN ? AND ? ORDER BY a.work_date DESC, a.clock_in", ps -> {
            ps.setObject(1, from);
            ps.setObject(2, to);
        });
    }

    public void insert(int userId, LocalDate day, LocalDateTime clockIn, int lateMinutes) throws SQLException {
        update("INSERT INTO attendance (user_id, work_date, clock_in, late_minutes) VALUES (?, ?, ?, ?)", ps -> {
            ps.setInt(1, userId);
            ps.setObject(2, day);
            ps.setObject(3, clockIn);
            ps.setInt(4, lateMinutes);
        });
    }

    public void saveReport(int attendanceId, LocalTime reportedTime, String note) throws SQLException {
        update("UPDATE attendance SET reported_time = ?, note = ? WHERE id = ?", ps -> {
            ps.setObject(1, reportedTime);
            ps.setString(2, note);
            ps.setInt(3, attendanceId);
        });
    }

    public void clockOut(int userId, LocalDate day, LocalDateTime time) throws SQLException {
        update("UPDATE attendance SET clock_out = ? WHERE user_id = ? AND work_date = ?", ps -> {
            ps.setObject(1, time);
            ps.setInt(2, userId);
            ps.setObject(3, day);
        });
    }

    @Override
    protected Attendance map(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setId(rs.getInt("id"));
        a.setUserId(rs.getInt("user_id"));
        a.setUsername(rs.getString("username"));
        a.setFullName(rs.getString("full_name"));
        a.setWorkDate(rs.getObject("work_date", LocalDate.class));
        a.setClockIn(rs.getObject("clock_in", LocalDateTime.class));
        a.setReportedTime(rs.getObject("reported_time", LocalTime.class));
        a.setLateMinutes(rs.getInt("late_minutes"));
        a.setNote(rs.getString("note"));
        a.setClockOut(rs.getObject("clock_out", LocalDateTime.class));
        return a;
    }
}
