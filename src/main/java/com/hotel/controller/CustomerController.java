package com.hotel.controller;

import com.hotel.dao.CustomerDAO;
import com.hotel.model.Customer;

import java.sql.SQLException;
import java.util.List;

public class CustomerController {

    private static final String PHONE_PATTERN = "\\+?[0-9][0-9 ()-]{6,19}";
    private static final String EMAIL_PATTERN = "[^@\\s]+@[^@\\s]+\\.[^@\\s]+";

    private final CustomerDAO customers = new CustomerDAO();

    public List<Customer> getAll() throws SQLException {
        return customers.findAll();
    }

    /** Trims the fields and checks they are sensible. Shared with the booking flow. */
    public static void validate(Customer c) throws ValidationException {
        c.setFullName(c.getFullName().trim());
        c.setPhone(c.getPhone().trim());
        c.setEmail(c.getEmail().trim());
        c.setIdNumber(c.getIdNumber().trim());
        c.setNationality(c.getNationality().trim());

        if (c.getFullName().length() < 2 || c.getFullName().length() > 100) {
            throw new ValidationException("Please enter the guest's full name.");
        }
        if (!c.getPhone().matches(PHONE_PATTERN)) {
            throw new ValidationException("Please enter a valid phone number, e.g. +232 76 123456.");
        }
        if (!c.getEmail().isEmpty() && (!c.getEmail().matches(EMAIL_PATTERN) || c.getEmail().length() > 100)) {
            throw new ValidationException("That email address doesn't look right.");
        }
        if (c.getIdNumber().length() > 50 || c.getNationality().length() > 50) {
            throw new ValidationException("ID number and nationality must be 50 characters or fewer.");
        }
    }

    public void save(Customer customer) throws ValidationException, SQLException {
        validate(customer);
        if (customer.getId() == 0) {
            customer.setId(customers.insert(customer));
            ActivityLog.record(ActivityLog.Action.GUEST_ADDED, null, null);
        } else {
            customers.update(customer);
        }
        EventBus.publish(EventBus.Topic.CUSTOMERS_CHANGED);
        EventBus.publish(EventBus.Topic.BOOKINGS_CHANGED);
    }

    public void delete(Customer customer) throws ValidationException, SQLException {
        if (customers.hasBookings(customer.getId())) {
            throw new ValidationException(customer.getFullName()
                    + " has bookings on record, so the guest profile cannot be deleted.");
        }
        customers.delete(customer.getId());
        EventBus.publish(EventBus.Topic.CUSTOMERS_CHANGED);
    }
}
