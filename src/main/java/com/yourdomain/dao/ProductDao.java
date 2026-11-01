package com.yourdomain.dao;

import com.yourdomain.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All SQL for the products (water inventory) table.
 */
public class ProductDao {

    public boolean addProduct(String name, double price, int stock) {
        String sql = "INSERT INTO products (name, price, stock) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.setInt(3, stock);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Save error: " + e.getMessage());
            return false;
        }
    }

    public List<Product> getAllProducts() {
        return query("SELECT * FROM products ORDER BY id", null);
    }

    public Optional<Product> findById(int id) {
        List<Product> found = query("SELECT * FROM products WHERE id = ?", id);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    public boolean setStock(int id, int newStock) {
        String sql = "UPDATE products SET stock = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newStock);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Update error: " + e.getMessage());
            return false;
        }
    }

    public boolean setPrice(int id, double newPrice) {
        String sql = "UPDATE products SET price = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newPrice);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Update error: " + e.getMessage());
            return false;
        }
    }

    private List<Product> query(String sql, Integer idParam) {
        List<Product> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (idParam != null) {
                ps.setInt(1, idParam);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Product(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getInt("stock")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Load error: " + e.getMessage());
        }
        return list;
    }
}
