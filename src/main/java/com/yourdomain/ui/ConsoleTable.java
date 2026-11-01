package com.yourdomain.ui;

import java.util.List;

/** Prints aligned console tables using String.format widths. */
public final class ConsoleTable {

    private ConsoleTable() {
    }

    /** Builds a border line such as +-----+-------+ for the given column widths. */
    public static String border(int... widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w + 2)).append("+");
        }
        return sb.toString();
    }

    /** Cuts text that is longer than the column so the table stays aligned. */
    public static String fit(String text, int width) {
        if (text == null) {
            return "";
        }
        return text.length() <= width ? text : text.substring(0, width - 2) + "..";
    }

    public static String row(int[] widths, String... cells) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            String cell = i < cells.length ? fit(cells[i], widths[i]) : "";
            sb.append(" ").append(String.format("%-" + widths[i] + "s", cell)).append(" |");
        }
        return sb.toString();
    }

    public static void print(String[] headers, int[] widths, List<String[]> rows) {
        String border = border(widths);
        System.out.println(border);
        System.out.println(row(widths, headers));
        System.out.println(border);
        if (rows.isEmpty()) {
            System.out.println("| " + String.format("%-" + (border.length() - 4) + "s", "No records found.") + " |");
        } else {
            for (String[] r : rows) {
                System.out.println(row(widths, r));
            }
        }
        System.out.println(border);
    }
}
