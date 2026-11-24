package com.hotel.model;

import java.util.List;

/** Basic room: uses the default pricing from {@link Room}. */
public class StandardRoom extends Room {

    @Override
    public String getType() {
        return STANDARD;
    }

    @Override
    public String getDisplayName() {
        return "Standard";
    }

    @Override
    public List<String> getDefaultAmenities() {
        return List.of("Free Wi-Fi", "Air conditioning", "En-suite bathroom", "Flat-screen TV");
    }
}
