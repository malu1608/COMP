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

## My contribution (35% of the work)
I owned the **OLLIR generation stage** (`BuildOllirVisitor.java`), the step that connects the front end to the backend:
- type inference for every expression
- temporary variables for nested expressions, e.g. `a = a + 5;` becomes
  `temp0.i32 :=.i32 a.i32 +.i32 5.i32; a.i32 :=.i32 temp0.i32;`
- control flow for `if` / `while` with generated labels and gotos
- method calls, field access and array handling

I also worked on the symbol table and the launcher, and helped debug the full pipeline until the public and hidden test suites passed.

## Team
Group of 3 · Francisco Serra · **Maria Luísa Salvador** · Mariana Teixeira

## Run
```bash
./gradlew installDist
./jmm path/to/file.jmm
```
Requires Java 17 and Gradle.

---
Academic project developed at FEUP (Faculty of Engineering, University of Porto) as part of the Informatics and Computing Engineering programme.
