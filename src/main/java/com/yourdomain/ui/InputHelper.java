package com.yourdomain.ui;

import java.util.Scanner;

/**
 * Console input helpers. Every method reads a whole line with nextLine(), so no
 * trailing newline token is ever left behind between numeric and text prompts.
 */
public final class InputHelper {

    private InputHelper() {
    }

    /** Reads one trimmed line (may be empty). */
    public static String readLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /** Keeps asking until the person types something. */
    public static String readRequired(Scanner scanner, String prompt) {
        while (true) {
            String value = readLine(scanner, prompt);
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("Input cannot be empty.");
        }
    }

    /** Keeps asking until a whole number >= 0 is typed. */
    public static int readInt(Scanner scanner, String prompt) {
        while (true) {
            String value = readLine(scanner, prompt);
            try {
                int number = Integer.parseInt(value);
                if (number >= 0) {
                    return number;
                }
                System.out.println("Please enter a number that is 0 or higher.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number.");
            }
        }
    }

    /** Keeps asking until a number greater than 0 is typed. */
    public static double readPositiveDouble(Scanner scanner, String prompt) {
        while (true) {
            String value = readLine(scanner, prompt);
            try {
                double number = Double.parseDouble(value);
                if (number > 0) {
                    return number;
                }
                System.out.println("Please enter an amount greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number (e.g., 25.00).");
            }
        }
    }

    /** Asks a yes/no question. Anything other than Y/YES counts as no. */
    public static boolean confirm(Scanner scanner, String prompt) {
        String value = readLine(scanner, prompt + " (Y/N): ").toUpperCase();
        return value.equals("Y") || value.equals("YES");
    }
}
