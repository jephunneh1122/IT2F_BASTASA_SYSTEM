package com.yourdomain.model;

import java.util.List;

/** One titled block of a report: a heading, column headers and data rows. */
public record ReportSection(String title, String[] headers, List<String[]> rows) {
}
