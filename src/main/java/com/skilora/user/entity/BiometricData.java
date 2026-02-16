package com.skilora.user.entity;

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
}
