package com.yourdomain.dao;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Makes sure every required table exists before the CLI menu starts.
 * On the very first run it also adds a default administrator account and a few
 * sample products so the system can be used right away.
 */
public final class DatabaseInitializer {

    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    private static final String CREATE_USERS = """
            CREATE TABLE IF NOT EXISTS users (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                role     TEXT NOT NULL CHECK (role IN ('ADMIN', 'STAFF', 'DELIVERY')),
                status   TEXT NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING', 'APPROVED', 'INACTIVE'))
            )
            """;

    private static final String CREATE_CUSTOMERS = """
            CREATE TABLE IF NOT EXISTS customers (
                id      INTEGER PRIMARY KEY AUTOINCREMENT,
                name    TEXT NOT NULL,
                phone   TEXT NOT NULL,
                address TEXT NOT NULL,
                status  TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE'))
            )
            """;

    private static final String CREATE_PRODUCTS = """
            CREATE TABLE IF NOT EXISTS products (
                id    INTEGER PRIMARY KEY AUTOINCREMENT,
                name  TEXT NOT NULL UNIQUE,
                price REAL NOT NULL,
                stock INTEGER NOT NULL DEFAULT 0
            )
            """;

    private static final String CREATE_ORDERS = """
            CREATE TABLE IF NOT EXISTS orders (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id      INTEGER NOT NULL REFERENCES customers(id),
                product_id       INTEGER NOT NULL REFERENCES products(id),
                quantity         INTEGER NOT NULL,
                total            REAL NOT NULL,
                order_type       TEXT NOT NULL CHECK (order_type IN ('PICKUP', 'DELIVERY')),
                status           TEXT NOT NULL DEFAULT 'PENDING'
                                 CHECK (status IN ('PENDING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'COMPLETED')),
                payment_status   TEXT NOT NULL DEFAULT 'UNPAID' CHECK (payment_status IN ('UNPAID', 'PAID')),
                delivery_user_id INTEGER REFERENCES users(id),
                created_at       TEXT NOT NULL
            )
            """;

    private static final String CREATE_SALES = """
            CREATE TABLE IF NOT EXISTS sales (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id     INTEGER NOT NULL REFERENCES orders(id),
                amount       REAL NOT NULL,
                paid_at      TEXT NOT NULL,
                processed_by TEXT NOT NULL
            )
            """;

    private static final String CREATE_LOGS = """
            CREATE TABLE IF NOT EXISTS activity_logs (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                username  TEXT NOT NULL,
                action    TEXT NOT NULL,
                details   TEXT,
                logged_at TEXT NOT NULL
            )
            """;

    private DatabaseInitializer() {
    }

    /** Call once at startup, before any menu is shown. */
    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_USERS);
            stmt.execute(CREATE_CUSTOMERS);
            stmt.execute(CREATE_PRODUCTS);
            stmt.execute(CREATE_ORDERS);
            stmt.execute(CREATE_SALES);
            stmt.execute(CREATE_LOGS);

            seedDefaultAdmin(conn);
            seedSampleProducts(conn);
        } catch (SQLException e) {
            System.out.println("Database setup failed: " + e.getMessage());
        }
    }

    private static boolean tableIsEmpty(Connection conn, String table) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    private static void seedDefaultAdmin(Connection conn) throws SQLException {
        if (!tableIsEmpty(conn, "users")) {
            return;
        }
        String sql = "INSERT INTO users (username, password, role, status) VALUES (?, ?, 'ADMIN', 'APPROVED')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DEFAULT_ADMIN_USERNAME);
            ps.setString(2, PasswordUtil.hash(DEFAULT_ADMIN_PASSWORD));
            ps.executeUpdate();
        }
        System.out.println("First run: default admin account created ("
                + DEFAULT_ADMIN_USERNAME + " / " + DEFAULT_ADMIN_PASSWORD + ").");
    }

    private static void seedSampleProducts(Connection conn) throws SQLException {
        if (!tableIsEmpty(conn, "products")) {
            return;
        }
        String sql = "INSERT INTO products (name, price, stock) VALUES (?, ?, ?)";
        Object[][] samples = {
                {"5-Gallon Refill", 25.00, 200},
                {"5-Gallon Container", 250.00, 50},
                {"Bottled Water 1L", 20.00, 100}
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] s : samples) {
                ps.setString(1, (String) s[0]);
                ps.setDouble(2, (Double) s[1]);
                ps.setInt(3, (Integer) s[2]);
                ps.executeUpdate();
            }
        }
    }
}
