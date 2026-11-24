package com.hotel.controller;

import com.hotel.model.User;

/** Holds the signed-in staff member for permission checks. */
public final class Session {

    private static volatile User current;

    private Session() { }

    static void start(User user) {
        current = user;
    }

    static void end() {
        current = null;
    }

    public static User currentUser() {
        return current;
    }

    public static boolean isAdmin() {
        User user = current;
        return user != null && user.isAdmin();
    }
}
