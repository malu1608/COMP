package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsCollections;

import java.util.*;

public class AnalyseVisitor extends PostorderJmmVisitor<MySymbolTable, List<Report>> {
    static final List<String> TYPES = Arrays.asList("int", "boolean", "void");
    static final List<String> ARITHMETIC_OPS = Arrays.asList("ADD", "SUB", "MUL", "DIV");
    static final List<String> COMPARISON_OP = List.of("LT");
    static final List<String> LOGICAL_OP = List.of("AND");

    @Override
    public void buildVisitor() {
    }

    public AnalyseVisitor() {
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("ReturnStatement", this::visitReturn);
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("NotExpression", this::visitNot);
        addVisit("LengthCall", this::visitLengthCall);
        addVisit("NewObject", this::visitObjectAllocation);
        addVisit("ArrayAccess", this::visitIndexing);
        addVisit("Assignment", this::visitAssignment);
        addVisit("NewIntArray", this::visitIntArrayAllocation);
        addVisit("Type", this::visitType);
        addVisit("WhileStatement", this::visitCondition);
        addVisit("ThisKeyword", this::visitThisKeyword);
        addVisit("IfThenElseStatement", this::visitCondition);
        addVisit("Identifier", this::visitIdentifier);

        addVisit("IntType", this::visitType);
        addVisit("StringType", this::visitType);
        addVisit("BooleanType", this::visitType);
        addVisit("IDType", this::visitType);


        setDefaultValue(Collections::emptyList);
        setReduceSimple(this::reduceReports);
    }

    private List<Report> visitClassDeclaration(JmmNode jmmNode, MySymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        if (!jmmNode.getAttributes().contains("extends")) {
            return reports;
        }

        String extendsName = jmmNode.get("extends");

        if (jmmNode.get("classname").equals(extendsName)) {
            reports.add(UtilsReport.cyclicInheritance(jmmNode, extendsName));
            return reports;
        }

        boolean foundImport = symbolTable.getImports().stream()
                .map(imp -> imp.substring(imp.lastIndexOf(".") + 1))
                .anyMatch(impName -> impName.equals(extendsName));

        if (foundImport) {
            return reports;
        }

        reports.add(UtilsReport.cannotFindSymbolReport(jmmNode, extendsName));

        return reports;
    }


    private List<Report> visitReturn(JmmNode jmmNode, MySymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        JmmNode parent = jmmNode.getJmmParent();
        if (parent == null || parent.getJmmParent() == null) {
            return reports;
        }

        String methodSignature = parent.getJmmParent().get("signature");
        Type returnType = symbolTable.getReturnType(methodSignature);
        Type expressionType = UtilsAnalyser.getType(jmmNode.getJmmChild(0), symbolTable);

        if (!returnType.getName().equals("#UNKNOWN") && !expressionType.getName().equals("#UNKNOWN")) {
            if (!typeIsCompatibleWith(returnType, expressionType, symbolTable)) {
                reports.add(UtilsReport.typeConversionErrorReport(jmmNode.getJmmChild(0), expressionType.print(), returnType.print()));
            }
        }

        return reports;
    }


    private List<Report> visitBinaryOp(JmmNode jmmNode, MySymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        JmmNode left = jmmNode.getChildren().get(0);
        JmmNode right = jmmNode.getChildren().get(1);
        String op = jmmNode.get("op");
        Type leftType = UtilsAnalyser.getType(left, symbolTable);
        Type rightType = UtilsAnalyser.getType(right, symbolTable);

        if (leftType.getName().equals("#UNKOWN") || rightType.getName().equals("#UNKOWN")) {
            putUnknownType(jmmNode);
        }

        if (!leftType.equals(rightType)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), "Cannot apply operator " + op));
            return reports;
        }

        if (ARITHMETIC_OPS.contains(op)) {
            if (leftType.isArray() || rightType.isArray()) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), "Cannot apply operator" + op + " to array"));
                return reports;
            }
            if (!(leftType.getName().equals("int") && rightType.getName().equals("int"))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), "Cannot apply operator" + op + " to " + leftType.getName()));
                return reports;
            }

        }

        return reports;
    }

    private boolean isValidBinaryOp(Type lhsType, String operator) {
        if (ARITHMETIC_OPS.contains(operator) || LOGICAL_OP.contains(operator)) {
            if (lhsType.isArray() || !lhsType.getName().equals("int")) {
                return false;
            }
            if (LOGICAL_OP.contains(operator) && !lhsType.getName().equals("boolean")) {
                return false;
            }
        }
        return TYPES.contains(lhsType.getName());
    }


    private List<Report> visitMethodCall(JmmNode jmmNode, MySymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        String methodName = jmmNode.get("name");
        String superClass = symbolTable.getSuper();

        Type classType = UtilsAnalyser.getTypeOperand(jmmNode.getJmmChild(0), symbolTable);
        String className = classType.getName();

        if (className.equals(symbolTable.getClassName())) {
            if (symbolTable.getMethods().contains(methodName)) {
                List<Symbol> methodParams = symbolTable.getParameters(methodName);
                int methodParamsSize = 0;
                if (methodParams != null) methodParamsSize = methodParams.size();
                JmmNode argumentsNode = jmmNode.getJmmChild(1);
                if (methodParamsSize != argumentsNode.getNumChildren()) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), "Method " + methodName + " has " + methodParams.size() + " parameters"));
                } else {
                    for (int i = 0; i < argumentsNode.getNumChildren(); i++) {
                        Type paramType = methodParams.get(i).getType();
                        Type argType = UtilsAnalyser.getType(argumentsNode.getJmmChild(i), symbolTable);
                        if (!paramType.equals(argType)) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), "Method " + methodName + " has " + methodParams.size() + " parameters"));
                        }
                    }
                }
            } else if (!(superClass != null && symbolTable.isClassImported(superClass))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), "Method " + methodName + " is not declared"));
            }
        } else {
            if (!symbolTable.isClassImported(className)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), "Method " + methodName + " is not declared"));
            }
        }
        return reports;
    }


    private List<Report> visitNot(JmmNode jmmNode, MySymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type type = UtilsAnalyser.getType(jmmNode.getJmmChild(0), symbolTable);

        if (type.getName().equals("#UKNOWN")) {
            putUnknownType(jmmNode);
            return reports;
        }

        if (type.isArray() || !type.getName().equals("boolean")) {
            putUnknownType(jmmNode);
            reports.add(UtilsReport.operatorCannotBeAppliedReport(jmmNode.getJmmChild(0), "!", type.print(), "boolean"));
        } else {
            putType(jmmNode, new Type("boolean", false));
        }

        return reports;
    }


    private List<Report> visitLengthCall(JmmNode jmmNode, MySymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type type = UtilsAnalyser.getType(jmmNode.getJmmChild(0), symbolTable);
        if (!type.getName().equals("#UNKNOWN") && !type.isArray()) {
            reports.add(UtilsReport.createArrayRequiredReport(jmmNode, type.print()));
            return reports;
        }

        putType(jmmNode, new Type("int", false));

        return reports;
    }


    private List<Report> visitObjectAllocation(JmmNode jmmNode, MySymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type objType = UtilsAnalyser.getType(jmmNode, symbolTable);
        if (objType.isArray() || TYPES.contains(objType.getName())) {
            putUnknownType(jmmNode);
            reports.add(UtilsReport.operatorCannotBeAppliedReport(jmmNode, "new", objType.print()));
            return reports;
        }

        return checkTypeImport(jmmNode, symbolTable);
    }

    private List<Report> visitIndexing(JmmNode jmmNode, MySymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type operandType = UtilsAnalyser.getType(jmmNode.getJmmChild(0), symbolTable);
        Type indexType = UtilsAnalyser.getType(jmmNode.getJmmChild(1), symbolTable);

        if (!indexType.getName().equals("#UNKNOWN") && (!indexType.getName().equals("int") || indexType.isArray())) {
            reports.add(UtilsReport.typeConversionErrorReport(jmmNode.getJmmChild(1), indexType.print(), "int"));
        }

        if (operandType.getName().equals("#UNKNOWN")) {
            putUnknownType(jmmNode);
        } else if (operandType.isArray()) {
            putType(jmmNode, new Type(operandType.getName(), false));
        } else {
            putUnknownType(jmmNode);
            reports.add(UtilsReport.createArrayRequiredReport(jmmNode.getJmmChild(0), operandType.print()));
        }

        return reports;
    }

    private List<Report> visitAssignment(JmmNode jmmNode, MySymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type lhsType = UtilsAnalyser.getType(jmmNode.getJmmChild(0), symbolTable);
        Type rhsType = UtilsAnalyser.getType(jmmNode.getJmmChild(1), symbolTable);

        if (lhsType.getName().equals("#UNKNOWN")) {
            return reports;
        }
        if (rhsType.getName().equals("#UNKNOWN")) {
            putType(jmmNode.getJmmChild(1), lhsType);
            return reports;
        }

        if (!typeIsCompatibleWith(lhsType, rhsType, symbolTable)) {
            reports.add(UtilsReport.typeConversionErrorReport(jmmNode, rhsType.print(), lhsType.print()));
        }

        return reports;
    }

    private List<Report> visitIntArrayAllocation(JmmNode jmmNode, MySymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type type = UtilsAnalyser.getType(jmmNode.getJmmChild(0), symbolTable);
        if (!type.getName().equals("#UNKNOWN") && !(type.getName().equals("int") && !type.isArray())) {
            reports.add(UtilsReport.typeConversionErrorReport(jmmNode, type.print(), "int"));
        }

        putType(jmmNode, new Type("int", true));

        return reports;
    }

    private List<Report> visitType(JmmNode jmmNode, MySymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type objType = UtilsAnalyser.getType(jmmNode, symbolTable);
        if (TYPES.contains(objType.getName())) {
            return reports;
        }

        return checkTypeImport(jmmNode, symbolTable);
    }

    private List<Report> visitCondition(JmmNode jmmNode, MySymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type conditionType = UtilsAnalyser.getType(jmmNode.getJmmChild(0), symbolTable);

        if (conditionType.getName().equals("#UNKNOWN")) {
            return reports;
        }

        if (conditionType.isArray() || !conditionType.getName().equals("boolean")) {
            reports.add(UtilsReport.typeConversionErrorReport(jmmNode.getJmmChild(0), conditionType.print(), "boolean"));
        }

        return reports;
    }

    private List<Report> visitThisKeyword(JmmNode jmmNode, MySymbolTable symbolTable) {
        putType(jmmNode, new Type(symbolTable.getClassName(), false));
        return new ArrayList<>();
    }

    private List<Report> visitIdentifier(JmmNode jmmNode, MySymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        String identifier = jmmNode.get("value");

        String methodSignature = jmmNode
                .getAncestor("MethodDef")
                .or(() -> jmmNode.getAncestor("MainMethodDef"))
                .get()
                .get("signature");

        Optional<Symbol> parameterSymbol = symbolTable
                .getParameters(methodSignature)
                .stream()
                .filter(p -> p.getName().equals(identifier))
                .findFirst();

        if (parameterSymbol.isPresent()) {
            putType(jmmNode, parameterSymbol.get().getType());
            return reports;
        }

        Optional<Symbol> localVarSymbol = symbolTable
                .getLocalVariables(methodSignature)
                .stream()
                .filter(l -> l.getName().equals(identifier))
                .findFirst();

        if (localVarSymbol.isPresent()) {
            putType(jmmNode, localVarSymbol.get().getType());
            return reports;
        }

        Optional<Symbol> fieldSymbol = symbolTable
                .getFields()
                .stream()
                .filter(f -> f.getName().equals(identifier))
                .findFirst();

        if (fieldSymbol.isPresent()) {
            putType(jmmNode, fieldSymbol.get().getType());
            return reports;
        }

        if (!jmmNode.getJmmParent().getKind().equals("MethodCall")
                || (
                !identifier.equals("String") &&
                        !symbolTable.getClassName().equals(identifier) &&
                        !symbolTable.getImports().stream().anyMatch(imp -> imp.substring(imp.lastIndexOf(".") + 1).equals(identifier)))
        ) {
            putUnknownType(jmmNode);
            reports.add(UtilsReport.cannotFindSymbolReport(jmmNode, identifier));
        }

        return reports;
    }


    private boolean signatureIsCompatibleWith(String sig1, String sig2, MySymbolTable symbolTable) {
        List<String> m1 = Arrays.asList(sig1.split("(?<!#)#"));
        List<String> m2 = Arrays.asList(sig2.split("(?<!#)#"));

        if (!m1.get(0).equals(m2.get(0)) || m1.size() != m2.size()) return false;
        m1 = m1.subList(1, m1.size());
        m2 = m2.subList(1, m2.size());

        for (int i = 0; i < m1.size(); i++) {
            if (m1.get(i).equals("#UNKNOWN") || m2.get(i).equals("#UNKNOWN")) continue;
            Type t1 = new Type(m1.get(i).split("\\[]")[0], m1.get(i).endsWith("[]"));
            Type t2 = new Type(m2.get(i).split("\\[]")[0], m2.get(i).endsWith("[]"));
            if (!typeIsCompatibleWith(t1, t2, symbolTable)) return false;
        }

        return true;
    }

    private String inferMethodSignature(JmmNode jmmNode, MySymbolTable symbolTable) {
        StringBuilder methodSignatureBuilder = new StringBuilder();
        methodSignatureBuilder.append(jmmNode.get("name"));
        for (JmmNode argument : jmmNode.getJmmChild(1).getChildren()) {
            Type argType = UtilsAnalyser.getType(argument, symbolTable);
            methodSignatureBuilder.append("#");
            methodSignatureBuilder.append(argType.print());
        }
        return methodSignatureBuilder.toString();
    }

    private List<Report> checkTypeImport(JmmNode jmmNode, MySymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type objType = UtilsAnalyser.getType(jmmNode, symbolTable);

        if (symbolTable.getClassName().equals(objType.getName()) || objType.getName().equals("String")) {
            return reports;
        }

        Optional<String> qualifiedName = symbolTable
                .getImports()
                .stream()
                .filter(imp -> imp.substring(imp.lastIndexOf(".") + 1).equals(objType.getName()))
                .findAny();

        if (qualifiedName.isPresent()) {
            return reports;
        }

        putUnknownType(jmmNode);
        reports.add(UtilsReport.cannotFindSymbolReport(jmmNode, objType.print()));

        return reports;
    }

    private boolean typeIsCompatibleWith(Type type1, Type type2, MySymbolTable symbolTable) {
        if (type1.equals(type2)) {
            return true;
        }

        if (type1.isArray() != type2.isArray() || TYPES.contains(type1.getName()) || TYPES.contains(type2.getName())) {
            return false;
        }

        if (type2.getName().equals(symbolTable.getClassName()) && symbolTable.getSuper() == null) {
            return false;
        }

        if (symbolTable.getSuper() == null) {
            return true;
        }

        return !(type1.getName().equals(symbolTable.getClassName()) && symbolTable.getSuper().equals(type2.getName()));
    }


    private void putType(JmmNode jmmNode, Type type) {
        jmmNode.put("type", type.getName());
        jmmNode.put("isArray", String.valueOf(type.isArray()));
    }

    private void putUnknownType(JmmNode jmmNode) {
        jmmNode.put("type", "#UNKNOWN");
    }

    private List<Report> reduceReports(List<Report> reports1, List<Report> reports2) {
        return SpecsCollections.concatList(reports1, reports2);
    }
}
