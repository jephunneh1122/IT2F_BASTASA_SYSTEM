package com.yourdomain.ui;

import com.yourdomain.model.Customer;
import com.yourdomain.model.Product;
import com.yourdomain.model.SaleRecord;
import com.yourdomain.model.User;
import com.yourdomain.model.WaterOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orders, payments, sales and delivery records.
 * Admin and Staff can create and update orders and process payments;
 * only the Administrator can delete orders.
 */
public class OrderMenu {

    private static final String LINE = "==================================================";

    private final AppContext ctx;
    private final User currentUser;
    private final boolean isAdmin;

    public OrderMenu(AppContext ctx, User currentUser) {
        this.ctx = ctx;
        this.currentUser = currentUser;
        this.isAdmin = currentUser.role().equals("ADMIN");
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println(LINE);
            System.out.println("          ORDERS & PAYMENTS");
            System.out.println(LINE);
            System.out.println("[1] Create New Order");
            System.out.println("[2] View All Orders");
            System.out.println("[3] Update Order Status");
            System.out.println("[4] Assign Delivery Staff");
            System.out.println("[5] Process Payment");
            System.out.println("[6] View Sales Records");
            System.out.println("[7] View Delivery Records");
            if (isAdmin) {
                System.out.println("[8] Delete Canceled / Incorrect Order");
            }
            System.out.println("[0] Back");
            System.out.println(LINE);
            String choice = InputHelper.readLine(ctx.scanner(), "Enter choice: ");

            switch (choice) {
                case "1" -> createOrder();
                case "2" -> viewAllOrders();
                case "3" -> updateOrderStatus();
                case "4" -> assignDeliveryStaff();
                case "5" -> processPayment();
                case "6" -> viewSalesRecords();
                case "7" -> showDeliveryRecords(ctx);
                case "8" -> {
                    if (isAdmin) {
                        deleteOrder();
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

    private void createOrder() {
        System.out.println("\n--- CREATE NEW ORDER ---");

        List<Customer> customers = ctx.customerDao().getActiveCustomers();
        if (customers.isEmpty()) {
            System.out.println("No active customers found. Add a customer first.");
            return;
        }
        CustomerMenu.printCustomerTable(customers);
        Customer customer = null;
        while (customer == null) {
            int id = InputHelper.readInt(ctx.scanner(), "Enter Customer ID (0 to cancel): ");
            if (id == 0) {
                System.out.println("Order cancelled.");
                return;
            }
            for (Customer c : customers) {
                if (c.id() == id) {
                    customer = c;
                }
            }
            if (customer == null) {
                System.out.println("ERROR: Customer ID " + id + " is not an active customer. Try again.");
            }
        }

        List<Product> products = ctx.productDao().getAllProducts();
        if (products.isEmpty()) {
            System.out.println("No products in inventory.");
            return;
        }
        System.out.println();
        InventoryMenu.printProductTable(products);
        Product product = null;
        while (product == null) {
            int id = InputHelper.readInt(ctx.scanner(), "Enter Product ID (0 to cancel): ");
            if (id == 0) {
                System.out.println("Order cancelled.");
                return;
            }
            for (Product p : products) {
                if (p.id() == id) {
                    product = p;
                }
            }
            if (product == null) {
                System.out.println("ERROR: Product ID " + id + " was not found. Try again.");
            } else if (product.stock() == 0) {
                System.out.println("ERROR: '" + product.name() + "' is out of stock. Choose another product.");
                product = null;
            }
        }

        int quantity;
        while (true) {
            quantity = InputHelper.readInt(ctx.scanner(), "Enter Quantity (0 to cancel): ");
            if (quantity == 0) {
                System.out.println("Order cancelled.");
                return;
            }
            if (quantity > product.stock()) {
                System.out.println("ERROR: Only " + product.stock() + " in stock. Enter a smaller quantity.");
            } else {
                break;
            }
        }

        String orderType;
        while (true) {
            System.out.println("Order Type:  1. Pickup   2. Delivery");
            int type = InputHelper.readInt(ctx.scanner(), "Choose (0 to cancel): ");
            if (type == 0) {
                System.out.println("Order cancelled.");
                return;
            }
            if (type == 1) {
                orderType = "PICKUP";
                break;
            }
            if (type == 2) {
                orderType = "DELIVERY";
                break;
            }
            System.out.println("Invalid choice.");
        }

        Integer deliveryUserId = null;
        if (orderType.equals("DELIVERY")) {
            deliveryUserId = askDeliveryStaff();
        }

        double total = Math.round(product.price() * quantity * 100.0) / 100.0;
        System.out.println("\nOrder summary: " + quantity + " x " + product.name()
                + " for " + customer.name() + " (" + orderType + ") = " + String.format("%,.2f", total));
        if (!InputHelper.confirm(ctx.scanner(), "Save this order?")) {
            System.out.println("Order cancelled.");
            return;
        }

        int newId = ctx.orderDao().createOrder(customer.id(), product.id(), quantity, orderType, deliveryUserId);
        if (newId > 0) {
            System.out.println("\nSUCCESS: Order #" + newId + " recorded! Total: " + String.format("%,.2f", total));
            ctx.logger().log(currentUser, "ORDER_CREATED", "Order #" + newId + ": " + quantity + " x "
                    + product.name() + " for '" + customer.name() + "' (" + orderType + ")");
        } else {
            System.out.println("\nFAILED: Order was not saved.");
        }
    }

    /** Lets the user choose an approved delivery staff, or leave the order unassigned (returns null). */
    private Integer askDeliveryStaff() {
        List<User> staff = ctx.userDao().getApprovedUsersByRole("DELIVERY");
        if (staff.isEmpty()) {
            System.out.println("No approved delivery staff yet. The order will be left unassigned.");
            return null;
        }
        System.out.println();
        AdminMenu.printUserTable(staff);
        while (true) {
            int id = InputHelper.readInt(ctx.scanner(), "Assign Delivery Staff ID (0 = assign later): ");
            if (id == 0) {
                return null;
            }
            for (User u : staff) {
                if (u.id() == id) {
                    return id;
                }
            }
            System.out.println("ERROR: ID " + id + " is not an approved delivery staff. Try again.");
        }
    }

    // -------------------------------------------------------------------- READ

    private void viewAllOrders() {
        List<WaterOrder> orders = ctx.orderDao().getAllOrders();
        System.out.println("\n--- ALL ORDERS ---");
        printOrderTable(orders);
        System.out.println("Total Records Displayed: " + orders.size());
    }

    private void viewSalesRecords() {
        List<SaleRecord> sales = ctx.orderDao().getAllSales();
        System.out.println("\n--- SALES RECORDS ---");
        List<String[]> rows = new ArrayList<>();
        double total = 0;
        for (SaleRecord s : sales) {
            rows.add(new String[]{String.valueOf(s.id()), String.valueOf(s.orderId()), s.customerName(),
                    s.productName(), String.valueOf(s.quantity()), String.format("%.2f", s.amount()), s.paidAt()});
            total += s.amount();
        }
        ConsoleTable.print(
                new String[]{"SALE", "ORDER", "CUSTOMER", "PRODUCT", "QTY", "AMOUNT", "PAID AT"},
                new int[]{5, 5, 18, 22, 4, 10, 19},
                rows);
        System.out.println("Total Sales Records: " + sales.size() + " | Total Amount: " + String.format("%,.2f", total));
    }

    /** Shared by the Admin, Staff and Order menus. */
    public static void showDeliveryRecords(AppContext ctx) {
        List<WaterOrder> deliveries = ctx.orderDao().getDeliveryOrders();
        System.out.println("\n--- DELIVERY RECORDS ---");
        printDeliveryTable(deliveries);
        System.out.println("Total Delivery Orders: " + deliveries.size());
    }

    // ------------------------------------------------------------------ UPDATE

    private void updateOrderStatus() {
        System.out.println("\n--- UPDATE ORDER STATUS ---");
        WaterOrder order = pickOrder(ctx.orderDao().getAllOrders(), "update");
        if (order == null) {
            return;
        }

        String[] options = order.orderType().equals("DELIVERY")
                ? new String[]{"PENDING", "OUT_FOR_DELIVERY", "DELIVERED"}
                : new String[]{"PENDING", "COMPLETED"};
        System.out.println("Order #" + order.id() + " (" + order.orderType() + ") is currently " + order.status());
        System.out.println("Select new status:");
        for (int i = 0; i < options.length; i++) {
            System.out.println("  " + (i + 1) + ". " + options[i]);
        }
        int pick = InputHelper.readInt(ctx.scanner(), "Choose (0 to cancel): ");
        if (pick == 0) {
            System.out.println("Update cancelled.");
            return;
        }
        if (pick < 1 || pick > options.length) {
            System.out.println("Invalid choice. Update cancelled.");
            return;
        }

        String newStatus = options[pick - 1];
        if (ctx.orderDao().updateStatus(order.id(), newStatus)) {
            System.out.println("\nSUCCESS: Order #" + order.id() + " is now " + newStatus + ".");
            ctx.logger().log(currentUser, "ORDER_STATUS_UPDATED",
                    "Order #" + order.id() + ": " + order.status() + " -> " + newStatus);
        } else {
            System.out.println("\nFAILED: Status was not updated.");
        }
    }

    private void assignDeliveryStaff() {
        System.out.println("\n--- ASSIGN DELIVERY STAFF ---");
        List<WaterOrder> deliveries = ctx.orderDao().getDeliveryOrders();
        WaterOrder order = pickOrder(deliveries, "assign");
        if (order == null) {
            return;
        }
        Integer staffId = askDeliveryStaff();
        if (staffId == null) {
            System.out.println("No delivery staff assigned.");
            return;
        }
        if (ctx.orderDao().assignDeliveryStaff(order.id(), staffId)) {
            System.out.println("\nSUCCESS: Delivery staff assigned to order #" + order.id() + ".");
            ctx.logger().log(currentUser, "DELIVERY_ASSIGNED",
                    "Order #" + order.id() + " assigned to user ID " + staffId);
        } else {
            System.out.println("\nFAILED: Delivery staff was not assigned.");
        }
    }

    private void processPayment() {
        System.out.println("\n--- PROCESS PAYMENT ---");
        List<WaterOrder> unpaid = new ArrayList<>();
        for (WaterOrder o : ctx.orderDao().getAllOrders()) {
            if (o.paymentStatus().equals("UNPAID")) {
                unpaid.add(o);
            }
        }
        WaterOrder order = pickOrder(unpaid, "pay");
        if (order == null) {
            return;
        }

        System.out.println("Amount due: " + String.format("%,.2f", order.total()));
        double cash;
        while (true) {
            cash = InputHelper.readPositiveDouble(ctx.scanner(), "Enter Cash Received: ");
            if (cash >= order.total()) {
                break;
            }
            System.out.println("ERROR: Cash received is less than the amount due.");
        }

        if (ctx.orderDao().processPayment(order.id(), currentUser.username())) {
            System.out.println("\nSUCCESS: Payment recorded for order #" + order.id() + ".");
            System.out.println("Amount Paid: " + String.format("%,.2f", order.total())
                    + " | Change: " + String.format("%,.2f", cash - order.total()));
            ctx.logger().log(currentUser, "PAYMENT_PROCESSED",
                    "Order #" + order.id() + " paid " + String.format("%.2f", order.total()));
        } else {
            System.out.println("\nFAILED: Payment was not recorded.");
        }
    }

    // ------------------------------------------------------------------ DELETE

    private void deleteOrder() {
        System.out.println("\n--- DELETE CANCELED / INCORRECT ORDER ---");
        WaterOrder order = pickOrder(ctx.orderDao().getAllOrders(), "delete");
        if (order == null) {
            return;
        }
        if (!InputHelper.confirm(ctx.scanner(), "Permanently delete order #" + order.id() + "?")) {
            System.out.println("Deletion cancelled.");
            return;
        }
        if (ctx.orderDao().deleteOrder(order.id())) {
            System.out.println("\nSUCCESS: Order #" + order.id() + " deleted.");
            ctx.logger().log(currentUser, "ORDER_DELETED", "Deleted order #" + order.id()
                    + " (" + order.quantity() + " x " + order.productName() + " for '" + order.customerName() + "')");
        } else {
            System.out.println("\nFAILED: Order was not deleted.");
        }
    }

    // ----------------------------------------------------------------- HELPERS

    /** Shows the orders, asks for an ID (0 cancels) and returns the chosen order, or null. */
    private WaterOrder pickOrder(List<WaterOrder> choices, String action) {
        if (choices.isEmpty()) {
            System.out.println("No matching orders found.");
            return null;
        }
        if (action.equals("assign")) {
            printDeliveryTable(choices);
        } else {
            printOrderTable(choices);
        }
        while (true) {
            int id = InputHelper.readInt(ctx.scanner(), "Enter Order ID to " + action + " (0 to cancel): ");
            if (id == 0) {
                System.out.println("Cancelled.");
                return null;
            }
            Optional<WaterOrder> match = choices.stream().filter(o -> o.id() == id).findFirst();
            if (match.isPresent()) {
                return match.get();
            }
            System.out.println("ERROR: Order ID " + id + " was not found in the list. Try again.");
        }
    }

    /** Prints orders as an aligned table. */
    public static void printOrderTable(List<WaterOrder> orders) {
        List<String[]> rows = new ArrayList<>();
        for (WaterOrder o : orders) {
            rows.add(new String[]{String.valueOf(o.id()), o.customerName(), o.productName(),
                    String.valueOf(o.quantity()), String.format("%.2f", o.total()),
                    o.orderType(), o.status(), o.paymentStatus()});
        }
        ConsoleTable.print(
                new String[]{"ID", "CUSTOMER", "PRODUCT", "QTY", "TOTAL", "TYPE", "STATUS", "PAYMENT"},
                new int[]{5, 16, 20, 4, 10, 8, 16, 7},
                rows);
    }

    /** Prints delivery orders with the customer delivery information. */
    public static void printDeliveryTable(List<WaterOrder> orders) {
        List<String[]> rows = new ArrayList<>();
        for (WaterOrder o : orders) {
            rows.add(new String[]{String.valueOf(o.id()), o.customerName(), o.customerPhone(),
                    o.customerAddress(), o.productName(), String.valueOf(o.quantity()), o.status(),
                    o.deliveryUserName() == null ? "UNASSIGNED" : o.deliveryUserName()});
        }
        ConsoleTable.print(
                new String[]{"ORDER", "CUSTOMER", "PHONE", "ADDRESS", "PRODUCT", "QTY", "STATUS", "DELIVERY STAFF"},
                new int[]{5, 16, 13, 24, 18, 4, 16, 14},
                rows);
    }
}
