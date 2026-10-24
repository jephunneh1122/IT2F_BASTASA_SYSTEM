package ui;

import dao.UserDao;

import java.util.Scanner;

public class RegisterScreen {

    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_STUDENT = "STUDENT";

    private final Scanner scanner;
    private final UserDao userDao;

    public RegisterScreen(Scanner scanner, UserDao userDao) {
        this.scanner = scanner;
        this.userDao = userDao;
    }

    public void show() {
        System.out.println("\n--- REGISTER ACCOUNT ---");

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }
        if (userDao.usernameExists(username)) {
            System.out.println("Username already taken.");
            return;
        }

        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        if (password.isEmpty()) {
            System.out.println("Password cannot be empty.");
            return;
        }

        System.out.println("Select role:");
        System.out.println("1. " + ROLE_TEACHER);
        System.out.println("2. " + ROLE_STUDENT);
        System.out.print("Choose: ");
        String roleChoice = scanner.nextLine().trim();

        String role;
        if (roleChoice.equals("1")) {
            role = ROLE_TEACHER;
        } else if (roleChoice.equals("2")) {
            role = ROLE_STUDENT;
        } else {
            System.out.println("Invalid role. Registration cancelled.");
            return;
        }

        if (userDao.register(username, password, role)) {
            System.out.println("Account created! Status: PENDING (waiting for approval).");
        } else {
            System.out.println("Registration failed.");
        }
    }
}
