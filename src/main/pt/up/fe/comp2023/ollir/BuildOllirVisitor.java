package pt.up.fe.comp2023.ollir;


import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.analysis.MySymbolTable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

//TODO
/*
* method calls !!!
* lidar com arrays
*
*/

public class BuildOllirVisitor extends AJmmVisitor<String,String> {
    private final MySymbolTable table;
    private String ollirCode;
    private int tempCounter = 0;

    private String currentMethod = null;

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
        addVisit("Integer",this::visitInteger);
        addVisit("Boolean",this::visitBoolean);
        addVisit("Identifier",this::visitIdentifier);
        addVisit("ReturnStmt", this::visitRetStat);
        addVisit("AssignStat", this::visitAssignment);
        addVisit("NewObject", this::visitNewObject);
        addVisit("BinaryOp", this::visitBinaryOp);

        setDefaultVisit(this::setDefault);
    }

    private String visitInteger(JmmNode jmmNode, String methodName) {  //nao uso
        return jmmNode.get("value") + parseType("int");
    }

    private String visitBoolean(JmmNode jmmNode, String methodName) {
        return jmmNode.get("value") + parseType("boolean");
    }


    private String visitIdentifier(JmmNode jmmNode, String methodName) {
        var varName = jmmNode.get("value");
        if(getOllirType(varName).equals(varName)){
            return "getfield(this, " +  varName + methodName +")" + methodName ;
        }

        return varName + getOllirType(varName);
    }


    private String nextTemp()  {
        var result = "temp_" + tempCounter;
        tempCounter++;
        return result;
    }
    private int getParameters(String methodName, String varName){
        int parameterNumber = 1;
        for (Symbol parameter : table.getParameters(methodName)) {
            if(parameter.getName().equals(varName))
                return parameterNumber;
            parameterNumber++;

        }
        return -1;
    }



    private String visitBinaryOp(JmmNode jmmNode, String methodName) {
        var op = jmmNode.get("op");
        var assign_type = getAssigmentRetType(op);
        var resultType = assign_type;

        var lhsValue = visit(jmmNode.getJmmChild(0) );
        var rhsValue =  visit(jmmNode.getJmmChild(1) );

        var result = nextTemp()  + resultType;

        var binOpCode = result + " :="+resultType + " " + lhsValue + " "+ op+resultType+" "
                + rhsValue +  ";\n";
        ollirCode += binOpCode;

        return result;
    }

    private String visitAssignment(JmmNode jmmNode, String s) {

        String var_name = jmmNode.get("value"); //assignStat

        var idType = getOllirType(var_name);
        var child = visit(jmmNode.getJmmChild(0),idType);

        if(table.isGlobal(var_name))
            ollirCode += "\t\tputfield(this, " + var_name + idType +  ", " + child +")" + idType +  ".V;\n";


        int nr ;
        nr = getParameters(jmmNode.getJmmParent().get("name"),var_name);
        if (nr != -1){
            String temp = Integer.toString(nr);
            var_name = "$" + temp + "." + var_name;
        }


        var assign = "\t\t"+ var_name + idType + " :=" + idType + " " + child + ";\n";
        ollirCode += assign;

        if(jmmNode.getJmmChild(0).getKind().equals("NewObject")){
            ollirCode += "\t\t"+"invokespecial(" + var_name+ idType + ", \"<init>\").V;\n";
        }

        return ollirCode;

    }

    public String getOllirType(String varName) {
        List<Symbol> var_list = table.getLocalVariables(currentMethod);
        for (Symbol symbol : var_list) {
            if(symbol.getName().equals(varName)) {
                return parseType(symbol.getType().getName());
            }
        }

        List<Symbol> param_list = table.getParameters(currentMethod);
        for (Symbol symbol : param_list) {
            if(symbol.getName().equals(varName)) {
                return parseType(symbol.getType().getName());
            }
        }

        List<Symbol> fields_list = table.getFields();
        for (Symbol symbol : fields_list) {
            if(symbol.getName().equals(varName)) {
                return parseType(symbol.getType().getName());
            }
        }

        return "";
    }


    private String visitRetStat(JmmNode jmmNode, String s) {

        if(jmmNode.getJmmChild(0).getKind().equals("BinaryOp")){
            var varName = jmmNode.getJmmChild(0).getJmmChild(0).get("value");
            var id = visit(jmmNode.getJmmChild(0));
            ollirCode += "\t\tret" +  getOllirType(varName) + " "+ id +  ";\n";
        }

        else if(jmmNode.getJmmChild(0).getKind().equals("Integer")){
            ollirCode += "\t\tret" + parseType("int") + " "+  jmmNode.getJmmChild(0).get("value") + parseType("int") + ";\n";
        }

        else if(jmmNode.getJmmChild(0).getKind().equals("Identifier") ) {  //boolean esquisito!
            if(jmmNode.getKind().equals("ReturnStmt") ){
                ollirCode += "\t\tret" + parseType("boolean") + " "+  jmmNode.getJmmChild(0).get("value") + parseType("boolean") + ";\n";
            }
            else{visit(jmmNode.getJmmChild(0));}
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
            ollirCode += "\t.field " + field.getName() + parseType(field.getType().getName(),field.getType().isArray())  + ";\n";
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

        currentMethod = jmmNode.get("name");
        tempCounter = 0;

        ollirCode += "\t.method public ";
        if (jmmNode.get("name").equals("main")) {
            ollirCode += "static ";
        }
        ollirCode += jmmNode.get("name") + "(";

        ollirCode += mainStr.toString();

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

        currentMethod = null;

        return ollirCode;
    }

    private String visitObjectMethodCall(JmmNode method, String s) {
        //lida com metodos do import

        JmmNode methodCall = method.getJmmChild(0);
        JmmNode identifier = methodCall.getJmmChild(0);

        String idType = getOllirType(identifier.get("value"));
        StringBuilder code = new StringBuilder();

        if (identifier.getKind().equals("Identifier") && table.checkVariableInImports(identifier.get("value")) ) {

            code.append("\t\tinvokestatic("  + identifier.get("value")   + ", \"" + methodCall.get("name") + "\"");

            if (methodCall.getChildren().size() > 0)
                code.append(", ");
            boolean grandchildren = false;
            for (JmmNode grandchild : methodCall.getChildren()) {
                grandchildren = true;
                if(!grandchild.get("value").equals(identifier.get("value")))
                    code.append(visit(grandchild) + ", ");

            }
            if (grandchildren)
                code.delete(code.length() - 2, code.length());
            //code.append(")." + parseType(table.getMethod(methodCall.get("name")).getReturnType().getName()));

            code.append(").V; \n");
        } else {

            code.append("invokevirtual(" + getOllirType(identifier.get("value")) + ", \"" + methodCall.get("name") + "\"");

        }
        ollirCode += code.toString() ;

        return ollirCode;
    }

    private String setDefault(JmmNode jmmNode, String s) {
        StringBuilder visitStr = new StringBuilder();
        return visitStr.toString();
    }

    private String parseType(String type, boolean isArray) {
        String r = "";
        if (isArray)  r += ".array" ;
        if (type.equals("int"))
            return r + ".i32";
        else if (type.equals("void"))
            return r + ".V";
        else if(type.equals("boolean"))
            return r + ".bool";
        else if(type.equals(":="))
            return r + ".i32";

        return "." + type;

    }

    private String parseType(String type) {

        if (type.equals("int"))
            return ".i32";
        else if (type.equals("void"))
            return ".V";
        else if(type.equals("boolean"))
            return ".bool";
        else if(type.equals(":="))
            return ".i32";

        return "." + type;
    }

    private String visitNewObject (JmmNode jmmNode, String idType) {
        return "new(" + jmmNode.get("value") + ")" + idType;
    }

    public String getCode(){
        return ollirCode;}


    private String getAssigmentRetType(String op) {
        return switch (op) {
            case "+", "-", "*", "/" -> ".i32";
            case "&&", "<", "==", "!=" -> ".bool";
            default -> "." +op;
        };
    }

}


