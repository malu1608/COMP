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
* lidar com arrays - ioPlus.printResult(a[d.func(0)]);
* this!!
* if , while esquisito?'
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
        addVisit("IfElseStat", this::visitIfElse);
        addVisit("WhileStat", this::visitWhile);
        addVisit("NewIntArray", this::visitNewArray);
        addVisit("Length", this::visitLength);
        addVisit("ArrayAccess", this::visitArray);
        addVisit("ArrayAssignStat", this::visitArrayAssign);


        setDefaultVisit(this::setDefault);
    }


    private String visitArrayAssign(JmmNode jmmNode, String s) {
        ollirCode += jmmNode.get("value") + "[" + visit(jmmNode.getJmmChild(0)) + "]" + ".i32 :="+ parseType("int") + " "+ visit(jmmNode.getJmmChild(1)) + ";\n";
        return ollirCode;
    }

    private String visitArray(JmmNode jmmNode, String s) {
        String temp = nextTemp() + ".i32";
        ollirCode += temp + " :=.i32 " + visit(jmmNode.getJmmChild(0)) + "[" + visit(jmmNode.getJmmChild(1)) + "].i32;\n";
        return temp;
    }

    private String visitLength(JmmNode jmmNode, String varType) {
        var value = nextTemp() + ".i32";

        String p_code = "";
        var child = visit(jmmNode.getJmmChild(0),value);
        p_code += "\t\t" + value +  " :=.i32 " +  "arraylength(" + child + ")" + ".i32"  + ";\n";
        ollirCode += p_code;

        return value;

}

    private String visitNewArray(JmmNode jmmNode, String varType) {

        var value = nextTemp() + varType;

        // Take care of array indice
        String p_code = "";
        var child = visit(jmmNode.getJmmChild(0),value);
        p_code += "\t\t" + value +  " :=" + varType + " " +  child  + ";\n";
        ollirCode += p_code;

        // Take of the new array line
        String value2 = nextTemp() + ".array.i32";
        ollirCode += "\t\t" + value2 + " :=.array.i32 " + "new(" + "array, " + value + ")" + ".array.i32;\n";

        return value2;
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

        var boolCond = visit(jmmNode.getJmmChild(0),".bool");

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

    private String visitInteger(JmmNode jmmNode, String methodName) {  //nao uso
        return jmmNode.get("value") + parseType("int");
    }

    private String visitBoolean(JmmNode jmmNode, String methodName) {
        return jmmNode.get("value") + parseType("boolean");
    }


    private String visitIdentifier(JmmNode jmmNode, String idType) {
        var varName = jmmNode.get("value");
        System.out.println("imprime aqui: " + idType);
        if(table.isGlobal(varName)){
            var varType = getOllirType(varName);
            var value = nextTemp();
            String p_code = "";
            p_code += value + varType + " :=" + varType + " getfield(this, " +  varName + varType +")" + varType + ";\n";
            ollirCode += p_code;
            return value+varType;
            //val = value.substring(0, value.length() - 4) + varType;
        }

        /*
        if(table.isLocalVariable(varName,currentMethod)){
            String idType2 = "";
            for(JmmNode jmmNode1 : jmmNode.getAncestor("MethodDef").get().getChildren()) {
                if (jmmNode1.getKind().equals("VarDecl") && jmmNode1.get("name").equals(varName)) {
                    idType2 = parseType(jmmNode1.getJmmChild(0).get("name"), Boolean.parseBoolean(jmmNode1.getJmmChild(0).get("isArray")));
                }
            }
            return varName + idType2 ;*/


        return varName + idType;
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


        var lhsValue = visit(jmmNode.getJmmChild(0) ,assign_type);
        var rhsValue =  visit(jmmNode.getJmmChild(1) ,assign_type);

        var result = nextTemp()  + assign_type;

        var binOpCode = result + " :="+assign_type + " " + lhsValue + " "+ op+assign_type+" "
                + rhsValue +  ";\n";
        ollirCode += binOpCode;

        return result;
    }


    public String getOllirType(String varName) {
        List<Symbol> var_list = table.getLocalVariables(currentMethod);
        for (Symbol symbol : var_list) {
            if(symbol.getName().equals(varName)) {
                return parseType(symbol.getType().getName(),symbol.getType().isArray());
            }
        }

        List<Symbol> param_list = table.getParameters(currentMethod);
        for (Symbol symbol : param_list) {
            if(symbol.getName().equals(varName)) {
                return parseType(symbol.getType().getName(),symbol.getType().isArray());
            }
        }

        List<Symbol> fields_list = table.getFields();
        for (Symbol symbol : fields_list) {
            if(symbol.getName().equals(varName)) {
                return parseType(symbol.getType().getName(),symbol.getType().isArray());
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

            ollirCode += "\t\tret" +  parseType("int")+  " "+  visit(jmmNode.getJmmChild(0),"int") +  ";\n";
        }
       
        else if (jmmNode.getJmmChild(0).getKind().equals("Identifier") ) {

            ollirCode += "\t\tret"+ parseType(jmmNode.getJmmParent().getJmmChild(0).get("name"),Boolean.parseBoolean(jmmNode.getJmmParent().getJmmChild(0).get("isArray"))) + " " + visit(jmmNode.getJmmChild(0),parseType(jmmNode.getJmmParent().getJmmChild(0).get("name")))  + ";\n";

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

        if(!hasConstructor) ollirCode += emptyConstructor();
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
                System.out.println("caralhhooo " + parameter.getType().isArray());
                ollirCode += parameter.getName() + parseType(parameter.getType().getName(),parameter.getType().isArray()) + ", ";

            }
            ollirCode = ollirCode.substring(0, ollirCode.length() - 2);
        }
        ollirCode += ")" + parseType(jmmNode.getJmmChild(0).get("name"),Boolean.parseBoolean(jmmNode.getJmmChild(0).get("isArray")))+ " {\n";

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
            methodName = table.getMethod(jmmNode.getAncestor("MethodDef").get().get("signature")).getReturnType().getName();
        }

        JmmNode methodCall = jmmNode;
        if(jmmNode.getJmmChild(0).getKind().equals("MethodCall")){
            methodCall = jmmNode.getJmmChild(0);
        }
        JmmNode identifier = methodCall.getJmmChild(0);
        StringBuilder code = new StringBuilder();
        /*
        if(methodCall.getChildren().size() > 1){
            JmmNode arguments = methodCall.getJmmChild(1);
            visit(arguments, methodName);
        }*/
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
            if( grandchild.getKind().equals("ArrayAccess")){
                code.append(" , " + visit(grandchild.getJmmChild(0)));
            }
            else if(grandchild.getKind().equals("Length")){
                var res = visit(grandchild);
                code.append(" , " + res);
            }
            else if(!grandchild.get("value").equals(identifier.get("value")) ){
                code.append( " , " + visit(grandchild));
            }


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
        //System.out.println("VAR: " + var_name + " " + idType);


        var child = visit(jmmNode.getJmmChild(0),idType);


        if(table.isGlobal(var_name) && !table.isLocalVariable(var_name,currentMethod) ){ //2 cond p caso de ter varlocal igual a um field

            ollirCode += "\t\tputfield(this, " + var_name + idType +  ", " + child +")" +  ".V;\n";

            return ollirCode;

        }

        for(JmmNode jmmNode1 : jmmNode.getJmmParent().getChildren()) {
            if (jmmNode1.getKind().equals("VarDecl") && jmmNode1.get("name").equals(var_name)) {
                idType = parseType(jmmNode1.getJmmChild(0).get("name"), Boolean.parseBoolean(jmmNode1.getJmmChild(0).get("isArray")));
            }
        }

        //visit(jmmNode.getJmmChild(0),idType);
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
        else if(type.equals("String"))
            return r + "." +type;

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

        for (Symbol locVar : table.getLocalVariables(currentMethod)){
            if(locVar.getName().equals(jmmNode.getJmmParent().get("value"))){
                var val = nextTemp();
                var varType = jmmNode.get("value");
                String p_code = "";
                p_code += val + "."+varType + " :=." + varType + " new(" + jmmNode.get("value")+ ")."+ varType+ ";\n";
                p_code += "\t\tinvokespecial(" + val+"."+varType +  ", \"\").V;\n";
                ollirCode += p_code;
                return val+"."+varType;
            }
        }

        return "new(" + jmmNode.get("value") + ")" + idType;
    }

    public String getCode(){
        return ollirCode;}


    private String getAssigmentRetType(String op) {
        return switch (op) {
            case "+", "-", "*", "/" -> ".i32";
            case "&&", "<", "==", "!=" , "!" -> ".bool";
            default -> "." +op;
        };
    }

}


