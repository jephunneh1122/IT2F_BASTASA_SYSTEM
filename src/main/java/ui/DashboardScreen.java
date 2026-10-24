package ui;

import dao.UserDao;
import model.User;

import java.util.List;
import java.util.Scanner;

/**
 * Shown after login. Has the data table and Logout.
 */
public class DashboardScreen {

    private final Scanner scanner;
    private final UserDao userDao;
    private final User currentUser;

    public DashboardScreen(Scanner scanner, UserDao userDao, User currentUser) {
        this.scanner = scanner;
        this.userDao = userDao;
        this.currentUser = currentUser;
    }

    public void show() {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==============================");
            System.out.println("DASHBOARD");
            System.out.println("User: " + currentUser.username()
                    + " | Role: " + currentUser.roles()
                    + " | Status: " + currentUser.status());
            System.out.println("==============================");
            System.out.println("1. View All Users (Data Table)");
            System.out.println("2. Logout");
            System.out.print("Choose: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> printUserTable(userDao.getAllUsers());
                case "2" -> {
                    System.out.println("Logged out successfully.");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    /** Prints users as a formatted console table (no extra library needed). */
    private void printUserTable(List<User> users) {
        String line = "+-----+----------------------+------------+------------+";
        System.out.println(line);
        System.out.printf("| %-3s | %-20s | %-10s | %-10s |%n", "ID", "USERNAME", "ROLE", "STATUS");
        System.out.println(line);
        for (User u : users) {
            System.out.printf("| %-3d | %-20s | %-10s | %-10s |%n",
                    u.id(), u.username(), u.roles(), u.status());
        }
        System.out.println(line);
    }
}
