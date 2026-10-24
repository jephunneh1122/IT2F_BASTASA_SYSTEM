package dao;

import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All SQL queries for the users table.
 */
public class UserDao {

    private static final String SQL_INSERT =
            "INSERT INTO users (username, password, roles, status) VALUES (?, ?, ?, ?)";
    private static final String SQL_LOGIN =
            "SELECT * FROM users WHERE username = ? AND password = ?";
    private static final String SQL_EXISTS =
            "SELECT 1 FROM users WHERE username = ?";
    private static final String SQL_ALL =
            "SELECT * FROM users ORDER BY id";

    private static final String DEFAULT_STATUS = "PENDING";

    /** Saves a new account. Status is always PENDING. Returns true if saved. */
    public boolean register(String username, String password, String role) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setString(4, DEFAULT_STATUS);
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

    /** Returns the User if username and password match, otherwise empty. */
    public Optional<User> login(String username, String password) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LOGIN)) {
            ps.setString(1, username);
            ps.setString(2, password);
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

    /** Used by the data table screen. */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(SQL_ALL)) {
            while (rs.next()) {
                users.add(mapRow(rs));
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
                rs.getString("roles"),
                rs.getString("status"));
    }
}
