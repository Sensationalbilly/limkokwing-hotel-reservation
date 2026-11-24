package com.hotel.model;

public enum BookingStatus {
    CONFIRMED("Confirmed"),
    CHECKED_IN("Checked in"),
    CHECKED_OUT("Checked out"),
    CANCELLED("Cancelled");

    private final String label;

    BookingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Active bookings block the room for their dates. */
    public boolean isActive() {
        return this == CONFIRMED || this == CHECKED_IN;
    }

    @Override
    public String toString() {
        return label;
    }
}
