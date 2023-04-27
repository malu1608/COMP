grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0]|([1-9][0-9]*) ;
ID : [$a-zA-Z_][$a-zA-Z_0-9]* ;
WS : [ \t\n\r\f]+ -> skip ;

SLC : '//' ~[\n]* -> skip ;
MLC : '/*' .*? '*/' -> skip ;

program
    : (importDeclaration)* classDeclaration EOF;

importDeclaration: 'import' path+=ID ( '.' path+=ID )* ';' #ImportDecl;

classDeclaration : 'class' classname=ID ('extends' name=ID)? '{' (varDeclaration)* (methodDeclaration)* '}' #ClassDecl;

varDeclaration : type name=ID ';' #VarDecl;

parameter: type name=ID #Param;

parameters : (parameter (',' parameter)*)? #Params;

//methodDeclaration : ('public')? ('static')? type name=ID '(' parameters ')' '{' (varDeclaration | statement )* ('return' expression ';')? '}' #MethodDef;

methodDeclaration : ('public')? ('static')? type name=ID '(' parameters ')' '{' (varDeclaration | statement )* (statement)? '}' #MethodDef;

type locals[boolean isArray=false]
    : name='int' ('[' ']' {$isArray=true;})?
    | name='String'('[' ']' {$isArray=true;})?
    | name='boolean'
    | name=ID
    ;

statement : '{' (statement)* '}' #Stat
    | 'if' '(' expression ')' statement 'else' statement #IfElseStat
    | 'while' '(' expression ')' statement #WhileStat
    | expression ';' #ExpStat
    | value=ID '=' expression ';' #AssignStat
    | value=ID '[' expression ']' '=' expression ';' #ArrayAssignStat
    | 'return' expression ';' #ReturnStmt
    ;

//returnStmt:



expression
    : '!' expression #Not
    |'(' expression ')' #UnaryOp
    | expression '[' expression ']' #ArrayAccess
    | expression '.'  'length' #Length
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-')  expression #BinaryOp
    | expression op=('<' | '>') expression #BinaryOp
    | expression op=('&&' | '||') expression #BinaryOp
    | expression '.' name=ID '(' (expression (',' expression)*)? ')' #MethodCall
    | 'new' 'int' '[' expression ']' #NewIntArray
    | 'new' value=ID '(' ')' #NewObject
    | value=INTEGER #Integer
    | value=('false' | 'true') #Boolean
    | 'this' #This
    | value= ID #Identifier;



