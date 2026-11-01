package com.yourdomain.ui;

import com.yourdomain.model.User;

public class StaffMenu {

    private final AppContext ctx;
    private final User user;

    public StaffMenu(AppContext ctx, User user) {
        this.ctx = ctx;
        this.user = user;
    }

    public void show() {
        boolean running = true;

        while (running) {
            System.out.println("\n==================================");
            System.out.println("          STAFF DASHBOARD");
            System.out.println("==================================");
            System.out.println("Welcome, " + user.username());
            System.out.println("[1] Manage Orders");
            System.out.println("[2] View Inventory");
            System.out.println("[3] View Reports");
            System.out.println("[0] Logout");
            System.out.println("==================================");

            String choice = InputHelper.readLine(ctx.scanner(), "Enter choice: ");

            switch (choice) {
                case "1" -> new OrderMenu(ctx, user).show();
                case "2" -> new InventoryMenu(ctx, user).show();
                case "3" -> new ReportMenu(ctx, user).show();
                case "0" -> running = false;
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }
}

