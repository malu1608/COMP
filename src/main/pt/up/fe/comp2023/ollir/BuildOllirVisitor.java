package pt.up.fe.comp2023.ollir;


import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.analysis.MySymbolTable;

import java.util.List;
import java.util.Objects;

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
    private int if_elseCounter = 0;
    private int whileCounter = 0;

    public BuildOllirVisitor(MySymbolTable new_table) {
        this.table = new_table;
        this.ollirCode = "";
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program",this::ProgramVisit);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("MethodDef", this::visitMethod);
        addVisit("MethodCall", this::visitObjectMethodCall);
        addVisit("ExpStat", this::visitObjectMethodCall);
        addVisit("Integer",this::visitInteger);
        addVisit("Boolean",this::visitBoolean);
        addVisit("Identifier",this::visitIdentifier);
        addVisit("ReturnStmt", this::visitRetStat);
        addVisit("AssignStat", this::visitAssignment);
        addVisit("NewObject", this::visitNewObject);
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit( "IfElseStat", this::visitIfElse);
        addVisit("WhileStat", this::visitWhile);

        setDefaultVisit(this::setDefault);
    }

    private String visitWhile(JmmNode jmmNode, String s) {

        int nextWhile = nextWhile();

        ollirCode += "\t\t"+ "goto while_cond_" + Integer.toString(nextWhile) + ";\n";
        ollirCode += "\t\t" +"while_body_" + nextWhile + ":\n";
        // Get the else expression
        for (JmmNode child: jmmNode.getJmmChild(1).getChildren()){
            var bodyExpr = visit(child);
            ollirCode = bodyExpr;
        }
        ollirCode += "\t\t"+ "while_cond_" + Integer.toString(nextWhile) + ":\n";

        // Get the if expression of the while

        var ifExpr = visit(jmmNode.getJmmChild(0));


        ollirCode +=  "\t\t" + "if (" + ifExpr+ ") " +"goto while_body_" + nextWhile + ";\n";



        return ollirCode;
    }

    private String visitIfElse(JmmNode jmmNode, String s) {

        var boolCond = visit(jmmNode.getJmmChild(0));

        int nextIf = nextIfElse();

        ollirCode += "\t\t"+"if(" + boolCond + ") goto if_then_" + Integer.toString(nextIf) + ";\n";
        // Get the else expression
        for (JmmNode child: jmmNode.getJmmChild(2).getChildren()){
            var elseExpr = visit(child);
            ollirCode = elseExpr;
        }
        ollirCode += "\t\t"+"goto if_end_" + nextIf + ";\n";
        ollirCode += "\t\t"+"if_then_" + nextIf + ":\n";
        // Get the if expression
        for (JmmNode child: jmmNode.getJmmChild(1).getChildren()){
            var ifExpr = visit(child);
            ollirCode = ifExpr;
        }
        ollirCode += "\t\t"+"if_end_" + nextIf + ":\n";

        return ollirCode;
    }




    private String visitExpStat(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren()){
            visit(child);
        }
        return ollirCode;
    }

    private String visitInteger(JmmNode jmmNode, String methodName) {  //nao uso
        return jmmNode.get("value") + parseType("int");
    }

    private String visitBoolean(JmmNode jmmNode, String methodName) {
        return jmmNode.get("value") + parseType("boolean");
    }


    private String visitIdentifier(JmmNode jmmNode, String methodName) {
        var varName = jmmNode.get("value");
        if(table.isGlobal(varName)){
            var varType = getOllirType(varName);
            var value = nextTemp();
            String p_code = "";
            p_code += value + varType + " :=" + varType + " getfield(this, " +  varName + methodName +")" + methodName + ";\n";
            ollirCode += p_code;
            return value+varType;
            //val = value.substring(0, value.length() - 4) + varType;
        }

        return varName + getOllirType(varName);
    }

    private int nextWhile() {
        var result = whileCounter;
        whileCounter++;
        return result;
    }
    private int nextIfElse()  {
        var result = if_elseCounter;
        if_elseCounter++;
        return result;
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

        var lhsValue = visit(jmmNode.getJmmChild(0) ,resultType);
        var rhsValue =  visit(jmmNode.getJmmChild(1) ,resultType);

        var result = nextTemp()  + resultType;

        var binOpCode = result + " :="+resultType + " " + lhsValue + " "+ op+assign_type+" "
                + rhsValue +  ";\n";
        ollirCode += binOpCode;

        return result;
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
       
        else if (jmmNode.getJmmChild(0).getKind().equals("Identifier") ) {

            var child = visit(jmmNode.getJmmChild(0));
            String idType = child.substring(child.indexOf(".")+1);

            ollirCode += "\t\tret" + parseType(idType) + " "+ child + ";\n";

            
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
            ollirCode += "\t.field " + "public " +field.getName() + parseType(field.getType().getName(),field.getType().isArray())  + ";\n";
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

        currentMethod = jmmNode.get("signature");
        tempCounter = 0;

        ollirCode += "\t.method public ";
        if (jmmNode.get("name").equals("main")) {
            ollirCode += "static ";
        }
        ollirCode += jmmNode.get("name") + "(";

        ollirCode += mainStr.toString();

        //for( Symbol p : table.getParameters(currentMethod)) System.out.println(p);
        //System.out.println(table.print());
        List<Symbol> parameters = table.getParameters(jmmNode.get("signature"));

        if (!parameters.isEmpty()) {
            for (Symbol parameter : parameters) {

                if (parameter.getType().isArray()) {
                    ollirCode += parameter.getName() + ".array." + parameter.getType().getName();
                } else {
                    ollirCode += " " + parameter.getName() + parseType(parameter.getType().getName(),parameter.getType().isArray()) + ",";

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

        if (table.getReturnType(jmmNode.get("signature")).getName().equals("void")) {
            ollirCode += "\t\tret" + parseType(table.getReturnType(jmmNode.get("signature")).getName(),table.getReturnType(jmmNode.get("signature")).isArray()) + ";\n";

        }
        ollirCode += "\t\n\t}\n";

        currentMethod = null;

        return ollirCode;
    }

    private String visitObjectMethodCall(JmmNode jmmNode, String s) {
        //lida com metodos do import
        String methodName = null;

        if(jmmNode.getJmmParent().getKind().equals("MethodDef") && jmmNode.getJmmParent().get("name").equals("main") ){
            methodName = "main";
        }else{
            if(jmmNode.getKind().equals("ExpStat") && !jmmNode.getJmmParent().getKind().equals("Stat")) methodName = jmmNode.getJmmParent().get("name");
            else if (jmmNode.getKind().equals("MethodCall")) methodName = jmmNode.getJmmParent().getJmmParent().get("name");
            else if(jmmNode.getKind().equals("ExpStat")) methodName = table.getMethod(jmmNode.getJmmParent().getJmmParent().getJmmParent().get("signature")).getReturnType().getName();
        }

        JmmNode methodCall = jmmNode;
        if(jmmNode.getJmmChild(0).getKind().equals("MethodCall")){
            methodCall = jmmNode.getJmmChild(0);
        }
        JmmNode identifier = methodCall.getJmmChild(0);
        StringBuilder code = new StringBuilder();
        if(methodCall.getChildren().size() > 1){
            JmmNode arguments = methodCall.getJmmChild(1);
            visit(arguments, methodName);
        }
        String idType = getOllirType(identifier.get("value"));
        boolean istatic = false;
        String returnType = ".V";
        if (identifier.getKind().equals("Identifier") && table.checkVariableInImports(identifier.get("value")) ) {

            code.append("\t\tinvokestatic("  +  identifier.get("value")   + ", \"" + methodCall.get("name") + "\"");
            istatic = true;
        }
        else {

            code.append("invokevirtual(" + identifier.get("value") +  idType + ", \"" + methodCall.get("name") + "\"" );

        }

        boolean grandchildren = false;
        for (JmmNode grandchild : methodCall.getChildren()) {
            grandchildren = true;
            if(!grandchild.get("value").equals(identifier.get("value")))
                code.append( " , " + visit(grandchild));

        }

        code.append(")");

        if(istatic) {
            code.append(".V;\n");
            ollirCode += code.toString();
            return ollirCode;
        }

        else{
            if(!jmmNode.getJmmParent().getKind().equals("AssignStat")){
                for (String method : table.getMethods()) {
                    if (method.equals(methodCall.get("name"))) {
                        returnType = parseType(table.getMethod(methodCall.get("name")).getReturnType().getName(), table.getMethod(method).getReturnType().isArray());
                        code.append(returnType).append(";\n");
                        ollirCode += code.toString();
                        return ollirCode;
                    }
                }

                code.append(returnType).append(";\n");
                ollirCode += code.toString();
                return ollirCode;
            }
            else{
                if(jmmNode.getJmmParent().getKind().equals("AssignStat")){
                    returnType = getOllirType(jmmNode.getJmmParent().get("value"));
                    code.append(returnType);
                    }
                }
        }    //code.append(".").append(returnType.split("\\.")[1]);

        return code.toString();

    }

    private String visitAssignment(JmmNode jmmNode, String s) {

        String var_name = jmmNode.get("value"); //assignStat

        var idType = getOllirType(jmmNode.get("value"));

        var child = visit(jmmNode.getJmmChild(0),idType);

        if(table.isGlobal(var_name)){

            ollirCode += "\t\tputfield(this, " + var_name + idType +  ", " + child +")" +  ".V;\n";
            //var var_assigned =  visit(jmmNode.getJmmParent().getJmmChild(jmmNode.getIndexOfSelf()+1).getJmmChild(0));

            return ollirCode;
            //ollirCode += "\t\t"+"invokespecial(" + result+ idType + ", \"<init>\").V;\n";
        }

        if(idType.equals("")){ //caso tenha temps!!!
            idType += child.substring(child.indexOf("."));
        }
        var assign = "\t\t"+ var_name + idType + " :=" + idType + " " + child + ";\n";

        ollirCode += assign;

        int nr ;

        if (jmmNode.getJmmParent().getKind().equals("MethodDef")) {
            nr = getParameters(jmmNode.getJmmParent().get("signature"), var_name);
            if (nr != -1) {
                String temp = Integer.toString(nr);
                var_name = "$" + temp + "." + var_name;
            }
        }







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

    /* private String visitIfElse(JmmNode jmmNode, Boolean tempVariable) {

        JmmNode condition = jmmNode.getChildren().get(0);
        StringBuilder expressionString = new StringBuilder();
        String condString = visit(condition,true);
        List<String> res = getLastLine(condString);
        int localLabel = labelCount;
        ifCount = localLabel;
        labelCount++;
        condString = this.buildCondition(res.get(1), false);
        expressionString.append(res.get(0));
        expressionString.append(res.get(0).isEmpty() ? "" : "\t\t").append(res.get(0));

        expressionString.append("\t\tif (").append(condString).append(") goto else").append(localLabel).append(";\n");

        for (int i = 1; i < jmmNode.getNumChildren(); i++) {
            expressionString.append("\t").append(visit(jmmNode.getChildren().get(i)));
            expressionString.append("\t\t\t").append(visit(jmmNode.getChildren().get(i))).append("\n");
        }
        expressionString.append("\t\t\tgoto endif").append(localLabel).append(";\n");
        expressionString.append("\t\t\tgoto endif").append(localLabel).append(";");
        return expressionString.toString();
        return "";
    }*/


}


