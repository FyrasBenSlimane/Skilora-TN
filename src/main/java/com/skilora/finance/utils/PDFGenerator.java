package com.skilora.finance.utils;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;

/**
 * PDF Generator for Finance Module
 * Generates comprehensive employee finance reports
 */
public class PDFGenerator {

    /**
     * Generate a comprehensive PDF report for an employee
     */
    public static File generateEmployeeReport(
            int employeeId,
            String employeeName,
            String contractInfo,
            String bankInfo,
            String bonusInfo,
            String payslipInfo,
            Stage ownerStage) {
        // Create file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Employee Finance Report");
        fileChooser.setInitialFileName(
                "Finance_Report_" + employeeName.replace(" ", "_") + "_" + LocalDate.now() + ".html");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("HTML Files", "*.html"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File file = fileChooser.showSaveDialog(ownerStage);

        if (file != null) {
            try {
                generateHTMLReport(file, employeeId, employeeName, contractInfo, bankInfo, bonusInfo, payslipInfo);
                return file;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }

    private static void generateHTMLReport(
            File file,
            int employeeId,
            String employeeName,
            String contractInfo,
            String bankInfo,
            String bonusInfo,
            String payslipInfo) throws IOException {

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("  <meta charset='UTF-8'>\n");
        html.append("  <title>Finance Report - ").append(employeeName).append("</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }\n");
        html.append(
                "    .container { max-width: 900px; margin: 0 auto; background: white; padding: 40px; box-shadow: 0 0 20px rgba(0,0,0,0.1); }\n");
        html.append(
                "    .header { text-align: center; border-bottom: 3px solid #6366f1; padding-bottom: 20px; margin-bottom: 30px; }\n");
        html.append("    .header h1 { color: #6366f1; margin: 0; font-size: 32px; }\n");
        html.append("    .header p { color: #666; margin: 10px 0 0 0; }\n");
        html.append("    .section { margin-bottom: 30px; }\n");
        html.append(
                "    .section h2 { color: #333; border-left: 4px solid #6366f1; padding-left: 12px; margin-bottom: 15px; }\n");
        html.append("    .info-row { display: flex; padding: 10px; border-bottom: 1px solid #eee; }\n");
        html.append("    .info-row:nth-child(even) { background: #f9f9f9; }\n");
        html.append("    .info-label { font-weight: bold; width: 200px; color: #555; }\n");
        html.append("    .info-value { flex: 1; color: #333; }\n");
        html.append(
                "    .footer { text-align: center; margin-top: 40px; padding-top: 20px; border-top: 2px solid #eee; color: #999; font-size: 12px; }\n");
        html.append(
                "    .badge { display: inline-block; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: bold; }\n");
        html.append("    .badge-success { background: #10b981; color: white; }\n");
        html.append("    .badge-warning { background: #f59e0b; color: white; }\n");
        html.append("    .badge-info { background: #3b82f6; color: white; }\n");
        html.append("    @media print { body { margin: 0; } .container { box-shadow: none; } }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <div class='container'>\n");

        // Header
        html.append("    <div class='header'>\n");
        html.append("      <h1>💰 FINANCE REPORT</h1>\n");
        html.append("      <p>Employee: <strong>").append(employeeName).append("</strong> (ID: #").append(employeeId)
                .append(")</p>\n");
        html.append("      <p>Generated on: ").append(LocalDate.now()).append("</p>\n");
        html.append("    </div>\n");

        // Contract Section
        if (contractInfo != null && !contractInfo.isEmpty()) {
            html.append("    <div class='section'>\n");
            html.append("      <h2>📋 Employment Contract</h2>\n");
            html.append("      ").append(contractInfo).append("\n");
            html.append("    </div>\n");
        }

        // Bank Account Section
        if (bankInfo != null && !bankInfo.isEmpty()) {
            html.append("    <div class='section'>\n");
            html.append("      <h2>🏦 Bank Account</h2>\n");
            html.append("      ").append(bankInfo).append("\n");
            html.append("    </div>\n");
        }

        // Bonuses Section
        if (bonusInfo != null && !bonusInfo.isEmpty()) {
            html.append("    <div class='section'>\n");
            html.append("      <h2>🎁 Bonuses</h2>\n");
            html.append("      ").append(bonusInfo).append("\n");
            html.append("    </div>\n");
        }

        // Payslips Section
        if (payslipInfo != null && !payslipInfo.isEmpty()) {
            html.append("    <div class='section'>\n");
            html.append("      <h2>📄 Recent Payslips</h2>\n");
            html.append("      ").append(payslipInfo).append("\n");
            html.append("    </div>\n");
        }

        // Footer
        html.append("    <div class='footer'>\n");
        html.append("      <p>© 2026 Skilora Finance Management System</p>\n");
        html.append("      <p>This document is confidential and for internal use only.</p>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");
        html.append("</body>\n");
        html.append("</html>");

        // Write to file
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(html.toString());
        }
    }

    /**
     * Generate a single Payslip PDF (HTML)
     */
    public static File generatePayslipPDF(
            com.skilora.finance.model.PayslipRow payslip,
            String employeeName,
            Stage ownerStage) {

        // Create file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Payslip PDF");
        fileChooser.setInitialFileName(
                "Payslip_" + employeeName.replace(" ", "_") + "_" + payslip.getMonth() + "-" + payslip.getYear()
                        + ".html");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("HTML Files", "*.html"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File file = fileChooser.showSaveDialog(ownerStage);

        if (file != null) {
            try {
                generatePayslipHTML(file, payslip, employeeName);
                return file;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null; // Cancelled
    }

    private static void generatePayslipHTML(File file, com.skilora.finance.model.PayslipRow p, String empName)
            throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Payslip</title>");
        html.append("<style>");
        html.append(
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #eef2f5; padding: 40px; }");
        html.append(
                ".payslip { background: white; max-width: 800px; margin: 0 auto; padding: 40px; border-radius: 8px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); }");
        html.append(
                ".header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #3b82f6; padding-bottom: 20px; margin-bottom: 30px; }");
        html.append(".company h1 { margin: 0; color: #1e3a8a; font-size: 28px; }");
        html.append(".company p { margin: 5px 0 0; color: #64748b; }");
        html.append(".title { text-align: right; }");
        html.append(".title h2 { margin: 0; color: #3b82f6; text-transform: uppercase; letter-spacing: 1px; }");
        html.append(".title p { margin: 5px 0 0; font-weight: bold; color: #1e293b; }");
        html.append(".info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 30px; margin-bottom: 30px; }");
        html.append(".info-box { background: #f8fafc; padding: 20px; border-radius: 6px; border: 1px solid #e2e8f0; }");
        html.append(".info-box h3 { margin: 0 0 15px 0; font-size: 14px; text-transform: uppercase; color: #64748b; }");
        html.append(".row { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 14px; }");
        html.append(".row strong { color: #1e293b; }");
        html.append(".table-container { margin-bottom: 30px; }");
        html.append("table { width: 100%; border-collapse: collapse; }");
        html.append(
                "th { text-align: left; padding: 12px; background: #f1f5f9; color: #475569; font-weight: 600; font-size: 13px; text-transform: uppercase; }");
        html.append("td { padding: 12px; border-bottom: 1px solid #e2e8f0; color: #334155; }");
        html.append(".amount { text-align: right; font-family: 'Consolas', monospace; font-weight: 600; }");
        html.append(".totals { background: #eff6ff; padding: 20px; border-radius: 6px; margin-bottom: 40px; }");
        html.append(
                ".total-row { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 15px; }");
        html.append(
                ".net-pay { display: flex; justify-content: space-between; margin-top: 15px; padding-top: 15px; border-top: 2px solid #bfdbfe; font-size: 20px; font-weight: bold; color: #1e40af; }");
        html.append(".footer { text-align: center; color: #94a3b8; font-size: 12px; margin-top: 50px; }");
        html.append("</style></head><body>");

        html.append("<div class='payslip'>");

        // Header
        html.append("<div class='header'>");
        html.append("<div class='company'><h1>SKILORA</h1><p>Tunisia Branch</p></div>");
        html.append("<div class='title'><h2>Payslip</h2><p>" + p.getMonth() + "/" + p.getYear() + "</p></div>");
        html.append("</div>");

        // Employee Info
        html.append("<div class='info-grid'>");
        html.append("<div class='info-box'><h3>Employee Details</h3>");
        html.append("<div class='row'><span>Name:</span><strong>" + empName + "</strong></div>");
        html.append("<div class='row'><span>ID:</span><strong>" + p.getUserId() + "</strong></div>");
        html.append("</div>");
        html.append("<div class='info-box'><h3>Pay Details</h3>");
        html.append("<div class='row'><span>Pay Date:</span><strong>" + LocalDate.now() + "</strong></div>");
        html.append("<div class='row'><span>Currency:</span><strong>" + p.getCurrency() + "</strong></div>");
        html.append("<div class='row'><span>Status:</span><strong>" + p.getStatus() + "</strong></div>");
        html.append("</div>");
        html.append("</div>");

        html.append("<table>");
        html.append("<tr><th>Description</th><th class='amount'>Amount</th></tr>");
        html.append("<tr><td>Base Salary</td><td class='amount'>" + String.format("%.2f", p.getBaseSalary())
                + "</td></tr>");
        html.append("<tr><td>Overtime (" + p.getOvertime() + "h)</td><td class='amount'>"
                + String.format("%.2f", p.getOvertimeTotal()) + "</td></tr>");
        html.append("<tr><td>Bonuses</td><td class='amount'>" + String.format("%.2f", p.getBonuses()) + "</td></tr>");
        html.append("<tr style='color:#ef4444'><td>Deductions (inc. Tax)</td><td class='amount'>-"
                + String.format("%.2f", p.getTotalDeductions()) + "</td></tr>");
        html.append("</table>");

        // Totals
        html.append("<div class='totals'>");
        html.append("<div class='total-row'><span>Total Earnings</span><strong>" + String.format("%.2f", p.getGross())
                + "</strong></div>");
        html.append("<div class='net-pay'><span>NET PAYABLE</span><span>" + String.format("%.2f", p.getNet()) + " "
                + p.getCurrency() + "</span></div>");
        html.append("</div>");

        html.append(
                "<div class='footer'><p>Payment initiated electronically. This payslip is computer generated.</p></div>");
        html.append("</div></body></html>");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(html.toString());
        }
    }
}
