package com.yourdomain.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens connections to the SQLite database file.
 * Table creation lives in {@link DatabaseInitializer}.
 */
public final class DatabaseConnection {

    private static final String DB_URL = "jdbc:sqlite:water_station.db";

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
