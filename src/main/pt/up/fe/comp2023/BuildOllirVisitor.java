package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;


public class BuildOllirVisitor extends AJmmVisitor<String,String> {

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::lulu);
        addVisit("ImportDeclaration", this::visitImport);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("MethodDef", this::visitMethod);

    }

    private String lulu(JmmNode jmmNode, String s) {
    }

}
