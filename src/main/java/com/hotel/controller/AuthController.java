package com.hotel.controller;

import com.hotel.dao.UserDAO;
import com.hotel.db.SchemaUpdater;
import com.hotel.model.Role;
import com.hotel.model.User;
import com.hotel.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Arrays;

public class AuthController {

    private final UserDAO users = new UserDAO();

    /**
     * Checks the database is reachable and creates the default staff accounts on first run.
     *
     * @return true if default accounts were just created
     */
    public boolean initialise() throws SQLException {
        SchemaUpdater.apply();
        if (users.count() > 0) {
            return false;
        }
        users.insert(new User("admin", PasswordUtil.hash("admin123".toCharArray()),
                "System Administrator", Role.ADMIN));
        users.insert(new User("reception", PasswordUtil.hash("reception123".toCharArray()),
                "Front Desk", Role.RECEPTIONIST));
        return true;
    }

    public User login(String username, char[] password) throws ValidationException, SQLException {
        return login(username, password, null);
    }

    /**
     * Signs in and checks the account matches the role picked on the "Who are you?" screen.
     *
     * @param expectedRole the role the user chose, or null to accept any role
     */
    public User login(String username, char[] password, Role expectedRole) throws ValidationException, SQLException {
        try {
            if (username == null || username.isBlank() || password.length == 0) {
                throw new ValidationException("Please enter your username and password.");
            }
            User user = users.findByUsername(username.trim());
            if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
                throw new ValidationException("Incorrect username or password.");
            }
            if (expectedRole != null && user.getRole() != expectedRole) {
                throw new ValidationException("This is " + (user.isAdmin() ? "an Administrator" : "a Receptionist")
                        + " account. Go back and choose \""
                        + (user.isAdmin() ? "Administrator" : "Receptionist / Staff") + "\".");
            }
            Session.start(user);
            return user;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Lets the signed-in user replace a temporary password with one only they know.
     * The new password must differ from the temporary one.
     */
    public void changeOwnPassword(char[] newPassword, char[] confirmPassword) throws ValidationException, SQLException {
        try {
            User me = Session.currentUser();
            if (me == null) {
                throw new ValidationException("Please sign in first.");
            }
            UserController.validatePassword(newPassword, confirmPassword);
            if (PasswordUtil.verify(newPassword.clone(), me.getPasswordHash())) {
                throw new ValidationException("Choose a different password from the temporary one.");
            }
            users.updatePassword(me.getId(), PasswordUtil.hash(newPassword), false);
            Session.start(users.findById(me.getId()));
        } finally {
            UserController.wipe(newPassword);
            UserController.wipe(confirmPassword);
        }
    }

    public void logout() {
        Session.end();
    }
}
