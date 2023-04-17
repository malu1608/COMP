package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.analysis.MySymbolTable;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


public class BuildOllirVisitor extends AJmmVisitor<String,String> {
    private final SymbolTable table;
    private String ollirCode;

    public BuildOllirVisitor(SymbolTable new_table) {
        this.table = new_table;
        this.ollirCode = "";
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program",this::ProgramVisit);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        //addVisit("MainDeclaration", this::visitMainDeclaration);
        addVisit("MethodDef", this::visitMethod);
        setDefaultVisit(this::setDefault);
    }
    /*
    private String visitMainDeclaration(JmmNode jmmNode, String s) {
        StringBuilder mainStr = new StringBuilder();

        //mainStr.append("\n\t.method public static main(args.array.String).V {\n");

        for (JmmNode methodChild: jmmNode.getChildren()) {
            mainStr.append(visit(methodChild));
        }

        mainStr.append("}\t}\n");
        ollirCode += mainStr.toString();
        return ollirCode;

    }
*/


    private String setDefault(JmmNode jmmNode, String s) {
        StringBuilder visitStr = new StringBuilder();
        for (JmmNode child : jmmNode.getChildren()) {
            visitStr.append(visit(child));
        }
        return visitStr.toString();
    }

    private String identifierVisit(JmmNode jmmNode, String s) {
        return "";
    }

    private String visitMethod(JmmNode jmmNode, String s) {
        StringBuilder mainStr = new StringBuilder();

        ollirCode += ".method public ";
        if(jmmNode.get("name").equals("main")) {
            ollirCode += "static ";
        }
        ollirCode += jmmNode.get("name") + "(";

        // Obter a lista de parâmetros do método
        List<Symbol> parameters = table.getParameters(jmmNode.get("name"));
        for (Symbol parameter : parameters) {
            String paramName = parameter.getName();
            boolean isArray = false;
            if(parameter.getType().isArray()) isArray = true;
            String paramTypeName = parameter.getType().getName();
            //String paramTypeName = parseType(paramType.toString());

            mainStr.append(paramName + "." + "array" + "." + paramTypeName );
            break;
        }

        //mainStr.append("array.java.lang.String");

        /*
        if (mainStr.length() > 2) {
            mainStr.delete(mainStr.length() - 2, mainStr.length());
        }
        */
        ollirCode += mainStr.toString() + ")" + ".V" + " {";

        /*

        String parsed = parseType(table.getReturnType(method_signature).toString());
        if (children)
            mainStr.delete(mainStr.length() - 2, mainStr.length());
        mainStr.append(")." + parseType(parsed + "{\n"));
        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("parameters")) continue;
            mainStr.append(visit(child));
        }*/
        //mainStr.append("\t}\n");

        return ollirCode;
    }

    private String visitClassDeclaration(JmmNode jmmNode, String s) {
        String class_name = table.getClassName();
        String super_class = table.getSuper();

        //se string nao nula adiciona extends e o nome da classe
        ollirCode += class_name + (super_class != null ? " extends " + super_class : "") + " {\n";

        boolean hasConstructor = false;

        for (JmmNode child : jmmNode.getChildren()) {

            if (//!child.getKind().equals("VarDeclaration")&&
            !hasConstructor) {
                ollirCode+= emptyConstructor();
                hasConstructor = true;
            }

            visit(child);
        }

        ollirCode += "\n}\n";
        return ollirCode;
    }

    private String emptyConstructor() {
        StringBuilder constructorCode = new StringBuilder();
        constructorCode.append(".construct ").append(table.getClassName()).append("().V {\n")
                .append("invokespecial(this, \"<init>\").V;\n").append("}\n\n");
        return constructorCode.toString();
    }


    private String ProgramVisit(JmmNode jmmNode, String s) {
        //List<Report> reports = new ArrayList<>();
        if (visit(jmmNode.getChildren().get(0)) == null) return "erro";
        else {
            for (String importString : table.getImports()) {
                ollirCode += "import " +importString + ";\n";
            }

            for(JmmNode child : jmmNode.getChildren()){
                visit(child);
            }
            return ollirCode;
        }
    }


    private String parseType(String type) {

        if (type.equals("int"))
            return "i32";
        else if (type.equals("void"))
            return "V";
        else if (type.equals("FALSE"))
            return "0.bool";
        else if (type.equals("TRUE"))
            return "1.bool";
        else if (type.equals("int array"))
            return "array.i32";
        return type;
    }

    public String getCode(){
        return ollirCode;
    }
}
