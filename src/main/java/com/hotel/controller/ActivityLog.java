package com.hotel.controller;

import com.hotel.dao.ActivityDAO;
import com.hotel.model.User;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Records which staff member did what, for the performance report. Logging never blocks the
 * real work: if it fails, the booking or check-in still succeeds and the error is printed.
 */
public final class ActivityLog {

    public enum Action { BOOKING_CREATED, CHECK_IN, CHECK_OUT, BOOKING_CANCELLED, GUEST_ADDED }

    private static final ActivityDAO DAO = new ActivityDAO();

    private ActivityLog() { }

    static void record(Action action, Integer bookingId, BigDecimal amount) {
        User user = Session.currentUser();
        if (user == null) return;
        try {
            DAO.insert(user.getId(), action.name(), bookingId, amount);
        } catch (SQLException e) {
            System.err.println("Could not record activity " + action + ": " + e.getMessage());
        }
    }
}
