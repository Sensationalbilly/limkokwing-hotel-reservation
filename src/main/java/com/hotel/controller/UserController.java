package com.hotel.controller;

import com.hotel.dao.UserDAO;
import com.hotel.model.Role;
import com.hotel.model.User;
import com.hotel.util.PasswordUtil;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Arrays;
import java.util.List;

/**
 * Staff account management. Only administrators may use it: they create accounts,
 * change usernames, names and roles, and reset forgotten passwords.
 */
public class UserController {

    public static final int MIN_PASSWORD_LENGTH = 6;
    private static final String USERNAME_PATTERN = "[A-Za-z0-9._-]{3,30}";

    private final UserDAO users = new UserDAO();

    public List<User> getAll() throws ValidationException, SQLException {
        requireAdmin();
        return users.findAll();
    }

    /** Creates a new account (when id is 0) or updates username, name and role. */
    public void save(User user, char[] newPassword, char[] confirmPassword) throws ValidationException, SQLException {
        requireAdmin();
        try {
            String username = user.getUsername() == null ? "" : user.getUsername().trim();
            String fullName = user.getFullName() == null ? "" : user.getFullName().trim();
            if (!username.matches(USERNAME_PATTERN)) {
                throw new ValidationException("Username must be 3-30 characters: letters, digits, dot, dash or underscore.");
            }
            if (fullName.length() < 2 || fullName.length() > 100) {
                throw new ValidationException("Please enter the staff member's full name.");
            }
            if (user.getRole() == null) {
                throw new ValidationException("Please choose a role.");
            }
            user.setUsername(username);
            user.setFullName(fullName);

            if (user.getId() == 0) {
                validatePassword(newPassword, confirmPassword);
                user.setPasswordHash(PasswordUtil.hash(newPassword));
                user.setId(users.insert(user));
            } else {
                User existing = users.findById(user.getId());
                if (existing == null) {
                    throw new ValidationException("This account no longer exists.");
                }
                if (existing.isAdmin() && user.getRole() != Role.ADMIN && users.countAdmins() <= 1) {
                    throw new ValidationException("This is the only administrator. Make someone else an administrator first.");
                }
                users.update(user);
                refreshSessionIfSelf(user.getId());
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("The username \"" + user.getUsername() + "\" is already taken.");
        } finally {
            wipe(newPassword);
            wipe(confirmPassword);
        }
    }

    /**
     * Sets a temporary password for any account, e.g. when a receptionist has forgotten theirs.
     * The user must choose their own new password the next time they sign in.
     */
    public void resetPassword(User user, char[] newPassword, char[] confirmPassword)
            throws ValidationException, SQLException {
        requireAdmin();
        try {
            validatePassword(newPassword, confirmPassword);
            users.updatePassword(user.getId(), PasswordUtil.hash(newPassword), true);
        } finally {
            wipe(newPassword);
            wipe(confirmPassword);
        }
    }

    public void delete(User user) throws ValidationException, SQLException {
        requireAdmin();
        User me = Session.currentUser();
        if (me != null && me.getId() == user.getId()) {
            throw new ValidationException("You cannot delete the account you are signed in with.");
        }
        if (user.isAdmin() && users.countAdmins() <= 1) {
            throw new ValidationException("You cannot delete the only administrator.");
        }
        users.delete(user.getId());
    }

    static void validatePassword(char[] password, char[] confirm) throws ValidationException {
        if (password == null || password.length < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (!Arrays.equals(password, confirm)) {
            throw new ValidationException("The two passwords do not match.");
        }
    }

    /** Keeps the signed-in user's details current if an admin edits their own account. */
    private void refreshSessionIfSelf(int userId) throws SQLException {
        User me = Session.currentUser();
        if (me != null && me.getId() == userId) {
            Session.start(users.findById(userId));
        }
    }

    private static void requireAdmin() throws ValidationException {
        if (!Session.isAdmin()) {
            throw new ValidationException("Only administrators can manage staff accounts.");
        }
    }

    static void wipe(char[] secret) {
        if (secret != null) Arrays.fill(secret, '\0');
    }
}
