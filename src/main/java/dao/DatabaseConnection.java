package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Handles the SQLite connection and creates the users table.
 */
public class DatabaseConnection {

    // Basic variables so values are written only once (avoids typos)
    private static final String DB_URL = "jdbc:sqlite:users.db";

    private static final String CREATE_USERS_TABLE = """
            CREATE TABLE IF NOT EXISTS users (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                roles    TEXT NOT NULL CHECK (roles IN ('TEACHER', 'STUDENT')),
                status   TEXT NOT NULL DEFAULT 'PENDING'
            )
            """;

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /** Call once when the app starts. */
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_USERS_TABLE);
        } catch (SQLException e) {
            System.out.println("Database setup failed: " + e.getMessage());
        }
    }
}
