package com.yourdomain.model;

/** One row of the products (water inventory) table. */
public record Product(int id, String name, double price, int stock) {
}
