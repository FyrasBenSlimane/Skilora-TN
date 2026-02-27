package com.skilora.model.entity.usermanagement;

public class BiometricData {
    private int id;
    private int userId;
    private byte[] faceTemplate;
    private String format;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public byte[] getFaceTemplate() { return faceTemplate; }
    public void setFaceTemplate(byte[] faceTemplate) { this.faceTemplate = faceTemplate; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}
