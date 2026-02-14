package com.skilora.model.entity.formation;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * TrainingMaterial Entity
 * 
 * Represents downloadable materials/resources for a training.
 */
public class TrainingMaterial {
    private int id;
    private int trainingId;
    private Integer lessonId; // Optional - can be associated with a specific lesson
    private String name;
    private String description;
    private String fileUrl; // URL or path to the file
    private String fileType; // PDF, DOCX, ZIP, etc.
    private long fileSizeBytes;
    private LocalDateTime createdAt;

    public TrainingMaterial() {
        this.createdAt = LocalDateTime.now();
    }

    public TrainingMaterial(int trainingId, String name, String fileUrl, String fileType) {
        this();
        this.trainingId = trainingId;
        this.name = name;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
    }

    // Getters
    public int getId() { return id; }
    public int getTrainingId() { return trainingId; }
    public Integer getLessonId() { return lessonId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getFileUrl() { return fileUrl; }
    public String getFileType() { return fileType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTrainingId(int trainingId) { this.trainingId = trainingId; }
    public void setLessonId(Integer lessonId) { this.lessonId = lessonId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getFormattedFileSize() {
        if (fileSizeBytes < 1024) {
            return fileSizeBytes + " B";
        } else if (fileSizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", fileSizeBytes / 1024.0);
        } else {
            return String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024.0));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrainingMaterial that = (TrainingMaterial) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
