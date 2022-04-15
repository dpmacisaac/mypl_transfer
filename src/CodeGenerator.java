/*
 * File: CodeGenerator.java
 * Date: Spring 2022
 * Auth: Dominic MacIsaac
 * Desc: Generates bytecode for each frame (function)
 */

import java.util.*;


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
  
  // helper function to clean up un-needed NOP instructions
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
  } //DONE
  
  public void visit(FunDecl node) throws MyPLException {
    // 1. create a new frame for the function
    currFrame = new VMFrame(node.funName.lexeme(),node.params.size());
    vm.add(currFrame);
    // 2. create a variable mapping for the frame
    varMap = new HashMap<>();
    currVarIndex = 0;
    // 3. store args
    for(FunParam param:node.params){
      varMap.put(param.paramName.lexeme(), currVarIndex);
      currFrame.instructions.add(VMInstr.STORE(currVarIndex));
      currVarIndex++;
    }
    // 4. visit statement nodes
    for(Stmt stmt: node.stmts){
      stmt.accept(this);
      fixCallStmt(stmt);
    }
    // 5. check to see if the last statement was a return (if not, add
    //    return nil)
    if(node.stmts == null|| node.stmts.size()==0 || !(node.stmts.get(node.stmts.size()-1) instanceof ReturnStmt)){
      currFrame.instructions.add(VMInstr.PUSH("nil"));
      currFrame.instructions.add(VMInstr.VRET());
    }

    fixNoOp();
  } //DONE
  
  public void visit(VarDeclStmt node) throws MyPLException {
    node.expr.accept(this);
    varMap.put(node.varName.lexeme(),currVarIndex);
    currFrame.instructions.add(VMInstr.STORE(currVarIndex));
    currVarIndex++;
  } //DONE
  
  public void visit(AssignStmt node) throws MyPLException {
    node.expr.accept(this);
    if(node.lvalue.size() == 1) {
      currFrame.instructions.add(VMInstr.STORE(varMap.get(node.lvalue.get(0).lexeme())));
    }
    else{
      currFrame.instructions.add(VMInstr.LOAD(varMap.get(node.lvalue.get(0).lexeme())));
      //currFrame.instructions.add(VMInstr.SWAP());
      for(int i = 1; i < node.lvalue.size()-1; i++){
        //currFrame.instructions.add(VMInstr.SWAP());
        currFrame.instructions.add(VMInstr.GETFLD(node.lvalue.get(i).lexeme()));
        //currFrame.instructions.add(VMInstr.SWAP());
      }
      currFrame.instructions.add(VMInstr.SWAP());
      currFrame.instructions.add(VMInstr.SETFLD(node.lvalue.get(node.lvalue.size()-1).lexeme()));
    }

  } //TODO: Add Paths
  
  public void visit(CondStmt node) throws MyPLException {

    node.ifPart.cond.accept(this);
    int jmpfIndex = currFrame.instructions.size();
    currFrame.instructions.add(VMInstr.JMPF(-1));
    ArrayList<Integer> jmpToEndIndexes = new ArrayList<>();

    for(Stmt stmt: node.ifPart.stmts){
      stmt.accept(this);
    }

    jmpToEndIndexes.add(currFrame.instructions.size());
    currFrame.instructions.add(VMInstr.JMP(-1)); //jump to end of conditional statements

    currFrame.instructions.set(jmpfIndex,VMInstr.JMPF(currFrame.instructions.size()));
    currFrame.instructions.add(VMInstr.NOP());

    for(BasicIf basicIf: node.elifs){
      basicIf.cond.accept(this);

      int elifFalseIndex = currFrame.instructions.size();
      currFrame.instructions.add(VMInstr.JMPF(-1));

      for(Stmt stmt: basicIf.stmts){
        stmt.accept(this);
      }

      jmpToEndIndexes.add(currFrame.instructions.size());
      currFrame.instructions.add(VMInstr.JMP(-1));

      currFrame.instructions.set(elifFalseIndex,VMInstr.JMPF(currFrame.instructions.size()));
      currFrame.instructions.add(VMInstr.NOP());
    }

    for(Stmt stmt: node.elseStmts){
      stmt.accept(this);
    }

    int bottom = currFrame.instructions.size();
    currFrame.instructions.add(VMInstr.NOP());

    for(Integer index: jmpToEndIndexes){
      currFrame.instructions.set(index,VMInstr.JMP(bottom));
    }

  } //DONE

  public void visit(WhileStmt node) throws MyPLException {
    int top = currFrame.instructions.size();
    node.cond.accept(this);
    int jmpfIndex = currFrame.instructions.size();
    currFrame.instructions.add(VMInstr.JMPF(-1));
    for(Stmt stmt: node.stmts){
      stmt.accept(this);
    }
    currFrame.instructions.add(VMInstr.JMP(top));
    currFrame.instructions.add(VMInstr.NOP());
    currFrame.instructions.set(jmpfIndex,VMInstr.JMPF(currFrame.instructions.size()));
  } //DONE

  public void visit(ForStmt node) throws MyPLException {

    //initialize the for loop
    node.start.accept(this);
    currFrame.instructions.add(VMInstr.STORE(currVarIndex));
    varMap.put(node.varName.lexeme(), currVarIndex);
    currVarIndex++;

    //loop
    //check
    int top = currFrame.instructions.size();
    currFrame.instructions.add(VMInstr.LOAD(varMap.get(node.varName.lexeme())));
    node.end.accept(this);
    if(node.upto){
      currFrame.instructions.add(VMInstr.CMPLE());
    }
    else{
      currFrame.instructions.add(VMInstr.CMPGE());
    }

    int jmpfIndex = currFrame.instructions.size();
    currFrame.instructions.add(VMInstr.JMPF(-1));

    for(Stmt stmt: node.stmts){
      stmt.accept(this);
    }
    //increment
    currFrame.instructions.add(VMInstr.LOAD(varMap.get(node.varName.lexeme())));
    currFrame.instructions.add(VMInstr.PUSH(1));
    if(node.upto){
      currFrame.instructions.add(VMInstr.ADD());
    }
    else{
      currFrame.instructions.add(VMInstr.SUB());
    }
    currFrame.instructions.add(VMInstr.STORE(varMap.get(node.varName.lexeme())));
    currFrame.instructions.add(VMInstr.JMP(top));
    //end of loop
    currFrame.instructions.add(VMInstr.NOP());
    currFrame.instructions.set(jmpfIndex,VMInstr.JMPF(currFrame.instructions.size()));
    //varMap.remove(node.varName.lexeme());
  }
  
  public void visit(ReturnStmt node) throws MyPLException {
    if(node.expr != null){
      node.expr.accept(this);
    }
    else{
      currFrame.instructions.add(VMInstr.PUSH("nil"));
    }
    currFrame.instructions.add(VMInstr.VRET());
  } //DONE
  
  public void visit(DeleteStmt node) throws MyPLException {
    currFrame.instructions.add(VMInstr.LOAD(varMap.get(node.varName.lexeme())));
    currFrame.instructions.add(VMInstr.FREE());
  } //DONE

  public void visit(CallExpr node) throws MyPLException {
    // push args (in order)
    for (Expr arg : node.args)
      arg.accept(this);
    // built-in functions:
    if (node.funName.lexeme().equals("print")) {
      currFrame.instructions.add(VMInstr.WRITE());
      currFrame.instructions.add(VMInstr.PUSH(VM.NIL_OBJ));
    }
    else if (node.funName.lexeme().equals("read")){
      currFrame.instructions.add(VMInstr.READ());
    }
    else if(node.funName.lexeme().equals("length")){
      currFrame.instructions.add(VMInstr.LEN());
    }
    else if(node.funName.lexeme().equals("length")){
      currFrame.instructions.add(VMInstr.LEN());
    }
    else if(node.funName.lexeme().equals("get")){
      currFrame.instructions.add(VMInstr.GETCHR());
    }
    else if(node.funName.lexeme().equals("stoi") || node.funName.lexeme().equals("dtoi")){
      currFrame.instructions.add(VMInstr.TOINT());
    }
    else if(node.funName.lexeme().equals("stod") || node.funName.lexeme().equals("itod")){
      currFrame.instructions.add(VMInstr.TODBL());
    }
    else if(node.funName.lexeme().equals("itos") || node.funName.lexeme().equals("dtos")){
      currFrame.instructions.add(VMInstr.TOSTR());
    }
    // user-defined functions
    else
      currFrame.instructions.add(VMInstr.CALL(node.funName.lexeme()));
  } //DONE
  
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
  } //DONE
  
  public void visit(NewRValue node) throws MyPLException {
    List<String> components = new ArrayList<>();
    components.addAll(typeInfo.components(node.typeName.lexeme()));
    currFrame.instructions.add(VMInstr.ALLOC(components));
    int i = 0;
    for(String component: components){
      currFrame.instructions.add(VMInstr.DUP());
      typeDecls.get(node.typeName.lexeme()).vdecls.get(i).expr.accept(this);
      //currFrame.instructions.add(VMInstr.PUSH(typeDecls.get(node.typeName.lexeme()).vdecls.get(i).expr));
      currFrame.instructions.add(VMInstr.SETFLD(component));
      i++;
    }
  } //DONE
  
  public void visit(IDRValue node) throws MyPLException {
    if (node.path.size() == 1) {
      currFrame.instructions.add(VMInstr.LOAD(varMap.get(node.path.get(0).lexeme())));
    }
    else{
      currFrame.instructions.add(VMInstr.LOAD(varMap.get(node.path.get(0).lexeme())));
      for(int i = 1; i<node.path.size(); i++){
        currFrame.instructions.add(VMInstr.GETFLD(node.path.get(i).lexeme()));
      }
    }

  } //DONE
      
  public void visit(NegatedRValue node) throws MyPLException {
    node.expr.accept(this);
    currFrame.instructions.add(VMInstr.NEG());
  } //DONE

  public void visit(Expr node) throws MyPLException {
    node.first.accept(this);
    if(node.rest != null) {
      node.rest.accept(this);
    }
    if(node.op != null){
      if(node.op.type() == TokenType.PLUS){
        currFrame.instructions.add(VMInstr.ADD());
      }
      else if(node.op.type() == TokenType.MINUS){
        currFrame.instructions.add(VMInstr.SUB());
      }
      else if(node.op.type() == TokenType.MULTIPLY){
        currFrame.instructions.add(VMInstr.MUL());
      }
      else if(node.op.type() == TokenType.DIVIDE){
        currFrame.instructions.add(VMInstr.DIV());
      }
      else if(node.op.type() == TokenType.MODULO){
        currFrame.instructions.add(VMInstr.MOD());
      }
      else if(node.op.type() == TokenType.AND){
        currFrame.instructions.add(VMInstr.AND());
      }
      else if(node.op.type() == TokenType.OR){
        currFrame.instructions.add(VMInstr.OR());
      }
      else if(node.op.type() == TokenType.MINUS){
        currFrame.instructions.add(VMInstr.SUB());
      }
      else if(node.op.type() == TokenType.LESS_THAN){
        currFrame.instructions.add(VMInstr.CMPLT());
      }
      else if(node.op.type() == TokenType.LESS_THAN_EQUAL){
        currFrame.instructions.add(VMInstr.CMPLE());
      }
      else if(node.op.type() == TokenType.GREATER_THAN){
        currFrame.instructions.add(VMInstr.CMPGT());
      }
      else if(node.op.type() == TokenType.GREATER_THAN_EQUAL){
        currFrame.instructions.add(VMInstr.CMPGE());
      }
      else if(node.op.type() == TokenType.EQUAL){
        currFrame.instructions.add(VMInstr.CMPEQ());
      }
      else if(node.op.type() == TokenType.NOT_EQUAL){
        currFrame.instructions.add(VMInstr.CMPNE());
      }
      else if(node.op.type() == TokenType.NEG){
        currFrame.instructions.add(VMInstr.NEG());
      }
    }

    if(node.logicallyNegated){
      currFrame.instructions.add(VMInstr.NOT());
    }
  } //DONE

  public void visit(SimpleTerm node) throws MyPLException {
    // defer to contained rvalue
    node.rvalue.accept(this);
  } //DONE
  
  public void visit(ComplexTerm node) throws MyPLException {
    // defer to contained expression
    node.expr.accept(this);
  } //DONE

}
