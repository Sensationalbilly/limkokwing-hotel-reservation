package com.hotel.dao;

import com.hotel.model.PasswordRequest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class PasswordRequestDAO extends BaseDAO<PasswordRequest> {

    private static final String SELECT =
            "SELECT r.id, r.user_id, r.note, r.status, r.created_at, r.handled_at,"
            + " u.username, u.full_name, a.full_name AS handled_by_name"
            + " FROM password_requests r"
            + " JOIN users u ON u.id = r.user_id"
            + " LEFT JOIN users a ON a.id = r.handled_by";

    public List<PasswordRequest> findPending() throws SQLException {
        return queryList(SELECT + " WHERE r.status = 'PENDING' ORDER BY r.created_at, r.id", NO_PARAMS);
    }

    public PasswordRequest findLatestForUser(int userId) throws SQLException {
        return queryOne(SELECT + " WHERE r.user_id = ? ORDER BY r.id DESC LIMIT 1", ps -> ps.setInt(1, userId));
    }

    public long countPending() throws SQLException {
        return queryLong("SELECT COUNT(*) FROM password_requests WHERE status = 'PENDING'", NO_PARAMS);
    }

    public int insert(int userId, String note) throws SQLException {
        return insert("INSERT INTO password_requests (user_id, note) VALUES (?, ?)", ps -> {
            ps.setInt(1, userId);
            ps.setString(2, note);
        });
    }

    public void markHandled(int requestId, PasswordRequest.Status status, int adminId) throws SQLException {
        update("UPDATE password_requests SET status = ?, handled_at = CURRENT_TIMESTAMP, handled_by = ? WHERE id = ?",
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, adminId);
                    ps.setInt(3, requestId);
                });
    }

    @Override
    protected PasswordRequest map(ResultSet rs) throws SQLException {
        PasswordRequest r = new PasswordRequest();
        r.setId(rs.getInt("id"));
        r.setUserId(rs.getInt("user_id"));
        r.setUsername(rs.getString("username"));
        r.setFullName(rs.getString("full_name"));
        r.setNote(rs.getString("note"));
        r.setStatus(PasswordRequest.Status.valueOf(rs.getString("status")));
        Timestamp created = rs.getTimestamp("created_at");
        r.setCreatedAt(created == null ? null : created.toLocalDateTime());
        Timestamp handled = rs.getTimestamp("handled_at");
        r.setHandledAt(handled == null ? null : handled.toLocalDateTime());
        r.setHandledByName(rs.getString("handled_by_name"));
        return r;
    }
}
