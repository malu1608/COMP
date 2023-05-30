package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class MySymbolTable implements SymbolTable {
    private List<String> importList = new ArrayList<>();
    private String className = null;
    private String superName = null;
    private List<Symbol> fieldsList = new ArrayList<>();
    private final Map<String, Method> methodsList = new HashMap<>();

    @Override
    public List<String> getImports() {
        return this.importList;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superName;
    }

    @Override
    public List<Symbol> getFields() {
        return fieldsList;
    }

    @Override
    public List<String> getMethods() {
        List<String> methodNames = new ArrayList<>();
        methodNames.addAll(methodsList.keySet());

        return methodNames;

    }

    private Method getMethodInternal(String s) {
        return this.methodsList.get(s);
        /*
        if (method == null) {
            method = new Method(null, Collections.emptyList(), Collections.emptyList());
            System.out.println("bye"+method);
        }*/
    }


    @Override
    public Type getReturnType(String s) {
        return methodsList.get(s).getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String s) {

        return methodsList.get(s).getParameters();
    }

    @Override
    public List<Symbol> getLocalVariables(String methodname) {

        return methodsList.get(methodname).getLocalVariables();
    }

    public void addImport(String _import) {
        importList.add(_import);
    }


    public void addMethod(String s, Type returnType, List<Symbol> parameters, List<Symbol> variables) {
        methodsList.put( s, new Method(returnType, parameters, variables));
    }

    public void addField(Symbol _field) {
        fieldsList.add(_field);
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setSuper(String superName) {
        this.superName = superName;
    }

    public boolean checkVariableInImports(String variableName) {
        for (String imp : getImports()) {
            if (imp.equals(variableName)) return true;
        }
        return false;
    }

    public Symbol getField(String fieldName) {
        for (Symbol symbol : getFields()) {
            if (symbol.getName().equals(fieldName)) return symbol;
        }
        return null;
    }

    public Method getMethod(String methodName) {
        return this.methodsList.get(methodName);
    }


    public Boolean isLocalVariable(String variableName, String methodName) {
        Symbol localVar = this.getMethod(methodName).getLocalVariable(variableName);
        if (localVar == null) return false;
        return true;
    }

    public boolean isGlobal(String variableName) {
        return this.getField(variableName) != null;
    }

    public Symbol findField(String name) {

        // iterate method local variables
        for (String method : methodsList.keySet()) {
            for (Symbol localVariable : methodsList.get(method).getLocalVariables()) {
                if (localVariable.getName().equals(name)) {
                    return localVariable;
                }
            }
        }

        // iterate method parameters
        for (String method : methodsList.keySet()) {
            for (Symbol parameter : methodsList.get(method).getParameters()) {
                if (parameter.getName().equals(name)) {
                    return parameter;
                }
            }
        }

        for (Symbol field : fieldsList) {
            if (field.getName().equals(name)) {
                return field;
            }
        }

        // check if it is in imports
        for (String imp : importList) {
            if (imp.endsWith(name)) {
                return new Symbol(new Type(name, false), name);
            }
        }

        return new Symbol(new Type("#UNKNOWN", false), name);
    }

    public Symbol findMethod(String name, String methodName) {
        for (String nameMethod : methodsList.keySet()) {
            String aux = nameMethod.split("#")[0];
            if (aux.equals(methodName) || name == methodName) {
                for (Symbol localVariable : methodsList.get(nameMethod).getLocalVariables()) {
                    if (localVariable.getName().equals(name) || name == methodName) {
                        return localVariable;
                    }
                }
                for (Symbol parameter : methodsList.get(nameMethod).getParameters()) {
                    if (parameter.getName().equals(name)) {
                        return parameter;
                    }
                }
            }
        }

        for (Symbol field : fieldsList) {
            if (field.getName().equals(name)) {
                return field;
            }
        }

        // check if it is in imports
        for (String imp : importList) {
            if (imp.endsWith(name)) {
                return new Symbol(new Type(name, false), name);
            }
        }

        return new Symbol(new Type("#UNKNOWN", false), name);
    }

    public boolean isClassImported(String className) {
        for (String imp : importList) {
            if (imp.endsWith(className)) {
                return true;
            }
        }
        return false;
    }


}
