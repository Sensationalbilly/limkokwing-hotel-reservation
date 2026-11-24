package com.hotel.dao;

import com.hotel.model.Role;
import com.hotel.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class UserDAO extends BaseDAO<User> {

    private static final String SELECT =
            "SELECT id, username, password_hash, full_name, role, must_change_password, rating FROM users";

    public List<User> findAll() throws SQLException {
        return queryList(SELECT + " ORDER BY role, full_name", NO_PARAMS);
    }

    public User findById(int id) throws SQLException {
        return queryOne(SELECT + " WHERE id = ?", ps -> ps.setInt(1, id));
    }

    public User findByUsername(String username) throws SQLException {
        return queryOne(SELECT + " WHERE username = ?", ps -> ps.setString(1, username));
    }

    public int insert(User user) throws SQLException {
        return insert("INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)", ps -> {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole().name());
        });
    }

    /** Updates username, name and role. The password is changed separately. */
    public void update(User user) throws SQLException {
        update("UPDATE users SET username = ?, full_name = ?, role = ? WHERE id = ?", ps -> {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getRole().name());
            ps.setInt(4, user.getId());
        });
    }

    /** @param mustChange true for a temporary password the user must replace at next sign-in */
    public void updatePassword(int userId, String passwordHash, boolean mustChange) throws SQLException {
        update("UPDATE users SET password_hash = ?, must_change_password = ? WHERE id = ?", ps -> {
            ps.setString(1, passwordHash);
            ps.setBoolean(2, mustChange);
            ps.setInt(3, userId);
        });
    }

    public void updateRating(int userId, Integer rating) throws SQLException {
        update("UPDATE users SET rating = ? WHERE id = ?", ps -> {
            if (rating == null) ps.setNull(1, java.sql.Types.TINYINT);
            else ps.setInt(1, rating);
            ps.setInt(2, userId);
        });
    }

    public void delete(int userId) throws SQLException {
        update("DELETE FROM users WHERE id = ?", ps -> ps.setInt(1, userId));
    }

    public long count() throws SQLException {
        return queryLong("SELECT COUNT(*) FROM users", NO_PARAMS);
    }

    public long countAdmins() throws SQLException {
        return queryLong("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'", NO_PARAMS);
    }

    @Override
    protected User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(Role.valueOf(rs.getString("role")));
        user.setMustChangePassword(rs.getBoolean("must_change_password"));
        int rating = rs.getInt("rating");
        user.setRating(rs.wasNull() ? null : rating);
        return user;
    }
}
