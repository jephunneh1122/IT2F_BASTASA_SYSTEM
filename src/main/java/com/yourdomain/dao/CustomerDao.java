package com.yourdomain.dao;

import com.yourdomain.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All SQL for the customers table.
 */
public class CustomerDao {

    public boolean addCustomer(String name, String phone, String address) {
        String sql = "INSERT INTO customers (name, phone, address, status) VALUES (?, ?, ?, 'ACTIVE')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, address);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Save error: " + e.getMessage());
            return false;
        }
    }

    public List<Customer> getAllCustomers() {
        return query("SELECT * FROM customers ORDER BY id", null);
    }

    public List<Customer> getActiveCustomers() {
        return query("SELECT * FROM customers WHERE status = 'ACTIVE' ORDER BY id", null);
    }

    public Optional<Customer> findById(int id) {
        List<Customer> found = query("SELECT * FROM customers WHERE id = ?", id);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    public boolean updateCustomer(int id, String name, String phone, String address) {
        String sql = "UPDATE customers SET name = ?, phone = ?, address = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, address);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Update error: " + e.getMessage());
            return false;
        }
    }

    /** Customers are deactivated (not erased) so their order history is kept. */
    public boolean deactivateCustomer(int id) {
        String sql = "UPDATE customers SET status = 'INACTIVE' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Update error: " + e.getMessage());
            return false;
        }
    }

    private List<Customer> query(String sql, Integer idParam) {
        List<Customer> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (idParam != null) {
                ps.setInt(1, idParam);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Customer(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("phone"),
                            rs.getString("address"),
                            rs.getString("status")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Load error: " + e.getMessage());
        }
        return list;
    }
}
