package com.yourdomain.model;

/**
 * One row of the users table.
 * Roles:    ADMIN, STAFF (Staff/Cashier), DELIVERY (Delivery Staff)
 * Statuses: PENDING, APPROVED, INACTIVE
 */
public record User(int id, String username, String password, String role, String status) {
}
