package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BuildSymbolTableVisitor extends AJmmVisitor<MySymbolTable, List<Report>> {

    @Override
    public void buildVisitor() {
    }

    public BuildSymbolTableVisitor() {
        addVisit("Program", this::visitProgram);
        addVisit("ImportDeclaration", this::visitImport);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("MethodDef", this::visitMethod);
        addVisit("MainMethodDef", this::visitMethod);
    }

    private void visitAndReduce(JmmNode jmmNode, MySymbolTable table, List<Report> reports) {
        reports.addAll(visit(jmmNode, table));
    }

    private List<Report> visitProgram(JmmNode jmmNode, MySymbolTable table) {
        List<Report> reports = new ArrayList<>();

        for (JmmNode child: jmmNode.getChildren()) {
            visitAndReduce(child, table, reports);
        }

        return reports;
    }

    private List<Report> visitImport(JmmNode jmmNode, MySymbolTable table) {
        List<Report> reports = new ArrayList<>();
        var importString = jmmNode.get("path");

        var importStr = jmmNode.getChildren().stream()
                .map(id -> id.get("path"))
                .collect(Collectors.joining("."));

        if (importStr.equals("")) {
            table.addImport(importString);
        } else {
            table.addImport(importString + "." + importStr);
        }

        return reports;
    }

    private List<Report> visitClassDeclaration(JmmNode jmmNode, MySymbolTable table) {
        List<Report> reports = new ArrayList<>();

        table.setClassName(jmmNode.get("classname"));
        if (jmmNode.getAttributes().contains("name")) {
            table.setSuper(jmmNode.get("name"));
        }

        for (JmmNode child: jmmNode.getChildren()) {
            if (child.getKind().equals("VarDeclaration")) {
                String name = child.get("name");
                boolean isArray = child.getAttributes().contains("isArray") && child.get("isArray").equals("true");
                Type type = new Type(child.getJmmChild(0).get("name"), isArray);
                Symbol symbol = new Symbol(type, name);
                table.addField(symbol);
            } else {
                visitAndReduce(child, table, reports);
            }
        }

        return reports;
    }

    private List<Report> visitMethod(JmmNode jmmNode, MySymbolTable table) {
        List<Report> reports = new ArrayList<>();

        String methodName = jmmNode.get("name");
        boolean isArray = jmmNode.getJmmChild(0).getAttributes().contains("isArray") && jmmNode.getJmmChild(0).get("isArray").equals("true");
        Type returnType = new Type(jmmNode.getJmmChild(0).get("name"), isArray);
        List<Symbol> parameters = new ArrayList<>();
        List<Symbol> variables = new ArrayList<>();
        for (JmmNode child: jmmNode.getChildren()) {
            System.out.println(child.getKind());
            if (child.getKind().equals("parameter")) {
                String name = child.get("name");
                isArray = child.getAttributes().contains("isArray") && child.get("isArray").equals("true");
                Type type = new Type(child.getJmmChild(0).get("name"), isArray);
                Symbol symbol = new Symbol(type, name);
                parameters.add(symbol);

            } else if (child.getKind().equals("parameters")) {
                for (JmmNode grandchild: child.getChildren()) {
                    if (grandchild.getKind().equals("VarDeclaration")) {
                        String name = grandchild.get("name");
                        isArray = grandchild.getAttributes().contains("isArray") && grandchild.get("isArray").equals("true");
                        Type type = new Type(grandchild.getJmmChild(0).get("name"), isArray);
                        Symbol symbol = new Symbol(type, name);
                        variables.add(symbol);
                    }
                }
            }
        }

        StringBuilder methodSignatureBuilder = new StringBuilder();
        methodSignatureBuilder.append(methodName);
        for (Symbol parameter: parameters) {
            methodSignatureBuilder.append("#");
            methodSignatureBuilder.append(parameter.getType().print());
        }
        String methodSignature = methodSignatureBuilder.toString();

        if (table.getMethods().contains(methodSignature)) {
            String symbolName;
            if (methodSignature.contains("#")) {
                symbolName = methodSignature.replaceFirst("#", "(")
                        .replaceAll("##UNKNOWN", ",?")
                        .replaceAll("\\(#UNKNOWN", "(?")
                        .replaceAll("#", ",")
                        .concat(")");
            } else symbolName = methodSignature.concat("()");
        } else {
            table.addMethod(methodSignature, returnType, parameters, variables);
        }

        jmmNode.put("signature", methodSignature);

        return reports;
    }
}
