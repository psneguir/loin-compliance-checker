package com.loin.checker.app;

import org.apache.commons.cli.*;

public class CliParser {

    public static final String OPT_IFC = "ifc";
    public static final String OPT_LOIN_GUID = "loin-guid";
    public static final String OPT_REPORT = "report";
    public static final String OPT_SEARCH = "search";
    public static final String OPT_LPH = "lph";
    public static final String OPT_AWF = "awf";
    public static final String OPT_HELP = "help";

    private final Options options;

    public CliParser() {
        options = new Options();
        options.addOption(Option.builder().longOpt(OPT_IFC)
                .hasArg().argName("path")
                .desc("Path to the IFC file to check")
                .build());
        options.addOption(Option.builder().longOpt(OPT_LOIN_GUID)
                .hasArg().argName("guid")
                .desc("GUID of the LOIN specification to use")
                .build());
        options.addOption(Option.builder().longOpt(OPT_REPORT)
                .hasArg().argName("path")
                .desc("Output path for the HTML report (default: reports/)")
                .build());
        options.addOption(Option.builder().longOpt(OPT_SEARCH)
                .desc("Search mode: list available LOIN specifications")
                .build());
        options.addOption(Option.builder().longOpt(OPT_LPH)
                .hasArg().argName("n")
                .desc("Filter by LPH value (used in search mode)")
                .build());
        options.addOption(Option.builder().longOpt(OPT_AWF)
                .hasArg().argName("n")
                .desc("Filter by AwF value (used in search mode)")
                .build());
        options.addOption(Option.builder()
                .longOpt(OPT_HELP).option("h")
                .desc("Show this help message")
                .build());
    }

    public CommandLine parse(String[] args) throws ParseException {
        DefaultParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    public void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);
        formatter.printHelp(
                "java -jar loin-checker.jar",
                "\nLOIN Compliance Checker for BIM/IFC Models\n\n",
                options,
                "\nExamples:\n" +
                        "  java -jar loin-checker.jar --ifc model.ifc --loin-guid 608e3ec2-ce6a-4f0f-8d96-2830e88a1e2e\n" +
                        "  java -jar loin-checker.jar --search --lph 1 --awf 100\n" +
                        "  java -jar loin-checker.jar --ifc model.ifc --loin-guid <guid> --report reports/\n",
                true
        );
    }

    public Options getOptions() {
        return options;
    }
}
