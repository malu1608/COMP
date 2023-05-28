package pt.up.fe.comp2023.jmm.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.ArrayList;

public class InstructionsJasminCode {
    private Method method;
    private Labels labels;

    public InstructionsJasminCode(Labels labels, Method method){
        this.method = method;
        this.labels = labels;
    }

    public String getJasminCode(Instruction instruction) {
        switch (instruction.getInstType()) {
            case ASSIGN:
                return getJasminCode((AssignInstruction) instruction);
            case CALL:
                return getJasminCode((CallInstruction) instruction);
            case GOTO:
                return getJasminCode((GotoInstruction) instruction);
            case BRANCH:
                return getJasminCode((CondBranchInstruction) instruction);
            case RETURN:
                return getJasminCode((ReturnInstruction) instruction);
            case PUTFIELD:
                return getJasminCode((PutFieldInstruction) instruction);
            case GETFIELD:
                return getJasminCode((GetFieldInstruction) instruction);
            case UNARYOPER:
                return getJasminCode((UnaryOpInstruction) instruction);
            case BINARYOPER:
                return getJasminCode((BinaryOpInstruction) instruction);
            case NOPER:
                return getJasminCode((SingleOpInstruction) instruction);
            default:
                throw new UnsupportedOperationException("Invalid instruction -> not supported: " + instruction.getInstType());
        }
    }

    //Assign instructions
    private String getJasminCode(AssignInstruction instruction) {
        StringBuilder code = new StringBuilder();

        Element dest = instruction.getDest();
        Instruction rhs = instruction.getRhs();

        if (rhs.getInstType() == InstructionType.BINARYOPER) {
            BinaryOpInstruction expression = (BinaryOpInstruction) rhs;

            if (expression.getOperation().getOpType() == OperationType.ADD || expression.getOperation().getOpType() == OperationType.SUB) {

                String sign;
                if (expression.getOperation().getOpType() == OperationType.ADD) {
                    sign = "";
                } else {
                    sign = "-";
                }

                int register = registerNum(dest);
                if (!expression.getLeftOperand().isLiteral() && expression.getRightOperand().isLiteral()) { // k=k+0
                    if (((Operand) expression.getLeftOperand()).getName().equals(((Operand) dest).getName())) {
                        String literal = sign + ((LiteralElement) expression.getRightOperand()).getLiteral();
                        return Instructions.iincInstr(register, literal);
                    }
                } else if (expression.getLeftOperand().isLiteral() && !expression.getRightOperand().isLiteral()) { // k=0+k
                    if (((Operand) expression.getRightOperand()).getName().equals(((Operand) dest).getName())) {
                        String literal = sign + ((LiteralElement) expression.getLeftOperand()).getLiteral();
                        return Instructions.iincInstr(register, literal);
                    }
                }
            }

        }
        String rhsString = getJasminCode(rhs);
        //store to lhs
        String val = store(dest, rhsString);
        code.append(val);

        return code.toString();
    }

    private String store(Element lhs, String rhs) {
        ElementType type = lhs.getType().getTypeOfElement();

        if (type == ElementType.INT32 || type == ElementType.BOOLEAN || type == ElementType.STRING) {
            ElementType variableType = getElement(lhs).getVarType().getTypeOfElement();
            if (variableType == ElementType.ARRAYREF) {
                return storeArray(lhs, rhs);
            }
            return rhs + Instructions.istore(registerNum(lhs));
        } else if (type == ElementType.OBJECTREF || type == ElementType.THIS || type == ElementType.ARRAYREF) {
            return rhs + Instructions.astore(registerNum(lhs));
        }

        return "";
    }

    private String storeArray(Element element, String rhs) {
        return getArray(element) + rhs + Instructions.iastore();
    }

    //Call instructions
    private String getJasminCode(CallInstruction instruction) {
        switch (instruction.getInvocationType()) {
            case arraylength:
                return getArrayLengthCode(instruction);
            case NEW:
                return getNewCallCode(instruction);
            case invokestatic, invokevirtual, invokespecial:
                return getInvokeCode(instruction);
            default:
                throw new NotImplementedException(instruction.getInvocationType());
        }
    }

    private String getArrayLengthCode(CallInstruction instruction) {
        return ( load(instruction.getFirstArg()) + Instructions.getArrayLength() );
    }

    private String load(Element element) {
        ElementType type = element.getType().getTypeOfElement();

        // iconst
        if (element.isLiteral()) {
            return Instructions.iconst(((LiteralElement) element).getLiteral());
        }

        // iload
        if (type == ElementType.INT32 || type == ElementType.STRING || type == ElementType.BOOLEAN) {
            int reg = registerNum(element);
            ElementType varType = getElement(element).getVarType().getTypeOfElement();
            if (varType == ElementType.ARRAYREF) {
                return loadArray(element);
            }
            return Instructions.iload(reg);
        }

        // aload
        if (type == ElementType.THIS || type == ElementType.OBJECTREF  || type == ElementType.ARRAYREF) {
            int reg = registerNum(element);
            return Instructions.aload(reg);
        }

        throw new NotImplementedException(type);
    }

    private int registerNum(Element element) {
        return getElement(element).getVirtualReg();
    }

    private Descriptor getElement(Element element) {
        Descriptor searchElement = method.getVarTable().get(((Operand) element).getName());
        return searchElement;
    }

    private String loadArray(Element element) {
        String arrayString = getArray(element) + Instructions.iaload();
        return arrayString;
    }

    private String getArray(Element element) {
        int arrayReg = registerNum(element);
        Element indexOp = ((ArrayOperand) element).getIndexOperands().get(0);
        int indexReg = registerNum(indexOp);
        return Instructions.aload(arrayReg) + Instructions.iload(indexReg);
    }

    private String getNewCallCode(CallInstruction instruction) {
        StringBuilder jasminCode = new StringBuilder();

        ElementType type = instruction.getReturnType().getTypeOfElement();
        if (type != ElementType.ARRAYREF) {
            String returnType = ((ClassType) instruction.getReturnType()).getName();
            jasminCode.append(Instructions.newInstr(getQualifiedPath(returnType)) + Instructions.dup());
        } else {
            jasminCode.append(load(instruction.getListOfOperands().get(0)));
            jasminCode.append(Instructions.newarrayInstr());
        }
        return jasminCode.toString();
    }

    private String getQualifiedPath(String className) {
        String qualifiedPath = MyJasmin.getQualifiedPath(this.method.getOllirClass(), className);
        return qualifiedPath;
    }

    private String getInvokeCode(CallInstruction instruction) {
        StringBuilder jasminCode = new StringBuilder();

        CallType callType = instruction.getInvocationType();
        if (callType != CallType.invokestatic) {
            jasminCode.append(load(instruction.getFirstArg()));
        }

        ArrayList<Element> parameters = instruction.getListOfOperands();
        jasminCode.append(getLoadParametersCode(parameters));

        String className;
        if (instruction.getInvocationType() != CallType.invokestatic) {
            className = getQualifiedPath(((ClassType) instruction.getFirstArg().getType()).getName());
        } else {
            className = getQualifiedPath(((Operand) instruction.getFirstArg()).getName());
        }

        String methodName = ((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", "");
        Type returnType = instruction.getReturnType();
        jasminCode.append(Instructions.invoke(callType, className, methodName, getArgumentTypes(parameters), parameters.size(), getJasminType(returnType)));

        return jasminCode.toString();
    }

    private String getLoadParametersCode(ArrayList<Element> parameters) {
        StringBuilder jasminCode = new StringBuilder();

        for (Element parameter : parameters) {
            jasminCode.append(load(parameter));
        }
        return jasminCode.toString();
    }

    private String getArgumentTypes(ArrayList<Element> parameters) {
        StringBuilder jasminCode = new StringBuilder();

        jasminCode.append("(");

        for (Element argument : parameters) {
            jasminCode.append(getJasminType(argument.getType()));
        }

        jasminCode.append(")");
        return jasminCode.toString();
    }

    private String getJasminType(Type type) {
        return MyJasmin.getJasminCode(this.method.getOllirClass(), type);
    }

    //Goto instructions
    private String getJasminCode(GotoInstruction instruction) {
        return Instructions.gotoInstr(instruction.getLabel());
    }

    //Branch instructions
    private String getJasminCode(CondBranchInstruction instruction) {
        StringBuilder jasminCode = new StringBuilder();
        Instruction condition = instruction.getCondition();
        String bodyLabel = instruction.getLabel();
        if (!(condition.getInstType() == InstructionType.BINARYOPER)) {
            jasminCode.append(ifCondition(condition, bodyLabel));
        } else {
            jasminCode.append(ifConditionBinary((BinaryOpInstruction) condition, bodyLabel));
        }
        return jasminCode.toString();
    }

    private String ifCondition(Instruction condition, String label) {
        return getJasminCode(condition) + Instructions.ifneInstr(label);
    }

    private String ifConditionBinary(BinaryOpInstruction condition, String label) {
        StringBuilder jasminCode = new StringBuilder();

        Element lhs = condition.getLeftOperand();
        Element rhs = condition.getRightOperand();

        OperationType opType = condition.getOperation().getOpType();

        // < and >= optimized
        if (opType == OperationType.LTH || opType == OperationType.GTE) { //to avoid extra gotos
            String s = "";
            jasminCode.append(load(lhs));
            if (rhs.isLiteral() && ((LiteralElement) rhs).getLiteral().equals("0")) { //for comparisons with 0
                if (opType == OperationType.LTH) {
                    s = Instructions.ifltInstr(label);
                } else {
                    s = Instructions.ifgeInstr(label);
                }
            } else {
                jasminCode.append(load(rhs));
                if (opType == OperationType.LTH) {
                    s = Instructions.if_icmpltInstr(label);
                } else {
                    s = Instructions.if_icmpgeInstr(label);
                }
            }
            jasminCode.append(s);
        } else {
            jasminCode.append(ifCondition(condition, label));
        }

        return jasminCode.toString();
    }

    //Return instructions
    private String getJasminCode(ReturnInstruction instruction) {
        StringBuilder jasminCode = new StringBuilder();

        if(!instruction.hasReturnValue())
            return "return\n";
        Element result = instruction.getOperand();
        jasminCode.append(load(result));
        ElementType type = result.getType().getTypeOfElement();
        if(type == ElementType.INT32 || type == ElementType.BOOLEAN) {
            jasminCode.append("ireturn\n");
        }
        else {
            jasminCode.append("areturn\n");
        }
        return jasminCode.toString();
    }

    //Putfield instructions
    private String getJasminCode(PutFieldInstruction instruction) {
        StringBuilder jasminCode = new StringBuilder();

        Element classElement = instruction.getFirstOperand();
        jasminCode.append(load(classElement));

        Element fieldElement = instruction.getSecondOperand();
        Element valueElement = instruction.getThirdOperand();
        jasminCode.append(load(valueElement));

        jasminCode.append(fieldOp(classElement, fieldElement, InstructionType.PUTFIELD));

        return jasminCode.toString();
    }

    private String fieldOp(Element classElement, Element fieldElement, InstructionType instructionType) {
        String fieldOp;
        if (instructionType.equals(InstructionType.GETFIELD)) {
            fieldOp = "GET";
        } else {
            fieldOp = "PUT";
        }

        String className = getQualifiedPath(((ClassType) classElement.getType()).getName());
        String fieldName = ((Operand) fieldElement).getName();
        String fieldType = getJasminType(fieldElement.getType());

        return Instructions.field(fieldOp, className, fieldName, fieldType);
    }

    //Getfield instructions
    private String getJasminCode(GetFieldInstruction instruction) {
        StringBuilder jasminCode = new StringBuilder();

        Element classElement = instruction.getFirstOperand();
        jasminCode.append(load(classElement));

        Element fieldElement = instruction.getSecondOperand();
        jasminCode.append(fieldOp(classElement, fieldElement, InstructionType.GETFIELD));

        return jasminCode.toString();
    }

    //UnaryOp Instructions
    private String getJasminCode(UnaryOpInstruction instruction) {
        StringBuilder jasminCode = new StringBuilder();

        Element element = instruction.getOperand();
        Operation op = instruction.getOperation();

        if (op.getOpType() == OperationType.NOTB) {
            jasminCode.append(Instructions.iconst("1"));
            jasminCode.append(load(element));
            jasminCode.append(Instructions.arithmeticOpsCode(OperationType.SUB));
        }

        return jasminCode.toString();
    }

    //BinaryOp Instructions
    private String getJasminCode(BinaryOpInstruction instruction) {
        StringBuilder jasminCode = new StringBuilder();

        Element lhs = instruction.getLeftOperand();
        Element rhs = instruction.getRightOperand();

        String leftLoad = load(lhs);
        String rightLoad = load(rhs);

        OperationType type = instruction.getOperation().getOpType();

        if (type == OperationType.LTH) {
            jasminCode.append(lth(lhs, rhs));
        } else if (type == OperationType.GTE) {
            jasminCode.append(gte(lhs, rhs));
        } else {
            jasminCode.append(leftLoad);
            jasminCode.append(rightLoad);
            jasminCode.append(Instructions.arithmeticOpsCode(type));
        }

        return jasminCode.toString();
    }

    private String lth(Element lhs, Element rhs) {
        StringBuilder jasminCode = new StringBuilder();

        jasminCode.append(load(lhs));

        String label1 = "LTH_" + this.labels.increaseCounter();
        String label2 = "LTH_" + this.labels.increaseCounter();

        if (rhs.isLiteral() && ((LiteralElement) rhs).getLiteral().equals("0")) {
            jasminCode.append(Instructions.ifltInstr(label1));
        } else {
            jasminCode.append(load(rhs) + Instructions.if_icmpltInstr(label1));
        }

        jasminCode.append(Instructions.iconst("0") + Instructions.gotoInstr(label2) + label1 + ":\n"
                + Instructions.iconst("1") + label2 + ":\n");

        return jasminCode.toString();
    }

    private String gte(Element lhs, Element rhs) {
        StringBuilder jasminCode = new StringBuilder();

        jasminCode.append(load(lhs));

        String label1 = "GTE_" + labels.increaseCounter();
        String label2 = "GTE_" + labels.increaseCounter();

        if (rhs.isLiteral() && ((LiteralElement) rhs).getLiteral().equals("0")) {
            jasminCode.append(Instructions.ifgeInstr(label1));
        } else {
            jasminCode.append(load(rhs) + Instructions.if_icmpgeInstr(label1));
        }

        jasminCode.append(Instructions.iconst("0") + Instructions.gotoInstr(label2) + label1 + ":\n"
                + Instructions.iconst("1") + label2 + ":\n");

        return jasminCode.toString();
    }

    //Noper instructions
    private String getJasminCode(SingleOpInstruction instruction) {
        Element element = instruction.getSingleOperand();
        return load(element);
    }

}