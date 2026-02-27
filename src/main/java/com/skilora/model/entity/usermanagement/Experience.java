package com.skilora.model.entity.usermanagement;

import java.time.LocalDate;

public class Experience {
    private int id;
    private int profileId;
    private String companyName;
    private String jobTitle;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean currentJob;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProfileId() { return profileId; }
    public void setProfileId(int profileId) { this.profileId = profileId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    /** Alias for DB column "company". */
    public String getCompany() { return companyName; }
    public void setCompany(String company) { this.companyName = company; }
    /** Alias for DB column "position". */
    public String getPosition() { return jobTitle; }
    public void setPosition(String position) { this.jobTitle = position; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public boolean isCurrentJob() { return currentJob; }
    public void setCurrentJob(boolean currentJob) { this.currentJob = currentJob; }
}
