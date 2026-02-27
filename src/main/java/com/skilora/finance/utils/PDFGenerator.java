package com.skilora.finance.utils;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

/**
 * Générateur de rapports Finance (PDF et HTML).
 * L'utilisateur choisit l'emplacement et le nom du fichier via une boîte de
 * dialogue.
 */
public class PDFGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
    // Skilora branding (teal from logo)
    private static final Color SKILORA_TEAL = new Color(20, 184, 166); // #14b8a6
    private static final Color SKILORA_DARK = new Color(51, 65, 85); // #334155
    private static final Color HEADER_COLOR = new Color(30, 58, 138);
    private static final Color LIGHT_BG = new Color(248, 250, 252);
    private static final Color SECTION_BG = new Color(241, 245, 249); // #f1f5f9
    // Style PDF (aligné HTML/CSS :root)
    private static final Color PDF_DARK_BG = new Color(248, 250, 252); // --bg #f8fafc
    private static final Color PDF_CARD_BG = new Color(255, 255, 255); // --card #ffffff
    private static final Color PDF_DARK_BORDER = new Color(226, 232, 240); // --border #e2e8f0
    private static final Color PDF_TEAL = new Color(37, 99, 235); // --primary #2563eb
    private static final Color PDF_TEXT = new Color(15, 23, 42); // --text-main #0f172a
    private static final Color PDF_MUTED = new Color(100, 116, 139); // --text-muted #64748b
    private static final Color PDF_GOLD = new Color(240, 192, 64); // --gold #f0c040

    /**
     * Ouvre une boîte de dialogue pour que l'utilisateur choisisse où enregistrer
     * le rapport.
     * Propose PDF et HTML. Répertoire initial : dossier personnel de l'utilisateur.
     * Si customSummary est fourni (non null et non vide), il est utilisé comme bloc
     * Résumé à la place du résumé générique.
     */
    public static File generateEmployeeReport(
            int employeeId,
            String employeeName,
            String contractInfo,
            String bankInfo,
            String bonusInfo,
            String payslipInfo,
            Stage ownerStage,
            String customSummary) {

        String safeName = (employeeName != null ? employeeName : "Employe").replaceAll("[^a-zA-Z0-9_-]", "_");
        String defaultName = "Rapport_Financier_" + safeName + "_" + LocalDate.now() + ".pdf";

        FileChooser fileChooser = createFileChooser(
                "Enregistrer le rapport financier",
                defaultName,
                "Rapports PDF (*.pdf)",
                "*.pdf",
                "Fichiers HTML (*.html)",
                "*.html");

        File file = fileChooser.showSaveDialog(ownerStage);
        if (file == null)
            return null;

        FileChooser.ExtensionFilter filter = fileChooser.getSelectedExtensionFilter();
        file = ensureExtension(file, filter);

        try {
            if (isPdf(file)) {
                try {
                    generateEmployeeReportPDFFromHtml(file, employeeId, employeeName, contractInfo, bankInfo, bonusInfo,
                            payslipInfo, customSummary);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (file.exists())
                        file.delete();
                    generateEmployeeReportPDF(file, employeeId, employeeName, contractInfo, bankInfo, bonusInfo,
                            payslipInfo, customSummary);
                }
            } else {
                generateEmployeeReportHTML(file, employeeId, employeeName, contractInfo, bankInfo, bonusInfo,
                        payslipInfo, customSummary);
            }
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Erreur PDF");
            alert.setHeaderText("Échec génération PDF");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            return null;
        }
    }

    /** Génération sans résumé personnalisé (résumé générique). */
    public static File generateEmployeeReport(
            int employeeId,
            String employeeName,
            String contractInfo,
            String bankInfo,
            String bonusInfo,
            String payslipInfo,
            Stage ownerStage) {
        return generateEmployeeReport(employeeId, employeeName, contractInfo, bankInfo, bonusInfo, payslipInfo,
                ownerStage, null);
    }

    /**
     * Ouvre une boîte de dialogue pour enregistrer le bulletin de paie (PDF ou
     * HTML).
     */
    public static File generatePayslipPDF(
            com.skilora.finance.model.PayslipRow payslip,
            String employeeName,
            Stage ownerStage) {

        String safeName = (employeeName != null ? employeeName : "Employe").replaceAll("[^a-zA-Z0-9_-]", "_");
        String defaultName = "Bulletin_" + safeName + "_" + payslip.getMonth() + "-" + payslip.getYear() + ".pdf";

        FileChooser fileChooser = createFileChooser(
                "Enregistrer le bulletin de paie",
                defaultName,
                "Rapports PDF (*.pdf)",
                "*.pdf",
                "Fichiers HTML (*.html)",
                "*.html");

        File file = fileChooser.showSaveDialog(ownerStage);
        if (file == null)
            return null;

        FileChooser.ExtensionFilter filter = fileChooser.getSelectedExtensionFilter();
        file = ensureExtension(file, filter);

        try {
            if (isPdf(file)) {
                generatePayslipPDF(file, payslip, employeeName);
            } else {
                generatePayslipHTML(file, payslip, employeeName);
            }
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ─── FileChooser : l'utilisateur choisit où enregistrer ─────────────────

    private static FileChooser createFileChooser(String title, String initialFileName,
            String filterDesc1, String ext1,
            String filterDesc2, String ext2) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.setInitialFileName(initialFileName);

        File userHome = new File(System.getProperty("user.home"));
        File documents = new File(userHome, "Documents");
        if (documents.isDirectory()) {
            fc.setInitialDirectory(documents);
        } else {
            fc.setInitialDirectory(userHome);
        }

        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(filterDesc1, ext1),
                new FileChooser.ExtensionFilter(filterDesc2, ext2),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
        return fc;
    }

    private static File ensureExtension(File file, FileChooser.ExtensionFilter selected) {
        if (file == null)
            return file;
        String path = file.getAbsolutePath();
        String name = file.getName().toLowerCase();
        if (selected != null && selected.getExtensions() != null) {
            if (selected.getExtensions().stream().anyMatch(e -> e != null && e.contains("pdf"))
                    && !name.endsWith(".pdf")) {
                return new File(path + ".pdf");
            }
            if (selected.getExtensions().stream().anyMatch(e -> e != null && e.contains("html"))
                    && !name.endsWith(".html")) {
                return new File(path + ".html");
            }
        }
        if (!name.endsWith(".pdf") && !name.endsWith(".html")) {
            return new File(path + ".pdf");
        }
        return file;
    }

    private static boolean isPdf(File file) {
        return file != null && file.getName().toLowerCase().endsWith(".pdf");
    }

    // ─── Génération PDF (OpenPDF) ─────────────────────────────────────────────

    /** Charge le logo Skilora depuis les ressources si présent. */
    private static Image loadLogoImage() {
        try {
            String[] paths = { "images/skilora-logo.png", "/images/skilora-logo.png",
                    "com/skilora/finance/images/skilora-logo.png" };
            for (String path : paths) {
                URL url = PDFGenerator.class.getClassLoader().getResource(path);
                if (url == null)
                    url = PDFGenerator.class.getResource("/" + path);
                if (url != null) {
                    Image img = Image.getInstance(url);
                    img.scaleToFit(180, 52);
                    img.setAlignment(Element.ALIGN_CENTER);
                    return img;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** En-tête PDF avec logo ou texte Skilora (nom + slogan). */
    private static void addSkiloraHeader(Document document) throws DocumentException {
        Image logo = loadLogoImage();
        if (logo != null) {
            document.add(logo);
            document.add(new Paragraph(" "));
        } else {
            Font fontLogo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, SKILORA_DARK);
            Font fontTagline = FontFactory.getFont(FontFactory.HELVETICA, 9, SKILORA_TEAL);
            Paragraph p = new Paragraph();
            p.add(new Chunk("SKILORA", fontLogo));
            p.setAlignment(Element.ALIGN_CENTER);
            document.add(p);
            Paragraph tag = new Paragraph("PROVE YOUR SKILLS  •  LAND YOUR JOB", fontTagline);
            tag.setAlignment(Element.ALIGN_CENTER);
            document.add(tag);
            document.add(new Paragraph(" "));
        }
    }

    /** Dessine le fond sombre (body) sur chaque page du PDF. */
    private static class DarkBackgroundEvent extends PdfPageEventHelper {
        @Override
        public void onStartPage(PdfWriter writer, Document document) {
            PdfContentByte under = writer.getDirectContentUnder();
            under.saveState();
            under.setColorFill(PDF_DARK_BG);
            under.rectangle(0, 0, document.getPageSize().getWidth(), document.getPageSize().getHeight());
            under.fill();
            under.restoreState();
        }
    }

    /**
     * Cellule de section : fond dark-card, bordure gauche teal (comme
     * .section::before).
     */
    private static void addSectionCard(Document document, String icon, String sectionTitle, Element content)
            throws DocumentException {
        PdfPTable card = new PdfPTable(1);
        card.setWidthPercentage(100f);
        card.setSpacingBefore(14f);
        card.setSpacingAfter(0f);
        card.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        card.getDefaultCell().setPadding(24);
        card.getDefaultCell().setPaddingLeft(28);

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100f);
        header.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        header.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
        header.addCell(new Phrase(icon + "  ", FontFactory.getFont(FontFactory.HELVETICA, 14, PDF_TEXT)));
        header.addCell(new Phrase(sectionTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PDF_TEAL)));
        PdfPCell headerCell = new PdfPCell(header);
        headerCell.setBorder(Rectangle.LEFT);
        headerCell.setBorderWidthLeft(4f);
        headerCell.setBorderColorLeft(PDF_TEAL);
        headerCell.setBackgroundColor(PDF_CARD_BG);
        headerCell.setPaddingBottom(14f);
        card.addCell(headerCell);

        PdfPCell bodyCell = new PdfPCell();
        bodyCell.setBorder(Rectangle.LEFT);
        bodyCell.setBorderWidthLeft(4f);
        bodyCell.setBorderColorLeft(PDF_TEAL);
        bodyCell.setBackgroundColor(PDF_CARD_BG);
        bodyCell.setPadding(16);
        bodyCell.setPaddingTop(12f);
        bodyCell.addElement(content);
        card.addCell(bodyCell);
        document.add(card);
    }

    private static void generateEmployeeReportPDF(File file, int employeeId, String employeeName,
            String contractInfo, String bankInfo, String bonusInfo, String payslipInfo, String customSummary)
            throws DocumentException, IOException {
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        writer.setPageEvent(new DarkBackgroundEvent());
        document.open();

        Font fontReportTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, PDF_TEXT);
        Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA, 11, PDF_TEAL);
        Font fontLogo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, PDF_TEXT);
        Font fontTagline = FontFactory.getFont(FontFactory.HELVETICA, 10, PDF_TEAL);
        Font fontMetaLabel = FontFactory.getFont(FontFactory.HELVETICA, 10, PDF_MUTED);
        Font fontMetaValue = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, PDF_TEXT);
        Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 13, PDF_TEXT);
        Font fontFooter = FontFactory.getFont(FontFactory.HELVETICA, 10, PDF_MUTED);

        // HEADER (comme .header)
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100f);
        headerTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        headerTable.getDefaultCell().setVerticalAlignment(Element.ALIGN_TOP);
        headerTable.addCell(new Phrase("Rapport\nFinancier", fontReportTitle));
        Paragraph logoBlock = new Paragraph();
        logoBlock.add(new Chunk("SKIL", fontLogo));
        logoBlock.add(new Chunk("O", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PDF_TEAL)));
        logoBlock.add(new Chunk("RA", fontLogo));
        logoBlock.setAlignment(Element.ALIGN_RIGHT);
        headerTable.addCell(logoBlock);
        document.add(headerTable);
        document.add(new Paragraph("Document confidentiel · Gestion Finance", fontSubtitle));
        document.add(new Paragraph("Prove your skills · Land your job", fontTagline));
        document.add(new Paragraph(" ", fontBody));
        document.add(new Paragraph(" ", fontBody));

        // META BAR (Employé, Émis le)
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidths(new float[] { 1f, 1f });
        metaTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        metaTable.addCell(new Phrase("EMPLOYE", fontMetaLabel));
        metaTable.addCell(new Phrase("EMIS LE", fontMetaLabel));
        metaTable.addCell(new Phrase(employeeName != null ? employeeName : "—", fontMetaValue));
        metaTable.addCell(new Phrase(LocalDate.now().format(DATE_FORMAT), fontMetaValue));
        document.add(metaTable);
        document.add(new Paragraph(" ", fontBody));

        String summaryText = (customSummary != null && !customSummary.isBlank())
                ? customSummary
                : buildReportSummaryPlainText(employeeName, contractInfo, bankInfo, bonusInfo, payslipInfo);
        if (!summaryText.isEmpty()) {
            Paragraph summaryPara = new Paragraph(summaryText, fontBody);
            addSectionCard(document, "\uD83D\uDCCD", "Résumé", summaryPara);
        }

        if (hasContent(contractInfo)) {
            Paragraph cont = new Paragraph(htmlToReadableText(contractInfo), fontBody);
            addSectionCard(document, "\uD83D\uDCCB", "Contrat(s)", cont);
        }
        if (hasContent(bankInfo)) {
            Paragraph bank = new Paragraph(htmlToReadableText(bankInfo), fontBody);
            addSectionCard(document, "\uD83C\uDFE6", "Compte(s) Bancaire(s)", bank);
        }
        if (hasContent(bonusInfo)) {
            Paragraph bonus = new Paragraph(htmlToReadableText(bonusInfo), fontBody);
            addSectionCard(document, "\u2B50", "Primes", bonus);
        }
        if (hasContent(payslipInfo)) {
            Paragraph pay = new Paragraph(htmlToReadableText(payslipInfo), fontBody);
            addSectionCard(document, "\uD83D\uDCFE", "Bulletins de Paie Récents", pay);
        }

        document.add(new Paragraph(" ", fontBody));
        document.add(new Paragraph("— Document confidentiel — Skilora Gestion Finance —", fontFooter));
        Paragraph seal = new Paragraph("\u2726", FontFactory.getFont(FontFactory.HELVETICA, 10, PDF_TEAL));
        seal.setAlignment(Element.ALIGN_CENTER);
        document.add(seal);
        document.add(new Paragraph("© " + LocalDate.now().getYear() + " Skilora", fontFooter));
        document.close();
    }

    private static void generatePayslipPDF(File file, com.skilora.finance.model.PayslipRow p, String empName)
            throws DocumentException, IOException {
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        writer.setPageEvent(new DarkBackgroundEvent());
        document.open();

        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PDF_TEAL);
        Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA, 9, PDF_MUTED);
        Font fontValue = FontFactory.getFont(FontFactory.HELVETICA, 10, PDF_TEXT);
        Font fontAmount = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PDF_TEAL);
        Font fontFooter = FontFactory.getFont(FontFactory.HELVETICA, 8, PDF_MUTED);

        Paragraph logo = new Paragraph();
        logo.add(new Chunk("SKIL", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PDF_TEXT)));
        logo.add(new Chunk("O", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PDF_TEAL)));
        logo.add(new Chunk("RA", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PDF_TEXT)));
        logo.setAlignment(Element.ALIGN_CENTER);
        document.add(logo);
        document.add(new Paragraph("Prove your skills · Land your job",
                FontFactory.getFont(FontFactory.HELVETICA, 7, PDF_TEAL)));
        document.add(new Paragraph(" ", fontValue));

        document.add(new Paragraph("Bulletin de paie", fontTitle));
        document.add(new Paragraph("Periode : " + p.getMonth() + " / " + p.getYear(), fontValue));
        document.add(new Paragraph("Employe : " + (empName != null ? empName : "—"), fontValue));
        document.add(new Paragraph(" ", fontValue));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(10f);
        table.getDefaultCell().setBackgroundColor(PDF_CARD_BG);
        table.getDefaultCell().setBorderColor(PDF_DARK_BORDER);
        table.addCell(cellPdf("Libelle", true));
        table.addCell(cellPdf("Montant (TND)", true));
        table.addCell(cellPdf("Salaire de base", false));
        table.addCell(cellPdf(String.format(Locale.FRANCE, "%.2f", p.getBaseSalary()), false));
        table.addCell(cellPdf("Heures supplementaires", false));
        table.addCell(cellPdf(String.format(Locale.FRANCE, "%.2f", p.getOvertimeTotal()), false));
        table.addCell(cellPdf("Primes", false));
        table.addCell(cellPdf(String.format(Locale.FRANCE, "%.2f", p.getBonuses()), false));
        table.addCell(cellPdf("Deductions (CNSS, IRPP)", false));
        table.addCell(cellPdf("- " + String.format(Locale.FRANCE, "%.2f", p.getTotalDeductions()), false));
        document.add(table);

        document.add(new Paragraph(" ", fontValue));
        document.add(new Paragraph("Salaire brut : " + String.format(Locale.FRANCE, "%.2f", p.getGross()) + " "
                + (p.getCurrency() != null ? p.getCurrency() : "TND"), fontValue));
        document.add(new Paragraph("Net a payer : " + String.format(Locale.FRANCE, "%.2f", p.getNet()) + " "
                + (p.getCurrency() != null ? p.getCurrency() : "TND"), fontAmount));
        document.add(new Paragraph(" ", fontValue));
        document.add(new Paragraph("— Document genere electroniquement — Skilora Gestion Finance —", fontFooter));
        document.close();
    }

    /** Cellule pour tableaux PDF style dark (fond card, texte clair). */
    private static PdfPCell cellPdf(String text, boolean header) {
        Font f = header ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PDF_TEXT)
                : FontFactory.getFont(FontFactory.HELVETICA, 10, PDF_TEXT);
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(PDF_CARD_BG);
        c.setBorderColor(PDF_DARK_BORDER);
        c.setPadding(8);
        return c;
    }

    /**
     * Convertit le HTML en texte lisible avec sauts de ligne (chaque bloc sur sa
     * propre ligne).
     */
    private static String htmlToReadableText(String html) {
        if (html == null)
            return "";
        String s = html
                .replaceAll("</div>", "\n</div>")
                .replaceAll("</span>", " </span>")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("[ \\t]+", " ")
                .replaceAll(" *\n *", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
        return s;
    }

    private static String stripHtml(String html) {
        if (html == null)
            return "";
        return html.replaceAll("<[^>]+>", " ").replaceAll("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }

    private static boolean hasContent(String s) {
        return s != null && !stripHtml(s).isEmpty();
    }

    // ─── Génération HTML (même style que rapport_financier_skilora.html) ───

    private static String loadRapportTemplate() throws IOException {
        URL url = PDFGenerator.class.getResource("/com/skilora/finance/rapport_financier_template.html");
        if (url == null)
            url = PDFGenerator.class.getClassLoader()
                    .getResource("com/skilora/finance/rapport_financier_template.html");
        if (url == null)
            return null;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /** Version texte du résumé pour le PDF OpenPDF (sans HTML). */
    private static String buildReportSummaryPlainText(String employeeName, String contractInfo, String bankInfo,
            String bonusInfo, String payslipInfo) {
        String name = (employeeName != null && !employeeName.isBlank()) ? employeeName : "L'employé";
        StringBuilder summary = new StringBuilder();
        summary.append("Ce rapport présente la situation financière de ").append(name).append(". ");
        boolean hasContract = hasContent(contractInfo);
        boolean hasBank = hasContent(bankInfo);
        boolean hasBonus = hasContent(bonusInfo);
        boolean hasPayslip = hasContent(payslipInfo);
        if (hasContract)
            summary.append("Un ou plusieurs contrats sont enregistrés. ");
        if (hasBank)
            summary.append("Des comptes bancaires sont renseignés pour le virement du salaire. ");
        if (hasBonus)
            summary.append("Des primes ont été versées et figurent dans l'historique. ");
        if (hasPayslip)
            summary.append("Des bulletins de paie sont disponibles pour consultation. ");
        if (!hasContract && !hasBank && !hasBonus && !hasPayslip)
            summary.append("Aucune donnée détaillée n'est disponible pour le moment.");
        return summary.toString().trim();
    }

    /**
     * Génère un résumé automatique du rapport (une phrase de synthèse à partir des
     * sections).
     */
    private static String buildReportSummary(String employeeName, String contractInfo, String bankInfo,
            String bonusInfo, String payslipInfo) {
        String name = (employeeName != null && !employeeName.isBlank()) ? employeeName : "L'employé";
        StringBuilder summary = new StringBuilder();
        summary.append("Ce rapport présente la situation financière de ").append(escape(name)).append(". ");

        boolean hasContract = hasContent(contractInfo);
        boolean hasBank = hasContent(bankInfo);
        boolean hasBonus = hasContent(bonusInfo);
        boolean hasPayslip = hasContent(payslipInfo);

        if (hasContract)
            summary.append("Un ou plusieurs contrats sont enregistrés. ");
        if (hasBank)
            summary.append("Des comptes bancaires sont renseignés pour le virement du salaire. ");
        if (hasBonus)
            summary.append("Des primes ont été versées et figurent dans l'historique. ");
        if (hasPayslip)
            summary.append("Des bulletins de paie sont disponibles pour consultation. ");

        if (!hasContract && !hasBank && !hasBonus && !hasPayslip)
            summary.append("Aucune donnée détaillée n'est disponible pour le moment.");

        return "<div class=\"section\"><div class=\"section-header\"><span class=\"sec-icon\">&#128204;</span><span class=\"sec-title\">R\u00e9sum\u00e9</span></div><p class=\"summary-text\">"
                + summary.toString().trim() + "</p></div>\n";
    }

    /**
     * Construit le HTML complet du rapport (même contenu que la capture /
     * template). customSummary si non vide remplace le résumé générique.
     */
    private static String buildFullReportHtml(String employeeName, String contractInfo, String bankInfo,
            String bonusInfo, String payslipInfo, String customSummary) throws IOException {
        String empName = escape(employeeName != null ? employeeName : "—");
        String dateStr = LocalDate.now().format(DATE_FORMAT);
        int year = LocalDate.now().getYear();

        // Icônes en entités HTML pour compatibilité XML (openhtmltopdf)
        String iconSummary = "&#128204;";
        String iconContract = "&#128203;";
        String iconBank = "&#127974;";
        String iconBonus = "&#11088;";
        String iconPayslip = "&#129534;";

        String sectionSummary = (customSummary != null && !customSummary.isBlank())
                ? "<div class=\"section\"><div class=\"section-header\"><span class=\"sec-icon\">" + iconSummary + "</span><span class=\"sec-title\">R\u00e9sum\u00e9</span></div><p class=\"summary-text\">"
                        + escape(customSummary) + "</p></div>\n"
                : buildReportSummary(employeeName, contractInfo, bankInfo, bonusInfo, payslipInfo);

        String sectionContracts = "";
        if (contractInfo != null && !contractInfo.isEmpty()) {
            sectionContracts = "<div class=\"section\"><div class=\"section-header\"><span class=\"sec-icon\">" + iconContract + "</span><span class=\"sec-title\">Contrat(s)</span></div>\n"
                    + contractInfo + "</div>\n";
        }
        String sectionBank = "";
        if (bankInfo != null && !bankInfo.isEmpty()) {
            sectionBank = "<div class=\"section\"><div class=\"section-header\"><span class=\"sec-icon\">" + iconBank + "</span><span class=\"sec-title\">Compte(s) Bancaire(s)</span></div>\n"
                    + bankInfo + "</div>\n";
        }
        String sectionBonus = "";
        if (bonusInfo != null && !bonusInfo.isEmpty()) {
            sectionBonus = "<div class=\"section\"><div class=\"section-header\"><span class=\"sec-icon\">" + iconBonus + "</span><span class=\"sec-title\">Primes</span></div>\n"
                    + bonusInfo + "</div>\n";
        }
        String sectionPayslips = "";
        if (payslipInfo != null && !payslipInfo.isEmpty()) {
            sectionPayslips = "<div class=\"section\"><div class=\"section-header\"><span class=\"sec-icon\">" + iconPayslip + "</span><span class=\"sec-title\">Bulletins de Paie R\u00e9cents</span></div>\n"
                    + payslipInfo + "</div>\n";
        }

        String template = loadRapportTemplate();
        if (template != null) {
            return template
                    .replace("{{EMPLOYEE_NAME}}", empName)
                    .replace("{{DATE_EMIS}}", dateStr)
                    .replace("{{SECTION_SUMMARY}}", sectionSummary)
                    .replace("{{SECTION_CONTRACTS}}", sectionContracts)
                    .replace("{{SECTION_BANK}}", sectionBank)
                    .replace("{{SECTION_BONUS}}", sectionBonus)
                    .replace("{{SECTION_PAYSLIPS}}", sectionPayslips)
                    .replace("{{YEAR}}", String.valueOf(year));
        }
        return buildRapportHtmlFallback(empName, dateStr, year, sectionSummary, sectionContracts, sectionBank,
                sectionBonus, sectionPayslips);
    }

    /**
     * Génère le PDF à partir du même HTML que la capture (style, cartes, couleurs,
     * ordre).
     * Utilise OpenHTML-to-PDF pour que le PDF soit identique au rendu de la page.
     */
    private static void generateEmployeeReportPDFFromHtml(File file, int employeeId, String employeeName,
            String contractInfo, String bankInfo, String bonusInfo, String payslipInfo, String customSummary)
            throws IOException {
        String html = buildFullReportHtml(employeeName, contractInfo, bankInfo, bonusInfo, payslipInfo, customSummary);

        // DEBUG: dump HTML to temp file to inspect invalid XML
        File debugFile = new File(System.getProperty("user.home"), "rapport_debug.html");
        try (java.io.OutputStreamWriter dw = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(debugFile), StandardCharsets.UTF_8)) {
            dw.write(html);
        } catch (Exception ignored) {
        }

        // Base URL vide : tout le CSS est inline dans le HTML, pas de ressources
        // relatives
        String baseUrl = "file:///";
        try (java.io.OutputStream os = new FileOutputStream(file)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, baseUrl);
            builder.toStream(os);
            builder.run();
        } catch (Exception e) {
            throw new IOException("Échec génération PDF (HTML→PDF): " + e.getMessage(), e);
        }
    }

    private static void generateEmployeeReportHTML(File file, int employeeId, String employeeName,
            String contractInfo, String bankInfo, String bonusInfo, String payslipInfo, String customSummary)
            throws IOException {
        String html = buildFullReportHtml(employeeName, contractInfo, bankInfo, bonusInfo, payslipInfo, customSummary);
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(html);
        }
    }

    /**
     * /** Fallback si le template n'est pas trouvé.
     */
    private static String buildRapportHtmlFallback(String empName, String dateStr, int year,
            String sectionSummary, String sectionContracts, String sectionBank, String sectionBonus,
            String sectionPayslips) {
        String template = "<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\"/><style>"
                + "body{font-family:\"Helvetica Neue\",Helvetica,Arial,sans-serif;margin:0;padding:20px;background:#f8fafc;color:#0f172a;line-height:1.5;}"
                + ".page{max-width:900px;margin:0 auto;background:#ffffff;box-shadow:0 4px 6px rgba(0,0,0,0.05);}"
                + ".header{background:#1e3a8a;color:#ffffff;padding:40px 50px;position:relative;}"
                + ".header h1{margin:0 0 10px 0;font-size:32px;font-weight:300;letter-spacing:0;}"
                + ".header .subtitle{margin:0;color:#93c5fd;font-size:14px;text-transform:uppercase;}"
                + ".top-meta{padding:20px 50px;background:#f1f5f9;border-bottom:1px solid #e2e8f0;font-size:14px;color:#475569;}"
                + ".main-container{width:100%;border-collapse:collapse;}"
                + ".left-col{width:35%;background:#fafaf9;vertical-align:top;padding:40px 30px 40px 50px;border-right:1px solid #e2e8f0;}"
                + ".right-col{width:65%;vertical-align:top;padding:40px 50px 40px 40px;}"
                + ".section-title{color:#1e3a8a;border-bottom:2px solid #cbd5e1;padding-bottom:8px;margin-top:0;margin-bottom:20px;font-size:16px;text-transform:uppercase;font-weight:bold;}"
                + ".left-col .section-title{color:#0f172a;border-bottom:2px solid #e2e8f0;}"
                + "table.data-table{width:100%;border-collapse:collapse;margin-bottom:25px;background:#ffffff;border:1px solid #e2e8f0;}"
                + "table.data-table th,table.data-table td{text-align:left;padding:12px 15px;border-bottom:1px solid #e2e8f0;font-size:14px;}"
                + "table.data-table tr:last-child th,table.data-table tr:last-child td{border-bottom:none;}"
                + "table.data-table th{color:#475569;font-weight:normal;background:#f8fafc;}"
                + "table.data-table td{color:#0f172a;font-weight:bold;}"
                + ".summary-box{font-size:14px;color:#334155;line-height:1.6;background:#ffffff;padding:20px;border-radius:8px;border-left:4px solid #38bdf8;box-shadow:0 1px 3px rgba(0,0,0,0.05);margin-bottom:30px;}"
                + ".footer{text-align:center;font-size:12px;color:#94a3b8;padding:30px;background:#f8fafc;border-top:1px solid #e2e8f0;text-transform:uppercase;}"
                + "</style></head><body><div class=\"page\"><div class=\"header\"><h1>Rapport Financier</h1><p class=\"subtitle\">Dossier Employé • Confidentiel</p></div><div class=\"top-meta\"><strong>Employé :</strong> {{EMPLOYEE_NAME}} &nbsp;&nbsp;|&nbsp;&nbsp; <strong>Date d'émission :</strong> {{DATE_EMIS}}</div><table class=\"main-container\"><tr><td class=\"left-col\"><h3 class=\"section-title\">Résumé Profil</h3><div class=\"summary-box\">{{SECTION_SUMMARY}}</div><h3 class=\"section-title\">Coordonnées Bancaires</h3>{{SECTION_BANK}}<h3 class=\"section-title\" style=\"margin-top: 40px;\">Bilan des Primes</h3>{{SECTION_BONUS}}</td><td class=\"right-col\"><h3 class=\"section-title\">Dossier &amp; Contrats</h3>{{SECTION_CONTRACTS}}<h3 class=\"section-title\" style=\"margin-top: 40px;\">Historique de Paie</h3>{{SECTION_PAYSLIPS}}</td></tr></table><div class=\"footer\">© {{YEAR}} Skilora Gestion Finance — Document généré automatiquement</div></div></body></html>";

        return template
                .replace("{{EMPLOYEE_NAME}}", empName)
                .replace("{{DATE_EMIS}}", dateStr)
                .replace("{{SECTION_SUMMARY}}", sectionSummary != null ? sectionSummary : "")
                .replace("{{SECTION_CONTRACTS}}", sectionContracts)
                .replace("{{SECTION_BANK}}", sectionBank)
                .replace("{{SECTION_BONUS}}", sectionBonus)
                .replace("{{SECTION_PAYSLIPS}}", sectionPayslips)
                .replace("{{YEAR}}", String.valueOf(year));
    }

    private static void generatePayslipHTML(File file, com.skilora.finance.model.PayslipRow p, String empName)
            throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"fr\">\n<head>\n  <meta charset=\"UTF-8\">\n");
        html.append("  <title>Bulletin de paie — ").append(p.getMonth()).append("/").append(p.getYear())
                .append("</title>\n");
        html.append("  <style>\n");
        html.append("    * { box-sizing: border-box; }\n");
        html.append(
                "    body { font-family: 'Segoe UI', 'Helvetica Neue', system-ui, sans-serif; margin: 0; padding: 40px; background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%); color: #1e293b; line-height: 1.5; }\n");
        html.append(
                "    .bulletin { max-width: 720px; margin: 0 auto; background: #fff; padding: 0; border-radius: 16px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); overflow: hidden; }\n");
        html.append("    .logo-bar { background: #334155; padding: 20px 32px; text-align: center; }\n");
        html.append(
                "    .logo-word { font-size: 1.6rem; font-weight: 800; letter-spacing: 0.08em; color: #fff; margin: 0; }\n");
        html.append("    .logo-word .o { color: #14b8a6; }\n");
        html.append(
                "    .logo-tagline { font-size: 0.65rem; letter-spacing: 0.2em; color: #14b8a6; margin-top: 4px; text-transform: uppercase; }\n");
        html.append("    .content { padding: 32px 40px 40px; }\n");
        html.append(
                "    .header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 20px; margin-bottom: 24px; border-bottom: 2px solid #14b8a6; }\n");
        html.append("    .company h1 { margin: 0; color: #334155; font-size: 1.25rem; font-weight: 700; }\n");
        html.append("    .company p { margin: 2px 0 0; color: #64748b; font-size: 0.85rem; }\n");
        html.append("    .periode { text-align: right; }\n");
        html.append(
                "    .periode h2 { margin: 0; color: #14b8a6; font-size: 1rem; text-transform: uppercase; letter-spacing: 0.05em; }\n");
        html.append("    .periode p { margin: 4px 0 0; font-weight: 600; color: #334155; }\n");
        html.append(
                "    .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 24px; }\n");
        html.append(
                "    .info-box { background: #f8fafc; padding: 16px; border-radius: 10px; border-left: 4px solid #14b8a6; }\n");
        html.append(
                "    .info-box h3 { margin: 0 0 12px 0; font-size: 0.7rem; text-transform: uppercase; color: #14b8a6; letter-spacing: 0.06em; font-weight: 700; }\n");
        html.append(
                "    .row { display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 0.9rem; }\n");
        html.append("    table { width: 100%; border-collapse: collapse; }\n");
        html.append(
                "    th { text-align: left; padding: 12px 14px; background: #334155; color: #fff; font-weight: 600; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.04em; }\n");
        html.append("    td { padding: 12px 14px; border-bottom: 1px solid #e2e8f0; }\n");
        html.append("    tr:nth-child(even) { background: #f8fafc; }\n");
        html.append("    .montant { text-align: right; font-variant-numeric: tabular-nums; font-weight: 600; }\n");
        html.append(
                "    .totaux { background: linear-gradient(135deg, #f0fdfa 0%, #ccfbf1 100%); padding: 20px; border-radius: 10px; margin-top: 24px; border: 1px solid #99f6e4; }\n");
        html.append(
                "    .total-row { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 0.95rem; }\n");
        html.append(
                "    .net { display: flex; justify-content: space-between; margin-top: 14px; padding-top: 14px; border-top: 2px solid #14b8a6; font-size: 1.15rem; font-weight: 700; color: #0d9488; }\n");
        html.append(
                "    .footer { text-align: center; padding: 16px; background: #334155; color: rgba(255,255,255,0.8); font-size: 0.75rem; }\n");
        html.append(
                "    @media print { body { background: #fff; } .bulletin { box-shadow: none; } .logo-bar, th, .footer { -webkit-print-color-adjust: exact; print-color-adjust: exact; } }\n");
        html.append("  </style>\n</head>\n<body>\n  <div class=\"bulletin\">\n");

        html.append("    <div class=\"logo-bar\">\n");
        html.append("      <h1 class=\"logo-word\">SKIL<span class=\"o\">O</span>RA</h1>\n");
        html.append("      <p class=\"logo-tagline\">Prove your skills &middot; Land your job</p>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"content\">\n");
        html.append("    <div class=\"header\">\n");
        html.append("      <div class=\"company\"><h1>Bulletin de paie</h1><p>Tunisia</p></div>");
        html.append("      <div class=\"periode\"><h2>Période</h2><p>").append(p.getMonth()).append(" / ")
                .append(p.getYear()).append("</p></div>\n");
        html.append("    </div>\n");

        html.append("    <div class=\"info-grid\">\n");
        html.append("      <div class=\"info-box\"><h3>Employé</h3>");
        html.append("        <div class=\"row\"><span>Nom</span><strong>")
                .append(escape(empName != null ? empName : "—")).append("</strong></div></div>\n");
        html.append("      <div class=\"info-box\"><h3>Paiement</h3>");
        html.append("        <div class=\"row\"><span>Date d'édition</span><strong>")
                .append(LocalDate.now().format(DATE_FORMAT)).append("</strong></div>");
        html.append("        <div class=\"row\"><span>Devise</span><strong>")
                .append(escape(p.getCurrency() != null ? p.getCurrency() : "TND")).append("</strong></div>");
        html.append("        <div class=\"row\"><span>Statut</span><strong>")
                .append(escape(p.getStatus() != null ? p.getStatus() : "—")).append("</strong></div></div>\n");
        html.append("    </div>\n");

        html.append("    <table>\n<tr><th>Désignation</th><th class=\"montant\">Montant</th></tr>\n");
        html.append("      <tr><td>Salaire de base</td><td class=\"montant\">").append(formatNum(p.getBaseSalary()))
                .append("</td></tr>\n");
        html.append("      <tr><td>Heures supplémentaires (").append(p.getOvertime())
                .append(" h)</td><td class=\"montant\">").append(formatNum(p.getOvertimeTotal()))
                .append("</td></tr>\n");
        html.append("      <tr><td>Primes</td><td class=\"montant\">").append(formatNum(p.getBonuses()))
                .append("</td></tr>\n");
        html.append("      <tr><td>Déductions (CNSS, IRPP, etc.)</td><td class=\"montant\">- ")
                .append(formatNum(p.getTotalDeductions())).append("</td></tr>\n");
        html.append("    </table>\n");

        html.append("    <div class=\"totaux\">\n");
        html.append("      <div class=\"total-row\"><span>Total brut</span><strong>").append(formatNum(p.getGross()))
                .append(" ").append(escape(p.getCurrency() != null ? p.getCurrency() : "TND"))
                .append("</strong></div>\n");
        html.append("      <div class=\"net\"><span>Net à payer</span><span>").append(formatNum(p.getNet())).append(" ")
                .append(escape(p.getCurrency() != null ? p.getCurrency() : "TND")).append("</span></div>\n");
        html.append("    </div>\n");

        html.append("    </div>\n");
        html.append(
                "    <div class=\"footer\"><p>Document généré électroniquement — Skilora Gestion Finance</p></div>\n");
        html.append("  </div>\n</body>\n</html>");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(html.toString());
        }
    }

    private static String formatNum(double value) {
        return String.format(Locale.FRANCE, "%.2f", value);
    }

    private static String escape(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
