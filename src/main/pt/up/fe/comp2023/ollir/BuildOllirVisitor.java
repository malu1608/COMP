package pt.up.fe.comp2023.ollir;


import com.google.gson.internal.bind.util.ISO8601Utils;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.analysis.MySymbolTable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.logging.Logger.global;


//TODO
/*
* method calls !!!
* lidar com arrays
*
*/

public class BuildOllirVisitor extends AJmmVisitor<String,String> {
    private final MySymbolTable table;
    private String ollirCode;
    private static int tempCounter = 0;

    public BuildOllirVisitor(MySymbolTable new_table) {
        this.table = new_table;
        this.ollirCode = "";
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program",this::ProgramVisit);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("MethodDef", this::visitMethod);
        addVisit("ExpStat", this::visitObjectMethodCall);
        addVisit("Identifier", this::visitRetStat);
        addVisit("INTEGER",this::visitRetStat);
        addVisit("BinaryOp", this::visitRetStat);
        addVisit("BooleanType", this::visitRetStat);
        addVisit("AssignStat", this::visitAssignment);
        addVisit("Identifier", this::visitBinaryOp);
        setDefaultVisit(this::setDefault);
    }

    private String visitBinaryOp(JmmNode jmmNode, String methodName) {
        StringBuilder code = new StringBuilder();

        if(methodName == null){return "";}
        Type retType = table.getReturnType(methodName);
        String getretType = parseType(retType.getName());
        Boolean getretTypeisArray = retType.isArray();
        String operator = jmmNode.getJmmParent().get("op");
        StringBuilder Ocode = new StringBuilder();

        int position;

        if ((position = table.getParameter(methodName, jmmNode.get("value"))) != -1) {
            Ocode.append("$").append(position).append(".");
            Ocode.append(jmmNode.get("value")).append(".").append(getretType);
        } else {
            JmmNode methodNode = jmmNode.getJmmParent();

            while (!methodNode.getKind().equals("MethodDef")) {
                methodNode = methodNode.getJmmParent();
            }

            for (JmmNode child : methodNode.getChildren()) {
                if (child.getKind().equals("VarDecl") && child.get("name").equals(jmmNode.get("value"))) {
                    Ocode.append(jmmNode.get("value")).append(parseType(getretType));
                }
            }
        }

        if(jmmNode.getJmmParent().getJmmChild(1).equals(jmmNode)) {ollirCode += Ocode.toString() + ";";}
        else{ollirCode += Ocode.toString() + " " + operator + getretType + " ";}

        return ollirCode;

    }

    private String visitAssignment(JmmNode jmmNode, String s) {
        String assign = ":=";
        String var_name = jmmNode.get("value"); //assignStat
        JmmNode child = jmmNode.getJmmChild(0); //integer ou identifier ou bool
        String value_child ;
        String var = jmmNode.getJmmChild(0).get("value");


        if(child.getKind().equals("INTEGER")) {value_child= "int";}
        else{ //bolean
            value_child= "";
        }

        //a = b
        List<Symbol> var_list = table.getLocalVariables(jmmNode.getJmmParent().get("name"));
        for (Symbol symbol: var_list){
            if(child.getKind().equals("Identifier") && child.get("value").equals(symbol.getName())){
                value_child = symbol.getType().getName();
            }
        }

        ollirCode += "\t\t" + var_name + parseType(value_child) +" " +assign + parseType(assign)  +" "+ var +  parseType(value_child) + ";\n";

        return ollirCode;
    }

    private String visitRetStat(JmmNode jmmNode, String s) {

        if(jmmNode.getKind().equals("BooleanType")){
            String ret_type = jmmNode.get("name");
            if(jmmNode.getJmmParent().getJmmChild(jmmNode.getJmmParent().getNumChildren()-1).getKind().equals("Identifier")){ //identifier e a variavel retornada , o seu tipo e do type
                String name_param = jmmNode.getJmmParent().getJmmChild(jmmNode.getJmmParent().getNumChildren()-1).get("value");

                ollirCode += "\t\tret" +  parseType(ret_type) + " "+ name_param + parseType(ret_type) + ";\n";
            }
        }

        else if(jmmNode.getKind().equals("INTEGER")){
            String type = "int";
            ollirCode += "\t\tret" +  parseType(type) + " " + jmmNode.get("value").toString() + parseType(type) + ";\n";
        }

        else if(jmmNode.getKind().equals("BinaryOp")){
            //WHILE?
            ollirCode+= "\t\tret" + parseType(table.getReturnType(jmmNode.getJmmParent().get("name").toString()).getName()) + " "  ;

            //Visit Identifiers
            for(JmmNode child: jmmNode.getChildren()){
                visit(child,jmmNode.getJmmParent().get("name"));
            }

        }
        return ollirCode;
    }

    private String ProgramVisit(JmmNode jmmNode, String s) {

        if (jmmNode.getNumChildren() == 0) return "erro";
        else {
            for (String importString : table.getImports()) {
                ollirCode += "import " + importString + ";\n";
            }
            ollirCode += "\n";
            for(JmmNode child : jmmNode.getChildren()){

                if(!child.getKind().equals("ImportDecl"))
                {
                    visit(child);
                }

            }
            return ollirCode;
        }
    }
    private String emptyConstructor() {
        StringBuilder constructorCode = new StringBuilder();
        constructorCode.append("\n\t.construct ").append(table.getClassName()).append("().V {\n")
                .append("\t\tinvokespecial(this, \"<init>\").V;\n").append("\t}\n\n");
        return constructorCode.toString();
    }
    private String visitClassDeclaration(JmmNode jmmNode, String s) {
        String class_name = table.getClassName();
        String super_class = table.getSuper();

        //se string nao nula adiciona extends e o nome da classe
        ollirCode += class_name + (super_class != null ? " extends " + super_class : "") + " {\n";

        //field da classe
        for(var field: table.getFields()){
            ollirCode += "\t.field " + field.getName() + parseType(field.getType().getName()) + ";\n";
        }

        boolean hasConstructor = false;

        for (JmmNode child : jmmNode.getChildren()) {

            if (!child.getKind().equals("VarDecl")&&
                    !hasConstructor) {
                ollirCode+= emptyConstructor();
                hasConstructor = true;

            }

            visit(child);
        }
        ollirCode += "\n}";
        return ollirCode;
    }
    private String visitMethod(JmmNode jmmNode, String s) {
        StringBuilder mainStr = new StringBuilder();

        ollirCode += "\t.method public ";
        if (jmmNode.get("name").equals("main")) {
            ollirCode += "static ";
        }
        ollirCode += jmmNode.get("name") + "(";

        ollirCode += mainStr.toString();

        // Obter a lista de parâmetros do método
        for (JmmNode node : jmmNode.getChildren()) {
            System.out.println("filhos: " + node);
        }

        List<Symbol> parameters = table.getParameters(jmmNode.get("name"));
        if (!parameters.isEmpty()) {
            for (Symbol parameter : parameters) {

                if (parameter.getType().isArray()) {
                    ollirCode += parameter.getName() + ".array." + parameter.getType().getName();
                } else {
                    ollirCode += " " + parameter.getName() + parseType(parameter.getType().getName()) + ",";

                }
                //remove ultima virgula
                if (!Objects.equals(jmmNode.get("name"), "main")) {
                    if (parameters.get(parameters.size() - 1) == parameter) {
                        ollirCode = ollirCode.substring(0, ollirCode.length() - 1);
                    }
                }

            }
        }
        ollirCode += ")" + parseType(jmmNode.getJmmChild(0).get("name"))+ " {\n";

        StringBuilder methodStr = new StringBuilder();
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child);
        }
        ollirCode += methodStr.toString() ;
        if (table.getReturnType(jmmNode.get("name")).getName().equals("void")) {
            ollirCode += "\t\tret" + parseType(table.getReturnType(jmmNode.get("name")).getName()) + ";\n";
        }
        ollirCode += "\t\n\t}\n";
        return ollirCode;
    }

    private String visitObjectMethodCall(JmmNode method, String s) {
        //lida com metodos do import
        StringBuilder methodStr = new StringBuilder();

        JmmNode methodCall = method.getJmmChild(0);
        JmmNode identifier = methodCall.getJmmChild(0);

        if (identifier.getKind().equals("Identifier") && table.checkVariableInImports(identifier.get("value").toString())) {
            methodStr.append("\t\tinvokestatic(" + identifier.get("value") + ", \"" + methodCall.get("name") + "\"");

            if (methodCall.getChildren().size() > 1)
                methodStr.append(", ");
            else methodStr.append(").V;\n");
        }
        ollirCode += methodStr.toString() ;
        /*
        if (table.getReturnType(methodCall.get("name")).getName().equals("void")) {
            ollirCode += "\t\tret" + parseType(table.getReturnType(method.get("name")).getName()) + ";\n";
        }*/

        ollirCode += "\t}\n\n";
        return ollirCode;
    }
    private String setDefault(JmmNode jmmNode, String s) {
        StringBuilder visitStr = new StringBuilder();
        return visitStr.toString();
    }

    private String parseType(String type) {

        if (type.equals("int"))
            return ".i32";
        else if (type.equals("void"))
            return ".V";
        else if (type.equals("false"))
            return "0.bool";
        else if (type.equals("true"))
            return "1.bool";
        else if (type.equals("int array"))
            return "array.i32";
        else if(type.equals("boolean"))
            return ".bool";
        else if(type.equals(":="))
            return ".i32";
        //else return "." falta!!
        return type;
    }

    public String getCode(){
        return ollirCode;
    }
}
