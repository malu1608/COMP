package pt.up.fe.comp2023.jmm.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import static org.specs.comp.ollir.ElementType.*;

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
        //fieldType.show();
        ElementType elementType = fieldType.getTypeOfElement();
        if (elementType == INT32) {
            return "I";
        } else if (elementType == BOOLEAN) {
            return "Z";
        } else if (elementType == ARRAYREF) {
            return "[" + getJasminCode(((ArrayType) fieldType).getElementType());
        } else if (elementType == OBJECTREF) {
            return "L" + getQualifiedPath(((ClassType) fieldType).getName()) + ";";
        } else if (elementType == CLASS) {
            return ("L" + getQualifiedPath(((ClassType) fieldType).getName()) + ";");
        } else if (elementType == THIS) {
            return "L" + getQualifiedPath(((ClassType) fieldType).getName()) + ";";
        } else if (elementType == STRING) {
            return "Ljava/lang/String";
        } else if (elementType == VOID) {
            return "V";
        }
        throw new NotImplementedException(fieldType);
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

    }
}