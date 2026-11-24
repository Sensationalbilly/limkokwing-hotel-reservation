package com.hotel.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Base class for every room type. Subclasses override the pricing and amenity rules,
 * so the rest of the system can work with any {@code Room} polymorphically.
 */
public abstract class Room {

    public static final String STANDARD = "STANDARD";
    public static final String DELUXE = "DELUXE";
    public static final String SUITE = "SUITE";
    public static final String[] TYPES = {STANDARD, DELUXE, SUITE};

    private int id;
    private String roomNumber = "";
    private int floor = 1;
    private int capacity = 1;
    private BigDecimal pricePerNight = BigDecimal.ZERO;
    private RoomStatus status = RoomStatus.AVAILABLE;
    private String description = "";

    /** Factory: builds the right subclass for a room type code stored in the database. */
    public static Room create(String type) {
        return switch (type == null ? "" : type.toUpperCase()) {
            case STANDARD -> new StandardRoom();
            case DELUXE -> new DeluxeRoom();
            case SUITE -> new SuiteRoom();
            default -> throw new IllegalArgumentException("Unknown room type: " + type);
        };
    }

    /** Type code stored in the database, e.g. "DELUXE". */
    public abstract String getType();

    /** Human-friendly name shown in the UI, e.g. "Deluxe". */
    public abstract String getDisplayName();

    public static final String BREAKFAST = "Complimentary breakfast";

    /** Every feature the admin can tick for a room (Rooms page checkboxes). */
    public static final List<String> ALL_AMENITIES = List.of(
            "Free Wi-Fi", "Air conditioning", "Ceiling fan", "En-suite bathroom", "Hot water",
            "Flat-screen TV", "Smart TV", "Mini fridge", "Mini bar", "Kettle & coffee",
            "Work desk", "Wardrobe", "Room safe", "Generator backup", "Balcony",
            "City view", "Sea view", "Lounge area", "Bathtub", BREAKFAST);

    /** Features chosen for this specific room; null means "use the type's defaults". */
    private List<String> amenities;

    /** The usual features for this room type; used until the admin picks the room's own. */
    public abstract List<String> getDefaultAmenities();

    /** This room's features: what the admin ticked, or the room type's defaults. */
    public List<String> getAmenities() {
        return amenities != null ? amenities : getDefaultAmenities();
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities == null ? null : List.copyOf(amenities);
    }

    /** Breakfast is free when the room's features include it. */
    public boolean includesBreakfast() {
        return getAmenities().contains(BREAKFAST);
    }

    /** Price of the room for a stay. Subclasses apply their own discounts or charges. */
    public BigDecimal calculateStayCost(long nights) {
        return pricePerNight.multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isBookable() {
        return status != RoomStatus.MAINTENANCE;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }

    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description == null ? "" : description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room other)) return false;
        return id != 0 && id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "Room " + roomNumber + " - " + getDisplayName();
    }
}
