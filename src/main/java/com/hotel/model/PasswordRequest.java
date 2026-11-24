package com.hotel.model;

import java.time.LocalDateTime;

/** A "forgot password" request sent from the sign-in screen to the administrators. */
public class PasswordRequest {

    public enum Status { PENDING, RESOLVED, DISMISSED }

    private int id;
    private int userId;
    private String username;
    private String fullName;
    private String note = "";
    private Status status = Status.PENDING;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;
    private String handledByName;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note == null ? "" : note; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(LocalDateTime handledAt) { this.handledAt = handledAt; }

    public String getHandledByName() { return handledByName; }
    public void setHandledByName(String handledByName) { this.handledByName = handledByName; }
}
