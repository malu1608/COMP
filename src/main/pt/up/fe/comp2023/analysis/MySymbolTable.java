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
        return new ArrayList<>(methodsList.keySet());
    }

    private Method getMethodInternal(String s) {
        var method = methodsList.get(s);

        if(method == null) {
            method = new Method(null, Collections.emptyList(), Collections.emptyList());
        }

        return method;
    }

    @Override
    public Type getReturnType(String s) {
        return getMethodInternal(s).getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String s) {
        return getMethodInternal(s).getParameters();
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        
        return getMethodInternal(s).getLocalVariables();
    }

    public void addImport(String _import) {
        importList.add(_import);
    }

    public void addMethod(String s, Type returnType, List<Symbol> parameters, List<Symbol> variables) {
        methodsList.put(s, new Method(returnType, parameters, variables));
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
        for (Symbol symbol: getFields()) {
            if (symbol.getName().equals(fieldName)) return symbol;
        }
        return null;
    }
    public Method getMethod(String methodName) {
        return this.methodsList.get(methodName);
    }


    public Symbol getVariable(String variableName, String methodName) {
        Symbol globalVar = this.getField(variableName);
        Symbol localVar = this.getMethod(methodName).getLocalVariable(variableName);
        return localVar != null ? localVar : globalVar;
    }
    public boolean isGlobal(String variableName) {
        return this.getField(variableName) != null;
    }


}
