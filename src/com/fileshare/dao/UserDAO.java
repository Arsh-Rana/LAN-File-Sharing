package com.fileshare.dao;

import com.fileshare.beans.UserBean;
import com.fileshare.database.DatabaseConnection;
import com.fileshare.utils.Constants;
import com.fileshare.utils.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Data Access Object (DAO) for User database operations.
 * Implements CRUD and authentication using PreparedStatements and try-with-resources.
 */
public class UserDAO {

    /**
     * Checks if a username is already registered in the system.
     *
     * @param username username to inspect
     * @return true if taken, false otherwise
     */
    public boolean isUsernameTaken(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error checking username existence: " + e.getMessage());
        }
        return false;
    }

    /**
     * Registers a new user with hashed password storage.
     *
     * @param user UserBean containing registration details
     * @return true if registration succeeded, false otherwise
     */
    public boolean registerUser(UserBean user) {
        if (user == null || user.getUsername() == null || user.getPassword() == null) {
            return false;
        }

        String sql = "INSERT INTO users (username, password_hash, full_name, email, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String hashedPassword = PasswordHasher.hash(user.getPassword());

            ps.setString(1, user.getUsername().trim());
            ps.setString(2, hashedPassword);
            ps.setString(3, user.getFullName() != null ? user.getFullName().trim() : "");
            ps.setString(4, user.getEmail() != null ? user.getEmail().trim() : "");
            ps.setString(5, Constants.STATUS_OFFLINE);

            int affected = ps.executeUpdate();
            return affected > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] Registration failed for " + user.getUsername() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Authenticates a user with username and password.
     * Updates status to ONLINE upon successful verification.
     *
     * @param username user identifier
     * @param plainPassword plain password input
     * @return populated UserBean if valid, null if credentials fail
     */
    public UserBean authenticateUser(String username, String plainPassword) {
        if (username == null || plainPassword == null) {
            return null;
        }

        String sql = "SELECT id, username, password_hash, full_name, email, status, created_at FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (PasswordHasher.verify(plainPassword, storedHash)) {
                        int id = rs.getInt("id");
                        String uName = rs.getString("username");
                        String fName = rs.getString("full_name");
                        String email = rs.getString("email");
                        Timestamp createdAt = rs.getTimestamp("created_at");

                        // Update status to ONLINE
                        updateUserStatus(uName, Constants.STATUS_ONLINE);

                        UserBean user = new UserBean(id, uName, fName, email, Constants.STATUS_ONLINE, createdAt);
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Authentication error for " + username + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Updates the online/offline status of a user.
     *
     * @param username username to update
     * @param status status string ("ONLINE" or "OFFLINE")
     * @return true if updated, false otherwise
     */
    public boolean updateUserStatus(String username, String status) {
        if (username == null) return false;

        String sql = "UPDATE users SET status = ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] Failed to update status for " + username + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves user details by username.
     */
    public UserBean getUserByUsername(String username) {
        if (username == null) return null;

        String sql = "SELECT id, username, full_name, email, status, created_at FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserBean(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("status"),
                            rs.getTimestamp("created_at")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error querying user " + username + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Resets all users to OFFLINE on server startup.
     */
    public void resetAllUserStatuses() {
        String sql = "UPDATE users SET status = 'OFFLINE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[UserDAO] Note: Could not reset user statuses: " + e.getMessage());
        }
    }
}
