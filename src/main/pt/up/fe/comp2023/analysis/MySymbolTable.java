package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MySymbolTable implements SymbolTable {
    private List<String> importList = new ArrayList<>();
    private String className = null;
    private String superName = null;
    private List<Symbol> fieldsList = new ArrayList<>();
    private final Map<String, MethodInfo> methodsList = new HashMap<>();

    @Override
    public List<String> getImports() {
        return importList;
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

    @Override
    public Type getReturnType(String s) {
        return methodsList.get(s).getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String s) {
        return methodsList.get(s).getParameters();
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        return methodsList.get(s).getLocalVariables();
    }

    public void addImport(String _import) {
        importList.add(_import);
    }

    public void addMethod(String s, Type returnType, List<Symbol> parameters, List<Symbol> variables) {
        methodsList.put(s, new MethodInfo(returnType, parameters, variables));
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

}
