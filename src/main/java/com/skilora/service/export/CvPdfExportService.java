package com.skilora.service.export;

import com.skilora.model.cv.CvData;
import com.skilora.model.cv.CvData.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * CvPdfExportService
 *
 * Generates a professional, ATS-friendly CV PDF from a {@link CvData} object.
 * Uses Apache PDFBox 2.x (no external account required).
 *
 * Layout (A4, single-column):
 *  ┌──────────────────────────────────────────────┐
 *  │  Dark-blue header  │ Name + Title + Contacts │
 *  ├──────────────────────────────────────────────┤
 *  │  Summary / Experience / Education / Skills    │
 *  │  Certifications / Projects                   │
 *  └──────────────────────────────────────────────┘
 */
public class CvPdfExportService {

    private static final Logger logger = LoggerFactory.getLogger(CvPdfExportService.class);

    // ── Colors ────────────────────────────────────────────────
    private static final Color COLOR_HEADER_BG   = new Color(15, 40, 80);      // #0f2850 deep navy
    private static final Color COLOR_ACCENT      = new Color(37, 99, 235);     // #2563eb blue
    private static final Color COLOR_TEXT        = new Color(17, 24, 39);      // #111827
    private static final Color COLOR_MUTED       = new Color(107, 114, 128);   // #6b7280
    private static final Color COLOR_WHITE       = Color.WHITE;
    // ── Fonts ────────────────────────────────────────────────
    private static final PDFont FONT_BOLD   = PDType1Font.HELVETICA_BOLD;
    private static final PDFont FONT_REG    = PDType1Font.HELVETICA;
    private static final PDFont FONT_ITALIC = PDType1Font.HELVETICA_OBLIQUE;

    // ── Page geometry (A4 in points: 595 x 842) ──────────────
    private static final float PAGE_W      = PDRectangle.A4.getWidth();
    private static final float PAGE_H      = PDRectangle.A4.getHeight();
    private static final float MARGIN_X    = 48f;
    private static final float CONTENT_W   = PAGE_W - 2 * MARGIN_X;
    private static final float HEADER_H    = 130f;

    // Mutable state per export (reset for each call)
    private PDDocument document;
    private PDPageContentStream cs;
    private float currentY;

    // ─────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────

    /**
     * Generates a professional PDF CV and saves it to {@code outputFile}.
     *
     * @param data       Populated CV data from the form
     * @param outputFile Target .pdf file (will be overwritten if it exists)
     * @throws IOException if PDF creation fails
     */
    public void export(CvData data, File outputFile) throws IOException {
        document = new PDDocument();
        newPage();   // initialises cs and currentY

        drawHeader(data);
        currentY -= 20;   // breathing room below header

        if (!isBlank(data.summary))
            drawSection("RÉSUMÉ PROFESSIONNEL", () -> drawParagraph(data.summary, FONT_ITALIC, 10));

        if (!data.experiences.isEmpty())
            drawSection("EXPÉRIENCE PROFESSIONNELLE", () -> drawExperiences(data.experiences));

        if (!data.educations.isEmpty())
            drawSection("FORMATION", () -> drawEducations(data.educations));

        if (!data.skills.isEmpty())
            drawSection("COMPÉTENCES", () -> drawSkills(data.skills));

        if (!data.certifications.isEmpty())
            drawSection("CERTIFICATIONS", () -> drawCertifications(data.certifications));

        if (!data.projects.isEmpty())
            drawSection("PROJETS", () -> drawProjects(data.projects));

        cs.close();
        document.save(outputFile);
        document.close();
        logger.info("CV PDF exported to {}", outputFile.getAbsolutePath());
    }

    // ─────────────────────────────────────────────────────────
    //  Page management
    // ─────────────────────────────────────────────────────────

    private void newPage() throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        if (cs != null) cs.close();
        cs = new PDPageContentStream(document, page);
        currentY = PAGE_H - MARGIN_X;
    }

    /** Ensures at least {@code needed} points remain on the current page. */
    private void ensureSpace(float needed) throws IOException {
        if (currentY - needed < 40) {
            newPage();
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Header
    // ─────────────────────────────────────────────────────────

    private void drawHeader(CvData data) throws IOException {
        float top = PAGE_H;

        // Background rectangle (fills full width, top of page)
        setFill(COLOR_HEADER_BG);
        cs.addRect(0, top - HEADER_H, PAGE_W, HEADER_H);
        cs.fill();

        float x    = MARGIN_X;
        float nameY = top - 44;

        // Name
        setFill(COLOR_WHITE);
        String name = isBlank(data.fullName) ? "Votre Nom" : sanitize(data.fullName);
        drawString(name, FONT_BOLD, 26, x, nameY);

        // Job title
        if (!isBlank(data.jobTitle)) {
            setFill(new Color(186, 209, 255));
            drawString(sanitize(data.jobTitle), FONT_REG, 13, x, nameY - 22);
        }

        // Contact line (icons approximated by brackets)
        List<String> contacts = new ArrayList<>();
        if (!isBlank(data.email))    contacts.add(sanitize(data.email));
        if (!isBlank(data.phone))    contacts.add(sanitize(data.phone));
        if (!isBlank(data.location)) contacts.add(sanitize(data.location));
        if (!isBlank(data.linkedIn)) contacts.add(sanitize(data.linkedIn));
        if (!isBlank(data.website))  contacts.add(sanitize(data.website));

        if (!contacts.isEmpty()) {
            setFill(new Color(209, 224, 255));
            String contactLine = String.join("  |  ", contacts);
            // wrap if too long
            List<String> cLines = wrapText(contactLine, FONT_REG, 10, CONTENT_W);
            float cy = nameY - 48;
            for (String cl : cLines) {
                drawString(cl, FONT_REG, 10, x, cy);
                cy -= 14;
            }
        }

        // Move currentY below header
        currentY = top - HEADER_H;
    }

    // ─────────────────────────────────────────────────────────
    //  Section wrapper
    // ─────────────────────────────────────────────────────────

    @FunctionalInterface
    interface ContentDrawer { void draw() throws IOException; }

    private void drawSection(String title, ContentDrawer content) throws IOException {
        ensureSpace(50);
        currentY -= 10;

        // Title text
        setFill(COLOR_ACCENT);
        drawString(sanitize(title), FONT_BOLD, 11, MARGIN_X, currentY);
        currentY -= 6;

        // Underline
        setFill(COLOR_ACCENT);
        cs.addRect(MARGIN_X, currentY, CONTENT_W, 1.5f);
        cs.fill();
        currentY -= 10;

        setFill(COLOR_TEXT);
        content.draw();

        currentY -= 6;
    }

    // ─────────────────────────────────────────────────────────
    //  Content drawers
    // ─────────────────────────────────────────────────────────

    private void drawExperiences(List<ExperienceEntry> list) throws IOException {
        for (ExperienceEntry e : list) {
            ensureSpace(55);

            // Company (left) + date range (right)
            String dateRange = formatRange(e.startDate, e.endDate, "Présent");
            drawKeyValue(sanitize(e.company), FONT_BOLD, 11,
                         sanitize(dateRange),  FONT_REG,  10);
            currentY -= 2;

            // Role
            if (!isBlank(e.role)) {
                setFill(COLOR_MUTED);
                drawString(sanitize(e.role), FONT_ITALIC, 10, MARGIN_X, currentY);
                currentY -= 14;
            }

            // Description as bullet points
            if (!isBlank(e.description)) {
                setFill(COLOR_TEXT);
                for (String bullet : splitBullets(e.description)) {
                    drawBullet(bullet);
                }
            }
            currentY -= 6;
        }
    }

    private void drawEducations(List<EducationEntry> list) throws IOException {
        for (EducationEntry e : list) {
            ensureSpace(40);

            String dateRange = formatRange(e.startYear, e.endYear, "");
            drawKeyValue(sanitize(e.institution), FONT_BOLD, 11,
                         sanitize(dateRange),      FONT_REG,  10);
            currentY -= 2;

            String degreeField = joinNonBlank(" — ", e.degree, e.field);
            if (!isBlank(degreeField)) {
                setFill(COLOR_MUTED);
                drawString(sanitize(degreeField), FONT_REG, 10, MARGIN_X, currentY);
                currentY -= 14;
            }
            currentY -= 4;
        }
    }

    private void drawSkills(List<String> skills) throws IOException {
        // Render skills in a wrapped flow (3 per row visually)
        StringBuilder line = new StringBuilder();
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < skills.size(); i++) {
            String s = sanitize(skills.get(i).trim());
            if (s.isEmpty()) continue;
            if (line.length() > 0) line.append("   •   ");
            line.append(s);
            // estimate width and break if needed
            float w = textWidth(line.toString(), FONT_REG, 10);
            if (w > CONTENT_W - 20 || i == skills.size() - 1) {
                rows.add(line.toString());
                line.setLength(0);
            }
        }
        for (String row : rows) {
            ensureSpace(15);
            setFill(COLOR_TEXT);
            drawString(row, FONT_REG, 10, MARGIN_X, currentY);
            currentY -= 15;
        }
    }

    private void drawCertifications(List<CertificationEntry> list) throws IOException {
        for (CertificationEntry c : list) {
            ensureSpace(15);
            String txt = sanitize(c.name);
            if (!isBlank(c.issuer)) txt += "  –  " + sanitize(c.issuer);
            if (!isBlank(c.year))   txt += "  (" + sanitize(c.year) + ")";
            drawBullet(txt);
        }
    }

    private void drawProjects(List<ProjectEntry> list) throws IOException {
        for (ProjectEntry p : list) {
            ensureSpace(50);

            // Project name
            setFill(COLOR_TEXT);
            drawString(sanitize(p.name), FONT_BOLD, 11, MARGIN_X, currentY);
            currentY -= 14;

            // Technologies
            if (!isBlank(p.technologies)) {
                setFill(COLOR_MUTED);
                drawString("Technologies : " + sanitize(p.technologies), FONT_ITALIC, 9, MARGIN_X, currentY);
                currentY -= 13;
            }

            // Description
            if (!isBlank(p.description)) {
                setFill(COLOR_TEXT);
                drawParagraph(p.description, FONT_REG, 10);
            }
            currentY -= 6;
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Low-level drawing primitives
    // ─────────────────────────────────────────────────────────

    private void drawString(String text, PDFont font, float size, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        currentY -= (size + 4);
    }

    /** Draws wrapped paragraph; updates currentY after. */
    private void drawParagraph(String text, PDFont font, float size) throws IOException {
        List<String> lines = wrapText(sanitize(text), font, size, CONTENT_W);
        for (String line : lines) {
            ensureSpace(size + 4);
            cs.beginText();
            cs.setFont(font, size);
            cs.newLineAtOffset(MARGIN_X, currentY);
            cs.showText(line);
            cs.endText();
            currentY -= (size + 4);
        }
    }

    /** Draws a bullet point (•) with wrapped text. */
    private void drawBullet(String text) throws IOException {
        float bulletX  = MARGIN_X + 8;
        float textX    = MARGIN_X + 20;
        float textW    = CONTENT_W - 20;
        float size     = 10;

        List<String> lines = wrapText(sanitize(text), FONT_REG, size, textW);
        boolean first = true;
        for (String line : lines) {
            ensureSpace(size + 4);
            if (first) {
                cs.beginText();
                cs.setFont(FONT_BOLD, size);
                cs.newLineAtOffset(bulletX, currentY);
                cs.showText("\u2022");
                cs.endText();
                first = false;
            }
            cs.beginText();
            cs.setFont(FONT_REG, size);
            cs.newLineAtOffset(textX, currentY);
            cs.showText(line);
            cs.endText();
            currentY -= (size + 4);
        }
    }

    /**
     * Draws a left-aligned label (bold) and a right-aligned value on the same line.
     * Advances currentY by line height.
     */
    private void drawKeyValue(String left, PDFont lFont, float lSize,
                               String right, PDFont rFont, float rSize) throws IOException {
        float lineH = Math.max(lSize, rSize) + 4;
        ensureSpace(lineH);

        cs.beginText();
        cs.setFont(lFont, lSize);
        cs.newLineAtOffset(MARGIN_X, currentY);
        cs.showText(left);
        cs.endText();

        if (!right.isEmpty()) {
            float rWidth = textWidth(right, rFont, rSize);
            setFill(COLOR_MUTED);
            cs.beginText();
            cs.setFont(rFont, rSize);
            cs.newLineAtOffset(PAGE_W - MARGIN_X - rWidth, currentY);
            cs.showText(right);
            cs.endText();
            setFill(COLOR_TEXT);
        }

        currentY -= lineH;
    }

    private void setFill(Color c) throws IOException {
        cs.setNonStrokingColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
    }

    // ─────────────────────────────────────────────────────────
    //  Utility helpers
    // ─────────────────────────────────────────────────────────

    private List<String> wrapText(String text, PDFont font, float size, float maxW) throws IOException {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) return result;

        // split on existing newlines first
        for (String paragraph : text.split("\n")) {
            String[] words = paragraph.split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                if (word.isEmpty()) continue;
                String test = line.length() == 0 ? word : line + " " + word;
                if (textWidth(test, font, size) > maxW && line.length() > 0) {
                    result.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(test);
                }
            }
            if (line.length() > 0) result.add(line.toString());
        }
        return result;
    }

    private float textWidth(String text, PDFont font, float size) {
        try {
            return font.getStringWidth(text) / 1000 * size;
        } catch (IOException e) {
            return text.length() * size * 0.55f; // fallback estimate
        }
    }

    /** Splits description text into individual bullet strings. */
    private List<String> splitBullets(String text) {
        List<String> bullets = new ArrayList<>();
        for (String line : text.split("\n")) {
            String trimmed = line.replaceFirst("^[•\\-*]\\s*", "").trim();
            if (!trimmed.isEmpty()) bullets.add(trimmed);
        }
        if (bullets.isEmpty()) bullets.add(text.trim());
        return bullets;
    }

    private String formatRange(String start, String end, String endFallback) {
        boolean hasStart = !isBlank(start);
        boolean hasEnd   = !isBlank(end);
        if (!hasStart && !hasEnd) return "";
        if (!hasStart) return hasEnd ? end : "";
        return hasStart && hasEnd ? start + " – " + end
                                  : start + " – " + endFallback;
    }

    private String joinNonBlank(String sep, String... parts) {
        List<String> nonEmpty = new ArrayList<>();
        for (String p : parts) if (!isBlank(p)) nonEmpty.add(p.trim());
        return String.join(sep, nonEmpty);
    }

    /**
     * Replaces characters not supported by Helvetica (Type1/WinAnsi) with ASCII equivalents.
     * French accented characters (é, è, à, ç, ù, …) ARE supported natively.
     */
    private String sanitize(String s) {
        if (s == null) return "";
        return s.replace("\u2019", "'")   // right single quotation mark
                .replace("\u201C", "\"")  // left double quote
                .replace("\u201D", "\"")  // right double quote
                .replace("\u2013", "-")   // en-dash
                .replace("\u2014", "--")  // em-dash
                .replace("\u2022", "*");  // bullet (will be redrawn as native bullet)
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
