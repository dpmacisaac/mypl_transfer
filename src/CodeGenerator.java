/*
 * File: CodeGenerator.java
 * Date: Spring 2022
 * Auth:
 * Desc: 
 */

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


public class CodeGenerator implements Visitor {

  // the user-defined type and function type information
  private TypeInfo typeInfo = null;

  // the virtual machine to add the code to
  private VM vm = null;

  // the current frame
  private VMFrame currFrame = null;

  // mapping from variables to their indices (in the frame)
  private Map<String,Integer> varMap = null;

  // the current variable index (in the frame)
  private int currVarIndex = 0;

  // to keep track of the typedecl objects for initialization
  Map<String,TypeDecl> typeDecls = new HashMap<>();


  //----------------------------------------------------------------------
  // HELPER FUNCTIONS
  //----------------------------------------------------------------------
  
  // helper function to clean up uneeded NOP instructions
  private void fixNoOp() {
    int nextIndex = currFrame.instructions.size();
    // check if there are any instructions
    if (nextIndex == 0)
      return;
    // get the last instuction added
    VMInstr instr = currFrame.instructions.get(nextIndex - 1);
    // check if it is a NOP
    if (instr.opcode() == OpCode.NOP)
      currFrame.instructions.remove(nextIndex - 1);
  }

  private void fixCallStmt(Stmt s) {
    // get the last instuction added
    if (s instanceof CallExpr) {
      VMInstr instr = VMInstr.POP();
      instr.addComment("clean up call return value");
      currFrame.instructions.add(instr);
    }

  }
  
  //----------------------------------------------------------------------  
  // Constructor
  //----------------------------------------------------------------------

  public CodeGenerator(TypeInfo typeInfo, VM vm) {
    this.typeInfo = typeInfo;
    this.vm = vm;
  }

  
  //----------------------------------------------------------------------
  // VISITOR FUNCTIONS
  //----------------------------------------------------------------------
  
  public void visit(Program node) throws MyPLException {

    // store UDTs for later
    for (TypeDecl tdecl : node.tdecls) {
      // add a mapping from type name to the TypeDecl
      typeDecls.put(tdecl.typeName.lexeme(), tdecl);
    }
    // only need to translate the function declarations
    for (FunDecl fdecl : node.fdecls)
      fdecl.accept(this);
  }

  public void visit(TypeDecl node) throws MyPLException {
    // Intentionally left blank -- nothing to do here
  }
  
  public void visit(FunDecl node) throws MyPLException {
    // TODO: 
    // 1. create a new frame for the function
    // 2. create a variable mapping for the frame
    // 3. store args
    // 4. visit statement nodes
    // 5. check to see if the last statement was a return (if not, add
    //    return nil)
  }
  
  public void visit(VarDeclStmt node) throws MyPLException {
    // TODO
  }
  
  public void visit(AssignStmt node) throws MyPLException {
    // TODO
  }
  
  public void visit(CondStmt node) throws MyPLException {
    // TODO
  }

  public void visit(WhileStmt node) throws MyPLException {
    // TODO
  }

  public void visit(ForStmt node) throws MyPLException {
    // TODO
  }
  
  public void visit(ReturnStmt node) throws MyPLException {
    // TODO
  }
  
  
  public void visit(DeleteStmt node) throws MyPLException {
    // TODO
  }

  public void visit(CallExpr node) throws MyPLException {
    // TODO: Finish the following (partially completed)

    // push args (in order)
    for (Expr arg : node.args)
      arg.accept(this);
    // built-in functions:
    if (node.funName.lexeme().equals("print")) {
      currFrame.instructions.add(VMInstr.WRITE());
      currFrame.instructions.add(VMInstr.PUSH(VM.NIL_OBJ));
    }
    else if (node.funName.lexeme().equals("read"))
      currFrame.instructions.add(VMInstr.READ());

    // TODO: add remaining built in functions

    // user-defined functions
    else
      currFrame.instructions.add(VMInstr.CALL(node.funName.lexeme()));
  }
  
  public void visit(SimpleRValue node) throws MyPLException {
    if (node.value.type() == TokenType.INT_VAL) {
      int val = Integer.parseInt(node.value.lexeme());
      currFrame.instructions.add(VMInstr.PUSH(val));
    }
    else if (node.value.type() == TokenType.DOUBLE_VAL) {
      double val = Double.parseDouble(node.value.lexeme());
      currFrame.instructions.add(VMInstr.PUSH(val));
    }
    else if (node.value.type() == TokenType.BOOL_VAL) {
      if (node.value.lexeme().equals("true"))
        currFrame.instructions.add(VMInstr.PUSH(true));
      else
        currFrame.instructions.add(VMInstr.PUSH(false));        
    }
    else if (node.value.type() == TokenType.CHAR_VAL) {
      String s = node.value.lexeme();
      s = s.replace("\\n", "\n");
      s = s.replace("\\t", "\t");
      s = s.replace("\\r", "\r");
      s = s.replace("\\\\", "\\");
      currFrame.instructions.add(VMInstr.PUSH(s));
    }
    else if (node.value.type() == TokenType.STRING_VAL) {
      String s = node.value.lexeme();
      s = s.replace("\\n", "\n");
      s = s.replace("\\t", "\t");
      s = s.replace("\\r", "\r");
      s = s.replace("\\\\", "\\");
      currFrame.instructions.add(VMInstr.PUSH(s));
    }
    else if (node.value.type() == TokenType.NIL) {
      currFrame.instructions.add(VMInstr.PUSH(VM.NIL_OBJ));
    }
  }
  
  public void visit(NewRValue node) throws MyPLException {
    // TODO
  }
  
  public void visit(IDRValue node) throws MyPLException {
    // TODO
  }
      
  public void visit(NegatedRValue node) throws MyPLException {
    // TODO
  }

  public void visit(Expr node) throws MyPLException {
    // TODO
  }

  public void visit(SimpleTerm node) throws MyPLException {
    // defer to contained rvalue
    node.rvalue.accept(this);
  }
  
  public void visit(ComplexTerm node) throws MyPLException {
    // defer to contained expression
    node.expr.accept(this);
  }

}
