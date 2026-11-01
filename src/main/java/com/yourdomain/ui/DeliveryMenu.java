package com.yourdomain.ui;

import com.yourdomain.model.User;
import com.yourdomain.model.WaterOrder;

import java.util.List;

/**
 * Menu for Delivery Staff: view their assigned deliveries and update the delivery status.
 * Cannot touch payments, prices or user accounts.
 */
public class DeliveryMenu {

    private static final String LINE = "==================================================";
    private static final String[] STATUSES = {"PENDING", "OUT_FOR_DELIVERY", "DELIVERED"};

    private final AppContext ctx;
    private final User currentUser;

    public DeliveryMenu(AppContext ctx, User currentUser) {
        this.ctx = ctx;
        this.currentUser = currentUser;
    }

    public void show() {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println();
            System.out.println(LINE);
            System.out.println("          DELIVERY STAFF MENU");
            System.out.println("          User: " + currentUser.username());
            System.out.println(LINE);
            System.out.println("[1] View My Assigned Deliveries");
            System.out.println("[2] Update Delivery Status");
            System.out.println("[0] Logout");
            System.out.println(LINE);
            String choice = InputHelper.readLine(ctx.scanner(), "Enter choice: ");

            switch (choice) {
                case "1" -> viewAssignedDeliveries();
                case "2" -> updateDeliveryStatus();
                case "0" -> loggedIn = false;
                default -> System.out.println("\nINVALID CHOICE: Please enter 1, 2, or 0.");
            }
        }
    }

    private void viewAssignedDeliveries() {
        List<WaterOrder> mine = ctx.orderDao().getAssignedDeliveries(currentUser.id());
        System.out.println("\n--- MY ASSIGNED DELIVERIES ---");
        OrderMenu.printDeliveryTable(mine);
        System.out.println("Total Assigned: " + mine.size());
    }

    private void updateDeliveryStatus() {
        List<WaterOrder> mine = ctx.orderDao().getAssignedDeliveries(currentUser.id());
        System.out.println("\n--- UPDATE DELIVERY STATUS ---");
        if (mine.isEmpty()) {
            System.out.println("You have no assigned deliveries.");
            return;
        }
        OrderMenu.printDeliveryTable(mine);

        while (true) {
            int orderId = InputHelper.readInt(ctx.scanner(), "Enter Order ID to update (0 to cancel): ");
            if (orderId == 0) {
                System.out.println("Update cancelled.");
                return;
            }
            WaterOrder order = null;
            for (WaterOrder o : mine) {
                if (o.id() == orderId) {
                    order = o;
                }
            }
            if (order == null) {
                System.out.println("ERROR: Order " + orderId + " is not assigned to you. Try again.");
                continue;
            }

            System.out.println("Current status: " + order.status());
            System.out.println("Select new status:");
            for (int i = 0; i < STATUSES.length; i++) {
                System.out.println("  " + (i + 1) + ". " + STATUSES[i]);
            }
            int pick = InputHelper.readInt(ctx.scanner(), "Choose (0 to cancel): ");
            if (pick == 0) {
                System.out.println("Update cancelled.");
                return;
            }
            if (pick < 1 || pick > STATUSES.length) {
                System.out.println("Invalid choice.");
                continue;
            }

            String newStatus = STATUSES[pick - 1];
            if (ctx.orderDao().updateStatus(orderId, newStatus)) {
                System.out.println("\nSUCCESS: Order #" + orderId + " is now " + newStatus + ".");
                ctx.logger().log(currentUser, "DELIVERY_STATUS_UPDATED",
                        "Order #" + orderId + ": " + order.status() + " -> " + newStatus);
            } else {
                System.out.println("\nFAILED: Could not update the delivery status.");
            }
            return;
        }
    }
}
