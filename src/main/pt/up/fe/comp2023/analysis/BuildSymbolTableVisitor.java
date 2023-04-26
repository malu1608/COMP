package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class BuildSymbolTableVisitor extends AJmmVisitor<MySymbolTable, List<Report>> {

    @Override
    public void buildVisitor() {
    }

    public BuildSymbolTableVisitor() {
        addVisit("Program", this::visitProgram);
        addVisit("ImportDeclaration", this::visitImport);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("MethodDef", this::visitMethod);
    }

    private void visitNode(JmmNode jmmNode, MySymbolTable table, List<Report> reports) {
        reports.addAll(visit(jmmNode, table));
    }

    private List<Report> visitProgram(JmmNode jmmNode, MySymbolTable table) {
        List<Report> reports = new ArrayList<>();

        for (JmmNode child: jmmNode.getChildren()) {
            visitNode(child, table, reports);
        }

        return reports;
    }

    private List<Report> visitImport(JmmNode jmmNode, MySymbolTable table) {
        List<Report> reports = new ArrayList<>();
        ArrayList<String> importPath = (ArrayList<String>) jmmNode.getObject("path");

        String imports = String.join(".",importPath);
        table.addImport(imports);

        return reports;
    }

    private List<Report> visitClassDeclaration(JmmNode jmmNode, MySymbolTable table) {
        List<Report> reports = new ArrayList<>();

        table.setClassName(jmmNode.get("classname"));

        if (jmmNode.getAttributes().contains("name")) {
            table.setSuper(jmmNode.get("name"));
        }

        for (JmmNode child: jmmNode.getChildren()) {
            if (child.getKind().equals("VarDecl")) {
                String name = child.get("name");
                boolean isArray = child.getJmmChild(0).get("isArray").equals("true");
                Type type = new Type(child.getJmmChild(0).get("name"), isArray);
                Symbol symbol = new Symbol(type, name);
                table.addField(symbol);
            } else {
                visitNode(child, table, reports);
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
            if (child.getKind().equals("VarDecl")) {
                String name = child.get("name");
                isArray = child.getAttributes().contains("isArray") && child.get("isArray").equals("true");
                Type type = new Type(child.getJmmChild(0).get("name"), isArray);
                Symbol symbol = new Symbol(type, name);
                variables.add(symbol);

            } else if (child.getKind().equals("Params")) {
                for (JmmNode grandchild : child.getChildren()) {
                    if (grandchild.getKind().equals("Param")) {
                        String name = grandchild.get("name");
                        JmmNode typeChild = grandchild.getJmmChild(0);
                        isArray = typeChild.getAttributes().contains("isArray") && typeChild.get("isArray").equals("true");
                        Type type = new Type(grandchild.getJmmChild(0).get("name"), isArray);
                        Symbol symbol = new Symbol(type, name);
                        parameters.add(symbol);
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

        table.addMethod(methodName, returnType, parameters, variables);
        jmmNode.put("signature", methodSignature);

        return reports;
    }


}
