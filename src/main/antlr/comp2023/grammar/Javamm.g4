grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z_0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;

SLC : '//' ~[\n]* -> skip ;
MLC : '/*' .*? '*/' -> skip ;

program : (importDeclaration)* classDeclaration EOF;

importDeclaration: 'import' ID ( '.' ID )* ';';

classDeclaration : 'class' ID ('extends' ID)? '{' (varDeclaration)* (methodDeclaration)* '}';

varDeclaration : type ID ';';

parameter: type name=ID;

parameters : (parameter (',' parameter)*)?;

methodDeclaration : ('public')? ('static')? type ID '(' parameters ')' '{' (varDeclaration | statement )* ('return' expression ';')? '}';

type : 'int' ('[' ']')? | 'boolean' | ID ('[' ']')?;

statement : '{' (statement)* '}'
    | 'if' '(' expression ')' statement 'else' statement
    | 'while' '(' expression ')' statement
    | expression ';'
    | ID '=' expression ';'
    | ID '[' expression ']' '=' expression ';';

expression
    : '(' expression ')'
    | expression '[' expression ']'
    | expression ('*' | '/') expression
    | expression ('+' | '-')  expression
    | expression '<' expression
    | expression '&&' expression
    | expression '.' 'length'
    | expression '.' ID '(' (expression (',' expression)*)? ')'
    | 'new' 'int' '[' expression ']'
    | 'new' ID '(' ')'
    | '!' expression
    | INTEGER
    | 'true'
    | 'false'
    | ID
    | 'this';

