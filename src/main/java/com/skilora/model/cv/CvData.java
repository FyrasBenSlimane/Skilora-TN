package com.skilora.model.cv;

import java.util.ArrayList;
import java.util.List;

/**
 * CvData – flat data container populated by the CV generator form.
 * Passed directly to CvPdfExportService.
 */
public class CvData {

    // ── Personal ──────────────────────────────────────────
    public String fullName  = "";
    public String jobTitle  = "";
    public String email     = "";
    public String phone     = "";
    public String location  = "";
    public String linkedIn  = "";
    public String website   = "";
    public String summary   = "";

    // ── Sections ──────────────────────────────────────────
    public List<ExperienceEntry>    experiences    = new ArrayList<>();
    public List<EducationEntry>     educations     = new ArrayList<>();
    public List<String>             skills         = new ArrayList<>();
    public List<CertificationEntry> certifications = new ArrayList<>();
    public List<ProjectEntry>       projects       = new ArrayList<>();

    // ─────────────────────────────────────────────────────
    //  Inner entry types
    // ─────────────────────────────────────────────────────

    public static class ExperienceEntry {
        public String company     = "";
        public String role        = "";
        public String startDate   = "";
        public String endDate     = "";
        public String description = "";

        public ExperienceEntry() {}

        public ExperienceEntry(String company, String role,
                               String startDate, String endDate, String description) {
            this.company     = company;
            this.role        = role;
            this.startDate   = startDate;
            this.endDate     = endDate;
            this.description = description;
        }
    }

    public static class EducationEntry {
        public String institution = "";
        public String degree      = "";
        public String field       = "";
        public String startYear   = "";
        public String endYear     = "";

        public EducationEntry() {}

        public EducationEntry(String institution, String degree,
                              String field, String startYear, String endYear) {
            this.institution = institution;
            this.degree      = degree;
            this.field       = field;
            this.startYear   = startYear;
            this.endYear     = endYear;
        }
    }

    public static class CertificationEntry {
        public String name   = "";
        public String issuer = "";
        public String year   = "";

        public CertificationEntry() {}

        public CertificationEntry(String name, String issuer, String year) {
            this.name   = name;
            this.issuer = issuer;
            this.year   = year;
        }
    }

    public static class ProjectEntry {
        public String name         = "";
        public String technologies = "";
        public String description  = "";

        public ProjectEntry() {}

        public ProjectEntry(String name, String technologies, String description) {
            this.name         = name;
            this.technologies = technologies;
            this.description  = description;
        }
    }
}
