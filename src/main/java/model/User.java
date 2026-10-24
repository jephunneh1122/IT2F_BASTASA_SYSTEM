package model;

/**
 * Java Record that represents one row of the users table.
 */
public record User(int id, String username, String password, String roles, String status) {
}
