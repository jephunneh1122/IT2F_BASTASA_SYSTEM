package com.yourdomain.ui;

import com.yourdomain.model.LogEntry;
import com.yourdomain.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu for the Administrator role: full access to every feature.
 */
public class AdminMenu {

    private static final String LINE = "==================================================";

    private final AppContext ctx;
    private final User currentUser;

    public AdminMenu(AppContext ctx, User currentUser) {
        this.ctx = ctx;
        this.currentUser = currentUser;
    }

    public void show() {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println();
            System.out.println(LINE);
            System.out.println("          ADMINISTRATOR MENU");
            System.out.println("          User: " + currentUser.username());
            System.out.println(LINE);
            System.out.println("[1]  View All System Users");
            System.out.println("[2]  View Pending Registrations");
            System.out.println("[3]  Approve User Account");
            System.out.println("[4]  Deactivate User Account");
            System.out.println("[5]  Manage Customers");
            System.out.println("[6]  Manage Inventory");
            System.out.println("[7]  Manage Orders & Payments");
            System.out.println("[8]  View Delivery Records");
            System.out.println("[9]  Reports (View / Export)");
            System.out.println("[10] View Activity Logs");
            System.out.println("[0]  Logout");
            System.out.println(LINE);
            String choice = InputHelper.readLine(ctx.scanner(), "Enter choice: ");

            switch (choice) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingRegistrations();
                case "3" -> approveUserAccount();
                case "4" -> deactivateUserAccount();
                case "5" -> new CustomerMenu(ctx, currentUser).show();
                case "6" -> new InventoryMenu(ctx, currentUser).show();
                case "7" -> new OrderMenu(ctx, currentUser).show();
                case "8" -> OrderMenu.showDeliveryRecords(ctx);
                case "9" -> new ReportMenu(ctx, currentUser).show();
                case "10" -> viewActivityLogs();
                case "0" -> loggedIn = false;
                default -> System.out.println("\nINVALID CHOICE: Please select a number from the menu.");
            }
        }
    }

    // ------------------------------------------------------------ USER ACCOUNTS

    private void viewAllUsers() {
        List<User> users = ctx.userDao().getAllUsers();
        System.out.println("\n--- ALL SYSTEM USERS ---");
        printUserTable(users);
        System.out.println("Total Users: " + users.size());
    }

    private void viewPendingRegistrations() {
        List<User> pending = ctx.userDao().getPendingUsers();
        System.out.println("\n--- PENDING REGISTRATIONS ---");
        if (pending.isEmpty()) {
            System.out.println("No accounts are awaiting approval.");
            return;
        }
        printUserTable(pending);
        System.out.println("Total Pending: " + pending.size());
    }

    private void approveUserAccount() {
        List<User> pending = ctx.userDao().getPendingUsers();
        System.out.println("\n--- APPROVE USER ACCOUNT ---");
        if (pending.isEmpty()) {
            System.out.println("No accounts are awaiting approval.");
            return;
        }
        printUserTable(pending);

        while (true) {
            int targetId = InputHelper.readInt(ctx.scanner(), "Enter Target User ID to approve (0 to cancel): ");
            if (targetId == 0) {
                System.out.println("Approval cancelled.");
                return;
            }

            User target = findInList(pending, targetId);
            if (target == null) {
                System.out.println("ERROR: User ID " + targetId + " is not in the pending list. Try again.");
                continue;
            }

            if (ctx.userDao().approveUser(targetId)) {
                System.out.println("\nSUCCESS: Account '" + target.username() + "' is now APPROVED.");
                ctx.logger().log(currentUser, "USER_APPROVED",
                        "Approved account '" + target.username() + "' (" + target.role() + ")");
            } else {
                System.out.println("\nFAILED: Could not approve the account.");
            }
            return;
        }
    }

    private void deactivateUserAccount() {
        List<User> candidates = new ArrayList<>();
        for (User u : ctx.userDao().getAllUsers()) {
            if (!u.status().equals("INACTIVE") && u.id() != currentUser.id()) {
                candidates.add(u);
            }
        }
        System.out.println("\n--- DEACTIVATE USER ACCOUNT ---");
        if (candidates.isEmpty()) {
            System.out.println("There are no other active accounts to deactivate.");
            return;
        }
        printUserTable(candidates);

        while (true) {
            int targetId = InputHelper.readInt(ctx.scanner(), "Enter Target User ID to deactivate (0 to cancel): ");
            if (targetId == 0) {
                System.out.println("Deactivation cancelled.");
                return;
            }
            User target = findInList(candidates, targetId);
            if (target == null) {
                System.out.println("ERROR: User ID " + targetId + " cannot be deactivated. Try again.");
                continue;
            }
            if (!InputHelper.confirm(ctx.scanner(), "Deactivate '" + target.username() + "'?")) {
                System.out.println("Deactivation cancelled.");
                return;
            }
            if (ctx.userDao().deactivateUser(targetId)) {
                System.out.println("\nSUCCESS: Account '" + target.username() + "' has been deactivated.");
                ctx.logger().log(currentUser, "USER_DEACTIVATED", "Deactivated account '" + target.username() + "'");
            } else {
                System.out.println("\nFAILED: Could not deactivate the account.");
            }
            return;
        }
    }

    private User findInList(List<User> users, int id) {
        for (User u : users) {
            if (u.id() == id) {
                return u;
            }
        }
        return null;
    }

    // ------------------------------------------------------------ ACTIVITY LOG

    private void viewActivityLogs() {
        List<LogEntry> logs = ctx.auditLogDao().getRecent(100);
        System.out.println("\n--- USER ACTIVITY LOG (latest 100) ---");
        List<String[]> rows = new ArrayList<>();
        for (LogEntry e : logs) {
            rows.add(new String[]{String.valueOf(e.id()), e.loggedAt(), e.username(), e.action(), e.details()});
        }
        ConsoleTable.print(
                new String[]{"ID", "TIMESTAMP", "USER", "ACTION", "DETAILS"},
                new int[]{5, 19, 15, 24, 46},
                rows);
    }

    // ------------------------------------------------------ TABLE RENDER HELPER

    /**
     * Prints any list of users as an aligned table using the column widths
     * %-5s (ID), %-15s (USERNAME), %-12s (ROLE) and %-10s (STATUS).
     */
    public static void printUserTable(List<User> users) {
        String border = ConsoleTable.border(5, 15, 12, 10);
        System.out.println(border);
        System.out.printf("| %-5s | %-15s | %-12s | %-10s |%n", "ID", "USERNAME", "ROLE", "STATUS");
        System.out.println(border);
        if (users.isEmpty()) {
            System.out.println("| " + String.format("%-" + (border.length() - 4) + "s", "No users found.") + " |");
        }
        for (User u : users) {
            System.out.printf("| %-5s | %-15s | %-12s | %-10s |%n",
                    u.id(),
                    ConsoleTable.fit(u.username(), 15),
                    ConsoleTable.fit(u.role(), 12),
                    ConsoleTable.fit(u.status(), 10));
        }
        System.out.println(border);
    }
}
