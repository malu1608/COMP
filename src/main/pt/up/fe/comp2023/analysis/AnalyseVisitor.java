package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsCollections;

import java.util.*;
public class AnalyseVisitor extends PostorderJmmVisitor<SymbolTable, List<Report>>{
    static final List<String> TYPES = Arrays.asList("int", "boolean", "void");
    static final List<String> ARITHMETIC_OPS = Arrays.asList("ADD", "SUB", "MUL", "DIV");
    static final List<String> COMPARISON_OP = List.of("LT");
    static final List<String> LOGICAL_OP = List.of("AND");

    public AnalyseVisitor() {
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("Type", this::visitType);
        addVisit("ReturnStatement", this::visitReturn);
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("Assignment", this::visitAssignment);
        addVisit("IfElseStatement", this::visitIfElse);
        addVisit("WhileStatement", this::visitWhile);
        addVisit("BinaryOp", this::visitBinaryOp);

    }

    private List<Report> visitClassDeclaration(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        if (!jmmNode.getAttributes().contains("extends"))
            return reports;

        String extendsName = jmmNode.get("extends");

        if (jmmNode.get("classname").equals(extendsName)) {
            reports.add(UtilsReport.cyclicInheritance(jmmNode, extendsName));
            return reports;
        }

        boolean foundImport = symbolTable
                .getImports()
                .stream()
                .anyMatch(imp -> imp.substring(imp.lastIndexOf(".") + 1).equals(extendsName));

        if (foundImport)
            return reports;

        reports.add(UtilsReport.cannotFindSymbolReport(jmmNode, extendsName));

        return reports;
    }

    private List<Report> visitType(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type objType = UtilsAnalyser.getType(jmmNode);

        if (TYPES.contains(objType.getName()))
            return reports;

        return checkTypeImport(jmmNode, symbolTable);
    }

    private List<Report> visitReturn(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        String methodSignature = jmmNode.getJmmParent().getJmmParent().get("signature");
        Type returnType = symbolTable.getReturnType(methodSignature);

        if (returnType.getName().equals("void"))
            return reports;

        if (jmmNode.getChildren().isEmpty()) {
            reports.add(UtilsReport.returnTypeMismatchReport(jmmNode, returnType.getName(), "void"));
            return reports;
        }

        Type exprType = UtilsAnalyser.getType(jmmNode.getJmmChild(0));

        if (!returnType.equals(exprType))
            reports.add(UtilsReport.returnTypeMismatchReport(jmmNode, returnType.getName(), exprType.getName()));

        return reports;
    }

    private List<Report> visitMethodCall(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        if (!jmmNode.getJmmChild(0).getAttributes().contains("type")) {
            if (jmmNode.getJmmChild(0).get("name").equals(symbolTable.getClassName())) {
                String methodSignature = inferMethodSignature(jmmNode);
                if (symbolTable.getMethods().contains(methodSignature)) {
                    String symbolName = UtilsAnalyser.getMethodSymbolName(methodSignature);
                    reports.add(UtilsReport.NonStaticInStaticContext(jmmNode, symbolName));
                } else if (symbolTable.getSuper() == null) {
                    String symbolName = UtilsAnalyser.getMethodSymbolName(methodSignature);
                    reports.add(UtilsReport.cannotFindSymbolReport(jmmNode, symbolName));
                }
            }
            putUnknownType(jmmNode);
            return reports;
        }

        Type operand = UtilsAnalyser.getType(jmmNode.getJmmChild(0));
        if (TYPES.contains(operand.getName())) {
            putUnknownType(jmmNode);
            reports.add(UtilsReport.cannotBeDereferencedReport(jmmNode, operand.getName()));
            return reports;
        }

        if (operand.getName().equals("#UNKNOWNTYPE") || operand.isArray() || !symbolTable.getClassName().equals(operand.getName())) {
            putUnknownType(jmmNode);
            return reports;
        }

        String methodSignature = inferMethodSignature(jmmNode);
        if (symbolTable.getMethods().contains(methodSignature)) {
            Type returnType = symbolTable.getReturnType(methodSignature);
            putType(jmmNode, returnType);
        } else {
            if (symbolTable.getSuper() == null) {
                String symbolName = UtilsAnalyser.getMethodSymbolName(methodSignature);
                reports.add(UtilsReport.cannotFindSymbolReport(jmmNode, symbolName));
            }
            putUnknownType(jmmNode);
        }

        return reports;
    }

    private List<Report> visitAssignment(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type leftType = UtilsAnalyser.getType(jmmNode.getJmmChild(0));
        Type rightType = UtilsAnalyser.getType(jmmNode.getJmmChild(1));

        if (leftType.getName().equals("#UNKNOWNTYPE") || rightType.getName().equals("#UNKNOWNTYPE"))
            return reports;

        if (leftType.isArray() || rightType.isArray()) {
            if (!leftType.equals(rightType))
                reports.add(UtilsReport.typeMismatchReport(jmmNode, leftType.getName(), rightType.getName()));
            return reports;
        }

        if(!leftType.equals(rightType))
            reports.add(UtilsReport.typeMismatchReport(jmmNode, leftType.getName(), rightType.getName()));

        return reports;
    }

    private List<Report> visitIfElse(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type conditionType = UtilsAnalyser.getType(jmmNode.getJmmChild(0));

        if (!conditionType.getName().equals("boolean") || conditionType.isArray())
            reports.add(UtilsReport.typeMismatchReport(jmmNode.getJmmChild(0), conditionType.getName(), "boolean"));

        return reports;
    }

    private List<Report> visitWhile(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type conditionType = UtilsAnalyser.getType(jmmNode.getJmmChild(0));

        if (!conditionType.getName().equals("boolean") || conditionType.isArray())
            reports.add(UtilsReport.typeMismatchReport(jmmNode.getJmmChild(0), conditionType.getName(), "boolean"));

        return reports;
    }

    private List<Report> visitBinaryOp(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type leftType = UtilsAnalyser.getType(jmmNode.getJmmChild(0));
        Type rightType = UtilsAnalyser.getType(jmmNode.getJmmChild(1));

        if (leftType.getName().equals("#UNKNOWNTYPE") || rightType.getName().equals("#UNKNOWNTYPE"))
            return reports;
        if (!leftType.equals(rightType)) {
            putUnknownType(jmmNode);
            reports.add(UtilsReport.typeMismatchReport(jmmNode, leftType.getName(), rightType.getName()));
            return reports;
        } else if(ARITHMETIC_OPS.contains(jmmNode.get("op")) || LOGICAL_OP.contains(jmmNode.get("op"))) {
            if(leftType.isArray()) {
                putUnknownType(jmmNode);
                reports.add(UtilsReport.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), leftType.getName(), rightType.getName()));
                return reports;
            }
        } else if(ARITHMETIC_OPS.contains(jmmNode.get("op")) || COMPARISON_OP.contains(jmmNode.get("op"))){
            if(!leftType.getName().equals("int")){
                putUnknownType(jmmNode);
                reports.add(UtilsReport.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), leftType.getName(), rightType.getName()));
                return reports;
            }
        } else if(LOGICAL_OP.contains(jmmNode.get("op"))){
            if(!leftType.getName().equals("boolean")){
                putUnknownType(jmmNode);
                reports.add(UtilsReport.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), leftType.getName(), rightType.getName()));
                return reports;
            }
        } else if(!TYPES.contains(leftType.getName())){
            putUnknownType(jmmNode);
            reports.add(UtilsReport.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), leftType.getName(), rightType.getName()));
            return reports;
        }
        if(COMPARISON_OP.contains(jmmNode.get("op")))
            putType(jmmNode, new Type("boolean", false));
        else
            putType(jmmNode, leftType);

        return reports;
    }

    private List<Report> checkTypeImport(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type objType = UtilsAnalyser.getType(jmmNode);

        boolean foundImport = symbolTable
                .getImports()
                .stream()
                .anyMatch(imp -> imp.substring(imp.lastIndexOf(".") + 1).equals(objType.getName()));

        if (foundImport)
            return reports;

        reports.add(UtilsReport.cannotFindSymbolReport(jmmNode, objType.getName()));

        return reports;
    }

    private String inferMethodSignature(JmmNode jmmNode) {
        StringBuilder methodSignatureBuilder = new StringBuilder();
        methodSignatureBuilder.append(jmmNode.get("methodnrame"));
        for (JmmNode argument: jmmNode.getJmmChild(1).getChildren()) {
            Type argType = UtilsAnalyser.getType(argument);
            methodSignatureBuilder.append("#");
            methodSignatureBuilder.append(argType.print());
        }
        return methodSignatureBuilder.toString();
    }

    private void putType(JmmNode jmmNode, Type type) {
        jmmNode.put("type", type.getName());
        jmmNode.put("isArray", String.valueOf(type.isArray()));
    }

    private void putUnknownType(JmmNode jmmNode) {
        jmmNode.put("type", "#UNKNOWNTYPE");
    }
}
