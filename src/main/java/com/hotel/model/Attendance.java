package com.hotel.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/** One staff member's attendance for one day. */
public class Attendance {

    private int id;
    private int userId;
    private String username;
    private String fullName;
    private LocalDate workDate;
    private LocalDateTime clockIn;      // recorded by the system
    private LocalTime reportedTime;     // typed by the staff member
    private int lateMinutes;
    private String note = "";
    private LocalDateTime clockOut;

    public boolean isPresent() {
        return clockIn != null;
    }

    public boolean isLate() {
        return lateMinutes > 0;
    }

    /**
     * Minutes between the typed time and the recorded time.
     * Positive means the staff member typed an earlier time than the system recorded.
     */
    public Integer reportedDifference() {
        if (clockIn == null || reportedTime == null) return null;
        return (int) ChronoUnit.MINUTES.between(reportedTime, clockIn.toLocalTime().truncatedTo(ChronoUnit.MINUTES));
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public LocalDateTime getClockIn() { return clockIn; }
    public void setClockIn(LocalDateTime clockIn) { this.clockIn = clockIn; }

    public LocalTime getReportedTime() { return reportedTime; }
    public void setReportedTime(LocalTime reportedTime) { this.reportedTime = reportedTime; }

    public int getLateMinutes() { return lateMinutes; }
    public void setLateMinutes(int lateMinutes) { this.lateMinutes = lateMinutes; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note == null ? "" : note; }

    public LocalDateTime getClockOut() { return clockOut; }
    public void setClockOut(LocalDateTime clockOut) { this.clockOut = clockOut; }
}
