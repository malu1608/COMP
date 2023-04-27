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

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClassUnit = ollirResult.getOllirClass();
        this.ollirClassUnit = ollirClassUnit;
        this.ollirClassUnit.buildVarTables();
        String jasminCode = getJasminCode();

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
                jasminCode.append(".method " + getJasminCode(method));
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
        StringBuilder code = new StringBuilder();

        // Modifiers
        AccessModifiers accessModifier = field.getFieldAccessModifier();
        if (accessModifier != AccessModifiers.DEFAULT) {
            code.append(accessModifier.name().toLowerCase() + " ");
        }
        if (field.isStaticField())
            code.append("static ");

        if (field.isFinalField()) {
            code.append("final ");
        }

        code.append(field.getFieldName() + " ");

        String jasminField = getJasminCode(field.getFieldType());
        code.append(jasminField);

        if (field.isInitialized()){
            code.append(" = ").append(field.getInitialValue());
        }

        return code.toString();
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

    private String getJasminCode(Method method) {
        StringBuilder code = new StringBuilder();

        // Modifiers
        AccessModifiers accessModifier = method.getMethodAccessModifier();
        if (accessModifier != AccessModifiers.DEFAULT) {
            code.append(accessModifier.name().toLowerCase() + " ");
        }
        if (method.isStaticMethod())
            code.append("static ");

        if (method.isFinalMethod()) {
            code.append("final ");
        }

        code.append(method.getMethodName() + "(");

        //Parameters
        for (Element element : method.getParams()) {
            String paramCode = getJasminCode(element.getType());
            code.append(paramCode);
        }

        code.append(")");

        //Return Type
        String jasminMethodReturnType = getJasminCode(method.getReturnType());
        code.append(jasminMethodReturnType + "\n");

        //Inside method
        code.append(".limit stack 99\n" + ".limit locals 99\n");
        for (Instruction instruction : method.getInstructions()) {
            String jasminMethodInstr = getJasminCode(method, instruction);
            code.append(jasminMethodInstr);
        }

        code.append(".end method\n");

        return code.toString();
    }

    private String getJasminCode(Method method, Instruction instruction) {
        if (instruction instanceof CallInstruction) {
            CallInstruction callInstr = (CallInstruction) instruction;
            return getJasminCode(method, callInstr);
        }
        if (instruction instanceof ReturnInstruction) {
            ReturnInstruction retInstr = (ReturnInstruction) instruction;
            return getJasminCode(method, retInstr);
        }
        if (instruction instanceof AssignInstruction) {
            AssignInstruction assignInstr = (AssignInstruction) instruction;
            return getJasminCode(method, assignInstr);
        }
        if (instruction instanceof GetFieldInstruction) {
            GetFieldInstruction getFieldInstr = (GetFieldInstruction) instruction;
            return getJasminCode(method, getFieldInstr);
        }
        if (instruction instanceof PutFieldInstruction) {
            PutFieldInstruction putFieldInstr = (PutFieldInstruction) instruction;
            return getJasminCode(method, putFieldInstr);
        }
        throw new NotImplementedException(instruction.getClass());
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
        StringBuilder code = new StringBuilder();

        String returnType = ((ClassType) (callInstruction.getReturnType())).getName();
        code.append("new " + getQualifiedPath(returnType) + "\n" + "dup\n");

        return code.toString();
    }

    private String getJasminCodeInvokeStatic(CallInstruction callInstruction, Method method) {
        StringBuilder code = new StringBuilder();

        HashMap<String, Descriptor> map = method.getVarTable();
        ArrayList<Element> parameters = callInstruction.getListOfOperands();

        for (Element parameter : parameters) {
            code.append(getLoad(map, parameter));
        }
        code.append("invokestatic ");

        String methodClass = ((Operand) callInstruction.getFirstArg()).getName();
        code.append(getQualifiedPath(methodClass) + "/");
        code.append( ((LiteralElement) (callInstruction.getSecondArg()) ).getLiteral().replace("\"", ""));
        code.append("(" + getArgCode(callInstruction.getListOfOperands()) + ")");
        code.append(getJasminCode(callInstruction.getReturnType()) + "\n");

        return code.toString();
    }

    private String getLoad(HashMap<String, Descriptor> map, Element element) {
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

    private String getArgCode(ArrayList<Element> operands) {
        StringBuilder code = new StringBuilder();

        for (Element arg : operands) {
            code.append(getJasminCode(arg.getType()));
        }

        return code.toString();
    }

    private String getJasminCodeInvokeSpecial(CallInstruction callInstruction, Method method) {
        StringBuilder code = new StringBuilder();

        HashMap<String, Descriptor> map = method.getVarTable();
        ArrayList<Element> params = callInstruction.getListOfOperands();

        for (Element param : params) {
            code.append(getLoad(map, param));
        }

        code.append(getLoad(map, callInstruction.getFirstArg()));

        String className = getQualifiedPath(((ClassType) callInstruction.getFirstArg().getType()).getName());
        code.append("invokespecial " + className + "/");

        String methodName = ((LiteralElement) callInstruction.getSecondArg()).getLiteral().replace("\"", "");
        code.append(methodName.replace("\"", ""));
        code.append("(" + getArgCode(params) + ")");

        Type returnType = callInstruction.getReturnType();
        code.append(getJasminCode(returnType) + "\n");

        return code.toString();
    }


    private String getJasminCodeInvokeVirtual(CallInstruction callInstruction, Method method) {
        StringBuilder code = new StringBuilder();

        HashMap<String, Descriptor> map = method.getVarTable();
        ArrayList<Element> parameters = callInstruction.getListOfOperands();

        code.append(getLoad(map, callInstruction.getFirstArg()));
        for (Element parameter : parameters) {
            code.append(getLoad(map, parameter));
        }

        String className = getQualifiedPath(((ClassType) callInstruction.getFirstArg().getType()).getName());
        code.append("invokevirtual ").append(className).append("/");

        String methodName = ((LiteralElement) callInstruction.getSecondArg()).getLiteral().replace("\"", "");
        code.append(methodName.replace("\"", ""));
        code.append(getArgCode(parameters));

        Type returnType = callInstruction.getReturnType();
        code.append(getJasminCode(returnType) + "\n");

        return code.toString();
    }

    private String getJasminCode(Method method, ReturnInstruction returnInstruction) {
        if (!returnInstruction.hasReturnValue()) {
            return "return\n";
        }

        StringBuilder code = new StringBuilder();

        Element result = returnInstruction.getOperand();
        code.append(getLoad(method.getVarTable(), result));

        ElementType elementType = result.getType().getTypeOfElement();
        if(elementType == ElementType.INT32 || elementType == ElementType.BOOLEAN) {
            code.append("ireturn\n");
        } else {
            code.append("areturn\n");
        }

        return code.toString();
    }

    private String getJasminCode(Method method, AssignInstruction assignInstruction) {
        StringBuilder code = new StringBuilder();

        Instruction rhs = assignInstruction.getRhs();
        Element dest = assignInstruction.getDest();

        String rhsString = getOpCode(method, rhs);

        String val = getStoreCode(dest, rhsString, method.getVarTable());
        code.append(val);

        return code.toString();
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
        return getLoad(map, element);
    }

    private String getUnaryOperCode(HashMap<String, Descriptor> map, UnaryOpInstruction unaryOpInstruction) {
        StringBuilder code = new StringBuilder();

        Element element = unaryOpInstruction.getOperand();
        Operation operation = unaryOpInstruction.getOperation();

        if (operation.getOpType() == OperationType.NOTB) {
            code.append(iconst("1") + getLoad(map, element) + "isub\n");
        }

        return code.toString();
    }

    private String getBinaryOperCode(HashMap<String, Descriptor> map, BinaryOpInstruction instruction) {
        StringBuilder code = new StringBuilder();
        Element leftOperand = instruction.getLeftOperand();
        Element rightOperand = instruction.getRightOperand();

        String leftLoad = getLoad(map, leftOperand);
        String rightLoad = getLoad(map, rightOperand);
        OperationType type = instruction.getOperation().getOpType();

        code.append(leftLoad);
        code.append(rightLoad);
        code.append(getArithmeticCode(type));

        return code.toString();
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
            return rhsString + load_store(register, "istore");
        } else if (type == ElementType.OBJECTREF || type == ElementType.THIS || type == ElementType.ARRAYREF) {
            return rhsString + load_store(register, "astore");
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
        StringBuilder code = new StringBuilder();

        Element firstOperand = getFieldInstruction.getFirstOperand();
        code.append(getLoad(method.getVarTable(), firstOperand));

        Element secondOperand = getFieldInstruction.getSecondOperand();
        code.append(getFieldOpCode(firstOperand, secondOperand, "getfield "));

        return code.toString();
    }

    private String getJasminCode(Method method, PutFieldInstruction putFieldInstruction) {
        StringBuilder code = new StringBuilder();

        Element firstOperand = putFieldInstruction.getFirstOperand();
        code.append(getLoad(method.getVarTable(), firstOperand));

        Element thirdOperand = putFieldInstruction.getThirdOperand();
        code.append(getLoad(method.getVarTable(), thirdOperand));

        Element secondOperand = putFieldInstruction.getSecondOperand();
        code.append(getFieldOpCode(firstOperand, secondOperand, "putfield "));

        return code.toString();
    }

    private String getFieldOpCode(Element classElement, Element fieldElement, String fieldOp) {
        StringBuilder code = new StringBuilder();

        String fieldName = ((Operand)fieldElement).getName();
        String className = getQualifiedPath(((ClassType)classElement.getType()).getName());

        code.append(fieldOp + className + "/" + fieldName + " " + getJasminCode(fieldElement.getType()) + "\n");

        return code.toString();
    }

}