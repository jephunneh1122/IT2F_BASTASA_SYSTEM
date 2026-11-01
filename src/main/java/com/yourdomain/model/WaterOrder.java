package com.yourdomain.model;

/**
 * One customer order, joined with customer, product and delivery staff details.
 * orderType:     PICKUP or DELIVERY
 * status:        PENDING, OUT_FOR_DELIVERY, DELIVERED, COMPLETED
 * paymentStatus: UNPAID or PAID
 */
public record WaterOrder(int id,
                         int customerId,
                         String customerName,
                         String customerPhone,
                         String customerAddress,
                         int productId,
                         String productName,
                         int quantity,
                         double total,
                         String orderType,
                         String status,
                         String paymentStatus,
                         Integer deliveryUserId,
                         String deliveryUserName,
                         String createdAt) {
}
