package ui;

import dao.UserDao;

import java.util.Scanner;

/**
 * Main menu: Register, Login, Exit.
 */
public class MainMenu {

    private final Scanner scanner = new Scanner(System.in);
    private final UserDao userDao = new UserDao();

    public void show() {
        boolean running = true;

        while (running) {
            System.out.println("\n==============================");
            System.out.println("        MAIN MENU");
            System.out.println("==============================");
            System.out.println("1. Register Account");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> new RegisterScreen(scanner, userDao).show();
                case "2" -> new LoginScreen(scanner, userDao).show();
                case "3" -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Enter 1, 2 or 3.");
            }
        }
    }
}
