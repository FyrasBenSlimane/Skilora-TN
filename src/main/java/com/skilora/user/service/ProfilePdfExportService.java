package com.skilora.user.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.skilora.user.entity.Experience;
import com.skilora.user.entity.Profile;
import com.skilora.user.entity.Skill;
import com.skilora.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ProfilePdfExportService - Exports user profiles to professional-looking PDF files.
 * Includes personal info, skills, experiences, and profile completion status.
 */
public class ProfilePdfExportService {

    private static final Logger logger = LoggerFactory.getLogger(ProfilePdfExportService.class);
    private static ProfilePdfExportService instance;

    // Fonts
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 24, Font.BOLD, new Color(0, 0, 0));
    private static final Font HEADLINE_FONT = new Font(Font.HELVETICA, 12, Font.ITALIC, new Color(100, 100, 100));
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(0, 102, 204));
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(80, 80, 80));
    private static final Font TEXT_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(0, 0, 0));
    private static final Font MUTED_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(120, 120, 120));
    private static final Font SKILL_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(0, 102, 204));

    private ProfilePdfExportService() {}

    public static synchronized ProfilePdfExportService getInstance() {
        if (instance == null) {
            instance = new ProfilePdfExportService();
        }
        return instance;
    }

    /**
     * Export user profile to PDF file.
     *
     * @param user     The user entity
     * @param file     The destination file
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    public CompletableFuture<Boolean> exportToPdf(User user, File file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProfileService profileService = ProfileService.getInstance();
                Profile profile = profileService.findProfileByUserId(user.getId());
                
                if (profile == null) {
                    logger.warn("No profile found for user {}", user.getId());
                    return false;
                }

                Map<String, Object> details = profileService.getProfileWithDetails(profile.getId());
                @SuppressWarnings("unchecked")
                List<Skill> skills = (List<Skill>) details.get("skills");
                @SuppressWarnings("unchecked")
                List<Experience> experiences = (List<Experience>) details.get("experiences");
                int completion = (int) details.get("completionPercentage");

                return generatePdf(user, profile, skills, experiences, completion, file);

            } catch (Exception e) {
                logger.error("Failed to export profile to PDF", e);
                return false;
            }
        });
    }

    /**
     * Generate the actual PDF document.
     */
    private boolean generatePdf(User user, Profile profile, List<Skill> skills, 
                                 List<Experience> experiences, int completion, File file) {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // ── Header Section ──
            addHeader(document, user, profile, completion);
            document.add(new Paragraph(" "));

            // ── Contact Information ──
            addContactSection(document, user, profile);
            document.add(new Paragraph(" "));

            // ── About / Bio ──
            if (profile.getBio() != null && !profile.getBio().isBlank()) {
                addAboutSection(document, profile);
                document.add(new Paragraph(" "));
            }

            // ── Skills Section ──
            if (skills != null && !skills.isEmpty()) {
                addSkillsSection(document, skills);
                document.add(new Paragraph(" "));
            }

            // ── Experience Section ──
            if (experiences != null && !experiences.isEmpty()) {
                addExperienceSection(document, experiences);
            }

            // ── Footer ──
            addFooter(document);

            document.close();
            logger.info("Profile PDF exported successfully to {}", file.getAbsolutePath());
            return true;

        } catch (DocumentException | IOException e) {
            logger.error("Failed to generate PDF", e);
            return false;
        }
    }

    private void addHeader(Document document, User user, Profile profile, int completion) throws DocumentException {
        // Name
        Paragraph name = new Paragraph(profile.getFullName(), TITLE_FONT);
        name.setAlignment(Element.ALIGN_CENTER);
        document.add(name);

        // Headline
        if (profile.getHeadline() != null && !profile.getHeadline().isBlank()) {
            Paragraph headline = new Paragraph(profile.getHeadline(), HEADLINE_FONT);
            headline.setAlignment(Element.ALIGN_CENTER);
            document.add(headline);
        }

        // Location
        if (profile.getLocation() != null && !profile.getLocation().isBlank()) {
            Paragraph location = new Paragraph("📍 " + profile.getLocation(), MUTED_FONT);
            location.setAlignment(Element.ALIGN_CENTER);
            document.add(location);
        }

        // Separator
        document.add(new Paragraph(" "));
        addSeparator(document);
    }

    private void addContactSection(Document document, User user, Profile profile) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Contact Information", SECTION_FONT);
        document.add(sectionTitle);
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});

        addTableRow(table, "Email", user.getEmail() != null ? user.getEmail() : "-");
        addTableRow(table, "Phone", profile.getPhone() != null ? profile.getPhone() : "-");
        if (profile.getWebsite() != null && !profile.getWebsite().isBlank()) {
            addTableRow(table, "Website", profile.getWebsite());
        }
        if (profile.getBirthDate() != null) {
            addTableRow(table, "Birth Date", profile.getBirthDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        }

        document.add(table);
    }

    private void addAboutSection(Document document, Profile profile) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("About", SECTION_FONT);
        document.add(sectionTitle);
        document.add(new Paragraph(" "));

        Paragraph bio = new Paragraph(profile.getBio(), TEXT_FONT);
        bio.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(bio);
    }

    private void addSkillsSection(Document document, List<Skill> skills) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Skills", SECTION_FONT);
        document.add(sectionTitle);
        document.add(new Paragraph(" "));

        StringBuilder skillsText = new StringBuilder();
        for (int i = 0; i < skills.size(); i++) {
            Skill skill = skills.get(i);
            skillsText.append(skill.getSkillName());
            if (skill.getYearsExperience() > 0) {
                skillsText.append(" (").append(skill.getYearsExperience()).append(" yrs)");
            }
            if (skill.isVerified()) {
                skillsText.append(" ✓");
            }
            if (i < skills.size() - 1) {
                skillsText.append("  •  ");
            }
        }

        Paragraph skillsParagraph = new Paragraph(skillsText.toString(), SKILL_FONT);
        document.add(skillsParagraph);
    }

    private void addExperienceSection(Document document, List<Experience> experiences) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Work Experience", SECTION_FONT);
        document.add(sectionTitle);
        document.add(new Paragraph(" "));

        for (Experience exp : experiences) {
            // Job title and company
            Paragraph title = new Paragraph(
                    (exp.getPosition() != null ? exp.getPosition() : "Position") + 
                    " at " + 
                    (exp.getCompany() != null ? exp.getCompany() : "Company"), 
                    LABEL_FONT);
            document.add(title);

            // Date range
            String dateRange = formatDateRange(exp.getStartDate(), exp.getEndDate(), exp.isCurrentJob());
            Paragraph dates = new Paragraph(dateRange, MUTED_FONT);
            document.add(dates);

            // Description
            if (exp.getDescription() != null && !exp.getDescription().isBlank()) {
                Paragraph desc = new Paragraph(exp.getDescription(), TEXT_FONT);
                desc.setIndentationLeft(10);
                document.add(desc);
            }

            document.add(new Paragraph(" "));
        }
    }

    private void addFooter(Document document) throws DocumentException {
        addSeparator(document);
        Paragraph footer = new Paragraph(
                "Generated from Skilora on " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                MUTED_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private void addSeparator(Document document) throws DocumentException {
        Paragraph line = new Paragraph("─".repeat(80), MUTED_FONT);
        line.setAlignment(Element.ALIGN_CENTER);
        document.add(line);
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, TEXT_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(5);
        table.addCell(valueCell);
    }

    private String formatDateRange(LocalDate start, LocalDate end, boolean isCurrent) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        String startStr = start != null ? start.format(formatter) : "N/A";
        String endStr = isCurrent ? "Present" : (end != null ? end.format(formatter) : "N/A");
        return startStr + " - " + endStr;
    }
}
