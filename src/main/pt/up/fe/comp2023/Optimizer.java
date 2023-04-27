package pt.up.fe.comp2023;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.analysis.MySymbolTable;
import pt.up.fe.comp2023.ollir.BuildOllirVisitor;

public class Optimizer implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        String ollir = ""; //tem codigo ollir
        BuildOllirVisitor visitor = new BuildOllirVisitor((MySymbolTable) jmmSemanticsResult.getSymbolTable());
        visitor.visit(jmmSemanticsResult.getRootNode());
        return new OllirResult(jmmSemanticsResult,ollir,jmmSemanticsResult.getReports());
    }

}
