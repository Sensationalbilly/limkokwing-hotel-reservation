package com.hotel.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Premium room: breakfast included by default, 10% service charge on the room rate. */
public class SuiteRoom extends Room {

    private static final BigDecimal SERVICE_CHARGE = new BigDecimal("1.10");

    @Override
    public String getType() {
        return SUITE;
    }

    @Override
    public String getDisplayName() {
        return "Suite";
    }

    @Override
    public List<String> getDefaultAmenities() {
        return List.of("Free Wi-Fi", "Air conditioning", BREAKFAST, "Lounge area",
                "Mini bar", "Bathtub", "Sea view");
    }

    @Override
    public BigDecimal calculateStayCost(long nights) {
        return super.calculateStayCost(nights).multiply(SERVICE_CHARGE).setScale(2, RoundingMode.HALF_UP);
    }
}
