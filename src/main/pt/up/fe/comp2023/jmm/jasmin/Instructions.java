package pt.up.fe.comp2023.jmm.jasmin;

import org.specs.comp.ollir.CallType;
import org.specs.comp.ollir.OperationType;

public class Instructions {

    //ASSIGN INSTR
    public static String iincInstr(int reg, String literal) {
        return ("iinc " + reg + " " + literal + "\n");
    }

    public static String iastore() {
        return "iastore\n";
    }

    public static String istore(int reg) {
        return registerInstruction("istore", reg);
    }

    public static String astore(int reg) {
        return registerInstruction("astore", reg);
    }

    //CALL INSTR
    public static String getArrayLength() {
        return "arraylength\n";
    }

    public static String iconst(String num) {
        int integer = Integer.parseInt(num);
        String instr = "";
        if (integer == -1) {
            instr = "iconst_m1";
        } else if (integer <= 5 && integer >= 0) {
            instr = "iconst_" + num;
        } else if (integer <= 127 && integer >= -128) {
            instr = "bipush " + num;
        } else if (integer <= 32767 && integer >= -32768) {
            instr = "sipush " + num;
        } else {
            instr = "ldc " + num;
        }
        return instr + "\n";
    }

    public static String iload(int reg) {
        return registerInstruction("iload", reg);
    }

    private static String registerInstruction(String inst, int reg) {
        if (reg >= 0 && reg <= 3) {
            inst += "_";
        } else {
            inst += " ";
        }
        return inst + reg + "\n";
    }

    public static String aload(int reg) {
        return registerInstruction("aload", reg);
    }

    public static String iaload() {
        return "iaload\n";
    }

    public static String newInstr(String className) {
        return "new " + className + '\n';
    }

    public static String dup() {
        return "dup\n";
    }

    public static String newarrayInstr() {
        return "newarray int\n";
    }

    public static String invoke(CallType callType, String className, String methodName, String argumentsTypes, int argCount, String returnType) {
        StringBuilder jasminCode = new StringBuilder();

        jasminCode.append(callType.toString() + " " + className + "/" + methodName + argumentsTypes + returnType + "\n");
        return jasminCode.toString();
    }

    public static String pop() {
        return "pop\n";
    }

    //GOTO INSTR
    public static String gotoInstr(String label) {
        return "goto " + label + '\n';
    }

    //BRANCH INSTR
    public static String ifltInstr(String label) {
        return "iflt " + label + '\n';
    }

    public static String ifgeInstr(String label) {
        return "ifge " + label + '\n';
    }

    public static String if_icmpltInstr(String label) {
        return "if_icmplt " + label + '\n';
    }

    public static String if_icmpgeInstr(String label) {
        return "if_icmpge " + label + '\n';
    }

    public static String ifneInstr(String label) {
        return "ifne " + label + '\n';
    }

    //PUTFIELD INSTR
    public static String field(String type, String className, String fieldName, String fieldType) {
        StringBuilder jasminCode = new StringBuilder();

        int stackDiff = type ==  "GET" ? 0 : -2;

        jasminCode.append(type.toString().toLowerCase() + "field" + " " + className + "/" + fieldName + " " + fieldType + "\n");

        return jasminCode.toString();
    }

    //UNARYOP INSTR
    public static String arithmeticOpsCode(OperationType type) {
        switch (type) {
            case ADD:
                return "iadd\n";
            case SUB:
                return "isub\n";
            case MUL, ANDB:
                return "imul\n";
            case DIV:
                return "idiv\n";
            case OR:
                return "ior\n";
            default: return "";
        }
    }
}