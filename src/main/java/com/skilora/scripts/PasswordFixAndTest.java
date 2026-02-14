package com.skilora.scripts;

import com.skilora.model.entity.User;
import com.skilora.model.enums.Role;
import com.skilora.model.service.AuthService;
import com.skilora.model.service.UserService;

import java.util.Optional;

/**
 * Script to fix passwords (hash with BCrypt) and test login functionality.
 * Usage: Run this class specifically from your IDE.
 */
public class PasswordFixAndTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   Starting Password Fix and Login Test Script    ");
        System.out.println("==================================================");

        UserService userService = UserService.getInstance();
        AuthService authService = AuthService.getInstance();

        // 1. Admin
        fixAndTest(userService, authService, "admin", "admin123", Role.ADMIN);

        // 2. User
        fixAndTest(userService, authService, "user", "user123", Role.USER);

        // 3. Employer
        fixAndTest(userService, authService, "employer", "emp123", Role.EMPLOYER);

        System.out.println("\n==================================================");
        System.out.println("                Script completed.                 ");
        System.out.println("==================================================");
        System.exit(0);
    }

    private static void fixAndTest(UserService userService, AuthService authService, String username, String password,
            Role role) {
        System.out.println("\n--------------------------------------------------");
        System.out.println("Processing user: " + username);

        try {
            Optional<User> userOpt = userService.findByUsername(username);

            if (userOpt.isEmpty()) {
                System.out.println("User '" + username + "' not found. Creating new user...");
                User newUser = new User();
                newUser.setUsername(username);
                newUser.setPassword(password); // Will be hashed by create
                newUser.setRole(role);
                newUser.setFullName(username.substring(0, 1).toUpperCase() + username.substring(1));
                newUser.setEmail(username + "@example.com");
                newUser.setActive(true);
                newUser.setVerified(true);
                newUser.setPhotoUrl(""); // Optional

                userService.create(newUser);
                System.out.println("Created new user: " + username + " with role " + role);
            } else {
                User existingUser = userOpt.get();
                System.out.println("User found (ID: " + existingUser.getId() + "). Updating password...");

                // Update password (hashes it automatically via UserService)
                boolean updated = userService.updatePassword(existingUser.getId(), password);

                if (updated) {
                    System.out.println("Password updated successfully (BCrypt hash generated).");
                } else {
                    System.out.println("ERROR: Failed to update password.");
                    // Continue to try login anyway, just in case
                }
            }

            // Check if locked out
            if (authService.isLockedOut(username)) {
                System.out.println(
                        "WARNING: User '" + username + "' is currently locked out due to previous failed attempts.");
                System.out.println("Login test may fail. Please wait 15 minutes or clear the 'login_attempts' table.");
            }

            // Test Login
            System.out.println("Testing login with new password: " + password);
            Optional<User> loggedInUser = authService.login(username, password);

            if (loggedInUser.isPresent()) {
                System.out.println("SUCCESS: Login verified! User " + loggedInUser.get().getUsername()
                        + " authenticated successfully.");
            } else {
                System.out.println("FAILURE: Login failed for " + username + ". Verify password or lockout status.");
            }

        } catch (Exception e) {
            System.out.println("EXCEPTION: An error occurred processing " + username);
            e.printStackTrace();
        }
    }
}
