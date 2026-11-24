package com.hotel.dao;

import com.hotel.db.DBConnection;
import com.hotel.model.Booking;
import com.hotel.model.BookingStatus;
import com.hotel.model.Customer;
import com.hotel.model.PaymentMethod;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class BookingDAO extends BaseDAO<Booking> {

    private static final String SELECT =
            "SELECT b.*, c.full_name, c.phone, c.email, c.id_number,"
            + " r.room_number, r.room_type, r.floor_no, r.capacity, r.price_per_night, r.status AS room_status, r.amenities"
            + " FROM bookings b"
            + " JOIN customers c ON c.id = b.customer_id"
            + " JOIN rooms r ON r.id = b.room_id";

    private static final String OVERLAP_SQL =
            "SELECT COUNT(*) FROM bookings WHERE room_id = ? AND status IN ('CONFIRMED', 'CHECKED_IN')"
            + " AND check_in < ? AND check_out > ?";

    public List<Booking> findAll() throws SQLException {
        return queryList(SELECT + " ORDER BY b.check_in DESC, b.id DESC", NO_PARAMS);
    }

    public Booking findById(int id) throws SQLException {
        return queryOne(SELECT + " WHERE b.id = ?", ps -> ps.setInt(1, id));
    }

    /**
     * Inserts the booking only if the room is still free, inside one transaction.
     * The room row is locked (SELECT ... FOR UPDATE) so two receptionists cannot
     * double-book the same room at the same moment.
     *
     * @return the new booking id, or -1 if the room was taken in the meantime
     */
    public int createIfAvailable(Booking b) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement lock = c.prepareStatement("SELECT id FROM rooms WHERE id = ? FOR UPDATE")) {
                    lock.setInt(1, b.getRoom().getId());
                    lock.executeQuery().close();
                }
                if (hasOverlap(c, b.getRoom().getId(), b.getCheckIn(), b.getCheckOut())) {
                    c.rollback();
                    return -1;
                }
                int id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO bookings (customer_id, room_id, check_in, check_out, guests, breakfast,"
                        + " airport_pickup, payment_method, total_amount, status, notes, created_by)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, b.getCustomer().getId());
                    ps.setInt(2, b.getRoom().getId());
                    ps.setObject(3, b.getCheckIn());
                    ps.setObject(4, b.getCheckOut());
                    ps.setInt(5, b.getGuests());
                    ps.setBoolean(6, b.isBreakfast());
                    ps.setBoolean(7, b.isAirportPickup());
                    ps.setString(8, b.getPaymentMethod().name());
                    ps.setBigDecimal(9, b.getTotalAmount());
                    ps.setString(10, b.getStatus().name());
                    ps.setString(11, b.getNotes());
                    if (b.getCreatedBy() == null) {
                        ps.setNull(12, Types.INTEGER);
                    } else {
                        ps.setInt(12, b.getCreatedBy());
                    }
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        id = keys.getInt(1);
                    }
                }
                c.commit();
                return id;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void updateStatus(int bookingId, BookingStatus status) throws SQLException {
        update("UPDATE bookings SET status = ? WHERE id = ?", ps -> {
            ps.setString(1, status.name());
            ps.setInt(2, bookingId);
        });
    }

    /** Changes a booking's status and its room's status together (check-in / check-out). */
    public void transition(int bookingId, BookingStatus bookingStatus, int roomId, RoomStatus roomStatus)
            throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement booking = c.prepareStatement("UPDATE bookings SET status = ? WHERE id = ?");
                 PreparedStatement room = c.prepareStatement("UPDATE rooms SET status = ? WHERE id = ?")) {
                booking.setString(1, bookingStatus.name());
                booking.setInt(2, bookingId);
                booking.executeUpdate();
                room.setString(1, roomStatus.name());
                room.setInt(2, roomId);
                room.executeUpdate();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void delete(int bookingId) throws SQLException {
        update("DELETE FROM bookings WHERE id = ?", ps -> ps.setInt(1, bookingId));
    }

    public long countArrivals(LocalDate date) throws SQLException {
        return queryLong("SELECT COUNT(*) FROM bookings WHERE check_in = ? AND status = 'CONFIRMED'",
                ps -> ps.setObject(1, date));
    }

    public BigDecimal totalRevenue() throws SQLException {
        return queryDecimal("SELECT COALESCE(SUM(total_amount), 0) FROM bookings WHERE status <> 'CANCELLED'",
                NO_PARAMS);
    }

    private boolean hasOverlap(Connection c, int roomId, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(OVERLAP_SQL)) {
            ps.setInt(1, roomId);
            ps.setObject(2, checkOut);
            ps.setObject(3, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        }
    }

    @Override
    protected Booking map(ResultSet rs) throws SQLException {
        Customer customer = new Customer();
        customer.setId(rs.getInt("customer_id"));
        customer.setFullName(rs.getString("full_name"));
        customer.setPhone(rs.getString("phone"));
        customer.setEmail(rs.getString("email"));
        customer.setIdNumber(rs.getString("id_number"));

        Room room = Room.create(rs.getString("room_type"));
        room.setId(rs.getInt("room_id"));
        room.setRoomNumber(rs.getString("room_number"));
        room.setFloor(rs.getInt("floor_no"));
        room.setCapacity(rs.getInt("capacity"));
        room.setPricePerNight(rs.getBigDecimal("price_per_night"));
        room.setStatus(RoomStatus.valueOf(rs.getString("room_status")));
        room.setAmenities(RoomDAO.decodeAmenities(rs.getString("amenities")));

        Booking b = new Booking();
        b.setId(rs.getInt("id"));
        b.setCustomer(customer);
        b.setRoom(room);
        b.setCheckIn(rs.getObject("check_in", LocalDate.class));
        b.setCheckOut(rs.getObject("check_out", LocalDate.class));
        b.setGuests(rs.getInt("guests"));
        b.setBreakfast(rs.getBoolean("breakfast"));
        b.setAirportPickup(rs.getBoolean("airport_pickup"));
        b.setPaymentMethod(PaymentMethod.valueOf(rs.getString("payment_method")));
        b.setTotalAmount(rs.getBigDecimal("total_amount"));
        b.setStatus(BookingStatus.valueOf(rs.getString("status")));
        b.setNotes(rs.getString("notes"));
        int createdBy = rs.getInt("created_by");
        b.setCreatedBy(rs.wasNull() ? null : createdBy);
        b.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return b;
    }
}
