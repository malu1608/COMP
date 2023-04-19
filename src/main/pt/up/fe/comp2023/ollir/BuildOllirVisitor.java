package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.analysis.MySymbolTable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static pt.up.fe.comp2023.ollir.OllirUtils.getCode;
import static pt.up.fe.comp2023.ollir.OllirUtils.parseType;

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
        addVisit("ExpStat", this::visitObjectMethodCall);
        setDefaultVisit(this::setDefault);
    }
    private String ProgramVisit(JmmNode jmmNode, String s) {
        //List<Report> reports = new ArrayList<>();
        if (visit(jmmNode.getChildren().get(0)) == null) return "erro";
        else {
            for (String importString : table.getImports()) {
                ollirCode += "import " + importString + ";\n";
            }
            ollirCode += "\n";
            for(JmmNode child : jmmNode.getChildren()){
                //System.out.println("especie de filho:"+child.getKind());
                if(!child.getKind().equals("ImportDecl"))
                {
                    System.out.println("pai: " + child);
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
        ollirCode += class_name + (super_class != null ? " extends " + super_class : "") + " {\n\n";

        //field da classe
        for(var field: table.getFields()){
            ollirCode += "\t.field " + field.getName() + parseType(field.getType().getName()) + ";\n";
        }

        boolean hasConstructor = false;

        for (JmmNode child : jmmNode.getChildren()) {
            //System.out.println("child: " + child);
            if (!child.getKind().equals("VarDecl")&&
                    !hasConstructor) {
                ollirCode+= emptyConstructor();
                hasConstructor = true;

            }
            System.out.println("pai classe: " + child);
            visit(child);
        }
        ollirCode += "\n}\n";
        return ollirCode;
    }
    private String visitMethod(JmmNode jmmNode, String s) {
        StringBuilder mainStr = new StringBuilder();
        //JmmNode method_right = jmmNode.getJmmParent().getChildren().get(0);

        ollirCode += "\t.method public ";
        if (jmmNode.get("name").equals("main")) {
            ollirCode += "static ";
        }
        ollirCode += jmmNode.get("name") + "(";

        ollirCode += mainStr.toString();

        //System.out.println("table variables: " + table.getLocalVariables(jmmNode.get("name")));
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
                    ollirCode += parameter.getType().getName();
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
        ollirCode += ")" + ".V {\n";
        //ollirGeneratedCode.append(parametersCode).append(").").append(getCode(symbolTable.getReturnType(methodName))).append(" {\n");

        StringBuilder methodStr = new StringBuilder();
        for (JmmNode child : jmmNode.getChildren()) {

            if(child.equals("ExpStat")){

            }
            if(child.getKind().equals("Identifier")){
                ollirCode += "\t\tret" +  parseType(child.get("value")) + ";\n";
            }
            visit(child);
        }
        ollirCode += methodStr.toString() ;
        if (table.getReturnType(jmmNode.get("name")).getName().equals("void")) {
            ollirCode += "\t\tret" + parseType(table.getReturnType(jmmNode.get("name")).getName()) + ";\n";
        }
        ollirCode += "\t}\n\n";
        /*

        */
        return ollirCode;
    }

    private String visitObjectMethodCall(JmmNode method, String s) {
        //lida com metodos do import
        StringBuilder methodStr = new StringBuilder();
        System.out.println("method: " + method);
        if (method.getKind().equals("ExpStat")) {
        JmmNode methodCall = method.getJmmChild(0);
        JmmNode identifier = methodCall.getJmmChild(0);
        //System.out.println("oi"+methodCall + " " + identifier);
        if (identifier.getKind().equals("Identifier") && table.checkVariableInImports(identifier.get("value"))) {
            methodStr.append("\t\tinvokestatic(" + identifier.get("value") + ", \"" + methodCall.get("name") + "\"");

            if (methodCall.getChildren().size() > 1)
                methodStr.append(", ");
            else methodStr.append(").V;\n");
        }}
        ollirCode += methodStr.toString() ;
        if (table.getReturnType(method.get("name")).getName().equals("void")) {
            ollirCode += "\t\tret" + parseType(table.getReturnType(method.get("name")).getName()) + ";\n";
        }

        ollirCode += "\t}\n\n";
        return ollirCode;
    }
    public static String extrairNomeVariavel(String variavel) {
        int posicaoTipo = variavel.indexOf("Type");
        if (posicaoTipo == -1) {
            // se a string não contém "Type", retorna a string original
            return variavel;
        } else {
            // extrai o nome da variável (o que estiver antes de "Type")
            return variavel.substring(0, posicaoTipo);
        }
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
