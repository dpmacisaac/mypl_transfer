/* 
 * File: Parser.java
 * Date: Spring 2022
 * Auth: Dominic MacIsaac
 * Desc: Parses tokens, checking for errors and putting statements together
 */


public class Parser {

  private Lexer lexer = null; 
  private Token currToken = null;
  private Token pastToken = null;
  private final boolean DEBUG = false;

  
  // constructor
  public Parser(Lexer lexer) {
    this.lexer = lexer;
  }

  // do the parse
  public void parse() throws MyPLException
  {
    // <program> ::= (<tdecl> | <fdecl>)*
    advance();
    while (!match(TokenType.EOS)) {
      if (match(TokenType.TYPE))
        tdecl();
      else
        fdecl();
    }
    advance(); // eat the EOS token
  }

  
  //------------------------------------------------------------ 
  // Helper Functions
  //------------------------------------------------------------

  // get next token
  private void advance() throws MyPLException {
    currToken = lexer.nextToken();
  }

  // advance if current token is of given type, otherwise error
  private void eat(TokenType t, String msg) throws MyPLException {
    if (match(t))
      advance();
    else
      error(msg);
  }

  // true if current token is of type t
  private boolean match(TokenType t) {
    return currToken.type() == t;
  }
  
  // throw a formatted parser error
  private void error(String msg) throws MyPLException {
    String s = msg + ", found '" + currToken.lexeme() + "' ";
    s += "at line " + currToken.line();
    s += ", column " + currToken.column();
    throw MyPLException.ParseError(s);
  }

  // output a debug message (if DEBUG is set)
  private void debug(String msg) {
    boolean DEBUG = false;
    if (DEBUG)
      System.out.println("[debug]: " + msg);
  }

  // return true if current token is a (non-id) primitive type
  private boolean isPrimitiveType() {
    return match(TokenType.INT_TYPE) || match(TokenType.DOUBLE_TYPE) ||
      match(TokenType.BOOL_TYPE) || match(TokenType.CHAR_TYPE) ||
      match(TokenType.STRING_TYPE);
  }

  // return true if current token is a (non-id) primitive value
  private boolean isPrimitiveValue() {
    return match(TokenType.INT_VAL) || match(TokenType.DOUBLE_VAL) ||
      match(TokenType.BOOL_VAL) || match(TokenType.CHAR_VAL) ||
      match(TokenType.STRING_VAL);
  }
    
  // return true if current token starts an expression
  private boolean isExpr() {
    return match(TokenType.NOT) || match(TokenType.LPAREN) ||
      match(TokenType.NIL) || match(TokenType.NEW) ||
      match(TokenType.ID) || match(TokenType.NEG) ||
      match(TokenType.INT_VAL) || match(TokenType.DOUBLE_VAL) ||
      match(TokenType.BOOL_VAL) || match(TokenType.CHAR_VAL) ||
      match(TokenType.STRING_VAL);
  }

  private boolean isOperator() {
    return match(TokenType.PLUS) || match(TokenType.MINUS) ||
      match(TokenType.DIVIDE) || match(TokenType.MULTIPLY) ||
      match(TokenType.MODULO) || match(TokenType.AND) ||
      match(TokenType.OR) || match(TokenType.EQUAL) ||
      match(TokenType.LESS_THAN) || match(TokenType.GREATER_THAN) ||
      match(TokenType.LESS_THAN_EQUAL) || match(TokenType.GREATER_THAN_EQUAL) ||
      match(TokenType.NOT_EQUAL);
  }

  
  //------------------------------------------------------------
  // Recursive Descent Functions 
  //------------------------------------------------------------

  private void tdecl() throws MyPLException { //done
    eat(TokenType.TYPE, "expecting type in tdecl");
    eat(TokenType.ID, "expecting ID in tdecl");
    eat(TokenType.LBRACE, "expecting LBRACE in tdecl");
    vdecls();
    eat(TokenType.RBRACE, "expecting RBRACE in tdecl");
  }

  private void vdecls() throws MyPLException{ //done
    while(match(TokenType.VAR)){
      vdecl_stmt();
    }
  }

  private void fdecl() throws MyPLException{ //done
    eat(TokenType.FUN, "expecting FUN in fdecl");
    if(!match(TokenType.VOID_TYPE)){
      dtype();
    }
    else{
      eat(TokenType.VOID_TYPE, "expecting VOID in fdecl");
    }
    eat(TokenType.ID, "expecting ID in fdecl");
    eat(TokenType.LPAREN, "expecting LPAREN in fdecl");
    params();
    eat(TokenType.RPAREN, "expecting RPAREN in fdecl");
    eat(TokenType.LBRACE, "expecting LBRACE in fdecl");
    stmts();
    eat(TokenType.RBRACE, "expecting RBRACE in fdecl");
  }

  private void params() throws MyPLException{ //done
    if(isPrimitiveType() || match(TokenType.ID)) {
      dtype();
      eat(TokenType.ID, "expecting ID in params");
      while (match(TokenType.COMMA)) {
        advance();
        dtype();
        eat(TokenType.ID, "expecting ID in params");
      }
    }

  }
  private void dtype() throws MyPLException{
    if(isPrimitiveType() || match(TokenType.ID)){
      advance();
    }
    else{
      error("expecting dtype");
    }
  }
  private void stmts() throws MyPLException{
    while(match(TokenType.VAR) || match(TokenType.ID) || match(TokenType.IF)
            || match(TokenType.WHILE) || match(TokenType.FOR)
            ||match(TokenType.RETURN) || match(TokenType.DELETE)){
      stmt();
    }
  }
  private void stmt() throws MyPLException{
    if(match(TokenType.VAR)){
      vdecl_stmt();
    }
    else if(match(TokenType.IF)){
      cond_stmt();
    }
    else if(match(TokenType.WHILE)){
      while_stmt();
    }
    else if(match(TokenType.FOR)){
      for_stmt();
    }
    else if(match(TokenType.RETURN)){
      ret_stmt();
    }
    else if(match(TokenType.DELETE)){
      delete_stmt();
    }
    else if(match(TokenType.ID)){
      pastToken = currToken;
      advance();
      if(match(TokenType.LPAREN)){
        call_expr();
      }
      else{
        assign_stmt();
      }
    }
    else{
      error("expecting stmt");
    }
  }

  private void vdecl_stmt() throws MyPLException{ //PROGRESS
    eat(TokenType.VAR, "expecting VAR in vdecl_stmt");
    if(isPrimitiveType() || match(TokenType.ID)){
      dtype();
    }
    if(!match(TokenType.ASSIGN)){
      eat(TokenType.ID, "expecting ID in vdecl_stmt");
    }
    eat(TokenType.ASSIGN, "expecting ASSIGN in vdecl_stmt");
    expr();
  }

  private void assign_stmt() throws MyPLException{ //done
    lvalue();
    eat(TokenType.ASSIGN, "expecting ASSIGN in assign_stmt");
    expr();
  }

  private void lvalue() throws MyPLException{ //done
    if(match(TokenType.ID) && pastToken == null){
      advance();
    }
    else if(pastToken.type() == TokenType.ID){
      pastToken = null;
    }
    else{
      error("expecting ID in lvalue");
    }
    //eat(TokenType.ID, "expecting ID in lvalue");
    while(match(TokenType.DOT)){
      advance();
      eat(TokenType.ID, "expecting ID in lvalue");
    }
  }

  private void cond_stmt() throws MyPLException{ //done
    eat(TokenType.IF, "expecting IF in cond_stmt");
    expr();
    eat(TokenType.LBRACE, "expecting LBRACE in cond_stmt");
    stmts();
    eat(TokenType.RBRACE, "expecting RBRACE in cond_stmt");
    condt();
  }

  private void condt() throws MyPLException{ //done
    if(match(TokenType.ELIF)) {
      advance();
      expr();
      eat(TokenType.LBRACE, "expecting LBRACE in condt");
      stmts();
      eat(TokenType.RBRACE, "expecting RBRACE in condt");
      condt();
    }
    else if (match(TokenType.ELSE)){
      advance();
      eat(TokenType.LBRACE, "expecting LBRACE in condt");
      stmts();
      eat(TokenType.RBRACE, "expecting RBRACE in condt");
    }
  }

  private void while_stmt() throws MyPLException{ //done
    eat(TokenType.WHILE, "expecting WHILE in while_stmt");
    expr();
    eat(TokenType.LBRACE, "expecting LBRACE in while_stmt");
    stmts();
    eat(TokenType.RBRACE, "expecting RBRACE in while_stmt");
  }

  private void for_stmt() throws MyPLException{ //done
    eat(TokenType.FOR, "expecting FOR in for_stmt");
    eat(TokenType.ID, "expecting ID in for_stmt");
    eat(TokenType.FROM, "expecting FROM in for_stmt");
    expr();
    if(match(TokenType.UPTO)){
      advance();
    }
    else if(match(TokenType.DOWNTO)){
      advance();
    }
    else{
      error("expecting UPTO or DOWNTO in for_stmt");
    }
    expr();
    eat(TokenType.LBRACE, "expecting LBRACE in for_stmt");
    stmts();
    eat(TokenType.RBRACE, "expecting RBRACE in for_stmt");
  }

  private void call_expr() throws MyPLException{ //done
    if(match(TokenType.ID) && pastToken == null){
      advance();
    }
    else if(pastToken.type() == TokenType.ID){
      pastToken = null;
    }
    else{
      error("expecting ID in call_expr");
    }
    eat(TokenType.LPAREN, "expecting LPAREN in call_expr");
    args();
    eat(TokenType.RPAREN, "expecting RPAREN in call_expr");
  }

  private void args() throws MyPLException{//done
    if(match(TokenType.NOT) || match(TokenType.LPAREN) || isPrimitiveValue()
            || match(TokenType.NIL) | match(TokenType.NEW)|| match(TokenType.NEG)
            || match(TokenType.ID)){
      expr();
    }
    while(match(TokenType.COMMA)){
      advance();
      expr();
    }
  }

  private void ret_stmt() throws MyPLException{ //done
    eat(TokenType.RETURN, "expecting RETURN in ret_stmt");
    if(match(TokenType.NOT) || match(TokenType.LPAREN) || isPrimitiveValue()
            || match(TokenType.NIL) | match(TokenType.NEW)|| match(TokenType.NEG)
            || match(TokenType.ID)){
      expr();
    }
  }

  private void delete_stmt() throws MyPLException{
    eat(TokenType.DELETE, "expecting DELETE in delete_stmt");
    eat(TokenType.ID, "expecting ID in delete_stmt");
  }

  private void expr() throws MyPLException{
    if(match(TokenType.NOT)){
      advance();
      expr();
    }
    else if(match(TokenType.LPAREN)){
      advance();
      expr();
      eat(TokenType.RPAREN, "expecting RPAREN in expr");
    }
    else{
      rvalue();
    }

    if(isOperator()){
      operator();
      expr();
    }

  }
  private void operator() throws MyPLException{
    if(isOperator()){
      advance();
    }
    else{
      error("expecting operator");
    }
  }

  private void rvalue() throws MyPLException{ // done
    if(isPrimitiveValue()){
      pval();
    }
    else if(match(TokenType.NIL)){
      advance();
    }
    else if(match(TokenType.NEG)){
      advance();
      expr();
    }
    else if(match(TokenType.NEW)){
      advance();
      eat(TokenType.ID, "expecting ID in rvalue");
    }
    else if(match(TokenType.ID)){
      pastToken = currToken;
      advance();
      if(match(TokenType.LPAREN)){
        call_expr();
      }
      else{
        idrval();
      }
    }
    else{
      error("expecting rvalue");
    }

  }

  private void pval() throws MyPLException{
    if(isPrimitiveValue()){
      advance();
    }
    else{
      error("expecting primitive value");
    }
  }

  private void idrval() throws MyPLException{
    if(match(TokenType.ID) && pastToken == null){
      advance();
    }
    else if(pastToken.type() == TokenType.ID){
      pastToken = null;
    }
    else{
      error("expecting ID in idrval");
    }
    while(match(TokenType.DOT)){
      advance();
      eat(TokenType.ID, "expecting ID in idrval");
    }
  }


}
