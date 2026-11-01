package com.yourdomain.dao;

import com.yourdomain.model.DailyReport;
import com.yourdomain.model.SaleRecord;
import com.yourdomain.model.WaterOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All SQL for orders, deliveries, sales and the daily summary report.
 * Operations that touch more than one table run inside a single transaction.
 */
public class OrderDao {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String SELECT_ORDERS = """
            SELECT o.id, o.customer_id, c.name AS customer_name, c.phone AS customer_phone,
                   c.address AS customer_address, o.product_id, p.name AS product_name,
                   o.quantity, o.total, o.order_type, o.status, o.payment_status,
                   o.delivery_user_id, u.username AS delivery_user, o.created_at
            FROM orders o
            JOIN customers c ON c.id = o.customer_id
            JOIN products p ON p.id = o.product_id
            LEFT JOIN users u ON u.id = o.delivery_user_id
            """;

    private static final String SELECT_SALES = """
            SELECT s.id, s.order_id, c.name AS customer_name, p.name AS product_name,
                   o.quantity, s.amount, s.paid_at
            FROM sales s
            JOIN orders o ON o.id = s.order_id
            JOIN customers c ON c.id = o.customer_id
            JOIN products p ON p.id = o.product_id
            """;

    // ------------------------------------------------------------------ CREATE

    /**
     * Records a new order and takes the quantity out of stock in one transaction.
     * Returns the new order ID, or -1 if it could not be saved (for example not enough stock).
     */
    public int createOrder(int customerId, int productId, int quantity,
                           String orderType, Integer deliveryUserId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                double price;
                try (PreparedStatement ps = conn.prepareStatement("SELECT price FROM products WHERE id = ?")) {
                    ps.setInt(1, productId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return -1;
                        }
                        price = rs.getDouble(1);
                    }
                }

                String takeStock = "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?";
                try (PreparedStatement ps = conn.prepareStatement(takeStock)) {
                    ps.setInt(1, quantity);
                    ps.setInt(2, productId);
                    ps.setInt(3, quantity);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return -1;
                    }
                }

                double total = Math.round(price * quantity * 100.0) / 100.0;
                String insert = "INSERT INTO orders (customer_id, product_id, quantity, total, order_type, "
                        + "status, payment_status, delivery_user_id, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, 'PENDING', 'UNPAID', ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, productId);
                    ps.setInt(3, quantity);
                    ps.setDouble(4, total);
                    ps.setString(5, orderType);
                    if (deliveryUserId == null) {
                        ps.setNull(6, Types.INTEGER);
                    } else {
                        ps.setInt(6, deliveryUserId);
                    }
                    ps.setString(7, LocalDateTime.now().format(TIMESTAMP));
                    ps.executeUpdate();
                }

                int newId;
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                    rs.next();
                    newId = rs.getInt(1);
                }
                conn.commit();
                return newId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("Save error: " + e.getMessage());
            return -1;
        }
    }

    // -------------------------------------------------------------------- READ

    public List<WaterOrder> getAllOrders() {
        return queryOrders(SELECT_ORDERS + " ORDER BY o.id", null);
    }

    public List<WaterOrder> getOrdersByCustomer(int customerId) {
        return queryOrders(SELECT_ORDERS + " WHERE o.customer_id = ? ORDER BY o.id", customerId);
    }

    /** Every order that is marked for delivery. */
    public List<WaterOrder> getDeliveryOrders() {
        return queryOrders(SELECT_ORDERS + " WHERE o.order_type = 'DELIVERY' ORDER BY o.id", null);
    }

    /** Delivery orders assigned to one delivery staff account. */
    public List<WaterOrder> getAssignedDeliveries(int deliveryUserId) {
        return queryOrders(SELECT_ORDERS
                + " WHERE o.order_type = 'DELIVERY' AND o.delivery_user_id = ? ORDER BY o.id", deliveryUserId);
    }

    public Optional<WaterOrder> findById(int orderId) {
        List<WaterOrder> found = queryOrders(SELECT_ORDERS + " WHERE o.id = ?", orderId);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    public List<SaleRecord> getAllSales() {
        return querySales(SELECT_SALES + " ORDER BY s.id", null);
    }

    /** Sales whose payment was made on the given date (yyyy-MM-dd). */
    public List<SaleRecord> getSalesByDate(String date) {
        return querySales(SELECT_SALES + " WHERE substr(s.paid_at, 1, 10) = ? ORDER BY s.id", date);
    }

    /** Builds the summary figures for one date (yyyy-MM-dd). */
    public DailyReport getDailyReport(String date) {
        double totalSales = 0;
        int containers = 0;
        int stock = 0;
        int completed = 0;
        int deliveries = 0;
        try (Connection conn = DatabaseConnection.getConnection()) {
            totalSales = scalar(conn,
                    "SELECT COALESCE(SUM(amount), 0) FROM sales WHERE substr(paid_at, 1, 10) = ?", date);
            containers = (int) scalar(conn,
                    "SELECT COALESCE(SUM(o.quantity), 0) FROM sales s JOIN orders o ON o.id = s.order_id "
                            + "WHERE substr(s.paid_at, 1, 10) = ?", date);
            stock = (int) scalar(conn, "SELECT COALESCE(SUM(stock), 0) FROM products", null);
            completed = (int) scalar(conn,
                    "SELECT COUNT(*) FROM orders WHERE status IN ('COMPLETED', 'DELIVERED') "
                            + "AND substr(created_at, 1, 10) = ?", date);
            deliveries = (int) scalar(conn,
                    "SELECT COUNT(*) FROM orders WHERE order_type = 'DELIVERY' "
                            + "AND substr(created_at, 1, 10) = ?", date);
        } catch (SQLException e) {
            System.out.println("Report error: " + e.getMessage());
        }
        return new DailyReport(date, totalSales, containers, stock, completed, deliveries);
    }

    // ------------------------------------------------------------------ UPDATE

    public boolean updateStatus(int orderId, String newStatus) {
        return updateOrder("UPDATE orders SET status = ? WHERE id = ?", newStatus, orderId);
    }

    public boolean assignDeliveryStaff(int orderId, int deliveryUserId) {
        String sql = "UPDATE orders SET delivery_user_id = ? WHERE id = ? AND order_type = 'DELIVERY'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deliveryUserId);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Update error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Marks an order as PAID and records the sale in one transaction.
     * Pick-up orders become COMPLETED once paid. Returns false if the order is
     * missing or already paid.
     */
    public boolean processPayment(int orderId, String processedBy) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                double total;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT total FROM orders WHERE id = ? AND payment_status = 'UNPAID'")) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        total = rs.getDouble(1);
                    }
                }

                String markPaid = "UPDATE orders SET payment_status = 'PAID', "
                        + "status = CASE WHEN order_type = 'PICKUP' THEN 'COMPLETED' ELSE status END "
                        + "WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(markPaid)) {
                    ps.setInt(1, orderId);
                    ps.executeUpdate();
                }

                String insertSale = "INSERT INTO sales (order_id, amount, paid_at, processed_by) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSale)) {
                    ps.setInt(1, orderId);
                    ps.setDouble(2, total);
                    ps.setString(3, LocalDateTime.now().format(TIMESTAMP));
                    ps.setString(4, processedBy);
                    ps.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("Payment error: " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------ DELETE

    /**
     * Removes a canceled or incorrect order. Its sale record is removed too, and the
     * quantity goes back into stock unless the order was already delivered/completed.
     */
    public boolean deleteOrder(int orderId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int productId;
                int quantity;
                String status;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT product_id, quantity, status FROM orders WHERE id = ?")) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        productId = rs.getInt("product_id");
                        quantity = rs.getInt("quantity");
                        status = rs.getString("status");
                    }
                }

                boolean goodsHandedOver = status.equals("DELIVERED") || status.equals("COMPLETED");
                if (!goodsHandedOver) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE products SET stock = stock + ? WHERE id = ?")) {
                        ps.setInt(1, quantity);
                        ps.setInt(2, productId);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sales WHERE order_id = ?")) {
                    ps.setInt(1, orderId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM orders WHERE id = ?")) {
                    ps.setInt(1, orderId);
                    ps.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("Delete error: " + e.getMessage());
            return false;
        }
    }

    // ----------------------------------------------------------------- HELPERS

    private boolean updateOrder(String sql, String text, int orderId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, text);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Update error: " + e.getMessage());
            return false;
        }
    }

    private double scalar(Connection conn, String sql, String param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        }
    }

    private List<WaterOrder> queryOrders(String sql, Integer idParam) {
        List<WaterOrder> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (idParam != null) {
                ps.setInt(1, idParam);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int deliveryId = rs.getInt("delivery_user_id");
                    Integer deliveryUserId = rs.wasNull() ? null : deliveryId;
                    list.add(new WaterOrder(
                            rs.getInt("id"),
                            rs.getInt("customer_id"),
                            rs.getString("customer_name"),
                            rs.getString("customer_phone"),
                            rs.getString("customer_address"),
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getInt("quantity"),
                            rs.getDouble("total"),
                            rs.getString("order_type"),
                            rs.getString("status"),
                            rs.getString("payment_status"),
                            deliveryUserId,
                            rs.getString("delivery_user"),
                            rs.getString("created_at")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Load error: " + e.getMessage());
        }
        return list;
    }

    private List<SaleRecord> querySales(String sql, String param) {
        List<SaleRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SaleRecord(
                            rs.getInt("id"),
                            rs.getInt("order_id"),
                            rs.getString("customer_name"),
                            rs.getString("product_name"),
                            rs.getInt("quantity"),
                            rs.getDouble("amount"),
                            rs.getString("paid_at")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Load error: " + e.getMessage());
        }
        return list;
    }
}
