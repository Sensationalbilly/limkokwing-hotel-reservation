package com.hotel.dao;

import com.hotel.model.Customer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CustomerDAO extends BaseDAO<Customer> {

    private static final String SELECT =
            "SELECT id, full_name, phone, email, id_number, nationality FROM customers";

    public List<Customer> findAll() throws SQLException {
        return queryList(SELECT + " ORDER BY full_name", NO_PARAMS);
    }

    public Customer findById(int id) throws SQLException {
        return queryOne(SELECT + " WHERE id = ?", ps -> ps.setInt(1, id));
    }

    public Customer findByPhone(String phone) throws SQLException {
        return queryOne(SELECT + " WHERE phone = ? LIMIT 1", ps -> ps.setString(1, phone));
    }

    public int insert(Customer customer) throws SQLException {
        return insert("INSERT INTO customers (full_name, phone, email, id_number, nationality) VALUES (?, ?, ?, ?, ?)",
                ps -> bind(ps, customer));
    }

    public void update(Customer customer) throws SQLException {
        update("UPDATE customers SET full_name = ?, phone = ?, email = ?, id_number = ?, nationality = ? WHERE id = ?",
                ps -> {
                    bind(ps, customer);
                    ps.setInt(6, customer.getId());
                });
    }

    public void delete(int customerId) throws SQLException {
        update("DELETE FROM customers WHERE id = ?", ps -> ps.setInt(1, customerId));
    }

    public boolean hasBookings(int customerId) throws SQLException {
        return queryLong("SELECT COUNT(*) FROM bookings WHERE customer_id = ?", ps -> ps.setInt(1, customerId)) > 0;
    }

    private void bind(PreparedStatement ps, Customer c) throws SQLException {
        ps.setString(1, c.getFullName());
        ps.setString(2, c.getPhone());
        ps.setString(3, c.getEmail().isEmpty() ? null : c.getEmail());
        ps.setString(4, c.getIdNumber());
        ps.setString(5, c.getNationality());
    }

    @Override
    protected Customer map(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setFullName(rs.getString("full_name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setIdNumber(rs.getString("id_number"));
        c.setNationality(rs.getString("nationality"));
        return c;
    }
}
