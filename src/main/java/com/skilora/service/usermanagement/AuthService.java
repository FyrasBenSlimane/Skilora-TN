package com.skilora.service.usermanagement;

import com.skilora.model.entity.usermanagement.User;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private static volatile AuthService instance;

    /** In-memory lockout: username -> lockout end time (millis). Can be replaced by DB later. */
    private final ConcurrentHashMap<String, Long> lockoutUntil = new ConcurrentHashMap<>();
    private static final int LOCKOUT_MINUTES = 15;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private final ConcurrentHashMap<String, Integer> failedAttempts = new ConcurrentHashMap<>();

    public static synchronized AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    /** Returns true if the account is currently locked due to failed attempts. */
    public boolean isLockedOut(String username) {
        if (username == null) return false;
        Long until = lockoutUntil.get(username);
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            lockoutUntil.remove(username);
            failedAttempts.remove(username);
            return false;
        }
        return true;
    }

    /** Minutes remaining until lockout ends; 0 if not locked. */
    public int getRemainingLockoutMinutes(String username) {
        if (username == null) return 0;
        Long until = lockoutUntil.get(username);
        if (until == null) return 0;
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0) return 0;
        return (int) Math.ceil(remaining / 60_000.0);
    }

    public Optional<User> login(String usernameOrEmail, String password) {
        try {
            UserService us = UserService.getInstance();
            Optional<User> user = us.findByUsername(usernameOrEmail);
            if (!user.isPresent()) user = us.findByEmail(usernameOrEmail);
            if (!user.isPresent()) {
                recordFailedAttempt(usernameOrEmail);
                return Optional.empty();
            }
            if (!UserService.verifyPassword(password, user.get().getPassword())) {
                recordFailedAttempt(usernameOrEmail);
                return Optional.empty();
            }
            clearFailedAttempts(usernameOrEmail);
            return user;
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void recordFailedAttempt(String username) {
        int attempts = failedAttempts.merge(username, 1, Integer::sum);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            lockoutUntil.put(username, System.currentTimeMillis() + LOCKOUT_MINUTES * 60_000L);
        }
    }

    private void clearFailedAttempts(String username) {
        failedAttempts.remove(username);
        lockoutUntil.remove(username);
    }

    /** Get user by username (e.g. for biometric login). */
    public Optional<User> getUser(String username) {
        try {
            UserService us = UserService.getInstance();
            Optional<User> u = us.findByUsername(username);
            if (u.isPresent()) return u;
            return us.findByEmail(username);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
