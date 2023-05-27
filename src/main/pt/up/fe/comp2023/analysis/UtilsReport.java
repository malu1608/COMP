package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
public class UtilsReport {
    private static Report baseReport(JmmNode at, ReportType type, Stage stage, String message) {
        return new Report(type, stage, Integer.parseInt(at.get("lineStart")), Integer.parseInt(at.get("colStart")), message);
    }

    public static Report cyclicInheritance(JmmNode at, String type) {
        String message = "cyclic inheritance involving " +
                type;
        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message);
    }

    public static Report nonStaticInStaticContext(JmmNode at, String type) {
        String message = "non-static method " +
                type +
                " cannot be referenced from a static context";

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message);
    }

    public static Report symbolAlreadyDefinedReport(JmmNode at, String symbolType, String symbol, String locationType, String location) {
        StringBuilder message = new StringBuilder();
        message.append(symbolType);
        message.append(" ");
        message.append(symbol);
        message.append(" is already defined in ");
        message.append(locationType);
        message.append(" ");
        message.append(location);

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report cannotBeDereferencedReport(JmmNode at, String type) {
        String message = "incompatible types: " +
                type +
                " cannot be dereferenced";
        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message);
    }

    public static Report typeMismatchReport(JmmNode at, String actual, String expected) {
        String message = "incompatible types: " +
                actual +
                " cannot be converted to " +
                expected;
        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message);
    }

    public static Report operatorCannotBeAppliedReport(JmmNode at, String operator, String left, String right) {
        String message = "operator '" +
                operator +
                "' cannot be applied to '" +
                left +
                "' and '" +
                right +
                "'";
        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message);
    }

    public static Report operatorCannotBeAppliedReport(JmmNode at, String operator, String type) {
        String message = "operator '" +
                operator +
                "' cannot be applied to '" +
                type +
                "'";
        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message);
    }

    public static Report createArrayRequiredReport(JmmNode at, String foundType) {
        String message = "array required, but '" +
                foundType +
                "' found";
        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message);
    }

    public static Report cannotFindSymbolReport(JmmNode at, String symbol) {
        String message = "cannot find symbol '" +
                symbol +
                "'";

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message);
    }

    public static Report returnTypeMismatchReport(JmmNode at, String expected, String found) {
        String message = "return type mismatch: expected '" +
                expected +
                "' but found '" +
                found +
                "'";

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message);
    }

    public static Report typeConversionErrorReport(JmmNode at, String left, String right) {
        String message = "incompatible types: " +
                left +
                " could not be converted to " +
                right;
        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message);
    }

    public static Report ambiguousMethodCallReport(JmmNode at, String methodname) {
        StringBuilder message = new StringBuilder();
        message.append("reference to ");
        message.append(methodname);
        message.append(" is ambiguous");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report alreadyImported(JmmNode at, String lastName, String other) {
        StringBuilder message = new StringBuilder();
        message.append("a type with the same simple name '");
        message.append(lastName);
        message.append("' has already been imported from '");
        message.append(other);
        message.append("'");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

}
