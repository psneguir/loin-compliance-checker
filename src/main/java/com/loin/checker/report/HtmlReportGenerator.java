package com.loin.checker.report;

import com.loin.checker.model.*;
import com.loin.checker.validation.ComplianceValidator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HtmlReportGenerator {

    private final ComplianceValidator validator = new ComplianceValidator();

    public String generateReport(LoinSpec loin, List<IfcElement> elements,
                                 List<ComplianceResult> results, String ifcFilePath,
                                 String outputPath) throws IOException {
        // Determine actual file path
        String filePath = resolveOutputPath(outputPath);
        File outFile = new File(filePath);
        if (outFile.getParentFile() != null) {
            outFile.getParentFile().mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outFile))) {
            writeHtml(writer, loin, elements, results, ifcFilePath);
        }
        return filePath;
    }

    private String resolveOutputPath(String outputPath) {
        if (outputPath == null || outputPath.isEmpty()) outputPath = "reports/";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        if (outputPath.endsWith("/") || outputPath.endsWith("\\") || new File(outputPath).isDirectory()) {
            String dir = outputPath.endsWith("/") || outputPath.endsWith("\\")
                    ? outputPath : outputPath + "/";
            return dir + "loin-report-" + timestamp + ".html";
        }
        return outputPath;
    }

    private void writeHtml(PrintWriter w, LoinSpec loin, List<IfcElement> elements,
                           List<ComplianceResult> results, String ifcFilePath) {
        long covered = validator.countCovered(results);
        long uncovered = elements.size() - covered;
        double overallRate = validator.overallComplianceRate(results);
        long passCount = validator.countByStatus(results, PropertyResult.Status.PASS);
        long missingCount = validator.countByStatus(results, PropertyResult.Status.MISSING);
        long incompleteCount = validator.countByStatus(results, PropertyResult.Status.INCOMPLETE);
        long totalProps = passCount + missingCount + incompleteCount;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        w.println("<!DOCTYPE html>");
        w.println("<html lang=\"en\">");
        w.println("<head>");
        w.println("<meta charset=\"UTF-8\">");
        w.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        w.println("<title>LOIN Compliance Report</title>");
        w.println("<style>");
        writeCss(w);
        w.println("</style>");
        w.println("</head>");
        w.println("<body>");

        // Header
        w.println("<div class=\"header\">");
        w.println("<h1>&#x1F4CB; LOIN Compliance Report</h1>");
        w.println("<p class=\"subtitle\">BIM Model Compliance Check against LOIN Specification</p>");
        w.println("</div>");

        w.println("<div class=\"container\">");

        // LOIN Info
        w.println("<div class=\"section\">");
        w.println("<h2>&#x1F4D0; LOIN Specification</h2>");
        w.println("<table class=\"info-table\">");
        writeInfoRow(w, "Name", loin.getName());
        writeInfoRow(w, "GUID", loin.getGuid());
        writeInfoRow(w, "LPH", loin.getLph());
        writeInfoRow(w, "AwF", loin.getAwf());
        writeInfoRow(w, "Description", loin.getDescription());
        w.println("</table>");
        w.println("</div>");

        // Model Info
        w.println("<div class=\"section\">");
        w.println("<h2>&#x1F3D7; IFC Model Information</h2>");
        w.println("<table class=\"info-table\">");
        writeInfoRow(w, "File", ifcFilePath);
        writeInfoRow(w, "Total Elements", String.valueOf(elements.size()));
        writeInfoRow(w, "Covered by LOIN", String.valueOf(covered));
        writeInfoRow(w, "Not Covered", String.valueOf(uncovered));
        w.println("</table>");
        w.println("</div>");

        // Summary Stats
        w.println("<div class=\"section\">");
        w.println("<h2>&#x1F4CA; Compliance Summary</h2>");
        w.println("<div class=\"stats-grid\">");
        writeStatCard(w, String.format("%.1f%%", overallRate), "Overall Compliance",
                overallRate >= 75 ? "stat-green" : overallRate >= 50 ? "stat-orange" : "stat-red");
        writeStatCard(w, String.valueOf(passCount), "PASS", "stat-green");
        writeStatCard(w, String.valueOf(missingCount), "MISSING", "stat-red");
        writeStatCard(w, String.valueOf(incompleteCount), "INCOMPLETE", "stat-orange");
        writeStatCard(w, String.valueOf(totalProps), "Total Properties Checked", "stat-blue");
        w.println("</div>");
        w.println("</div>");

        // Summary Table
        w.println("<div class=\"section\">");
        w.println("<h2>&#x1F4CB; Element Summary</h2>");
        w.println("<table class=\"data-table\">");
        w.println("<thead><tr><th>Element ID</th><th>IFC Class</th><th>Name</th>" +
                "<th>LOIN Object Type</th><th>Compliance</th><th>Status</th></tr></thead>");
        w.println("<tbody>");
        for (ComplianceResult result : results) {
            IfcElement el = result.getElement();
            if (result.isCoveredByLoin()) {
                String rateStr = String.format("%.1f%%", result.getComplianceRate());
                String rowClass = result.getComplianceRate() >= 75 ? "pass-row"
                        : result.getComplianceRate() >= 50 ? "incomplete-row" : "missing-row";
                w.printf("<tr class=\"%s\"><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td>" +
                                "<td><span class=\"badge badge-%s\">COVERED</span></td></tr>%n",
                        rowClass, esc(el.getId()), esc(el.getIfcClass()),
                        esc(el.getName()), esc(result.getMatchedObjectType().getName()),
                        rateStr, result.getComplianceRate() >= 75 ? "pass"
                                : result.getComplianceRate() >= 50 ? "incomplete" : "missing");
            } else {
                w.printf("<tr class=\"uncovered-row\"><td>%s</td><td>%s</td><td>%s</td>" +
                                "<td>-</td><td>-</td><td><span class=\"badge badge-uncovered\">NOT COVERED</span></td></tr>%n",
                        esc(el.getId()), esc(el.getIfcClass()), esc(el.getName()));
            }
        }
        w.println("</tbody></table>");
        w.println("</div>");

        // Detailed Results per Element
        List<ComplianceResult> coveredResults = results.stream()
                .filter(ComplianceResult::isCoveredByLoin).toList();
        if (!coveredResults.isEmpty()) {
            w.println("<div class=\"section\">");
            w.println("<h2>&#x1F50D; Detailed Compliance Results</h2>");
            for (ComplianceResult result : coveredResults) {
                writeElementDetail(w, result);
            }
            w.println("</div>");
        }

        // Uncovered Elements
        List<ComplianceResult> uncoveredResults = results.stream()
                .filter(r -> !r.isCoveredByLoin()).toList();
        if (!uncoveredResults.isEmpty()) {
            w.println("<div class=\"section\">");
            w.println("<h2>&#x26A0; Elements Not Covered by LOIN</h2>");
            w.println("<p>The following elements have no matching LOIN object type and are excluded from compliance checks:</p>");
            w.println("<ul class=\"uncovered-list\">");
            for (ComplianceResult result : uncoveredResults) {
                IfcElement el = result.getElement();
                w.printf("<li><strong>%s</strong> (%s) - %s</li>%n",
                        esc(el.getId()), esc(el.getIfcClass()), esc(el.getName()));
            }
            w.println("</ul>");
            w.println("</div>");
        }

        // Footer
        w.println("<div class=\"footer\">");
        w.printf("<p>Generated by LOIN Compliance Checker &bull; %s</p>%n", timestamp);
        w.println("</div>");

        w.println("</div>"); // container
        w.println("</body></html>");
    }

    private void writeElementDetail(PrintWriter w, ComplianceResult result) {
        IfcElement el = result.getElement();
        String rateStr = String.format("%.1f%%", result.getComplianceRate());
        String cardClass = result.getComplianceRate() >= 75 ? "card-pass"
                : result.getComplianceRate() >= 50 ? "card-incomplete" : "card-missing";

        w.printf("<div class=\"element-card %s\">%n", cardClass);
        w.printf("<div class=\"element-header\"><span class=\"element-id\">%s</span>" +
                        " <span class=\"element-class\">%s</span> &mdash; %s" +
                        " <span class=\"compliance-badge\">%s</span></div>%n",
                esc(el.getId()), esc(el.getIfcClass()), esc(el.getName()), rateStr);

        if (!result.getPropertyResults().isEmpty()) {
            w.println("<table class=\"prop-table\">");
            w.println("<thead><tr><th>Property Set</th><th>Property</th><th>Value</th><th>Status</th></tr></thead>");
            w.println("<tbody>");
            for (PropertyResult pr : result.getPropertyResults()) {
                String statusClass = switch (pr.getStatus()) {
                    case PASS -> "status-pass";
                    case MISSING -> "status-missing";
                    case INCOMPLETE -> "status-incomplete";
                };
                String statusLabel = pr.getStatus().name();
                w.printf("<tr><td>%s</td><td>%s</td><td>%s</td>" +
                                "<td><span class=\"status-badge %s\">%s</span></td></tr>%n",
                        esc(pr.getPropertySetName()), esc(pr.getPropertyName()),
                        esc(pr.getValue()), statusClass, statusLabel);
            }
            w.println("</tbody></table>");
        }
        w.println("</div>");
    }

    private void writeInfoRow(PrintWriter w, String label, String value) {
        w.printf("<tr><td class=\"label\">%s</td><td>%s</td></tr>%n",
                esc(label), esc(value));
    }

    private void writeStatCard(PrintWriter w, String value, String label, String cssClass) {
        w.printf("<div class=\"stat-card %s\"><div class=\"stat-value\">%s</div>" +
                "<div class=\"stat-label\">%s</div></div>%n", cssClass, esc(value), esc(label));
    }

    private String esc(String s) {
        if (s == null) return "<em>N/A</em>";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void writeCss(PrintWriter w) {
        w.println("""
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body { font-family: 'Segoe UI', Arial, sans-serif; background: #f0f2f5; color: #333; }
                .header { background: linear-gradient(135deg, #1a3a5c 0%, #2d6a9f 100%);
                          color: white; padding: 30px 40px; }
                .header h1 { font-size: 2em; margin-bottom: 8px; }
                .subtitle { opacity: 0.85; font-size: 1.1em; }
                .container { max-width: 1200px; margin: 30px auto; padding: 0 20px; }
                .section { background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.08);
                           padding: 24px; margin-bottom: 24px; }
                .section h2 { color: #1a3a5c; margin-bottom: 16px; font-size: 1.3em;
                              border-bottom: 2px solid #e8ecf0; padding-bottom: 8px; }
                .info-table { width: 100%; border-collapse: collapse; }
                .info-table td { padding: 8px 12px; border-bottom: 1px solid #f0f2f5; }
                .info-table td.label { font-weight: 600; color: #555; width: 180px; }
                .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 16px; }
                .stat-card { border-radius: 8px; padding: 20px; text-align: center; }
                .stat-value { font-size: 2em; font-weight: 700; }
                .stat-label { font-size: 0.85em; margin-top: 4px; opacity: 0.85; }
                .stat-green { background: #e8f5e9; color: #2e7d32; }
                .stat-red { background: #ffebee; color: #c62828; }
                .stat-orange { background: #fff3e0; color: #e65100; }
                .stat-blue { background: #e3f2fd; color: #1565c0; }
                .data-table { width: 100%; border-collapse: collapse; font-size: 0.9em; }
                .data-table th { background: #1a3a5c; color: white; padding: 10px 12px; text-align: left; }
                .data-table td { padding: 9px 12px; border-bottom: 1px solid #f0f2f5; }
                .data-table tbody tr:hover { background: #f8f9fa; }
                .pass-row { }
                .incomplete-row { background: #fffde7; }
                .missing-row { background: #fff5f5; }
                .uncovered-row { background: #f5f5f5; color: #999; }
                .badge { border-radius: 12px; padding: 3px 10px; font-size: 0.8em; font-weight: 600; }
                .badge-pass { background: #e8f5e9; color: #2e7d32; }
                .badge-incomplete { background: #fff3e0; color: #e65100; }
                .badge-missing { background: #ffebee; color: #c62828; }
                .badge-uncovered { background: #eeeeee; color: #757575; }
                .element-card { border-radius: 8px; border-left: 5px solid #ccc;
                                padding: 16px; margin-bottom: 16px; background: #fafafa; }
                .card-pass { border-left-color: #43a047; }
                .card-incomplete { border-left-color: #fb8c00; }
                .card-missing { border-left-color: #e53935; }
                .element-header { margin-bottom: 12px; font-size: 1em; }
                .element-id { font-weight: 700; color: #1a3a5c; }
                .element-class { background: #e3f2fd; color: #1565c0; border-radius: 4px;
                                 padding: 2px 7px; font-size: 0.85em; font-family: monospace; }
                .compliance-badge { float: right; font-weight: 700; font-size: 1.1em; }
                .prop-table { width: 100%; border-collapse: collapse; font-size: 0.88em; }
                .prop-table th { background: #e8ecf0; color: #444; padding: 7px 10px; text-align: left; }
                .prop-table td { padding: 6px 10px; border-bottom: 1px solid #eee; }
                .status-badge { border-radius: 10px; padding: 2px 9px; font-size: 0.8em; font-weight: 600; }
                .status-pass { background: #e8f5e9; color: #2e7d32; }
                .status-missing { background: #ffebee; color: #c62828; }
                .status-incomplete { background: #fff3e0; color: #e65100; }
                .uncovered-list { padding-left: 24px; }
                .uncovered-list li { margin: 6px 0; }
                .footer { text-align: center; color: #999; padding: 20px; font-size: 0.85em; }
                """);
    }
}
