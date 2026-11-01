package com.yourdomain.ui;

import com.yourdomain.model.User;

import java.util.Optional;

/**
 * Handles registration, login and routing each user to the menu for their role.
 * Main only calls start(); all startup interaction lives here.
 */
public class Authentication {

    private static final String LINE = "==================================================";

    private final AppContext ctx;

    public Authentication(AppContext ctx) {
        this.ctx = ctx;
    }

    /** Main menu loop: Login, Register, Exit. */
    public void start() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println(LINE);
            System.out.println("        WATER REFILLING STATION SYSTEM");
            System.out.println(LINE);
            System.out.println("[1] Login");
            System.out.println("[2] Register Account");
            System.out.println("[3] Exit System");
            System.out.println(LINE);
            String choice = InputHelper.readLine(ctx.scanner(), "Enter choice: ");

            switch (choice) {
                case "1" -> {
                    login();
                }
                case "2" -> {
                    register();
                }
                case "3" -> {
                    System.out.println("\nExiting system... Thank you!");
                    running = false;
                }
                default -> {
                    System.out.println("\nINVALID CHOICE: Please enter 1, 2, or 3.");
                }
            }
        }
    }

    // ------------------------------------------------------------------ LOGIN

    private void login() {
        System.out.println("\n--- LOGIN ---");
        String username = InputHelper.readLine(ctx.scanner(), "Username: ");
        String password = InputHelper.readLine(ctx.scanner(), "Password: ");

        Optional<User> result = ctx.userDao().login(username, password);
        if (result.isEmpty()) {
            System.out.println("\nInvalid username or password.");
            if (!username.isEmpty()) {
                ctx.logger().log(username, "LOGIN_FAILED", "Wrong username or password");
            }
            return;
        }

        User user = result.get();
        switch (user.status()) {
            case "PENDING" -> {
                System.out.println("\nYour account is still PENDING. Please wait for an administrator to approve it.");
                return;
            }
            case "INACTIVE" -> {
                System.out.println("\nYour account has been deactivated. Please contact the administrator.");
                return;
            }
            default -> {
                // APPROVED: continue below
            }
        }

        System.out.println("\nLogin successful! Welcome, " + user.username() + " (" + roleLabel(user.role()) + ").");
        ctx.logger().log(user, "LOGIN", "Logged in as " + user.role());

        routeToMenu(user);

        ctx.logger().log(user, "LOGOUT", "Logged out");
        System.out.println("\nLogged out successfully.");
    }

    /** Sends the logged-in user to the menu that matches their assigned role. */
    private void routeToMenu(User user) {
        switch (user.role()) {
            case "ADMIN" -> {
                new AdminMenu(ctx, user).show();
            }
            case "STAFF" -> {
                new StaffMenu(ctx, user).show();
            }
            case "DELIVERY" -> {
                new DeliveryMenu(ctx, user).show();
            }
            default -> {
                System.out.println("Unknown role '" + user.role() + "'. Contact the administrator.");
            }
        }
    }

    // --------------------------------------------------------------- REGISTER

    private void register() {
        System.out.println("\n--- REGISTER ACCOUNT ---");

        String username = InputHelper.readLine(ctx.scanner(), "Username (3-15 letters/numbers/underscore): ");
        if (!username.matches("[A-Za-z0-9_]{3,15}")) {
            System.out.println("Invalid username. Registration cancelled.");
            return;
        }
        if (ctx.userDao().usernameExists(username)) {
            System.out.println("Username already taken. Registration cancelled.");
            return;
        }

        String password = InputHelper.readLine(ctx.scanner(), "Password (at least 4 characters): ");
        if (password.length() < 4) {
            System.out.println("Password is too short. Registration cancelled.");
            return;
        }

        System.out.println("Select role:");
        System.out.println("  1. Administrator");
        System.out.println("  2. Staff / Cashier");
        System.out.println("  3. Delivery Staff");
        String roleChoice = InputHelper.readLine(ctx.scanner(), "Choose role (0 to cancel): ");

        String role;
        switch (roleChoice) {
            case "1" -> {
                role = "ADMIN";
            }
            case "2" -> {
                role = "STAFF";
            }
            case "3" -> {
                role = "DELIVERY";
            }
            default -> {
                System.out.println("Registration cancelled.");
                return;
            }
        }

        if (ctx.userDao().register(username, password, role)) {
            System.out.println("\nAccount created! Status: PENDING (waiting for administrator approval).");
            ctx.logger().log(username, "REGISTER", "New " + role + " account registered (PENDING)");
        } else {
            System.out.println("\nRegistration failed.");
        }
    }

    static String roleLabel(String role) {
        return switch (role) {
            case "ADMIN" -> {
                yield "Administrator";
            }
            case "STAFF" -> {
                yield "Staff/Cashier";
            }
            case "DELIVERY" -> {
                yield "Delivery Staff";
            }
            default -> {
                yield role;
            }
        };
    }
}
