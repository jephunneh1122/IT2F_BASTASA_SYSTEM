package com.yourdomain.model;

/** One recorded sales transaction (a paid order). */
public record SaleRecord(int id,
                         int orderId,
                         String customerName,
                         String productName,
                         int quantity,
                         double amount,
                         String paidAt) {
}
