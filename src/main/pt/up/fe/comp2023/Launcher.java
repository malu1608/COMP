package pt.up.fe.comp2023;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.analysis.Analyser;
import pt.up.fe.comp2023.jmm.jasmin.MyJasmin;
import pt.up.fe.comp2023.ollir.Optimizer;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        // Setups console logging and other things
        SpecsSystem.programStandardInit();

        // Parse arguments as a map with predefined options
        var config = parseArgs(args);

        // Get input file
        File inputFile = new File(config.get("inputFile"));

        // Check if file exists
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + inputFile + "'.");
        }

        // Read contents of input file
        String code = SpecsIo.read(inputFile);

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        code = SpecsIo.read("test/pt/up/fe/comp/cpf/2_semantic_analysis/lookup/VarLookup_Field.jmm");

        // Parse stage
        JmmParserResult parserResult = parser.parse(code, config);

        System.out.println(parserResult.getRootNode().toTree());
        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

        //analysis stage
        var analyser = new Analyser();

        //var semanticsResult = analyser.semanticAnalysis(parserResult);
        //TestUtils.noErrors(semanticsResult);
        System.out.println("Analysis:\n---------\n" + analyser.semanticAnalysis(parserResult).getSymbolTable().print());

        //instantiate Optimizer
        var optimizer = new Optimizer();
        OllirResult ollirResult = optimizer.toOllir(analyser.semanticAnalysis(parserResult));
        String ollirCode = ollirResult.getOllirCode();
        System.out.println("Ollir code:\n---------\n" + ollirCode);

        //Jasmin
        MyJasmin myJasmin = new MyJasmin();
        JasminResult jasminResult = myJasmin.toJasmin(ollirResult);
        String jasminCode = jasminResult.getJasminCode();
        System.out.println("Jasmin code:\n---------\n" + jasminCode);

        // ... add remaining stages

    }

    private static Map<String, String> parseArgs(String[] args) {
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // Check if there is at least one argument
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        return config;
    }

}
