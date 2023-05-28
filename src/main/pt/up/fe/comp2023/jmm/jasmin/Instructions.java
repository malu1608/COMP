package pt.up.fe.comp2023.jmm.jasmin;

import org.specs.comp.ollir.CallType;
import org.specs.comp.ollir.OperationType;

public class Instructions {
    public static Limits limits = new Limits();

    //ASSIGN INSTR
    public static String iincInstr(int reg, String literal) {
        return ("iinc " + reg + " " + literal + "\n");
    }

    public static String iastore() {
        limits.updateStack(-3);
        return "iastore\n";
    }

    public static String istore(int reg) {
        limits.updateStack(-1);
        return registerInstruction("istore", reg);
    }

    public static String astore(int reg) {
        limits.updateStack(-1);
        return registerInstruction("astore", reg);
    }

    //CALL INSTR
    public static String getArrayLength() {
        limits.updateStack(0);
        return "arraylength\n";
    }

    public static String iconst(String num) {
        limits.updateStack(1);
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
        limits.updateStack(1);
        return registerInstruction("iload", reg);
    }

    private static String registerInstruction(String inst, int reg) {
        limits.updateRegisters(reg);
        if (reg >= 0 && reg <= 3) {
            inst += "_";
        } else {
            inst += " ";
        }
        return inst + reg + "\n";
    }

    public static String aload(int reg) {
        limits.updateStack(1);
        return registerInstruction("aload", reg);
    }

    public static String iaload() {
        limits.updateStack(-1);
        return "iaload\n";
    }

    public static String newInstr(String className) {
        limits.updateStack(1);
        return "new " + className + '\n';
    }

    public static String dup() {
        limits.updateStack(1);
        return "dup\n";
    }

    public static String newarrayInstr() {
        limits.updateStack(0);
        return "newarray int\n";
    }

    public static String invoke(CallType callType, String className, String methodName, String argumentsTypes, int argCount, String returnType) {
        StringBuilder jasminCode = new StringBuilder();

        limits.updateStack(-argCount);
        jasminCode.append(callType.toString() + " " + className + "/" + methodName + argumentsTypes + returnType + "\n");
        return jasminCode.toString();
    }

    public static String pop() {
        limits.updateStack(-1);
        return "pop\n";
    }

    //GOTO INSTR
    public static String gotoInstr(String label) {
        limits.updateStack(0);
        return "goto " + label + '\n';
    }

    //BRANCH INSTR
    public static String ifltInstr(String label) {
        limits.updateStack(-1);
        return "iflt " + label + '\n';
    }

    public static String ifgeInstr(String label) {
        limits.updateStack(-1);
        return "ifge " + label + '\n';
    }

    public static String if_icmpltInstr(String label) {
        limits.updateStack(-2);
        return "if_icmplt " + label + '\n';
    }

    public static String if_icmpgeInstr(String label) {
        limits.updateStack(-2);
        return "if_icmpge " + label + '\n';
    }

    public static String ifneInstr(String label) {
        limits.updateStack(-1);
        return "ifne " + label + '\n';
    }

    //PUTFIELD INSTR
    public static String field(String type, String className, String fieldName, String fieldType) {
        StringBuilder jasminCode = new StringBuilder();

        int stackDiff = type ==  "GET" ? 0 : -2;

        limits.updateStack(stackDiff);
        jasminCode.append(type.toString().toLowerCase() + "field" + " " + className + "/" + fieldName + " " + fieldType + "\n");

        return jasminCode.toString();
    }

    //UNARYOP INSTR
    public static String arithmeticOpsCode(OperationType type) {
        limits.updateStack(-1);
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