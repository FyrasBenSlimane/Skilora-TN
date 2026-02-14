package com.skilora.controller;

import com.skilora.framework.components.*;
import com.skilora.model.entity.User;
import com.skilora.model.enums.Role;

import javafx.fxml.FXML;

public class UserFormController {

    @FXML
    private TLTextField usernameField;
    @FXML
    private TLTextField fullNameField;
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
            roleSelect.setValue(user.getRole());
        }
    }

    public User getUser() {
        User user = existingUser != null ? existingUser : new User();
        user.setUsername(usernameField.getText());
        user.setFullName(fullNameField.getText());
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
        
        // Username validation
        if (usernameField.getText() == null || usernameField.getText().trim().isEmpty()) {
            usernameField.getControl().setStyle("-fx-border-color: #dc2626;");
            valid = false;
        } else {
            usernameField.getControl().setStyle("");
        }
        
        // Full name validation
        if (fullNameField.getText() == null || fullNameField.getText().trim().isEmpty()) {
            fullNameField.getControl().setStyle("-fx-border-color: #dc2626;");
            valid = false;
        } else {
            fullNameField.getControl().setStyle("");
        }
        
        // Password validation (only for new users)
        if (existingUser == null) {
            String password = passwordField.getText();
            if (password == null || password.trim().isEmpty()) {
                passwordField.getControl().setStyle("-fx-border-color: #dc2626;");
                valid = false;
            } else {
                passwordField.getControl().setStyle("");
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
