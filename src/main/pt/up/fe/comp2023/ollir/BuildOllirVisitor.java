package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.analysis.MySymbolTable;

import java.util.List;
import java.util.Objects;

import static pt.up.fe.comp2023.ollir.OllirUtils.getCode;

public class BuildOllirVisitor extends AJmmVisitor<String,String> {
    private final MySymbolTable table;
    private String ollirCode;

    public BuildOllirVisitor(MySymbolTable new_table) {
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
    private String ProgramVisit(JmmNode jmmNode, String s) {
        //List<Report> reports = new ArrayList<>();
        if (visit(jmmNode.getChildren().get(0)) == null) return "erro";
        else {
            for (String importString : table.getImports()) {
                ollirCode += "import " + importString + ";\n";
            }

            for(JmmNode child : jmmNode.getChildren()){
                //System.out.println("especie de filho:"+child.getKind());
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
        constructorCode.append(".construct ").append(table.getClassName()).append("().V {\n")
                .append("invokespecial(this, \"<init>\").V;\n").append("}\n\n");
        return constructorCode.toString();
    }
    private String visitClassDeclaration(JmmNode jmmNode, String s) {
        String class_name = table.getClassName();
        String super_class = table.getSuper();

        //se string nao nula adiciona extends e o nome da classe
        ollirCode += class_name + (super_class != null ? " extends " + super_class : "") + " {\n";

        //field da classe
        for(var field: table.getFields()){
            ollirCode += ".field " + field.getName() + parseType(field.getType().getName()) + ";\n";
        }

        boolean hasConstructor = false;

        for (JmmNode child : jmmNode.getChildren()) {
            //System.out.println("child: " + child);
            if (!child.getKind().equals("VarDecl")&&
                    !hasConstructor) {
                ollirCode+= emptyConstructor();
                hasConstructor = true;

            }
            visit(child);
        }

        ollirCode += "\n}\n";
        return ollirCode;
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

        ollirCode += mainStr.toString() ;
        System.out.println("table variables: " + table.getLocalVariables(jmmNode.get("name")));
        // Obter a lista de parâmetros do método
        List<Symbol> parameters = table.getParameters(jmmNode.get("name"));
        for (Symbol parameter : parameters) {
            System.out.println("parametro: " + parameter);
            System.out.println(parameter.getType().isArray());

            if (parameter.getType().isArray()) {
                ollirCode += parameter.getName()+ ".array." + parameter.getType().getName();
            } else {
                ollirCode += parameter.getType().getName();
                ollirCode += " " + parameter.getName() + parseType(parameter.getType().getName()) + ",";

            }
            //remove ultima virgula
            if(!Objects.equals(jmmNode.get("name"), "main")){
                if (parameters.get(parameters.size()-1) == parameter) {
                    ollirCode += ollirCode.substring(0,ollirCode.length()-1);
                }
            }

        }
        ollirCode += ")" + ".V {\n";
        ollirGeneratedCode.append(parametersCode).append(").").append(getCode(symbolTable.getReturnType(methodName))).append(" {\n");

        int index = 0;
        for (JmmNode methodChild: jmmNode.getChildren()) {
            if((jmmNode.getChildren().size() - 1 == index) && !methodChild.getKind().equals("Parameter")){
                visitReturnStatement(methodChild, jmmNode.get("name"));
                break;
            }

            if (!methodChild.getKind().equals("Type") && !methodChild.getKind().equals("Parameter")) {
                visit(methodChild, methodName);
            }
            index++;
        }

        if(table.getReturnType(jmmNode.get("name")).getName().equals("void")){
            ollirCode += "ret.V;\n";
        }

        return ollirCode;
    }

    private String visitObjectMethod(JmmNode jmmNode, String s) {
        StringBuilder methodStr = new StringBuilder();

        ollirCode += methodStr.toString();
        return ollirCode;
    }

    private String setDefault(JmmNode jmmNode, String s) {
        StringBuilder visitStr = new StringBuilder();
        /*
        for (JmmNode child : jmmNode.getChildren()) {
            visitStr.append(visit(child));
        }*/
        return visitStr.toString();
    }

    private String identifierVisit(JmmNode jmmNode, String s) {
        return "";
    }








    private String parseType(String type) {

        if (type.equals("int"))
            return ".i32";
        else if (type.equals("void"))
            return ".V";
        else if (type.equals("FALSE"))
            return "0.bool";
        else if (type.equals("TRUE"))
            return "1.bool";
        else if (type.equals("int array"))
            return "array.i32";
        else if(type.equals("boolean"))
            return ".bool";
        //else return "." falta!!
        return type;
    }

    public String getCode(){
        return ollirCode;
    }
}
