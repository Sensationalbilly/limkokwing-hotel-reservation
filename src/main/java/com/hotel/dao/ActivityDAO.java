package com.hotel.dao;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

/** Records who did what (bookings, check-ins, ...) for the staff performance report. */
public class ActivityDAO extends BaseDAO<ActivityDAO.Count> {

    /** How many times one user performed one action, and the total amount involved. */
    public record Count(int userId, String action, long times, BigDecimal amount) { }

    public void insert(int userId, String action, Integer bookingId, BigDecimal amount) throws SQLException {
        update("INSERT INTO activity_log (user_id, action, booking_id, amount) VALUES (?, ?, ?, ?)", ps -> {
            ps.setInt(1, userId);
            ps.setString(2, action);
            if (bookingId == null) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, bookingId);
            ps.setBigDecimal(4, amount);
        });
    }

    public List<Count> countsSince(LocalDateTime since) throws SQLException {
        return queryList("SELECT user_id, action, COUNT(*) AS times, COALESCE(SUM(amount), 0) AS amount"
                + " FROM activity_log WHERE user_id IS NOT NULL AND created_at >= ?"
                + " GROUP BY user_id, action", ps -> ps.setObject(1, since));
    }

    @Override
    protected Count map(ResultSet rs) throws SQLException {
        return new Count(rs.getInt("user_id"), rs.getString("action"), rs.getLong("times"), rs.getBigDecimal("amount"));
    }
}
