package pt.up.fe.comp2023.ollir;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.analysis.MySymbolTable;


public class Optimizer implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {

        BuildOllirVisitor visitor = new BuildOllirVisitor((MySymbolTable) jmmSemanticsResult.getSymbolTable());
        //BuildOllirVisitor visitor = new buildVisitor();
        visitor.visit(jmmSemanticsResult.getRootNode());

        String ollirCode = visitor.getCode();

        return new OllirResult(jmmSemanticsResult,ollirCode,jmmSemanticsResult.getReports());


    }

}
