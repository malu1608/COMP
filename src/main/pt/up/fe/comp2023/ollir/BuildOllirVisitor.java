package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import java.util.List;

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
        addVisit("MethodDef", this::visitMethod);
        addVisit("ObjectDeclaration", this::visitObjectMethod);
        setDefaultVisit(this::setDefault);
    }

    private String visitObjectMethod(JmmNode jmmNode, String s) {
        StringBuilder methodStr = new StringBuilder();

        JmmNode identifier = jmmNode.getChildren().get(0);
        System.out.println("no:" + identifier);
        JmmNode call = jmmNode.getChildren().get(1);

        // Visitar filhos da call
        // Construir lista de argumentos

        boolean varinImports = false;
        for (String imp : table.getImports()) {
            if (imp.equals(identifier.get("name"))) varinImports=true;
        }
        System.out.println("ola");
        System.out.println(identifier.getKind().equals("IDENTIFIER"));
        if (identifier.getKind().equals("IDENTIFIER") && varinImports) {
            methodStr.append("invokestatic(" + identifier.get("name") + ", \"" + call.get("name") + "\"");
            if (call.getChildren().size() > 0)
                methodStr.append(", ");
            boolean grandchildren = false;
        }

        ollirCode += methodStr.toString();
        return ollirCode;
    }

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

        List<String> methods = table.getMethods();
        for (String method : methods){
            if(method.equals(jmmNode.get("signature"))){
                //duplicado erro do report
                return "";
            }
        }
        ollirCode += ".method public ";
        if(jmmNode.get("name").equals("main")) {
            ollirCode += "static ";
        }
        ollirCode += jmmNode.get("name") + "(";

        // Obter a lista de parâmetros do método
        List<Symbol> parameters = table.getParameters(jmmNode.get("name"));
        for (Symbol parameter : parameters) {
            System.out.println("parametro: " + parameter);
            System.out.println(parameter.getType().isArray());
            boolean isArray = false;
            String typeName = parameter.getName();
            if (parameter.getType().isArray()) {
                typeName += ".array";
                isArray = true;
                mainStr.append(typeName + parameter.getType().getName() + ",");
            }

            else{
                mainStr.append(parameter.getType().getName());
            }
            mainStr.append(" " + parameter.getName() + ",");
        }
        // remove the last comma
        if (parameters.size() > 0) {
            mainStr.deleteCharAt(mainStr.length() - 1);
        }

        ollirCode += mainStr.toString() + ")" + ".V" + " {\n";

        // Visitar o corpo do método
        for (JmmNode child : jmmNode.getChildren()) {
            System.out.println("child : " + child);
            if(child.hasAttribute("name") && child.get("name").equals("void")){
                ollirCode += "\nret.V";
            }else{
                for(JmmNode parameter : child.getChildren()){
                    System.out.println("child2 : " + parameter);
                    String getParamName = parameter.get("name");
                    JmmNode String_Type = parameter.getChildren().get(jmmNode.get("isArray"));
                    if(String_Type.get("isArray") == "true"){
                        ollirCode+= getParamName + ".array"+ getParamName + ",";
                    }

                }
            }
            visit(child);
        }

        ollirCode += "\n}\n";
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
