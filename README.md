# Java-- Compiler (JMM → JVM bytecode)

A compiler for **Java--**, a subset of Java, that turns source code into JVM bytecode through four stages.

```
source.jmm → Parser (ANTLR) → Semantic analysis → OLLIR (intermediate code) → Jasmin → JVM bytecode
```

## What it does
- **Parsing:** ANTLR grammar that builds the abstract syntax tree
- **Semantic analysis:** symbol table (classes, fields, methods, locals) and type checking with error reports
- **Intermediate code (OLLIR):** turns the tree into linear three-address code with explicit types, temporaries and labels
- **Backend:** generates Jasmin assembly, which becomes `.class` files that run on the JVM
- Optional optimisations

## Run
```bash
./gradlew installDist
./jmm path/to/file.jmm
```
Requires Java 17 and Gradle.

---
Academic project developed at FEUP (Faculty of Engineering, University of Porto) as part of the Informatics and Computing Engineering programme.
