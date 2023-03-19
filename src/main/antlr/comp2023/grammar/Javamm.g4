grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0-9]+ ;
ID : [$a-zA-Z_][$a-zA-Z_0-9]* ; //TODO
WS : [ \t\n\r\f]+ -> skip ;

SLC : '//' ~[\n]* -> skip ;
MLC : '/*' .*? '*/' -> skip ;

program
    : (importDeclaration)* classDeclaration EOF;

importDeclaration: 'import' path=ID ( '.' path=ID )* ';';

classDeclaration : 'class' classname=ID ('extends' name=ID)? '{' (varDeclaration)* (methodDeclaration)* '}';

varDeclaration : type name=ID ';';

parameter: type name=ID;

parameters : (parameter (',' parameter)*)?;

methodDeclaration : ('public')? ('static')? type name=ID '(' parameters ')' '{' (varDeclaration | statement )* ('return' expression ';')? '}' #MethodDef;

type locals[boolean isArray=false]
    : name='int' ('[' ']' {$isArray=true;})?
    | name='String'('[' ']' {$isArray=true;})?
    | name='boolean'
    | name=ID
    ;

statement : '{' (statement)* '}'
    | 'if' '(' expression ')' statement 'else' statement
    | 'while' '(' expression ')' statement
    | expression ';'
    | ID '=' expression ';'
    | ID '[' expression ']' '=' expression ';'
    ;

expression
    : '!' expression #Not
    |'(' expression ')' #UnaryOp
    | expression '[' expression ']' #ArrayAccess
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-')  expression #BinaryOp
    | expression op=('<' | '>') expression #BinaryOp
    | expression op=('&&' | '||') expression #BinaryOp
    | expression '.'  'length' #Length
    | expression '.' name=ID '(' (expression (',' expression)*)? ')' #MethodCall
    | 'new' 'int' '[' expression ']' #NewIntArray
    | 'new' ID '(' ')' #NewObject
    | value=INTEGER #INTEGER
    | value= ID #Identifier;



