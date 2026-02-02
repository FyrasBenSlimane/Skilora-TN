package com.skilora.user.controller;

import com.skilora.framework.components.*;
import com.skilora.user.entity.User;
import com.skilora.user.enums.Role;
import com.skilora.utils.Validators;

import javafx.fxml.FXML;

public class UserFormController {

    @FXML
    private TLTextField usernameField;
    @FXML
    private TLTextField fullNameField;
    @FXML
    private TLTextField emailField;
    @FXML
    private TLPasswordField passwordField;
    @FXML
    private TLSelect<Role> roleSelect;

    private User existingUser;

    @FXML
    public void initialize() {
        // Populate role dropdown
        roleSelect.getItems().addAll(Role.values());
        roleSelect.setValue(Role.USER);
    }

    public void setUser(User user) {
        this.existingUser = user;
        if (user != null) {
            usernameField.setText(user.getUsername());
            fullNameField.setText(user.getFullName());
            emailField.setText(user.getEmail() != null ? user.getEmail() : "");
            roleSelect.setValue(user.getRole());
        }
    }

    public User getUser() {
        User user = existingUser != null ? existingUser : new User();
        user.setUsername(usernameField.getText());
        user.setFullName(fullNameField.getText());
        String email = emailField.getText();
        user.setEmail(email != null && !email.trim().isEmpty() ? email.trim() : null);
        user.setRole(roleSelect.getValue());
        
        // Only set password if provided
        String password = passwordField.getText();
        if (password != null && !password.isEmpty()) {
            user.setPassword(password);
        } else if (existingUser == null) {
            // Should not reach here if validate() was called first
            throw new IllegalStateException("Password is required for new users");
        }
        
        return user;
    }

    public boolean validate() {
        boolean valid = true;
        
        // Username validation (format + length)
        String usernameError = Validators.validateUsername(usernameField.getText());
        if (usernameError != null) {
            usernameField.getControl().setStyle("-fx-border-color: #dc2626;");
            valid = false;
        } else {
            usernameField.getControl().setStyle("");
        }
        
        // Full name validation (format + length)
        String fullNameError = Validators.validateFullName(fullNameField.getText());
        if (fullNameError != null) {
            fullNameField.getControl().setStyle("-fx-border-color: #dc2626;");
            valid = false;
        } else {
            fullNameField.getControl().setStyle("");
        }
        
        // Email validation (optional but must be valid if provided)
        String email = emailField.getText();
        if (email != null && !email.trim().isEmpty()) {
            String emailError = Validators.validateEmail(email);
            if (emailError != null) {
                emailField.getControl().setStyle("-fx-border-color: #dc2626;");
                valid = false;
            } else {
                emailField.getControl().setStyle("");
            }
        } else {
            emailField.getControl().setStyle("");
        }
        
        // Password validation (required for new users, strength check always)
        if (existingUser == null) {
            String passwordError = Validators.validatePasswordStrength(passwordField.getText());
            if (passwordError != null) {
                passwordField.getControl().setStyle("-fx-border-color: #dc2626;");
                valid = false;
            } else {
                passwordField.getControl().setStyle("");
            }
        } else {
            // For existing users, only validate strength if password was entered
            String password = passwordField.getText();
            if (password != null && !password.isEmpty()) {
                String passwordError = Validators.validatePasswordStrength(password);
                if (passwordError != null) {
                    passwordField.getControl().setStyle("-fx-border-color: #dc2626;");
                    valid = false;
                } else {
                    passwordField.getControl().setStyle("");
                }
            }
        }
        
        // Role validation
        if (roleSelect.getValue() == null) {
            roleSelect.setStyle("-fx-border-color: #dc2626;");
            valid = false;
        } else {
            roleSelect.setStyle("");
        }
        
        return valid;
    }
}
