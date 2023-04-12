package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
public class UtilsReport {
    private static Report baseReport(JmmNode at, ReportType type, Stage stage, String message) {
        return new Report(type, stage, Integer.parseInt(at.get("line")), Integer.parseInt(at.get("col")), message);
    }

    public static Report cyclicInheritance(JmmNode at, String type) {
        String message = "cyclic inheritance involving " +
                type;
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

    public static Report cannotBeDereferencedReport(JmmNode at, String type) {
        String message = "incompatible types: " +
                type +
                " cannot be dereferenced";
        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message);
    }

    public static Report NonStaticInStaticContext(JmmNode at, String type) {
        String message = "non-static method " +
                type +
                " cannot be referenced from a static context";

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
                "and" +
                right +
                "'";
        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message);
    }
}
