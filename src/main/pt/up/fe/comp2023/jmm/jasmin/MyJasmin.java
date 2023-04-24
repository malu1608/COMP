package pt.up.fe.comp2023.jmm.jasmin;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Field;
import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

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


}