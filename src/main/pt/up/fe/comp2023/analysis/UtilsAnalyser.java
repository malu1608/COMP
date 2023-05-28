package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class UtilsAnalyser {
    public static Type getType(JmmNode jmmNode, MySymbolTable symbolTable) {
        switch (jmmNode.getKind()) {
            case "NotExpression", "Boolean", "BooleanType" -> {
                return new Type("boolean", false);
            }

            case "BinaryOp" -> {
                if (jmmNode.get("op").equals("&&") || jmmNode.get("op").equals("<") || jmmNode.get("op").equals(">") || jmmNode.get("op").equals("||"))
                    return new Type("boolean", false);
                else
                    return new Type("int", false);
            }

            case "ArrayAccess", "Length", "Integer" -> {
                return new Type("int", false);
            }

            case "NewIntArray" -> {
                return new Type("int", true);
            }

            case "NewObject" -> {
                return new Type(jmmNode.get("value"), false);
            }

            case "UnaryOp" -> {
                return getType(jmmNode.getChildren().get(0), symbolTable);
            }

            case "Identifier" -> {

                //aqui faltam casos!!  o .get("op") é qd o pai é binary op, mas aqui tb entram method calls ou seja .get("name")
                String varName = jmmNode.getJmmParent().get("op");

                if (jmmNode.getAncestor("MethodDef").isEmpty()) {
                    Symbol varSymbol = symbolTable.findField(varName);
                    if (varSymbol.getType().getName().equals("#UNKNOWN"))
                        return new Type("#UNKNOWN", false);
                    return varSymbol.getType();
                }

                Symbol varSymbol = symbolTable.findMethod(varName, jmmNode.getAncestor("MethodDef").get().get("name"));
                if (varSymbol.getType().getName().equals("#UNKNOWN"))
                    return new Type("#UNKNOWN", false);
                return varSymbol.getType();
            }

            case "IDType" -> {
                String varName = jmmNode.get("name");
                if (jmmNode.getAncestor("MethodDef").isEmpty()) {
                    Symbol varSymbol = symbolTable.findField(varName);
                    if (varSymbol.getType().getName().equals("#UNKNOWN"))
                        return new Type("#UNKNOWN", false);
                    return varSymbol.getType();
                }

                Symbol varSymbol = symbolTable.findMethod(varName, jmmNode.getAncestor("MethodDef").get().get("name"));
                if (varSymbol.getType().getName().equals("#UNKNOWN"))
                    return new Type("#UNKNOWN", false);
                return varSymbol.getType();
            }

            case "MethodCall" -> {
                String methodName = jmmNode.get("name");
                return (symbolTable.getReturnType(methodName));
            }

            case "This" -> {
                return new Type(symbolTable.getClassName(), false);
            }

            case "IntType" -> {
                boolean isArray = jmmNode.getAttributes().contains("isArray") && jmmNode.get("isArray").equals("true");
                return new Type("int", isArray);
            }

            case "StringType" -> {
                boolean isArray = jmmNode.getAttributes().contains("isArray") && jmmNode.get("isArray").equals("true");
                return new Type("String", isArray);
            }

            default -> throw new RuntimeException("Could not get type from node: " + jmmNode.getKind());
        }
    }

    public static Type getTypeOperand(JmmNode jmmNode, MySymbolTable symbolTable) {

        String kind = jmmNode.getKind();
        switch (kind) {
            case "UnaryOp" -> {
                return getType(jmmNode.getChildren().get(0), symbolTable);
            }
            case "ArrayAccess" -> {
                Type leftType = getType(jmmNode.getChildren().get(0), symbolTable);
                if (leftType.isArray() && leftType.getName().equals("int"))
                    return new Type("int", false);
                if (leftType.isArray() && leftType.getName().equals("String"))
                    return new Type("String", false);
                return new Type("#UNKNOWN", false);
            }
            case "ArrayLength", "Integer" -> {
                return new Type("int", false);
            }
            case "BinaryOp" -> {
                return getTypeOperand(jmmNode.getJmmChild(1), symbolTable);
            }
            case "NotExpression", "Boolean" -> {
                return new Type("boolean", false);
            }
            case "NewIntArray" -> {
                return new Type("int", true);
            }
            case "NewObject" -> {
                return new Type(jmmNode.get("name"), false);
            }
            case "MethodCall" -> {
                return getTypeOperand(jmmNode.getJmmChild(0), symbolTable);
            }
            case "Identifier" -> {
                String varName = jmmNode.get("value");
                if (jmmNode.getAncestor("MethodDef").isEmpty()) {
                    Symbol varSymbol = symbolTable.findField(varName);
                    if (varSymbol.getType().getName().equals("#UNKNOWN"))
                        return new Type("#UNKNOWN", false);
                    return varSymbol.getType();
                }

                Symbol varSymbol = symbolTable.findMethod(varName, jmmNode.getAncestor("MethodDef").get().get("name"));
                if (varSymbol.getType().getName().equals("#UNKNOWN"))
                    return new Type("#UNKNOWN", false);
                return varSymbol.getType();
            }
            case "This" -> {
                return new Type(symbolTable.getClassName(), false);
            }
            default -> throw new RuntimeException("Unknown kind: " + kind);
        }

    }


    public static String getMethodSymbolName(String methodSignature) {
        if (methodSignature.contains("#")) {
            return methodSignature.replaceFirst("#", "(")
                    .replaceAll("##UNKNOWN", ",?")
                    .replaceAll("\\(#UNKNOWN", "(?")
                    .replaceAll("#", ",")
                    .concat(")");
        }
        return methodSignature.concat("()");
    }
}
