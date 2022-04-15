/*
 * File: PrintVisitor.java
 * Date: Spring 2022
 * Auth: 
 * Desc: 
 */

import java.io.PrintStream;


public class PrintVisitor implements Visitor {

  // output stream for printing
  private PrintStream out;
  // current indent level (number of spaces)
  private int indent = 0;
  // indentation amount
  private final int INDENT_AMT = 2;
  
  //------------------------------------------------------------
  // HELPER FUNCTIONS
  //------------------------------------------------------------
  
  private String getIndent() {
    return " ".repeat(indent);
  }

  private void incIndent() {
    indent += INDENT_AMT;
  }

  private void decIndent() {
    indent -= INDENT_AMT;
  }

  //------------------------------------------------------------
  // VISITOR FUNCTIONS
  //------------------------------------------------------------

  // Hint: To help deal with call expressions, which can be statements
  // or expressions, statements should not indent themselves and add
  // newlines. Instead, the function asking statements to print
  // themselves should add the indent and newlines.
  

  // constructor
  public PrintVisitor(PrintStream printStream) {
    out = printStream;
  }

  
  // top-level nodes

  @Override
  public void visit(Program node) throws MyPLException {
    for (TypeDecl d : node.tdecls) {
      d.accept(this);
      out.print("\n");
    }
    // print function decls second
    for (FunDecl d : node.fdecls) {
      d.accept(this);
      out.print("\n");
    }
  }

  @Override
  public void visit(TypeDecl node) throws MyPLException {
    out.print(getIndent() + "type ");
    out.print(node.typeName.lexeme() + " ");
    out.print("{\n");
    incIndent();
    for(VarDeclStmt v : node.vdecls) {
      out.print(getIndent());
      v.accept(this);
      out.print("\n");
    }
    decIndent();
    out.print(getIndent() +"}\n");
  }

  @Override
  public void visit(FunDecl node) throws MyPLException {
    out.print(getIndent() +"fun ");
    out.print(node.returnType.lexeme() + " ");
    out.print(node.funName.lexeme());
    out.print("(");
    int index = 1;
    incIndent();
    for(FunParam fp: node.params){
      out.print(fp.paramType.lexeme() + " " + fp.paramName.lexeme());
      if(index < node.params.size()){
        out.print(", ");
      }
      index++;
    }
    out.print(") {\n");
    for(Stmt s : node.stmts){
      out.print(getIndent());
      s.accept(this);
      out.print("\n");
    }
    decIndent();
    out.print(getIndent() +"}\n");

  }

  @Override
  public void visit(VarDeclStmt node) throws MyPLException {
    out.print("var ");
    if(node.typeName != null) {
      out.print(node.typeName.lexeme() + " ");
    }
    out.print(node.varName.lexeme() + " = ");
    node.expr.accept(this);
  }

  @Override
  public void visit(AssignStmt node) throws MyPLException {
    int index = 1;
    for(Token t: node.lvalue){
      out.print(t.lexeme());
      if(index < node.lvalue.size()){
        out.print(".");
      }
      index++;
    }
    out.print(" = ");
    node.expr.accept(this);
  }

  @Override
  public void visit(CondStmt node) throws MyPLException {
    out.print("if "); //IFS
    node.ifPart.cond.accept(this);
    out.print(" {\n");
    incIndent();
    int index = 1;
    for(Stmt s : node.ifPart.stmts){
      out.print(getIndent());
      s.accept(this);
      //if(index < node.ifPart.stmts.size()){
        out.print("\n");
      //}
      index++;
    }
    decIndent();
    out.print(getIndent() +"}");

    for(BasicIf bsi : node.elifs){ //ELIFS
      out.print("\n" +getIndent() +"elif ");
      bsi.cond.accept(this);
      out.print(" {\n");
      incIndent();
      index = 1;
      for(Stmt bss: bsi.stmts){
        out.print(getIndent());
        bss.accept(this);
        //if(index < bsi.stmts.size()){
          out.print("\n");
        //}
        if(index >= bsi.stmts.size()){
          decIndent();
          out.print(getIndent() +"}");
        }
        index++;
      }
    }


    if(node.elseStmts.size() > 0){
      out.print("\n" +getIndent() +"else ");
      out.print("{\n");
      index = 1;
      incIndent();
      for(Stmt elseStmts: node.elseStmts){
        out.print(getIndent());
        elseStmts.accept(this);
        //if(index < node.elseStmts.size()){
          out.print("\n");
        //}
        index++;
      }
      decIndent();
      out.print(getIndent() + "}");
    }

  }

  @Override
  public void visit(WhileStmt node) throws MyPLException {
    out.print("while ");
    node.cond.accept(this);
    out.print(" {\n");
    incIndent();
    int index = 1;
    for(Stmt s : node.stmts){
      out.print(getIndent());
      s.accept(this);
      //if(index < node.stmts.size()){
        out.print("\n");
      //}
      index++;
    }
    decIndent();
    out.print(getIndent() +"}");
  }


  @Override
  public void visit(ForStmt node) throws MyPLException {
    out.print("for ");
    incIndent();
    out.print(node.varName.lexeme() + " from ");
    node.start.accept(this);
    if(node.upto){
      out.print(" upto ");
    }
    else{
      out.print(" downto ");
    }
    node.end.accept(this);
    out.print(" {\n");
    for(Stmt s: node.stmts){
      out.print(getIndent());
      s.accept(this);
      out.print("\n");
    }
    decIndent();
    out.print(getIndent() +"}");

  }

  @Override
  public void visit(ReturnStmt node) throws MyPLException {
    out.print("return ");
    if(node.expr != null) {
      node.expr.accept(this);
    }
  }

  @Override
  public void visit(DeleteStmt node) throws MyPLException {
    out.print("delete " + node.varName.lexeme());
  }

  @Override
  public void visit(CallExpr node) throws MyPLException {
    out.print(node.funName.lexeme() + "(");
    int index = 1;
    for(Expr ex : node.args){
      ex.accept(this);
      if(index < node.args.size()){
        out.print(", ");
      }
      index++;
    }
    out.print(")");
  }

  @Override
  public void visit(SimpleRValue node) throws MyPLException {
    if(node.value.type() == TokenType.STRING_VAL){
      out.print("\"" + node.value.lexeme() +"\"");
    }
    else if(node.value.type() == TokenType.CHAR_VAL){
      out.print("\'" + node.value.lexeme() +"\'");
    }
    else{
      out.print(node.value.lexeme());
    }
  }

  @Override
  public void visit(NewRValue node) throws MyPLException {
    out.print("new ");
    out.print(node.typeName.lexeme());
  }

  @Override
  public void visit(IDRValue node) throws MyPLException {
    int index = 1;
    for(Token t: node.path){
      out.print(t.lexeme());
      if(index < node.path.size()){
        out.print(".");
      }
      index++;
    }
  }

  @Override
  public void visit(NegatedRValue node) throws MyPLException {
    out.print("neg ");
    node.expr.accept(this);
  }

  @Override
  public void visit(Expr node) throws MyPLException {
    if(node.logicallyNegated){
      out.print("(not ");
    }

    if(node.op != null) {
      out.print("(");
      node.first.accept(this);
      out.print(" "+ node.op.lexeme() + " ");
      node.rest.accept(this);
      out.print(")");
    }
    else{
      node.first.accept(this);
    }
    if(node.logicallyNegated){
      out.print(")");
    }

  }

  @Override
  public void visit(SimpleTerm node) throws MyPLException {
    node.rvalue.accept(this);
  }

  @Override
  public void visit(ComplexTerm node) throws MyPLException {
    node.expr.accept(this);
  }
  
}
