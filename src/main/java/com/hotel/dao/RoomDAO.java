package com.hotel.dao;

import com.hotel.model.Room;
import com.hotel.model.RoomStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class RoomDAO extends BaseDAO<Room> {

    private static final String SELECT =
            "SELECT id, room_number, room_type, floor_no, capacity, price_per_night, status, description, amenities FROM rooms";
    private static final String SEPARATOR = ";";

    public List<Room> findAll() throws SQLException {
        return queryList(SELECT + " ORDER BY room_number", NO_PARAMS);
    }

    public Room findById(int id) throws SQLException {
        return queryOne(SELECT + " WHERE id = ?", ps -> ps.setInt(1, id));
    }

    /**
     * Rooms that are not under maintenance, fit the party, and have no active booking
     * overlapping [checkIn, checkOut). Two stays overlap when each starts before the other ends.
     */
    public List<Room> findAvailable(LocalDate checkIn, LocalDate checkOut, String typeOrNull, int guests)
            throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT)
                .append(" r WHERE r.status <> 'MAINTENANCE' AND r.capacity >= ?");
        if (typeOrNull != null) {
            sql.append(" AND r.room_type = ?");
        }
        sql.append(" AND NOT EXISTS (SELECT 1 FROM bookings b WHERE b.room_id = r.id")
                .append(" AND b.status IN ('CONFIRMED', 'CHECKED_IN')")
                .append(" AND b.check_in < ? AND b.check_out > ?)")
                .append(" ORDER BY r.price_per_night, r.room_number");

        return queryList(sql.toString(), ps -> {
            int i = 1;
            ps.setInt(i++, guests);
            if (typeOrNull != null) {
                ps.setString(i++, typeOrNull);
            }
            ps.setObject(i++, checkOut);
            ps.setObject(i, checkIn);
        });
    }

    public int insert(Room room) throws SQLException {
        return insert("INSERT INTO rooms (room_number, room_type, floor_no, capacity, price_per_night, status,"
                + " description, amenities) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", ps -> bind(ps, room));
    }

    public void update(Room room) throws SQLException {
        update("UPDATE rooms SET room_number = ?, room_type = ?, floor_no = ?, capacity = ?, price_per_night = ?,"
                + " status = ?, description = ?, amenities = ? WHERE id = ?", ps -> {
            bind(ps, room);
            ps.setInt(9, room.getId());
        });
    }

    /** Stored as "Free Wi-Fi;Air conditioning;...". */
    static String encodeAmenities(List<String> amenities) {
        return String.join(SEPARATOR, amenities);
    }

    /** null (never set) means the room uses its type's defaults. */
    static List<String> decodeAmenities(String stored) {
        if (stored == null) return null;
        return stored.isBlank() ? List.of() : List.of(stored.split(SEPARATOR));
    }

    public void updateStatus(int roomId, RoomStatus status) throws SQLException {
        update("UPDATE rooms SET status = ? WHERE id = ?", ps -> {
            ps.setString(1, status.name());
            ps.setInt(2, roomId);
        });
    }

    public void delete(int roomId) throws SQLException {
        update("DELETE FROM rooms WHERE id = ?", ps -> ps.setInt(1, roomId));
    }

    public boolean hasBookings(int roomId) throws SQLException {
        return queryLong("SELECT COUNT(*) FROM bookings WHERE room_id = ?", ps -> ps.setInt(1, roomId)) > 0;
    }

    private void bind(PreparedStatement ps, Room room) throws SQLException {
        ps.setString(1, room.getRoomNumber());
        ps.setString(2, room.getType());
        ps.setInt(3, room.getFloor());
        ps.setInt(4, room.getCapacity());
        ps.setBigDecimal(5, room.getPricePerNight());
        ps.setString(6, room.getStatus().name());
        ps.setString(7, room.getDescription());
        ps.setString(8, encodeAmenities(room.getAmenities()));
    }

    @Override
    protected Room map(ResultSet rs) throws SQLException {
        Room room = Room.create(rs.getString("room_type"));
        room.setId(rs.getInt("id"));
        room.setRoomNumber(rs.getString("room_number"));
        room.setFloor(rs.getInt("floor_no"));
        room.setCapacity(rs.getInt("capacity"));
        room.setPricePerNight(rs.getBigDecimal("price_per_night"));
        room.setStatus(RoomStatus.valueOf(rs.getString("status")));
        room.setDescription(rs.getString("description"));
        room.setAmenities(decodeAmenities(rs.getString("amenities")));
        return room;
    }
}
