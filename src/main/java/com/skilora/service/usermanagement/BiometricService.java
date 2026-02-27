package com.skilora.service.usermanagement;

import com.skilora.model.entity.usermanagement.User;
import javafx.scene.image.Image;

import java.util.Optional;

public class BiometricService {
    private static volatile BiometricService instance;

    public static synchronized BiometricService getInstance() {
        if (instance == null) instance = new BiometricService();
        return instance;
    }

    public Optional<User> verifyFace(Image image) {
        return Optional.empty();
    }

    /** Returns true if the user has biometric data registered (e.g. face). */
    public boolean hasBiometricData(String username) {
        if (username == null || username.isBlank()) return false;
        // Stub: no persistence yet
        return false;
    }
}
