package pt.up.fe.comp2023.ollir;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.function.Function;

public class OllirUtils {

    public static String getCode(Symbol symbol) {
        return symbol.getName() + "." + getCode(symbol.getType());
    }

    public static String getCode(Symbol symbol, String index) {
        return symbol.getName() + "[" + index + "]." + getCode(symbol.getType(), true);
    }

    public static String getCode(String value, Type type) {
        return value + "." + getCode(type);
    }

    public static String getTempCode(String value, Type type) {
        return "temp_" + value + "." + getCode(type);
    }

    public static String getTempCodeIndexed(String value, Type type) {
        return "temp_" + value + "." + getCode(type, true);
    }

    public static String getTempCode(Symbol symbol, String index) {
        return "temp_" + symbol.getName() + "[" + index + "]." + getCode(symbol.getType(), true);
    }

    public static String getCode(Type type, Boolean indexed) {
        StringBuilder code = new StringBuilder();

        if (type.isArray() && !indexed) {
            code.append("array.");
        }

        code.append(parseType(type.getName()));

        return code.toString();
    }
    public static String getCode(Type type) {
        return getCode(type, true);
    }
    public static String parseType(String type) {
        if (type.equals("int"))
            return ".i32";
        else if (type.equals("void"))
            return ".V";
        else if (type.equals("FALSE"))
            return "0.bool";
        else if (type.equals("TRUE"))
            return "1.bool";
        else if (type.equals("int array"))
            return ".array.i32";
        return type;
    }
}
