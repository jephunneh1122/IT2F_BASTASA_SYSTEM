package com.yourdomain.ui;

import com.yourdomain.model.DailyReport;
import com.yourdomain.model.Product;
import com.yourdomain.model.SaleRecord;
import com.yourdomain.model.User;
import com.yourdomain.model.ReportSection;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Daily sales and inventory summary report (Administrator only).
 * The report can be viewed on screen or exported as PDF, CSV and Excel (XLSX).
 */
public class ReportMenu {

    private static final String LINE = "==================================================";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AppContext ctx;
    private final User currentUser;

    public ReportMenu(AppContext ctx, User currentUser) {
        this.ctx = ctx;
        this.currentUser = currentUser;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println(LINE);
            System.out.println("          SALES & INVENTORY REPORTS");
            System.out.println(LINE);
            System.out.println("[1] View Daily Summary Report");
            System.out.println("[2] Export Daily Report (PDF / CSV / XLSX)");
            System.out.println("[0] Back");
            System.out.println(LINE);
            String choice = InputHelper.readLine(ctx.scanner(), "Enter choice: ");

            switch (choice) {
                case "1" -> viewReport();
                case "2" -> exportReport();
                case "0" -> running = false;
                default -> System.out.println("\nINVALID CHOICE: Please enter 1, 2, or 0.");
            }
        }
    }

    private void viewReport() {
        String date = askDate();
        if (date == null) {
            return;
        }
        DailyReport r = ctx.orderDao().getDailyReport(date);

        System.out.println();
        System.out.println(LINE);
        System.out.println("          DAILY SUMMARY REPORT - " + r.date());
        System.out.println(LINE);
        System.out.printf("%-26s: %s%n", "Total Daily Sales (PHP)", String.format("%,.2f", r.totalSales()));
        System.out.printf("%-26s: %d%n", "Water Containers Sold", r.containersSold());
        System.out.printf("%-26s: %d%n", "Available Water Stock", r.availableStock());
        System.out.printf("%-26s: %d%n", "Completed Orders", r.completedOrders());
        System.out.printf("%-26s: %d%n", "Delivery Orders", r.deliveryOrders());
        System.out.println(LINE);
    }

    private void exportReport() {
        String date = askDate();
        if (date == null) {
            return;
        }

        System.out.println("Export format:");
        System.out.println("  1. PDF");
        System.out.println("  2. CSV");
        System.out.println("  3. Excel (XLSX)");
        System.out.println("  4. All formats");
        int format = InputHelper.readInt(ctx.scanner(), "Choose (0 to cancel): ");
        if (format == 0) {
            System.out.println("Export cancelled.");
            return;
        }
        if (format > 4) {
            System.out.println("Invalid choice. Export cancelled.");
            return;
        }

        String title = "WATER REFILLING STATION - DAILY SUMMARY REPORT (" + date + ")";
        List<ReportSection> sections = buildSections(date);
        String baseName = "daily_report_" + date;

        try {
            List<Path> created = new ArrayList<>();
            if (format == 1 || format == 4) {
                created.add(ctx.exporter().exportPdf(baseName, title, sections));
            }
            if (format == 2 || format == 4) {
                created.add(ctx.exporter().exportCsv(baseName, title, sections));
            }
            if (format == 3 || format == 4) {
                created.add(ctx.exporter().exportXlsx(baseName, title, sections));
            }
            System.out.println("\nSUCCESS: Report exported:");
            for (Path p : created) {
                System.out.println("  " + p.toAbsolutePath());
            }
            ctx.logger().log(currentUser, "REPORT_EXPORTED", "Exported daily report for " + date
                    + " (" + created.size() + " file" + (created.size() == 1 ? "" : "s") + ")");
        } catch (IOException e) {
            System.out.println("\nFAILED: Could not write the report file - " + e.getMessage());
        }
    }

    private List<ReportSection> buildSections(String date) {
        DailyReport r = ctx.orderDao().getDailyReport(date);

        List<String[]> summary = new ArrayList<>();
        summary.add(new String[]{"Report Date", r.date()});
        summary.add(new String[]{"Generated At", LocalDateTime.now().format(TIMESTAMP)});
        summary.add(new String[]{"Generated By", currentUser.username()});
        summary.add(new String[]{"Total Daily Sales (PHP)", String.format("%.2f", r.totalSales())});
        summary.add(new String[]{"Water Containers Sold", String.valueOf(r.containersSold())});
        summary.add(new String[]{"Available Water Stock", String.valueOf(r.availableStock())});
        summary.add(new String[]{"Completed Orders", String.valueOf(r.completedOrders())});
        summary.add(new String[]{"Delivery Orders", String.valueOf(r.deliveryOrders())});

        List<String[]> stockRows = new ArrayList<>();
        for (Product p : ctx.productDao().getAllProducts()) {
            stockRows.add(new String[]{String.valueOf(p.id()), p.name(),
                    String.format("%.2f", p.price()), String.valueOf(p.stock())});
        }

        List<String[]> saleRows = new ArrayList<>();
        for (SaleRecord s : ctx.orderDao().getSalesByDate(date)) {
            saleRows.add(new String[]{String.valueOf(s.id()), String.valueOf(s.orderId()), s.customerName(),
                    s.productName(), String.valueOf(s.quantity()), String.format("%.2f", s.amount()), s.paidAt()});
        }

        List<ReportSection> sections = new ArrayList<>();
        sections.add(new ReportSection("Daily Summary", new String[]{"Metric", "Value"}, summary));
        sections.add(new ReportSection("Water Stock", new String[]{"ID", "Product", "Price", "Stock"}, stockRows));
        sections.add(new ReportSection("Sales Transactions",
                new String[]{"Sale ID", "Order ID", "Customer", "Product", "Qty", "Amount", "Paid At"}, saleRows));
        return sections;
    }

    /** Asks for a date; Enter means today. Returns null if cancelled. */
    private String askDate() {
        while (true) {
            String input = InputHelper.readLine(ctx.scanner(),
                    "Enter date (yyyy-MM-dd), Enter for today, or 0 to cancel: ");
            if (input.equals("0")) {
                System.out.println("Cancelled.");
                return null;
            }
            if (input.isEmpty()) {
                return LocalDate.now().toString();
            }
            try {
                return LocalDate.parse(input).toString();
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Please use the format yyyy-MM-dd (e.g., 2026-11-01).");
            }
        }
    }
}
