package com.yourdomain.dao;

import com.yourdomain.model.LogEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the activity (audit) log table.
 */
public class AuditLogDao {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void insert(String username, String action, String details) {
        String sql = "INSERT INTO activity_logs (username, action, details, logged_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, action);
            ps.setString(3, details);
            ps.setString(4, LocalDateTime.now().format(TIMESTAMP));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Log error: " + e.getMessage());
        }
    }

    /** Newest entries first. */
    public List<LogEntry> getRecent(int limit) {
        List<LogEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM activity_logs ORDER BY id DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new LogEntry(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("action"),
                            rs.getString("details"),
                            rs.getString("logged_at")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Load error: " + e.getMessage());
        }
        return list;
    }
}
