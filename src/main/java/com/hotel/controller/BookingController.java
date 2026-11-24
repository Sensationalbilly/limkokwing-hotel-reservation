package com.hotel.controller;

import com.hotel.dao.BookingDAO;
import com.hotel.dao.CustomerDAO;
import com.hotel.dao.RoomDAO;
import com.hotel.model.Booking;
import com.hotel.model.BookingStatus;
import com.hotel.model.Customer;
import com.hotel.model.PaymentMethod;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.model.User;
import com.hotel.util.Formats;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class BookingController {

    public static final BigDecimal BREAKFAST_RATE = new BigDecimal("150.00");     // per guest, per night
    public static final BigDecimal AIRPORT_PICKUP_FEE = new BigDecimal("450.00"); // flat fee
    public static final int MAX_NIGHTS = 60;

    /** Price breakdown shown live while the receptionist fills in the form. */
    public record Quote(long nights, BigDecimal roomCost, BigDecimal extrasCost, BigDecimal total) { }

    private final BookingDAO bookings = new BookingDAO();
    private final RoomDAO rooms = new RoomDAO();
    private final CustomerDAO customers = new CustomerDAO();

    public List<Booking> getAll() throws SQLException {
        return bookings.findAll();
    }

    public long countArrivalsToday() throws SQLException {
        return bookings.countArrivals(LocalDate.now());
    }

    public BigDecimal totalRevenue() throws SQLException {
        return bookings.totalRevenue();
    }

    public void validateDates(LocalDate checkIn, LocalDate checkOut) throws ValidationException {
        if (checkIn == null || checkOut == null) {
            throw new ValidationException("Please choose check-in and check-out dates.");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new ValidationException("Check-in date cannot be in the past.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new ValidationException("Check-out must be at least one day after check-in.");
        }
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > MAX_NIGHTS) {
            throw new ValidationException("Bookings are limited to " + MAX_NIGHTS + " nights.");
        }
    }

    /** @param roomType a {@link Room} type code, or null for any type */
    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut, String roomType, int guests)
            throws ValidationException, SQLException {
        validateDates(checkIn, checkOut);
        return rooms.findAvailable(checkIn, checkOut, roomType, guests);
    }

    /** Works for any room subclass: each one prices the stay its own way (polymorphism). */
    public Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, int guests,
                       boolean breakfast, boolean airportPickup) {
        long nights = (checkIn == null || checkOut == null) ? 0 : Math.max(0, ChronoUnit.DAYS.between(checkIn, checkOut));
        BigDecimal roomCost = room == null ? BigDecimal.ZERO : room.calculateStayCost(nights);
        BigDecimal extras = BigDecimal.ZERO;
        if (breakfast && room != null && !room.includesBreakfast()) {
            extras = extras.add(BREAKFAST_RATE.multiply(BigDecimal.valueOf((long) guests * nights)));
        }
        if (airportPickup) {
            extras = extras.add(AIRPORT_PICKUP_FEE);
        }
        return new Quote(nights, roomCost, extras, roomCost.add(extras));
    }

    public Booking create(Customer customer, Room room, LocalDate checkIn, LocalDate checkOut, int guests,
                          boolean breakfast, boolean airportPickup, PaymentMethod payment, String notes)
            throws ValidationException, SQLException {
        if (customer == null) {
            throw new ValidationException("Please select or enter a guest.");
        }
        if (room == null) {
            throw new ValidationException("Please search and select a room.");
        }
        validateDates(checkIn, checkOut);
        if (guests < 1 || guests > room.getCapacity()) {
            throw new ValidationException("Room " + room.getRoomNumber() + " sleeps up to "
                    + room.getCapacity() + " guests.");
        }
        if (notes != null && notes.length() > 255) {
            throw new ValidationException("Notes must be 255 characters or fewer.");
        }

        if (customer.getId() == 0) {
            CustomerController.validate(customer);
            Customer existing = customers.findByPhone(customer.getPhone());
            if (existing != null) {
                customer = existing;  // returning guest typed in again: reuse their profile
            } else {
                customer.setId(customers.insert(customer));
                ActivityLog.record(ActivityLog.Action.GUEST_ADDED, null, null);
                EventBus.publish(EventBus.Topic.CUSTOMERS_CHANGED);
            }
        }

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setRoom(room);
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setGuests(guests);
        boolean chargeBreakfast = breakfast && !room.includesBreakfast();
        booking.setBreakfast(breakfast || room.includesBreakfast());
        booking.setAirportPickup(airportPickup);
        booking.setPaymentMethod(payment == null ? PaymentMethod.CASH : payment);
        booking.setNotes(notes == null ? "" : notes.trim());
        booking.setTotalAmount(quote(room, checkIn, checkOut, guests, chargeBreakfast, airportPickup).total());
        User user = Session.currentUser();
        booking.setCreatedBy(user == null ? null : user.getId());

        int id = bookings.createIfAvailable(booking);
        if (id < 0) {
            throw new ValidationException("Sorry, room " + room.getRoomNumber()
                    + " was just booked for those dates. Please search again.");
        }
        booking.setId(id);
        ActivityLog.record(ActivityLog.Action.BOOKING_CREATED, id, booking.getTotalAmount());
        EventBus.publish(EventBus.Topic.BOOKINGS_CHANGED);
        return booking;
    }

    public void checkIn(Booking b) throws ValidationException, SQLException {
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            throw new ValidationException("Only confirmed bookings can be checked in.");
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(b.getCheckIn())) {
            throw new ValidationException("This booking starts on " + Formats.date(b.getCheckIn())
                    + ". Guests cannot check in before their arrival date.");
        }
        if (!today.isBefore(b.getCheckOut())) {
            throw new ValidationException("This booking's dates have already passed.");
        }
        bookings.transition(b.getId(), BookingStatus.CHECKED_IN, b.getRoom().getId(), RoomStatus.OCCUPIED);
        ActivityLog.record(ActivityLog.Action.CHECK_IN, b.getId(), null);
        publishStayChange();
    }

    public void checkOut(Booking b) throws ValidationException, SQLException {
        if (b.getStatus() != BookingStatus.CHECKED_IN) {
            throw new ValidationException("Only guests who are checked in can be checked out.");
        }
        bookings.transition(b.getId(), BookingStatus.CHECKED_OUT, b.getRoom().getId(), RoomStatus.AVAILABLE);
        ActivityLog.record(ActivityLog.Action.CHECK_OUT, b.getId(), null);
        publishStayChange();
    }

    public void cancel(Booking b) throws ValidationException, SQLException {
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            throw new ValidationException("Only confirmed bookings that have not checked in can be cancelled.");
        }
        bookings.updateStatus(b.getId(), BookingStatus.CANCELLED);
        ActivityLog.record(ActivityLog.Action.BOOKING_CANCELLED, b.getId(), null);
        EventBus.publish(EventBus.Topic.BOOKINGS_CHANGED);
    }

    public void delete(Booking b) throws ValidationException, SQLException {
        if (!Session.isAdmin()) {
            throw new ValidationException("Only administrators can delete bookings.");
        }
        if (b.getStatus() == BookingStatus.CHECKED_IN) {
            throw new ValidationException("Check the guest out before deleting this booking.");
        }
        bookings.delete(b.getId());
        EventBus.publish(EventBus.Topic.BOOKINGS_CHANGED);
    }

    private void publishStayChange() {
        EventBus.publish(EventBus.Topic.BOOKINGS_CHANGED);
        EventBus.publish(EventBus.Topic.ROOMS_CHANGED);
    }
}
