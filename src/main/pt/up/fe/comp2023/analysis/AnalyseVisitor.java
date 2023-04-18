package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsCollections;

import java.util.*;
public class AnalyseVisitor extends PostorderJmmVisitor<SymbolTable, Void> {
    static final List<String> TYPES = Arrays.asList("int", "boolean", "void");
    static final List<String> ARITHMETIC_OPS = Arrays.asList("ADD", "SUB", "MUL", "DIV");
    static final List<String> COMPARISON_OP = List.of("LT");
    static final List<String> LOGICAL_OP = List.of("AND");

    List<Report> reports = new ArrayList<>();

    public List<Report> getReports() {
        return reports;
    }

    @Override
    protected void buildVisitor() {
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("Type", this::visitType);
        addVisit("ReturnStatement", this::visitReturn);
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("Assignment", this::visitAssignment);
        addVisit("IfElseStatement", this::visitIfElse);
        addVisit("WhileStatement", this::visitWhile);
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("NotExpression", this::visitNotExpression);
        addVisit("IntArrayAllocation", this::visitIntArrayAllocation);
        addVisit("ObjectAllocation", this::visitObjectAllocation);
        addVisit("This", this::visitThis);
        addVisit("Identifier", this::visitIdentifier);
        addVisit("Index", this::visitIndex);
        addVisit("Length", this::visitLength);

        setDefaultVisit(this::defaultVisit);
    }

    private Void defaultVisit(JmmNode jmmNode, SymbolTable symbolTable) {

        for (JmmNode child : jmmNode.getChildren())
            visit(child, symbolTable);

        return null;
    }

    private Void visitClassDeclaration(JmmNode jmmNode, SymbolTable symbolTable) {

        if (!jmmNode.getAttributes().contains("extends"))
            return null;

        String extendsName = jmmNode.get("extends");

        if (jmmNode.get("classname").equals(extendsName)) {
            reports.add(UtilsReport.cyclicInheritance(jmmNode, extendsName));
            return null;
        }

        boolean foundImport = symbolTable
                .getImports()
                .stream()
                .anyMatch(imp -> imp.substring(imp.lastIndexOf(".") + 1).equals(extendsName));

        if (foundImport)
            return null;

        reports.add(UtilsReport.cannotFindSymbolReport(jmmNode, extendsName));

        return null;
    }

    private Void visitType(JmmNode jmmNode, SymbolTable symbolTable) {
        Type objType = UtilsAnalyser.getType(jmmNode);

        if (TYPES.contains(objType.getName()))
            return null;
        checkTypeImport(jmmNode, symbolTable);

        return null;
    }

    private Void visitReturn(JmmNode jmmNode, SymbolTable symbolTable) {
        String methodSignature = jmmNode.getJmmParent().getJmmParent().get("signature");
        Type returnType = symbolTable.getReturnType(methodSignature);

        if (returnType.getName().equals("void"))
            return null;

        if (jmmNode.getChildren().isEmpty()) {
            reports.add(UtilsReport.returnTypeMismatchReport(jmmNode, returnType.getName(), "void"));
            return null;
        }

        Type exprType = UtilsAnalyser.getType(jmmNode.getJmmChild(0));

        if (!returnType.equals(exprType))
            reports.add(UtilsReport.returnTypeMismatchReport(jmmNode, returnType.getName(), exprType.getName()));

        return null;
    }

    private Void visitMethodCall(JmmNode jmmNode, SymbolTable symbolTable) {

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
            return null;
        }

        Type operand = UtilsAnalyser.getType(jmmNode.getJmmChild(0));
        if (TYPES.contains(operand.getName())) {
            putUnknownType(jmmNode);
            reports.add(UtilsReport.cannotBeDereferencedReport(jmmNode, operand.getName()));
            return null;
        }

        if (operand.getName().equals("#UNKNOWNTYPE") || operand.isArray() || !symbolTable.getClassName().equals(operand.getName())) {
            putUnknownType(jmmNode);
            return null;
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

        return null;
    }

    private Void visitAssignment(JmmNode jmmNode, SymbolTable symbolTable) {

        Type leftType = UtilsAnalyser.getType(jmmNode.getJmmChild(0));
        Type rightType = UtilsAnalyser.getType(jmmNode.getJmmChild(1));

        if (leftType.getName().equals("#UNKNOWNTYPE") || rightType.getName().equals("#UNKNOWNTYPE"))
            return null;

        if (leftType.isArray() || rightType.isArray()) {
            if (!leftType.equals(rightType))
                reports.add(UtilsReport.typeMismatchReport(jmmNode, leftType.getName(), rightType.getName()));
            return null;
        }

        if(!leftType.equals(rightType))
            reports.add(UtilsReport.typeMismatchReport(jmmNode, leftType.getName(), rightType.getName()));

        return null;
    }

    private Void visitIfElse(JmmNode jmmNode, SymbolTable symbolTable) {

        Type conditionType = UtilsAnalyser.getType(jmmNode.getJmmChild(0));

        if (!conditionType.getName().equals("boolean") || conditionType.isArray())
            reports.add(UtilsReport.typeMismatchReport(jmmNode.getJmmChild(0), conditionType.getName(), "boolean"));

        return null;
    }

    private Void visitWhile(JmmNode jmmNode, SymbolTable symbolTable) {

        Type conditionType = UtilsAnalyser.getType(jmmNode.getJmmChild(0));

        if (!conditionType.getName().equals("boolean") || conditionType.isArray())
            reports.add(UtilsReport.typeMismatchReport(jmmNode.getJmmChild(0), conditionType.getName(), "boolean"));

        return null;
    }

    private Void visitBinaryOp(JmmNode jmmNode, SymbolTable symbolTable) {

        Type leftType = UtilsAnalyser.getType(jmmNode.getJmmChild(0));
        Type rightType = UtilsAnalyser.getType(jmmNode.getJmmChild(1));

        if (leftType.getName().equals("#UNKNOWNTYPE") || rightType.getName().equals("#UNKNOWNTYPE"))
            return null;
        if (!leftType.equals(rightType)) {
            putUnknownType(jmmNode);
            reports.add(UtilsReport.typeMismatchReport(jmmNode, leftType.getName(), rightType.getName()));
            return null;
        } else if(ARITHMETIC_OPS.contains(jmmNode.get("op")) || LOGICAL_OP.contains(jmmNode.get("op"))) {
            if(leftType.isArray()) {
                getReports(jmmNode, reports, leftType, rightType);
                return null;
            }
        } else if(ARITHMETIC_OPS.contains(jmmNode.get("op")) || COMPARISON_OP.contains(jmmNode.get("op"))){
            if(!leftType.getName().equals("int")){
                getReports(jmmNode, reports, leftType, rightType);
                return null;
            }
        } else if(LOGICAL_OP.contains(jmmNode.get("op"))){
            if(!leftType.getName().equals("boolean")){
                getReports(jmmNode, reports, leftType, rightType);
                return null;
            }
        } else if(!TYPES.contains(leftType.getName())){
            getReports(jmmNode, reports, leftType, rightType);
            return null;
        }
        if(COMPARISON_OP.contains(jmmNode.get("op")))
            putType(jmmNode, new Type("boolean", false));
        else
            putType(jmmNode, leftType);

        return null;
    }

    private Void visitNotExpression(JmmNode jmmNode, SymbolTable symbolTable) {

        Type operandType = UtilsAnalyser.getType(jmmNode.getJmmChild(0));

        if (operandType.getName().equals("#UNKNOWNTYPE"))
            return null;

        if (!operandType.getName().equals("boolean") || operandType.isArray()){
            putUnknownType(jmmNode);
            reports.add(UtilsReport.operatorCannotBeAppliedReport(jmmNode.getJmmChild(0), "!", operandType.print(), "boolean"));
            return null;
        }
        putType(jmmNode, new Type("boolean", false));
        return null;
    }

    private Void visitIntArrayAllocation(JmmNode jmmNode, SymbolTable symbolTable) {

        Type sizeType = UtilsAnalyser.getType(jmmNode.getJmmChild(0));

        if (sizeType.getName().equals("#UNKNOWNTYPE"))
            return null;

        if (!sizeType.getName().equals("int") || sizeType.isArray()){
            putUnknownType(jmmNode);
            reports.add(UtilsReport.typeConversionErrorReport(jmmNode.getJmmChild(0), sizeType.print(), "int"));
            return null;
        }
        putType(jmmNode, new Type("int", true));
        return null;
    }

    private Void visitObjectAllocation(JmmNode jmmNode, SymbolTable symbolTable) {

        Type objType = UtilsAnalyser.getType(jmmNode.getJmmChild(0));
        if(objType.getName().equals("#UNKNOWNTYPE"))
            return null;

        if(TYPES.contains(objType.getName()) || objType.isArray()){
            putUnknownType(jmmNode);
            reports.add(UtilsReport.operatorCannotBeAppliedReport(jmmNode, "new", objType.print()));
            return null;
        }
        checkTypeImport(jmmNode, symbolTable);
        return null;
    }

    private Void visitThis (JmmNode jmmNode, SymbolTable symbolTable) {

        putType(jmmNode, new Type(symbolTable.getClassName(), false));
        return null;
    }

    private Void visitIdentifier(JmmNode jmmNode, SymbolTable symbolTable) {

        String identifier = jmmNode.get("name");
        String methodSignature = getMethodSignature(jmmNode);

        Optional<Symbol> parameterSymbol = symbolTable.getParameters(methodSignature).stream()
                .filter(p -> p.getName().equals(identifier)).findFirst();
        Optional<Symbol> localVarSymbol = symbolTable.getLocalVariables(methodSignature).stream()
                .filter(l -> l.getName().equals(identifier)).findFirst();
        Optional<Symbol> fieldSymbol = symbolTable.getFields().stream()
                .filter(f -> f.getName().equals(identifier)).findFirst();

        if (parameterSymbol.isPresent()) {
            putType(jmmNode, parameterSymbol.get().getType());
        } else if (localVarSymbol.isPresent()) {
            putType(jmmNode, localVarSymbol.get().getType());
        } else if (fieldSymbol.isPresent()) {
            putType(jmmNode, fieldSymbol.get().getType());
        } else if (shouldPutUnknownType(jmmNode, symbolTable, identifier)) {
            putUnknownType(jmmNode);
            reports.add(UtilsReport.cannotFindSymbolReport(jmmNode, identifier));
        }

        return null;
    }

    private Void visitIndex(JmmNode jmmNode, SymbolTable symbolTable) {

        Type operandType = UtilsAnalyser.getType(jmmNode.getJmmChild(0));
        Type indexType = UtilsAnalyser.getType(jmmNode.getJmmChild(1));

        if(!indexType.getName().equals("#UNKNOWNTYPE") && (!indexType.getName().equals("int") || indexType.isArray())){
            reports.add(UtilsReport.typeConversionErrorReport(jmmNode.getJmmChild(1), indexType.print(), "int"));
        }
        if(operandType.getName().equals("#UNKNOWNTYPE"))
            putUnknownType(jmmNode);
        else if(!operandType.isArray()){
            putType(jmmNode, new Type(operandType.getName(), false));
        } else {
            putUnknownType(jmmNode);
            reports.add(UtilsReport.createArrayRequiredReport(jmmNode.getJmmChild(0), operandType.print()));
        }
        return null;
    }

    private Void visitLength(JmmNode jmmNode, SymbolTable symbolTable) {

        Type operandType = UtilsAnalyser.getType(jmmNode.getJmmChild(0));

        if(!operandType.getName().equals("#UNKNOWNTYPE") && !operandType.isArray())
            reports.add(UtilsReport.createArrayRequiredReport(jmmNode.getJmmChild(0), operandType.print()));

        return null;
    }

    // Helper method to get the method signature for the given identifier
    private String getMethodSignature(JmmNode jmmNode) {
        return jmmNode.getAncestor("MethodDef").or(() -> jmmNode.getAncestor("MainMethodDef")).get().get("signature");
    }

    // Helper method to check if the identifier should have an unknown type
    private boolean shouldPutUnknownType(JmmNode jmmNode, SymbolTable symbolTable, String identifier) {
        return !jmmNode.getJmmParent().getKind().equals("MethodCall")
                || (!symbolTable.getClassName().equals(identifier) && !symbolTable.getImports().contains(identifier));
    }

    private Void getReports(JmmNode jmmNode, List<Report> reports, Type leftType, Type rightType) {
        putUnknownType(jmmNode);
        reports.add(UtilsReport.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), leftType.getName(), rightType.getName()));
        return null;
    }

    private Void checkTypeImport(JmmNode jmmNode, SymbolTable symbolTable) {
        Type objType = UtilsAnalyser.getType(jmmNode);

        boolean foundImport = symbolTable
                .getImports()
                .stream()
                .anyMatch(imp -> imp.substring(imp.lastIndexOf(".") + 1).equals(objType.getName()));

        if (foundImport)
            return null;

        reports.add(UtilsReport.cannotFindSymbolReport(jmmNode, objType.getName()));

        return null;
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
