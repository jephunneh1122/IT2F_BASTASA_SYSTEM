package com.yourdomain.dao;

import com.yourdomain.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All SQL for the users table.
 */
public class UserDao {

    private static final String SQL_INSERT =
            "INSERT INTO users (username, password, role, status) VALUES (?, ?, ?, 'PENDING')";
    private static final String SQL_LOGIN =
            "SELECT * FROM users WHERE username = ? AND password = ?";
    private static final String SQL_EXISTS =
            "SELECT 1 FROM users WHERE username = ?";
    private static final String SQL_ALL =
            "SELECT * FROM users ORDER BY id";
    private static final String SQL_PENDING =
            "SELECT * FROM users WHERE status = 'PENDING' ORDER BY id";
    private static final String SQL_ACTIVE_BY_ROLE =
            "SELECT * FROM users WHERE role = ? AND status = 'APPROVED' ORDER BY id";
    private static final String SQL_APPROVE =
            "UPDATE users SET status = 'APPROVED' WHERE id = ?;";
    private static final String SQL_DEACTIVATE =
            "UPDATE users SET status = 'INACTIVE' WHERE id = ?";

    /** Saves a new account. Status is always PENDING until an admin approves it. */
    public boolean register(String username, String password, String role) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, role);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Register error: " + e.getMessage());
            return false;
        }
    }

    public boolean usernameExists(String username) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_EXISTS)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Check error: " + e.getMessage());
            return false;
        }
    }

    /** Returns the User if username and password match (any status), otherwise empty. */
    public Optional<User> login(String username, String password) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LOGIN)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
        }
        return Optional.empty();
    }

    /** All registered accounts regardless of role or status. */
    public List<User> getAllUsers() {
        return queryUsers(SQL_ALL, null);
    }

    /** Accounts across every role whose status is PENDING. */
    public List<User> getPendingUsers() {
        return queryUsers(SQL_PENDING, null);
    }

    /** Approved accounts of one role (used to pick a delivery staff for an order). */
    public List<User> getApprovedUsersByRole(String role) {
        return queryUsers(SQL_ACTIVE_BY_ROLE, role);
    }

    /** Changes a PENDING account to APPROVED. Returns true if a row was updated. */
    public boolean approveUser(int userId) {
        return updateById(SQL_APPROVE, userId);
    }

    /** Deactivates an account (it can no longer log in). */
    public boolean deactivateUser(int userId) {
        return updateById(SQL_DEACTIVATE, userId);
    }

    private boolean updateById(String sql, int userId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Update error: " + e.getMessage());
            return false;
        }
    }

    private List<User> queryUsers(String sql, String param) {
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Load error: " + e.getMessage());
        }
        return users;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("role"),
                rs.getString("status"));
    }
}
