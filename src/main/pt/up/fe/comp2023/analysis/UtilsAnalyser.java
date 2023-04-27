package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;

public class UtilsAnalyser {
    public static Type getType(JmmNode jmmNode) {
        boolean isArray = jmmNode.getAttributes().contains("isArray") && jmmNode.get("isArray").equals("true");
        return new Type(jmmNode.getKind(), isArray);
    }

    public static String getMethodSymbolName(String methodSignature) {
        if (methodSignature.contains("#")) {
            return methodSignature.replaceFirst("#", "(")
                    .replaceAll("##UNKNOWN", ",?")
                    .replaceAll("\\(#UNKNOWN", "(?")
                    .replaceAll("#", ",")
                    .concat(")");
        }
        return  methodSignature.concat("()");
    }
}
