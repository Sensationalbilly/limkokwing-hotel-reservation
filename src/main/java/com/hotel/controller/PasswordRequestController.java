package com.hotel.controller;

import com.hotel.dao.PasswordRequestDAO;
import com.hotel.dao.UserDAO;
import com.hotel.model.PasswordRequest;
import com.hotel.model.User;
import com.hotel.util.Formats;

import java.sql.SQLException;
import java.util.List;

/**
 * "Forgot password" workflow:
 * staff send a request from the sign-in screen, an administrator sets a temporary password
 * (given to them in person), and the staff member must choose their own password at next sign-in.
 */
public class PasswordRequestController {

    private final PasswordRequestDAO requests = new PasswordRequestDAO();
    private final UserDAO users = new UserDAO();
    private final UserController userController = new UserController();

    /** Sent from the sign-in screen, so no one is signed in yet. */
    public void submit(String username, String note) throws ValidationException, SQLException {
        User user = findUser(username);
        String text = note == null ? "" : note.trim();
        if (text.length() > 255) {
            throw new ValidationException("Please keep the note under 255 characters.");
        }
        PasswordRequest latest = requests.findLatestForUser(user.getId());
        if (latest != null && latest.getStatus() == PasswordRequest.Status.PENDING) {
            throw new ValidationException("You already sent a request on " + when(latest)
                    + ". It is still waiting for an administrator.");
        }
        requests.insert(user.getId(), text);
    }

    /** The most recent request for this username, or null if they never sent one. */
    public PasswordRequest latestFor(String username) throws ValidationException, SQLException {
        return requests.findLatestForUser(findUser(username).getId());
    }

    public List<PasswordRequest> pending() throws ValidationException, SQLException {
        requireAdmin();
        return requests.findPending();
    }

    public long countPending() throws SQLException {
        return Session.isAdmin() ? requests.countPending() : 0;
    }

    /** Sets a temporary password for the requester and marks the request as done. */
    public void resolve(PasswordRequest request, char[] temporaryPassword, char[] confirm)
            throws ValidationException, SQLException {
        requireAdmin();
        User target = new User();
        target.setId(request.getUserId());
        userController.resetPassword(target, temporaryPassword, confirm);
        requests.markHandled(request.getId(), PasswordRequest.Status.RESOLVED, Session.currentUser().getId());
        EventBus.publish(EventBus.Topic.PASSWORD_REQUESTS_CHANGED);
    }

    public void dismiss(PasswordRequest request) throws ValidationException, SQLException {
        requireAdmin();
        requests.markHandled(request.getId(), PasswordRequest.Status.DISMISSED, Session.currentUser().getId());
        EventBus.publish(EventBus.Topic.PASSWORD_REQUESTS_CHANGED);
    }

    /** Human-readable status for the staff member who sent the request. */
    public static String describe(PasswordRequest r) {
        return switch (r.getStatus()) {
            case PENDING -> "Waiting: your request from " + when(r) + " has not been handled yet. "
                    + "Please check again later, or remind your administrator.";
            case RESOLVED -> "Done: " + (r.getHandledByName() == null ? "an administrator" : r.getHandledByName())
                    + " reset your password" + (r.getHandledAt() == null ? "" : " on " + Formats.dateTime(r.getHandledAt()))
                    + ". Ask them for your temporary password. When you sign in with it, you will choose your own new password.";
            case DISMISSED -> "Closed: an administrator closed your request without resetting the password. "
                    + "Please speak to them in person.";
        };
    }

    private User findUser(String username) throws ValidationException, SQLException {
        String name = username == null ? "" : username.trim();
        if (name.isEmpty()) {
            throw new ValidationException("Enter your username so the administrator knows whose password to reset.");
        }
        User user = users.findByUsername(name);
        if (user == null) {
            throw new ValidationException("We couldn't find the username \"" + name
                    + "\". Check the spelling, or ask an administrator.");
        }
        return user;
    }

    private static String when(PasswordRequest r) {
        return r.getCreatedAt() == null ? "earlier" : Formats.dateTime(r.getCreatedAt());
    }

    private static void requireAdmin() throws ValidationException {
        if (!Session.isAdmin()) {
            throw new ValidationException("Only administrators can handle password requests.");
        }
    }
}
