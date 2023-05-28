package pt.up.fe.comp2023.jmm.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;

import static org.specs.comp.ollir.CallType.*;
import static org.specs.comp.ollir.ElementType.*;
import static org.specs.comp.ollir.InstructionType.*;

public class MyJasmin implements JasminBackend {

    private ClassUnit ollirClassUnit;
    private Labels labels;

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClassUnit = ollirResult.getOllirClass();
        this.ollirClassUnit = ollirClassUnit;
        this.ollirClassUnit.buildVarTables();
        this.labels = new Labels();
        String jasminCode = getJasminCode();

        if (ollirResult.getConfig().getOrDefault("debug", "false").equals("true")) {
            System.out.println("JASMIN CODE:");
            System.out.println(jasminCode);
        }

        return new JasminResult(jasminCode);
    }

    private String getJasminCode() {
        StringBuilder jasminCode = new StringBuilder();

        // Class name
        jasminCode.append(".class public " + this.ollirClassUnit.getClassName() + "\n");

        // Super class name
        String superClass = this.ollirClassUnit.getSuperClass();
        if (superClass == null) {
            superClass = "java/lang/Object";
        } else {
            superClass = getQualifiedPath(superClass);
        }
        jasminCode.append(".super ").append(superClass).append("\n\n");

        // Field name
        for (Field field : this.ollirClassUnit.getFields()) {
            jasminCode.append(".field " + getJasminCode(field) + "\n");
        }

        // Super constructor
        jasminCode.append(".method public <init>()V\n" +
                "aload_0\n" +
                "invokespecial " + superClass + "/<init>()V\n" +
                "return\n" +
                ".end method\n\n");

        // Methods
        for (Method method : this.ollirClassUnit.getMethods()) {
            if (!method.isConstructMethod()) {
                jasminCode.append(".method " + getJasminCode(labels, method));
            }
        }

        return jasminCode.toString();
    }

    private String getQualifiedPath(String superClass) {
        for (String importString : this.ollirClassUnit.getImports()) {
            String[] splitImports = importString.split("\\.");

            String path;
            if (splitImports.length != 0) {
                path = splitImports[splitImports.length - 1];
            } else {
                path = importString;
            }

            if (path.equals(superClass)) {
                return importString.replace('.', '/');
            }
        }
        return this.ollirClassUnit.getClassName().replace("\\.", "/");
    }

    private String getJasminCode(Field field) {
        StringBuilder jasminCode = new StringBuilder();

        // Modifiers
        AccessModifiers accessModifier = field.getFieldAccessModifier();
        if (accessModifier != AccessModifiers.DEFAULT) {
            jasminCode.append(accessModifier.name().toLowerCase() + " ");
        }
        if (field.isStaticField())
            jasminCode.append("static ");

        if (field.isFinalField()) {
            jasminCode.append("final ");
        }

        jasminCode.append(field.getFieldName() + " ");

        String jasminField = getJasminCode(field.getFieldType());
        jasminCode.append(jasminField);

        if (field.isInitialized()){
            jasminCode.append(" = ").append(field.getInitialValue());
        }

        return jasminCode.toString();
    }

    private String getJasminCode(Type fieldType) {
        if (fieldType instanceof ArrayType) {
            return "[" + getJasminCode(((ArrayType) fieldType).getElementType());
        }
        if (fieldType instanceof ClassType) {
            return "L" + getQualifiedPath(((ClassType) fieldType).getName()) + ";";
        }
        return getJasminCode(fieldType.getTypeOfElement());
    }

    private String getJasminCode(ElementType elementType) {
        if (elementType == INT32) {
            return "I";
        } else if (elementType == BOOLEAN) {
            return "Z";
        } else if (elementType == STRING) {
            return "Ljava/lang/String;";
        } else if (elementType == VOID) {
            return "V";
        }
        throw new NotImplementedException(elementType);
    }

    private String getJasminCode(Labels labels, Method method) {
        StringBuilder jasminCode = new StringBuilder();

        // Modifiers
        AccessModifiers accessModifier = method.getMethodAccessModifier();
        if (accessModifier != AccessModifiers.DEFAULT) {
            jasminCode.append(accessModifier.name().toLowerCase() + " ");
        }
        if (method.isStaticMethod())
            jasminCode.append("static ");

        if (method.isFinalMethod()) {
            jasminCode.append("final ");
        }

        jasminCode.append(method.getMethodName() + "(");

        //Parameters
        for (Element element : method.getParams()) {
            String paramCode = getJasminCode(element.getType());
            jasminCode.append(paramCode);
        }

        jasminCode.append(")");

        //Return Type
        String jasminMethodReturnType = getJasminCode(method.getReturnType());
        jasminCode.append(jasminMethodReturnType + "\n");

        //Inside method
        StringBuilder jasminMethodCode = new StringBuilder();
        InstructionsJasminCode buildInstr = new InstructionsJasminCode(labels, method);

        Instructions.limits.reset();
        Instructions.limits.registersUntil(method.getParams().size() + 1);

        HashMap<String, Instruction> labelMap = method.getLabels();

        for (Instruction instruction : method.getInstructions()) {
            for (String label : labelMap.keySet()) {
                if (labelMap.get(label) == instruction) {
                    jasminMethodCode.append(label + ":\n");
                }
            }

            String jasminMethodInstr = buildInstr.getJasminCode(instruction);
            jasminMethodCode.append(jasminMethodInstr);

            if (instruction.getInstType() == InstructionType.CALL) {
                ElementType returnType = ((CallInstruction) instruction).getReturnType().getTypeOfElement();
                if (returnType != ElementType.VOID || ((CallInstruction) instruction).getInvocationType() == CallType.invokespecial) {
                    jasminMethodCode.append(Instructions.pop());
                }
            }

        }

        jasminCode.append(".limit stack ").append(Instructions.limits.getStackLimit()).append("\n");
        jasminCode.append(".limit locals ").append(Instructions.limits.getLocalsLimit()).append("\n");

        jasminCode.append(jasminMethodCode + ".end method\n");

        return jasminCode.toString();
    }

    private String getJasminCode(Method method, CallInstruction callInstruction) {
        if (callInstruction.getInvocationType() == NEW) {
            return getJasminCodeNew(callInstruction, method);
        } else if (callInstruction.getInvocationType() == invokestatic) {
            return getJasminCodeInvokeStatic(callInstruction, method);
        } else if (callInstruction.getInvocationType() == invokespecial) {
            return getJasminCodeInvokeSpecial(callInstruction, method);
        } else if (callInstruction.getInvocationType() == invokevirtual) {
            return getJasminCodeInvokeVirtual(callInstruction, method);
        } else {
            throw new NotImplementedException(callInstruction.getInvocationType());
        }
    }

    private String getJasminCodeNew(CallInstruction callInstruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();

        String returnType = ((ClassType) (callInstruction.getReturnType())).getName();
        jasminCode.append("new " + getQualifiedPath(returnType) + "\n" + "dup\n");

        return jasminCode.toString();
    }

    private String getJasminCodeInvokeStatic(CallInstruction callInstruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();

        HashMap<String, Descriptor> map = method.getVarTable();
        ArrayList<Element> parameters = callInstruction.getListOfOperands();

        for (Element parameter : parameters) {
            jasminCode.append(getLoadCode(map, parameter));
        }
        jasminCode.append("invokestatic ");

        String methodClass = ((Operand) callInstruction.getFirstArg()).getName();
        jasminCode.append(getQualifiedPath(methodClass) + "/");
        jasminCode.append( ((LiteralElement) (callInstruction.getSecondArg()) ).getLiteral().replace("\"", ""));
        jasminCode.append("(" + getArgsCode(callInstruction.getListOfOperands()) + ")");
        jasminCode.append(getJasminCode(callInstruction.getReturnType()) + "\n");

        return jasminCode.toString();
    }

    private String getLoadCode(HashMap<String, Descriptor> map, Element element) {
        ElementType type = element.getType().getTypeOfElement();

        // instructions iconst
        if (element.isLiteral()) {
            return iconst(((LiteralElement) element).getLiteral());
        }

        // instructions iload
        if (type == ElementType.INT32 || type == ElementType.STRING || type == ElementType.BOOLEAN) {
            int virtualReg = map.get(((Operand) element).getName()).getVirtualReg();
            return load_store(virtualReg, "iload");
        }

        // instructions aload
        if (type == ElementType.OBJECTREF || type == ElementType.ARRAYREF || type == ElementType.THIS) {
            int virtualReg = map.get(((Operand) element).getName()).getVirtualReg();
            return load_store(virtualReg, "aload");
        }
        return "";
    }

    private String iconst(String number) {
        String instr = "";

        int integer = Integer.parseInt(number);

        if (integer == -1) {
            instr = "iconst_m1";
        } else if (integer <= 5 && integer >= 0) {
            instr = ("iconst_" + number);
        } else if (integer <= 127 && integer >= -128) {
            instr = ("bipush " + number);
        } else if (integer <= 32767 && integer >= -32768) {
            instr = ("sipush " + number);
        } else {
            instr = ("ldc " + number);
        }

        return (instr + "\n");
    }

    private String getArgsCode(ArrayList<Element> operands) {
        StringBuilder jasminCode = new StringBuilder();

        for (Element arg : operands) {
            jasminCode.append(getJasminCode(arg.getType()));
        }

        return jasminCode.toString();
    }

    private String getJasminCodeInvokeSpecial(CallInstruction callInstruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();

        HashMap<String, Descriptor> map = method.getVarTable();
        ArrayList<Element> params = callInstruction.getListOfOperands();

        for (Element param : params) {
            jasminCode.append(getLoadCode(map, param));
        }

        jasminCode.append(getLoadCode(map, callInstruction.getFirstArg()));

        String className = getQualifiedPath(((ClassType) callInstruction.getFirstArg().getType()).getName());
        jasminCode.append("invokespecial " + className + "/");

        String methodName = ((LiteralElement) callInstruction.getSecondArg()).getLiteral().replace("\"", "");
        jasminCode.append(methodName.replace("\"", ""));
        jasminCode.append("(" + getArgsCode(params) + ")");

        Type returnType = callInstruction.getReturnType();
        jasminCode.append(getJasminCode(returnType) + "\n");

        return jasminCode.toString();
    }


    private String getJasminCodeInvokeVirtual(CallInstruction callInstruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();

        HashMap<String, Descriptor> map = method.getVarTable();
        ArrayList<Element> parameters = callInstruction.getListOfOperands();

        jasminCode.append(getLoadCode(map, callInstruction.getFirstArg()));
        for (Element parameter : parameters) {
            jasminCode.append(getLoadCode(map, parameter));
        }

        String className = getQualifiedPath(((ClassType) callInstruction.getFirstArg().getType()).getName());
        jasminCode.append("invokevirtual ").append(className).append("/");

        String methodName = ((LiteralElement) callInstruction.getSecondArg()).getLiteral().replace("\"", "");
        jasminCode.append(methodName.replace("\"", ""));
        jasminCode.append("(" + getArgsCode(parameters) + ")");

        Type returnType = callInstruction.getReturnType();
        jasminCode.append(getJasminCode(returnType) + "\n");

        return jasminCode.toString();
    }

    private String getJasminCode(Method method, ReturnInstruction returnInstruction) {
        if (!returnInstruction.hasReturnValue()) {
            return "return\n";
        }

        StringBuilder jasminCode = new StringBuilder();

        Element result = returnInstruction.getOperand();
        jasminCode.append(getLoadCode(method.getVarTable(), result));

        ElementType elementType = result.getType().getTypeOfElement();
        if(elementType == ElementType.INT32 || elementType == ElementType.BOOLEAN) {
            jasminCode.append("ireturn\n");
        } else {
            jasminCode.append("areturn\n");
        }

        return jasminCode.toString();
    }

    private String getJasminCode(Method method, AssignInstruction assignInstruction) {
        StringBuilder jasminCode = new StringBuilder();

        Instruction rhs = assignInstruction.getRhs();
        Element dest = assignInstruction.getDest();

        String rhsString = getOpCode(method, rhs);

        String val = getStoreCode(dest, rhsString, method.getVarTable());
        jasminCode.append(val);

        return jasminCode.toString();
    }

    private String getOpCode(Method method, Instruction instr) {
        if (instr.getInstType() == NOPER) {
            SingleOpInstruction singleOpInstruction = (SingleOpInstruction) instr;
            return getNoperCode(method.getVarTable(), singleOpInstruction);
        } else if (instr.getInstType() == UNARYOPER){
            UnaryOpInstruction unaryOpInstruction = (UnaryOpInstruction) instr;
            return getUnaryOperCode(method.getVarTable(), unaryOpInstruction);
        } else if (instr.getInstType() == BINARYOPER){
            BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) instr;
            return getBinaryOperCode(method.getVarTable(), binaryOpInstruction);
        } else if (instr.getInstType() == CALL){
            CallInstruction callInstruction = (CallInstruction) instr;
            return getJasminCode(method, callInstruction);
        } else if (instr.getInstType() == GETFIELD){
            GetFieldInstruction getFieldInstruction = (GetFieldInstruction) instr;
            return getJasminCode(method, getFieldInstruction);
        } else {
            return "\n";
        }
    }

    private String getNoperCode(HashMap<String, Descriptor> map, SingleOpInstruction singleOpInstruction) {
        Element element = singleOpInstruction.getSingleOperand();
        return getLoadCode(map, element);
    }

    private String getUnaryOperCode(HashMap<String, Descriptor> map, UnaryOpInstruction unaryOpInstruction) {
        StringBuilder jasminCode = new StringBuilder();

        Element element = unaryOpInstruction.getOperand();
        Operation operation = unaryOpInstruction.getOperation();

        if (operation.getOpType() == OperationType.NOTB) {
            jasminCode.append(iconst("1") + getLoadCode(map, element) + "isub\n");
        }

        return jasminCode.toString();
    }

    private String getBinaryOperCode(HashMap<String, Descriptor> map, BinaryOpInstruction instruction) {
        StringBuilder jasminCode = new StringBuilder();
        Element leftOperand = instruction.getLeftOperand();
        Element rightOperand = instruction.getRightOperand();

        String leftLoad = getLoadCode(map, leftOperand);
        String rightLoad = getLoadCode(map, rightOperand);
        OperationType type = instruction.getOperation().getOpType();

        jasminCode.append(leftLoad);
        jasminCode.append(rightLoad);
        jasminCode.append(getArithmeticCode(type));

        return jasminCode.toString();
    }

    private String getArithmeticCode(OperationType operationType) {
        switch (operationType) {
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
            default:
                return "";
        }
    }

    private String getStoreCode(Element dest, String rhsString, HashMap<String, Descriptor> map) {
        ElementType type = dest.getType().getTypeOfElement();
        int register = map.get(((Operand) dest).getName()).getVirtualReg();

        if (type == ElementType.INT32 || type == ElementType.STRING || type == ElementType.BOOLEAN) {
            return (rhsString + load_store(register, "istore"));
        } else if (type == ElementType.OBJECTREF || type == ElementType.THIS || type == ElementType.ARRAYREF) {
            return (rhsString + load_store(register, "astore"));
        }
        return "";
    }

    private String load_store(int reg, String instr) {
        if (reg > 3 || reg < 0) {
            instr = instr + " ";
        } else {
            instr = instr + "_";
        }

        return (instr + reg + "\n");
    }

    private String getJasminCode(Method method, GetFieldInstruction getFieldInstruction) {
        StringBuilder jasminCode = new StringBuilder();

        Element firstOperand = getFieldInstruction.getFirstOperand();
        jasminCode.append(getLoadCode(method.getVarTable(), firstOperand));

        Element secondOperand = getFieldInstruction.getSecondOperand();
        jasminCode.append(getFieldOpCode(firstOperand, secondOperand, "getfield "));

        return jasminCode.toString();
    }

    private String getJasminCode(Method method, PutFieldInstruction putFieldInstruction) {
        StringBuilder jasminCode = new StringBuilder();

        Element firstOperand = putFieldInstruction.getFirstOperand();
        jasminCode.append(getLoadCode(method.getVarTable(), firstOperand));

        Element thirdOperand = putFieldInstruction.getThirdOperand();
        jasminCode.append(getLoadCode(method.getVarTable(), thirdOperand));

        Element secondOperand = putFieldInstruction.getSecondOperand();
        jasminCode.append(getFieldOpCode(firstOperand, secondOperand, "putfield "));

        return jasminCode.toString();
    }

    private String getFieldOpCode(Element classElement, Element fieldElement, String fieldOp) {
        StringBuilder jasminCode = new StringBuilder();

        String fieldName = ((Operand)fieldElement).getName();
        String className = getQualifiedPath(((ClassType)classElement.getType()).getName());

        jasminCode.append(fieldOp + className + "/" + fieldName + " " + getJasminCode(fieldElement.getType()) + "\n");

        return jasminCode.toString();
    }

    public static String getQualifiedPath(ClassUnit ollirClass, String className) {
        for (String importString : ollirClass.getImports()) {
            var splitImports = importString.split("\\.");
            String lastName;
            if (splitImports.length == 0) {
                lastName = importString;
            } else {
                lastName = splitImports[splitImports.length - 1];
            }
            if (lastName.equals(className)) {
                return importString.replace('.', '/');
            }
        }
        return ollirClass.getClassName().replace("\\.", "/");
    }

    public static String getJasminCode(ClassUnit ollirClass, Type type) {
        if (type instanceof ArrayType) {
            return "[" + getJasminType(((ArrayType)type).getElementType().getTypeOfElement());
        }
        if (type instanceof ClassType) {
            return "L" + getQualifiedPath(ollirClass, ((ClassType) type).getName()) + ";";
        }
        return getJasminType(type.getTypeOfElement());
    }

    public static String getJasminType(ElementType type) {
        switch (type) {
            case INT32:
                return "I";
            case STRING:
                return "Ljava/lang/String;";
            case VOID:
                return "V";
            case BOOLEAN:
                return "Z";
            default: throw new NotImplementedException(type);
        }
    }
}