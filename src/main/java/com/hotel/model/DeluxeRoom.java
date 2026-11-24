package com.hotel.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Mid-range room with a long-stay discount. */
public class DeluxeRoom extends Room {

    public static final int LONG_STAY_NIGHTS = 5;
    private static final BigDecimal LONG_STAY_RATE = new BigDecimal("0.95");

    @Override
    public String getType() {
        return DELUXE;
    }

    @Override
    public String getDisplayName() {
        return "Deluxe";
    }

    @Override
    public List<String> getDefaultAmenities() {
        return List.of("Free Wi-Fi", "Air conditioning", "Smart TV", "Mini fridge", "Work desk", "Balcony");
    }

    /** 5% off for stays of five nights or more. */
    @Override
    public BigDecimal calculateStayCost(long nights) {
        BigDecimal cost = super.calculateStayCost(nights);
        if (nights >= LONG_STAY_NIGHTS) {
            return cost.multiply(LONG_STAY_RATE).setScale(2, RoundingMode.HALF_UP);
        }
        return cost;
    }
}
