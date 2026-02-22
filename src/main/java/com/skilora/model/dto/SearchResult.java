package com.skilora.model.dto;

public class SearchResult {
    private String entityType;
    private int entityId;
    private String name;
    private String details;
    private String icon;

    public SearchResult(String entityType, int entityId, String name, String details, String icon) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.name = name;
        this.details = details;
        this.icon = icon;
    }

    public String getEntityType() {
        return entityType;
    }

    public int getEntityId() {
        return entityId;
    }

    public String getName() {
        return name;
    }

    public String getDetails() {
        return details;
    }

    public String getIcon() {
        return icon;
    }
}
