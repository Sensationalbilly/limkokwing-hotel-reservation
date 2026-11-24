package com.hotel.model;

public enum PaymentMethod {
    CASH("Cash"),
    CARD("Card"),
    ORANGE_MONEY("Orange Money"),
    AFRIMONEY("Afrimoney");

    private final String label;

    PaymentMethod(String label) {
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
