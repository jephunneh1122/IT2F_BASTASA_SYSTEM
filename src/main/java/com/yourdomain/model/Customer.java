package com.yourdomain.model;

/** One row of the customers table. Status is ACTIVE or INACTIVE. */
public record Customer(int id, String name, String phone, String address, String status) {
}
