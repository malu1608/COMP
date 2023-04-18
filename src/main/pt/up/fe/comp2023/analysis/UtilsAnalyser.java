package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
public class UtilsAnalyser {
    public static Type getType(JmmNode jmmNode) {
        boolean isArray = jmmNode.getAttributes().contains("isArray") && jmmNode.get("isArray").equals("true");
        return new Type(jmmNode.get("name"), isArray);
    }

    public static Symbol getSymbol(JmmNode jmmNode) {
        String name = jmmNode.get("name");
        Type type = getType(jmmNode.getJmmChild(0));
        return new Symbol(type, name);
    }

    public static String getMethodSymbolName(String methodSignature) {
        if(methodSignature.contains("#")) {
            return methodSignature.replaceFirst("#", "(").replaceAll("##UNKNOWNTYPE", ",?")
                    .replaceAll("#", ",").concat(")");
        }
        return methodSignature.concat("()");
    }
}
