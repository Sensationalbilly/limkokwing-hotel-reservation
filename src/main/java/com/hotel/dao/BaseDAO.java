package com.hotel.dao;

import com.hotel.db.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared JDBC plumbing. Each DAO only supplies its SQL and overrides {@link #map(ResultSet)}
 * to turn a row into a model object (template method pattern). Every query uses
 * PreparedStatement parameters, which prevents SQL injection.
 */
public abstract class BaseDAO<T> {

    @FunctionalInterface
    protected interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    protected static final Binder NO_PARAMS = ps -> { };

    protected abstract T map(ResultSet rs) throws SQLException;

    protected List<T> queryList(String sql, Binder binder) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(map(rs));
                }
                return results;
            }
        }
    }

    protected T queryOne(String sql, Binder binder) throws SQLException {
        List<T> results = queryList(sql, binder);
        return results.isEmpty() ? null : results.get(0);
    }

    /** Runs an INSERT and returns the generated primary key. */
    protected int insert(String sql, Binder binder) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(ps);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert did not return a generated id");
                }
                return keys.getInt(1);
            }
        }
    }

    /** Runs an UPDATE or DELETE and returns the number of affected rows. */
    protected int update(String sql, Binder binder) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        }
    }

    protected long queryLong(String sql, Binder binder) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    protected BigDecimal queryDecimal(String sql, Binder binder) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                BigDecimal value = rs.next() ? rs.getBigDecimal(1) : null;
                return value == null ? BigDecimal.ZERO : value;
            }
        }
    }
}
