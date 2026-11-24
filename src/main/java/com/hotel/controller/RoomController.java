package com.hotel.controller;

import com.hotel.dao.RoomDAO;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

public class RoomController {

    private final RoomDAO rooms = new RoomDAO();

    public List<Room> getAll() throws SQLException {
        return rooms.findAll();
    }

    public void save(Room room) throws ValidationException, SQLException {
        requireAdmin();
        String number = room.getRoomNumber() == null ? "" : room.getRoomNumber().trim().toUpperCase();
        if (!number.matches("[A-Z0-9-]{1,10}")) {
            throw new ValidationException("Room number must be 1-10 letters, digits or dashes (e.g. 101 or B-12).");
        }
        room.setRoomNumber(number);
        if (room.getFloor() < 0 || room.getFloor() > 50) {
            throw new ValidationException("Floor must be between 0 and 50.");
        }
        if (room.getCapacity() < 1 || room.getCapacity() > 10) {
            throw new ValidationException("Capacity must be between 1 and 10 guests.");
        }
        if (room.getPricePerNight() == null || room.getPricePerNight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Price per night must be greater than zero.");
        }
        if (room.getDescription().length() > 255) {
            throw new ValidationException("Description must be 255 characters or fewer.");
        }

        if (room.getId() == 0) {
            if (room.getStatus() == RoomStatus.OCCUPIED) {
                throw new ValidationException("A new room cannot start as Occupied.");
            }
        } else {
            Room existing = rooms.findById(room.getId());
            if (existing != null && existing.getStatus() != room.getStatus()
                    && (existing.getStatus() == RoomStatus.OCCUPIED || room.getStatus() == RoomStatus.OCCUPIED)) {
                throw new ValidationException("Occupancy is set automatically when guests check in or out.");
            }
        }

        try {
            if (room.getId() == 0) {
                room.setId(rooms.insert(room));
            } else {
                rooms.update(room);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("Room " + number + " already exists.");
        }
        EventBus.publish(EventBus.Topic.ROOMS_CHANGED);
    }

    /** Quick switch between Available and Maintenance from the overview screen. */
    public void setStatus(Room room, RoomStatus status) throws ValidationException, SQLException {
        requireAdmin();
        if (room.getStatus() == RoomStatus.OCCUPIED || status == RoomStatus.OCCUPIED) {
            throw new ValidationException("Occupancy is set automatically when guests check in or out.");
        }
        rooms.updateStatus(room.getId(), status);
        EventBus.publish(EventBus.Topic.ROOMS_CHANGED);
    }

    public void delete(Room room) throws ValidationException, SQLException {
        requireAdmin();
        if (rooms.hasBookings(room.getId())) {
            throw new ValidationException("Room " + room.getRoomNumber()
                    + " has booking history and cannot be deleted. Set it to Maintenance instead.");
        }
        rooms.delete(room.getId());
        EventBus.publish(EventBus.Topic.ROOMS_CHANGED);
    }

    private void requireAdmin() throws ValidationException {
        if (!Session.isAdmin()) {
            throw new ValidationException("Only administrators can change room settings.");
        }
    }
}
