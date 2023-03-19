package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsCollections;

import java.util.ArrayList;
import java.util.List;

public class Analyser implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        MySymbolTable symbolTable = new MySymbolTable();

        List<Report> symbolTableReports = new BuildSymbolTableVisitor().visit(parserResult.getRootNode(), symbolTable);
        // List<Report> analysisReports = new AnalyseVisitor().visit(parserResult.getRootNode(), symbolTable);
        List<Report> reports =  new ArrayList<>();// SpecsCollections.concat(symbolTableReports, analysisReports);

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}
