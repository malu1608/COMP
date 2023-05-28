package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsCollections;

import java.util.*;
import java.util.stream.Collectors;

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
        Type lhsType = UtilsAnalyser.getType(jmmNode.getJmmChild(0), symbolTable);
        Type rhsType = UtilsAnalyser.getType(jmmNode.getJmmChild(1), symbolTable);
        String operator = jmmNode.get("op");

        if (lhsType.getName().equals("#UNKNOWN") || rhsType.getName().equals("#UNKNOWN")) {
            putUnknownType(jmmNode);
            return reports;
        }

        if (!lhsType.equals(rhsType) || !isValidBinaryOp(lhsType, operator)) {
            putUnknownType(jmmNode);
            reports.add(UtilsReport.operatorCannotBeAppliedReport(jmmNode, operator, lhsType.print(), rhsType.print()));
            return reports;
        }

        if (COMPARISON_OP.contains(operator)) {
            putType(jmmNode, new Type("boolean", false));
        } else {
            putType(jmmNode, lhsType);
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

        // Static method call nodes have no attribute "type" so that may be used to check if the method call is static
        if (!jmmNode.getJmmChild(0).getAttributes().contains("type")) {
            if (jmmNode.getJmmChild(0).get("value").equals(symbolTable.getClassName())) {
                String methodSignature = inferMethodSignature(jmmNode, symbolTable);
                boolean methodExists = symbolTable
                        .getMethods()
                        .stream()
                        .anyMatch(m -> m.substring(0, m.indexOf("#")).equals(jmmNode.get("name")));

                if (methodExists) {
                    String symbolName = UtilsAnalyser.getMethodSymbolName(methodSignature);
                    reports.add(UtilsReport.nonStaticInStaticContext(jmmNode, symbolName));
                } else if (symbolTable.getSuper() == null) {
                    String symbolName = UtilsAnalyser.getMethodSymbolName(methodSignature);
                    reports.add(UtilsReport.cannotFindSymbolReport(jmmNode, symbolName));
                }
            }
            putUnknownType(jmmNode);
            return reports;
        }

        Type operand = UtilsAnalyser.getType(jmmNode.getJmmChild(0), symbolTable);
        if (TYPES.contains(operand.getName())) {
            putUnknownType(jmmNode);
            reports.add(UtilsReport.cannotBeDereferencedReport(jmmNode, operand.getName()));
            return reports;
        }

        if (operand.getName().equals("#UNKNOWN") || operand.isArray() || !symbolTable.getClassName().equals(operand.getName())) {
            putUnknownType(jmmNode);
            return reports;
        }

        String methodSignature = inferMethodSignature(jmmNode, symbolTable);
        Optional<String> foundMethodSignature;
        if (symbolTable.getMethods().contains(methodSignature)) {
            foundMethodSignature = Optional.of(methodSignature);
        } else {
            List<String> foundMethodSignatures = symbolTable
                    .getMethods()
                    .stream()
                    .filter(m -> signatureIsCompatibleWith(m, methodSignature, symbolTable))
                    .collect(Collectors.toList());
            if (foundMethodSignatures.size() > 1) {
                reports.add(UtilsReport.ambiguousMethodCallReport(jmmNode, jmmNode.get("name")));
                putUnknownType(jmmNode);
                return reports;
            } else {
                foundMethodSignature = foundMethodSignatures.stream().findAny();
            }
        }

        if (foundMethodSignature.isPresent()) {
            Type returnType = symbolTable.getReturnType(foundMethodSignature.get());
            putType(jmmNode, returnType);
        } else {
            boolean incompleteSignature = methodSignature.contains("##UNKNOWN");
            boolean methodLooselyExists = incompleteSignature &&
                    symbolTable
                            .getMethods()
                            .stream()
                            .anyMatch(m -> m.split("#")[0].equals(jmmNode.get("name")));
            if (!methodLooselyExists && symbolTable.getSuper() == null) {
                String symbolName = UtilsAnalyser.getMethodSymbolName(methodSignature);
                reports.add(UtilsReport.cannotFindSymbolReport(jmmNode, symbolName));
            }
            putUnknownType(jmmNode);
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
