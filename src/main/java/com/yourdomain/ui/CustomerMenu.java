package com.yourdomain.ui;

import com.yourdomain.model.Customer;
import com.yourdomain.model.User;
import com.yourdomain.model.WaterOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * Customer records. Admin and Staff can add, view and update customers;
 * only the Administrator can deactivate a customer.
 */
public class CustomerMenu {

    private static final String LINE = "==================================================";

    private final AppContext ctx;
    private final User currentUser;
    private final boolean isAdmin;

    public CustomerMenu(AppContext ctx, User currentUser) {
        this.ctx = ctx;
        this.currentUser = currentUser;
        this.isAdmin = currentUser.role().equals("ADMIN");
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println(LINE);
            System.out.println("          CUSTOMER MANAGEMENT");
            System.out.println(LINE);
            System.out.println("[1] Add New Customer");
            System.out.println("[2] View All Customers");
            System.out.println("[3] View Customer Details & Order History");
            System.out.println("[4] Update Customer Information");
            if (isAdmin) {
                System.out.println("[5] Deactivate Customer");
            }
            System.out.println("[0] Back");
            System.out.println(LINE);
            String choice = InputHelper.readLine(ctx.scanner(), "Enter choice: ");

            switch (choice) {
                case "1" -> addCustomer();
                case "2" -> viewAllCustomers();
                case "3" -> viewCustomerHistory();
                case "4" -> updateCustomer();
                case "5" -> {
                    if (isAdmin) {
                        deactivateCustomer();
                    } else {
                        System.out.println("\nINVALID CHOICE: Please select a number from the menu.");
                    }
                }
                case "0" -> running = false;
                default -> System.out.println("\nINVALID CHOICE: Please select a number from the menu.");
            }
        }
    }

    // ------------------------------------------------------------------ CREATE

    private void addCustomer() {
        System.out.println("\n--- ADD NEW CUSTOMER ---");
        String name = InputHelper.readRequired(ctx.scanner(), "Enter Customer Name: ");
        String phone = readPhone("Enter Phone Number: ");
        String address = InputHelper.readRequired(ctx.scanner(), "Enter Address: ");

        if (ctx.customerDao().addCustomer(name, phone, address)) {
            System.out.println("\nSUCCESS: Customer record added!");
            ctx.logger().log(currentUser, "CUSTOMER_ADDED", "Added customer '" + name + "'");
        } else {
            System.out.println("\nFAILED: Customer was not saved.");
        }
    }

    // -------------------------------------------------------------------- READ

    private void viewAllCustomers() {
        List<Customer> customers = ctx.customerDao().getAllCustomers();
        System.out.println("\n--- ALL CUSTOMERS ---");
        printCustomerTable(customers);
        System.out.println("Total Records Displayed: " + customers.size());
    }

    private void viewCustomerHistory() {
        System.out.println("\n--- CUSTOMER DETAILS & ORDER HISTORY ---");
        Customer customer = pickCustomer(ctx.customerDao().getAllCustomers(), "view");
        if (customer == null) {
            return;
        }
        System.out.println("\nName    : " + customer.name());
        System.out.println("Phone   : " + customer.phone());
        System.out.println("Address : " + customer.address());
        System.out.println("Status  : " + customer.status());

        List<WaterOrder> orders = ctx.orderDao().getOrdersByCustomer(customer.id());
        System.out.println("\nOrder History:");
        OrderMenu.printOrderTable(orders);
        double spent = 0;
        for (WaterOrder o : orders) {
            if (o.paymentStatus().equals("PAID")) {
                spent += o.total();
            }
        }
        System.out.println("Total Orders: " + orders.size() + " | Total Paid: " + String.format("%,.2f", spent));
    }

    // ------------------------------------------------------------------ UPDATE

    private void updateCustomer() {
        System.out.println("\n--- UPDATE CUSTOMER INFORMATION ---");
        Customer customer = pickCustomer(ctx.customerDao().getAllCustomers(), "update");
        if (customer == null) {
            return;
        }
        System.out.println("Press Enter to keep the current value.");

        String name = InputHelper.readLine(ctx.scanner(), "New Name [" + customer.name() + "]: ");
        if (name.isEmpty()) {
            name = customer.name();
        }

        String phone;
        while (true) {
            phone = InputHelper.readLine(ctx.scanner(), "New Phone [" + customer.phone() + "]: ");
            if (phone.isEmpty()) {
                phone = customer.phone();
                break;
            }
            if (isValidPhone(phone)) {
                break;
            }
            System.out.println("Invalid phone number. Use 7-15 digits (you may include + or -).");
        }

        String address = InputHelper.readLine(ctx.scanner(), "New Address [" + customer.address() + "]: ");
        if (address.isEmpty()) {
            address = customer.address();
        }

        if (ctx.customerDao().updateCustomer(customer.id(), name, phone, address)) {
            System.out.println("\nSUCCESS: Customer information updated!");
            ctx.logger().log(currentUser, "CUSTOMER_UPDATED", "Updated customer #" + customer.id() + " '" + name + "'");
        } else {
            System.out.println("\nFAILED: Customer was not updated.");
        }
    }

    // ------------------------------------------------------------------ DELETE

    private void deactivateCustomer() {
        System.out.println("\n--- DEACTIVATE CUSTOMER ---");
        List<Customer> active = ctx.customerDao().getActiveCustomers();
        Customer customer = pickCustomer(active, "deactivate");
        if (customer == null) {
            return;
        }
        if (!InputHelper.confirm(ctx.scanner(), "Deactivate customer '" + customer.name() + "'?")) {
            System.out.println("Deactivation cancelled.");
            return;
        }
        if (ctx.customerDao().deactivateCustomer(customer.id())) {
            System.out.println("\nSUCCESS: Customer has been deactivated. Order history is kept.");
            ctx.logger().log(currentUser, "CUSTOMER_DEACTIVATED",
                    "Deactivated customer #" + customer.id() + " '" + customer.name() + "'");
        } else {
            System.out.println("\nFAILED: Customer was not deactivated.");
        }
    }

    // ----------------------------------------------------------------- HELPERS

    /** Shows the list, asks for an ID (0 cancels) and returns the chosen customer, or null. */
    private Customer pickCustomer(List<Customer> choices, String action) {
        if (choices.isEmpty()) {
            System.out.println("No customer records available.");
            return null;
        }
        printCustomerTable(choices);
        while (true) {
            int id = InputHelper.readInt(ctx.scanner(), "Enter Customer ID to " + action + " (0 to cancel): ");
            if (id == 0) {
                System.out.println("Cancelled.");
                return null;
            }
            for (Customer c : choices) {
                if (c.id() == id) {
                    return c;
                }
            }
            System.out.println("ERROR: Customer ID " + id + " was not found in the list. Try again.");
        }
    }

    private String readPhone(String prompt) {
        while (true) {
            String phone = InputHelper.readLine(ctx.scanner(), prompt);
            if (isValidPhone(phone)) {
                return phone;
            }
            System.out.println("Invalid phone number. Use 7-15 digits (you may include + or -).");
        }
    }

    private boolean isValidPhone(String phone) {
        return phone.matches("[0-9+\\- ]{7,15}");
    }

    /** Prints customers as an aligned table. */
    public static void printCustomerTable(List<Customer> customers) {
        List<String[]> rows = new ArrayList<>();
        for (Customer c : customers) {
            rows.add(new String[]{String.valueOf(c.id()), c.name(), c.phone(), c.address(), c.status()});
        }
        ConsoleTable.print(
                new String[]{"ID", "CUSTOMER NAME", "PHONE", "ADDRESS", "STATUS"},
                new int[]{5, 20, 15, 30, 8},
                rows);
    }
}
