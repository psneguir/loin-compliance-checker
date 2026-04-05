package com.loin.checker.app;

import com.loin.checker.api.LoinApiClient;
import com.loin.checker.ifc.IfcParser;
import com.loin.checker.model.*;
import com.loin.checker.report.HtmlReportGenerator;
import com.loin.checker.validation.ComplianceValidator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        CliParser cliParser = new CliParser();

        if (args.length == 0) {
            cliParser.printHelp();
            System.exit(0);
        }

        CommandLine cmd;
        try {
            cmd = cliParser.parse(args);
        } catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            cliParser.printHelp();
            System.exit(1);
            return;
        }

        if (cmd.hasOption(CliParser.OPT_HELP)) {
            cliParser.printHelp();
            System.exit(0);
        }

        try {
            if (cmd.hasOption(CliParser.OPT_SEARCH)) {
                runSearchMode(cmd);
            } else {
                runCheckMode(cmd, cliParser);
            }
        } catch (Exception e) {
            System.err.println("\n[ERROR] " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("  Caused by: " + e.getCause().getMessage());
            }
            System.exit(1);
        }
    }

    private static void runSearchMode(CommandLine cmd) {
        LoinApiClient client = new LoinApiClient();

        Integer lph = null;
        Integer awf = null;
        if (cmd.hasOption(CliParser.OPT_LPH)) {
            try { lph = Integer.parseInt(cmd.getOptionValue(CliParser.OPT_LPH)); }
            catch (NumberFormatException e) { System.err.println("Warning: invalid LPH value, ignoring"); }
        }
        if (cmd.hasOption(CliParser.OPT_AWF)) {
            try { awf = Integer.parseInt(cmd.getOptionValue(CliParser.OPT_AWF)); }
            catch (NumberFormatException e) { System.err.println("Warning: invalid AwF value, ignoring"); }
        }

        System.out.println("Searching LOIN specifications...");
        if (lph != null) System.out.println("  Filter LPH: " + lph);
        if (awf != null) System.out.println("  Filter AwF: " + awf);
        System.out.println();

        List<LoinSpec> loins = client.searchLoins(lph, awf, 0, 20);

        if (loins.isEmpty()) {
            System.out.println("No LOIN specifications found.");
            return;
        }

        System.out.printf("%-4s  %-40s  %s%n", "#", "GUID", "Name");
        System.out.println("-".repeat(100));
        for (int i = 0; i < loins.size(); i++) {
            LoinSpec l = loins.get(i);
            System.out.printf("%-4d  %-40s  %s%n",
                    i + 1,
                    l.getGuid() != null ? l.getGuid() : "-",
                    l.getName() != null ? l.getName() : "-");
        }
        System.out.println();
        System.out.println("Use --loin-guid <guid> with --ifc <file> to run a compliance check.");
    }

    private static void runCheckMode(CommandLine cmd, CliParser cliParser) throws Exception {
        if (!cmd.hasOption(CliParser.OPT_IFC) || !cmd.hasOption(CliParser.OPT_LOIN_GUID)) {
            System.err.println("Error: --ifc and --loin-guid are required for compliance checking.");
            System.err.println();
            cliParser.printHelp();
            System.exit(1);
        }

        String ifcPath = cmd.getOptionValue(CliParser.OPT_IFC);
        String guid = cmd.getOptionValue(CliParser.OPT_LOIN_GUID);
        String reportPath = cmd.getOptionValue(CliParser.OPT_REPORT, "reports/");

        // Step 1: Load LOIN
        System.out.println("Loading LOIN specification...");
        LoinApiClient apiClient = new LoinApiClient();
        LoinSpec loin = apiClient.getLoinByGuid(guid);
        System.out.println("  Name:  " + loin.getName());
        System.out.println("  GUID:  " + loin.getGuid());
        System.out.println("  LPH:   " + loin.getLph());
        System.out.println("  AwF:   " + loin.getAwf());
        System.out.println("  Object Types: " + loin.getObjectTypes().size());
        System.out.println();

        // Step 2: Parse IFC
        System.out.println("Parsing IFC model: " + ifcPath);
        IfcParser ifcParser = new IfcParser();
        List<IfcElement> elements = ifcParser.parse(ifcPath);
        System.out.println("  Found " + elements.size() + " relevant IFC elements.");
        System.out.println();

        // Step 3: Validate
        System.out.println("Running compliance validation...");
        ComplianceValidator validator = new ComplianceValidator();
        List<ComplianceResult> results = validator.validate(loin, elements);

        long covered = validator.countCovered(results);
        long uncovered = elements.size() - covered;
        double rate = validator.overallComplianceRate(results);
        long pass = validator.countByStatus(results, PropertyResult.Status.PASS);
        long missing = validator.countByStatus(results, PropertyResult.Status.MISSING);
        long incomplete = validator.countByStatus(results, PropertyResult.Status.INCOMPLETE);

        System.out.println("  Elements covered by LOIN: " + covered + " / " + elements.size());
        System.out.println("  Elements not covered:     " + uncovered);
        System.out.printf("  Overall compliance rate:  %.1f%%%n", rate);
        System.out.println("  Properties PASS:          " + pass);
        System.out.println("  Properties MISSING:       " + missing);
        System.out.println("  Properties INCOMPLETE:    " + incomplete);
        System.out.println();

        // Step 4: Generate report
        System.out.println("Generating HTML report...");
        HtmlReportGenerator reportGenerator = new HtmlReportGenerator();
        String generatedPath = reportGenerator.generateReport(loin, elements, results, ifcPath, reportPath);
        System.out.println("Report generated: " + generatedPath);
    }
}
