package com.yourdomain.model;

/** One row of the activity (audit) log. */
public record LogEntry(int id, String username, String action, String details, String loggedAt) {
}
