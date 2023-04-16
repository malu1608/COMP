package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;


import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class BuildOllirVisitor extends AJmmVisitor<String,String> {
    private final SymbolTable table;
    private String ollirCode;

    public BuildOllirVisitor(SymbolTable new_table) {
        this.table = new_table;
        this.ollirCode = "";
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program",this::ProgramVisit);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("MainDeclaration", this::visitMainDeclaration);
        addVisit("MethodDef", this::visitMethod);
        setDefaultVisit(this::setDefault);
    }

    private String visitMainDeclaration(JmmNode jmmNode, String s) {
        return "";
    }

    private String setDefault(JmmNode jmmNode, String s) {
        StringBuilder visitStr = new StringBuilder();
        for (JmmNode child : jmmNode.getChildren()) {
            visitStr.append(visit(child));
        }
        return visitStr.toString();
    }

    private String identifierVisit(JmmNode jmmNode, String s) {
        return "";
    }

    private String visitMethod(JmmNode jmmNode, String s) {
        return "";
    }

    private String visitClassDeclaration(JmmNode jmmNode, String s) {
        String class_name = table.getClassName();
        String super_class = table.getSuper();

        //se string nao nula adiciona extends e o nome da classe
        ollirCode += "class "+ class_name + (super_class != null ? " extends " + super_class : "") + " {\n";

        for (JmmNode child : jmmNode.getChildren()) {
            visit(child);
        }

        ollirCode += "}\n";

        return "";
    }


    private String ProgramVisit(JmmNode jmmNode, String s) {
        //List<Report> reports = new ArrayList<>();
        if (visit(jmmNode.getChildren().get(0)) == null) return "erro";
        else {
            for (String importString : table.getImports()) {
                ollirCode += "import " +importString + ";\n";
            }

            for(JmmNode child : jmmNode.getChildren()){
                visit(child);
            }
            return "";
        }
    }

    public String getCode(){
        return ollirCode;
    }
}
