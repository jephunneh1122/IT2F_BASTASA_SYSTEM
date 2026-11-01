package com.yourdomain.ui;

import com.yourdomain.model.Product;
import com.yourdomain.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Water inventory. Admin and Staff can view inventory and update stock quantities;
 * only the Administrator can change prices or add new products.
 */
public class InventoryMenu {

    private static final String LINE = "==================================================";

    private final AppContext ctx;
    private final User currentUser;
    private final boolean isAdmin;

    public InventoryMenu(AppContext ctx, User currentUser) {
        this.ctx = ctx;
        this.currentUser = currentUser;
        this.isAdmin = currentUser.role().equals("ADMIN");
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println(LINE);
            System.out.println("          WATER INVENTORY");
            System.out.println(LINE);
            System.out.println("[1] View Inventory");
            System.out.println("[2] Update Stock Quantity");
            if (isAdmin) {
                System.out.println("[3] Update Product Price");
                System.out.println("[4] Add New Product");
            }
            System.out.println("[0] Back");
            System.out.println(LINE);
            String choice = InputHelper.readLine(ctx.scanner(), "Enter choice: ");

            switch (choice) {
                case "1" -> viewInventory();
                case "2" -> updateStock();
                case "3" -> {
                    if (isAdmin) {
                        updatePrice();
                    } else {
                        System.out.println("\nINVALID CHOICE: Please select a number from the menu.");
                    }
                }
                case "4" -> {
                    if (isAdmin) {
                        addProduct();
                    } else {
                        System.out.println("\nINVALID CHOICE: Please select a number from the menu.");
                    }
                }
                case "0" -> running = false;
                default -> System.out.println("\nINVALID CHOICE: Please select a number from the menu.");
            }
        }
    }

    private void viewInventory() {
        List<Product> products = ctx.productDao().getAllProducts();
        System.out.println("\n--- WATER INVENTORY ---");
        printProductTable(products);
        int totalStock = 0;
        for (Product p : products) {
            totalStock += p.stock();
        }
        System.out.println("Total Products: " + products.size() + " | Total Stock Quantity: " + totalStock);
    }

    private void updateStock() {
        System.out.println("\n--- UPDATE STOCK QUANTITY ---");
        Product product = pickProduct();
        if (product == null) {
            return;
        }
        System.out.println("Current stock of '" + product.name() + "': " + product.stock());
        System.out.println("  1. Add stock (restock)");
        System.out.println("  2. Set exact quantity");

        int mode = InputHelper.readInt(ctx.scanner(), "Choose (0 to cancel): ");
        int newStock;
        if (mode == 1) {
            int add = InputHelper.readInt(ctx.scanner(), "Enter quantity to add: ");
            newStock = product.stock() + add;
        } else if (mode == 2) {
            newStock = InputHelper.readInt(ctx.scanner(), "Enter new stock quantity: ");
        } else {
            System.out.println("Update cancelled.");
            return;
        }

        if (ctx.productDao().setStock(product.id(), newStock)) {
            System.out.println("\nSUCCESS: Stock updated (" + product.stock() + " -> " + newStock + ").");
            ctx.logger().log(currentUser, "STOCK_UPDATED",
                    "'" + product.name() + "' stock " + product.stock() + " -> " + newStock);
        } else {
            System.out.println("\nFAILED: Stock was not updated.");
        }
    }

    private void updatePrice() {
        System.out.println("\n--- UPDATE PRODUCT PRICE ---");
        Product product = pickProduct();
        if (product == null) {
            return;
        }
        System.out.println("Current price of '" + product.name() + "': " + String.format("%.2f", product.price()));
        double newPrice = InputHelper.readPositiveDouble(ctx.scanner(), "Enter new price: ");

        if (ctx.productDao().setPrice(product.id(), newPrice)) {
            System.out.println("\nSUCCESS: Price updated!");
            ctx.logger().log(currentUser, "PRICE_UPDATED", "'" + product.name() + "' price "
                    + String.format("%.2f", product.price()) + " -> " + String.format("%.2f", newPrice));
        } else {
            System.out.println("\nFAILED: Price was not updated.");
        }
    }

    private void addProduct() {
        System.out.println("\n--- ADD NEW PRODUCT ---");
        String name = InputHelper.readRequired(ctx.scanner(), "Enter Product Name: ");
        double price = InputHelper.readPositiveDouble(ctx.scanner(), "Enter Price: ");
        int stock = InputHelper.readInt(ctx.scanner(), "Enter Starting Stock: ");

        if (ctx.productDao().addProduct(name, price, stock)) {
            System.out.println("\nSUCCESS: Product added to inventory!");
            ctx.logger().log(currentUser, "PRODUCT_ADDED", "Added product '" + name + "'");
        } else {
            System.out.println("\nFAILED: Product was not saved (the name may already exist).");
        }
    }

    /** Shows the products and returns the chosen one, or null if cancelled. */
    private Product pickProduct() {
        List<Product> products = ctx.productDao().getAllProducts();
        if (products.isEmpty()) {
            System.out.println("No products in inventory.");
            return null;
        }
        printProductTable(products);
        while (true) {
            int id = InputHelper.readInt(ctx.scanner(), "Enter Product ID (0 to cancel): ");
            if (id == 0) {
                System.out.println("Cancelled.");
                return null;
            }
            for (Product p : products) {
                if (p.id() == id) {
                    return p;
                }
            }
            System.out.println("ERROR: Product ID " + id + " was not found. Try again.");
        }
    }

    /** Prints products as an aligned table. */
    public static void printProductTable(List<Product> products) {
        List<String[]> rows = new ArrayList<>();
        for (Product p : products) {
            rows.add(new String[]{String.valueOf(p.id()), p.name(),
                    String.format("%.2f", p.price()), String.valueOf(p.stock())});
        }
        ConsoleTable.print(
                new String[]{"ID", "PRODUCT", "PRICE", "STOCK"},
                new int[]{5, 28, 10, 7},
                rows);
    }
}
