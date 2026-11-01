package com.yourdomain.model;

/** Summary figures for one day. */
public record DailyReport(String date,
                          double totalSales,
                          int containersSold,
                          int availableStock,
                          int completedOrders,
                          int deliveryOrders) {
}
