package com.hotel.model;

/** A staff member who can sign in. */
public class User {

    private int id;
    private String username;
    private String passwordHash;
    private String fullName;
    private Role role = Role.RECEPTIONIST;
    private boolean mustChangePassword;
    private Integer rating;

    public User() { }

    public User(String username, String passwordHash, String fullName, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    /** True after an admin reset: the user must pick a new password at next sign-in. */
    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    /** Administrator's 1-5 star rating, or null if not rated yet. */
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
}
