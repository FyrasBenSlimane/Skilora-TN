package com.skilora.user.entity;

import java.util.Objects;

/**
 * BiometricData Entity
 * 
 * Stores face encoding data for biometric authentication.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class BiometricData {
    private String username;
    private String faceEncodingJson;
    private long registeredAt;

    public BiometricData() {
    }

    public BiometricData(String username, String faceEncodingJson) {
        this.username = username;
        this.faceEncodingJson = faceEncodingJson;
        this.registeredAt = System.currentTimeMillis();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFaceEncodingJson() {
        return faceEncodingJson;
    }

    public void setFaceEncodingJson(String faceEncodingJson) {
        this.faceEncodingJson = faceEncodingJson;
    }

    public long getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(long registeredAt) {
        this.registeredAt = registeredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BiometricData that = (BiometricData) o;
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() { return Objects.hash(username); }

    @Override
    public String toString() {
        return "BiometricData{username='" + username + "', registeredAt=" + registeredAt + "}";
    }
}
