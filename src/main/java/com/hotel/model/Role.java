package com.hotel.model;

public enum Role {
    ADMIN("Administrator"),
    RECEPTIONIST("Receptionist");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
