/*
 * File: Lexer.java
 * Date: Spring 2022
 * Auth: Dominic MacIsaac
 * Desc: Reads and Tokenizes input from MyPL.java
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class Lexer {

  private BufferedReader buffer; // handle to input stream
  private int line = 1;          // current line number
  private int column = 0;        // current column number
  private int currChar;
  private int tokenLine;
  private int tokenColumn;
  private Token returnToken;
  private StringBuilder lexeme;
  private boolean whitespaceTrue;


  //--------------------------------------------------------------------
  // Constructor
  //--------------------------------------------------------------------

  public Lexer(InputStream instream) {
    buffer = new BufferedReader(new InputStreamReader(instream));
  }


  //--------------------------------------------------------------------
  // Private helper methods
  //--------------------------------------------------------------------

  // Returns next character in the stream. Returns -1 if end of file.
  private int read() throws MyPLException {
    try {
      return buffer.read();
    } catch(IOException e) {
      error("read error", line, column + 1);
    }
    return -1;
  }


  // Returns next character without removing it from the stream.
  private int peek() throws MyPLException {
    int ch = -1;
    try {
      buffer.mark(1);
      ch = read();
      buffer.reset();
    } catch(IOException e) {
      error("read error", line, column + 1);
    }
    return ch;
  }


  // Print an error message and exit the program.
  private void error(String msg, int line, int column) throws MyPLException {
    msg = msg + " at line " + line + ", column " + column;
    throw MyPLException.LexerError(msg);
  }


  // Checks for whitespace
  public static boolean isWhitespace(int ch) {
    return Character.isWhitespace((char)ch);
  }


  // Checks for digit
  private static boolean isDigit(int ch) {
    return Character.isDigit((char)ch);
  }


  // Checks for letter
  private static boolean isLetter(int ch) {
    return Character.isLetter((char)ch);
  }


  // Checks if given symbol
  private static boolean isSymbol(int ch, char symbol) {
    return (char)ch == symbol;
  }


  // Checks if end-of-file
  private static boolean isEOF(int ch) {
    return ch == -1;
  }


  //--------------------------------------------------------------------
  // Public next_token function
  //--------------------------------------------------------------------

  //bazel build //:mypl
  //bazel test --test_output=errors //:lexer-test
  //bazel-bin/mypl

  // Returns next token in input stream
  public Token nextToken() throws MyPLException {
    returnToken = null;


    while(true){
      currChar = read();
      column++;
      tokenColumn = column;
      tokenLine = line;
      lexeme = new StringBuilder();

      //Whitespace
      checkWhiteSpace();
      //WORDS
      checkWord();
      if(returnToken != null){
        return returnToken;
      }
      //NUMBERS
      checkNumbers();
      if(returnToken != null){
        return returnToken;
      }

      //Special Characters
      if (isSymbol(currChar, '(')) {
        return new Token(TokenType.LPAREN, "(", tokenLine, tokenColumn);
      }
      else if (isSymbol(currChar, ')')) {
        return new Token(TokenType.RPAREN, ")", tokenLine, tokenColumn);
      }
      else if (isSymbol(currChar, '{')) {
        return new Token(TokenType.LBRACE, "{", tokenLine, tokenColumn);
      }
      else if (isSymbol(currChar, '}')) {
        return new Token(TokenType.RBRACE, "}", tokenLine, tokenColumn);
      }
      else if (isSymbol(currChar, ',')) {
        return new Token(TokenType.COMMA, ",", tokenLine, tokenColumn);
      }
      else if (isSymbol(currChar, '.')) {
        return new Token(TokenType.DOT, ".", tokenLine, tokenColumn);
      }

      //OPERATORS
      else if (isSymbol(currChar, '/')) {
        return new Token(TokenType.DIVIDE, "/", tokenLine, tokenColumn);
      }
      else if (isSymbol(currChar, '-')) {
        return new Token(TokenType.MINUS, "-", tokenLine, tokenColumn);
      }
      else if (isSymbol(currChar, '+')) {
        return new Token(TokenType.PLUS, "+", tokenLine, tokenColumn);
      }
      else if (isSymbol(currChar, '%')) {
        return new Token(TokenType.MODULO, "%", tokenLine, tokenColumn);
      }
      else if (isSymbol(currChar, '*')) {
        return new Token(TokenType.MULTIPLY, "*", tokenLine, tokenColumn);
      }
      else if (isSymbol(currChar, '=')) {
        if (isSymbol(peek(), '=')) {
          currChar = read();
          column++;
          return new Token(TokenType.EQUAL, "==", tokenLine, tokenColumn);
        }
        else{
          return new Token(TokenType.ASSIGN, "=", tokenLine, tokenColumn);
        }
      }
      else if (isSymbol(currChar, '>')) {
        if (isSymbol(peek(), '=')) {
          column++;
          currChar = read();
          return new Token(TokenType.GREATER_THAN_EQUAL, ">=", tokenLine, tokenColumn);
        } else {
          return new Token(TokenType.GREATER_THAN, ">", tokenLine, tokenColumn);
        }
      }
      else if (isSymbol(currChar, '<')) {
        if (isSymbol(peek(), '=')) {
          column++;
          currChar = read();
          return new Token(TokenType.LESS_THAN_EQUAL, "<=", tokenLine, tokenColumn);
        } else {
          return new Token(TokenType.LESS_THAN, "<", tokenLine, tokenColumn);
        }
      }
      else if (isSymbol(currChar, '!')) {
        currChar = read();
        column++;
        if (isSymbol(currChar, '=')) {
          return new Token(TokenType.NOT_EQUAL, "!=", tokenLine, tokenColumn);
        } else {
          error("expecting '=', found '" + (char)currChar + "'", tokenLine, column);
        }
      }

      //Strings and Chars
      else if (isSymbol(currChar, '"')) {
        currChar = read();
        while (true) {
          if (isSymbol(currChar, '"')) {
            column++;
            return new Token(TokenType.STRING_VAL, lexeme.toString(), tokenLine, tokenColumn);
          }
          else if (isEOF(currChar)) {
            error("found end-of-file in string", tokenLine, column);
          }
          else if(isSymbol(currChar, '\r') || isSymbol(currChar, '\n') ){
            column++;
            error("found newline within string", tokenLine, column);
          }
          else {
            lexeme.append((char) currChar);
            currChar = read();
            column++;
          }
        }
      }
      else if (isSymbol(currChar, '\'')) {
        boolean empty = true;
        currChar = read();
        boolean reading = true;
        boolean literal = false;
        while (reading) {
          if (isSymbol(currChar, '\'')) {
            if(empty){
              error("empty character", tokenLine, tokenColumn);
            }
            column++;
            return new Token(TokenType.CHAR_VAL, lexeme.toString(), tokenLine, tokenColumn);
          }
          else if (isEOF(currChar)) {
            error("empty character", tokenLine, tokenColumn);
          }/*
          else if(isSymbol(currChar, '\r')){
            if(isSymbol(peek(), '\n')){
              currChar = read();
              line++;
              column = 0;
              return new Token(TokenType.CHAR_VAL, lexeme.toString(), tokenLine, tokenColumn);
            }
          }*/
          else if(isSymbol(currChar, '\n')){
            tokenColumn++;
            error("found newline in character", tokenLine, tokenColumn);
          }
          else {
            lexeme.append((char)currChar);
            empty = false;

            if(isSymbol(currChar, '\\')) { //LITERAL CHECK
              currChar = read();
              column++;
              literal = true;
              if (isSymbol(currChar, 't') || isSymbol(currChar, 'n') || isSymbol(currChar, '\\')) {
                lexeme.append((char) currChar);
              }
            }

            if (lexeme.length() > 1 && !literal || lexeme.length() > 2 && literal || lexeme.length() == 1  && literal) {
              error("expecting ' found, '" + (char) currChar + "'", tokenLine, column);
            }
            column++;
            currChar = read();
          }
        }
      }

      //Comments
      else if (isSymbol(currChar, '#')) {
        currChar = read();
        while (true) {
          if(isSymbol(currChar, '\r') || isSymbol(currChar, '\n') ){
            checkWhiteSpace();
            break;
          }
          else {
            currChar = read();
          }
        }
      }
      //End of File
      else if(isEOF(currChar)){
        return new Token(TokenType.EOS, "end-of-file", tokenLine, column);
      }

      else{
        if(!whitespaceTrue) {
          error("invalid symbol '" + (char) currChar + "'", tokenLine, tokenColumn);
        }
      }
    }
  }

  private void checkWhiteSpace() throws MyPLException{
    if(isWhitespace(currChar)){
      if(isSymbol(currChar, '\r') || isSymbol(currChar,'\n') ){
        currChar = read();
        line++;
        column = 0;
      }
      else if (isSymbol(currChar, '\t')) {
        column = column + 4;
      }
      whitespaceTrue = true;
    }
  }

  private void checkWord() throws MyPLException{
    if(isLetter(currChar)){
      lexeme.append((char)currChar);
      boolean writing = true;

      while(writing) { //READING THE STRING TIL THE END
        if (isLetter(peek()) || isDigit(peek()) || isSymbol(peek(), '_')) {
          currChar = read();
          column++;
          lexeme.append((char) currChar);
        }
        else{
          writing = false;
        }
      }
      //CHECKS FOR IF THE WORD IS A DEFINED TERM
      if(lexeme.toString().equals("fun")){
        returnToken = new Token(TokenType.FUN, "fun", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("int")){
        returnToken = new Token(TokenType.INT_TYPE, "int", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("double")){
        returnToken = new Token(TokenType.DOUBLE_TYPE, "double", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("char")){
        returnToken = new Token(TokenType.CHAR_TYPE, "char", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("string")){
        returnToken = new Token(TokenType.STRING_TYPE, "string", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("bool")){
        returnToken = new Token(TokenType.BOOL_TYPE, "bool", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("true")){
        returnToken = new Token(TokenType.BOOL_VAL, "true", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("false")){
        returnToken = new Token(TokenType.BOOL_VAL, "false", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("and")){
        returnToken = new Token(TokenType.AND, "and", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("or")){
        returnToken = new Token(TokenType.OR, "or", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("if")){
        returnToken = new Token(TokenType.IF, "if", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("elif")){
        returnToken = new Token(TokenType.ELIF, "elif", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("else")){
        returnToken = new Token(TokenType.ELSE, "else", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("not")){
        returnToken = new Token(TokenType.NOT, "not", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("neg")){
        returnToken = new Token(TokenType.NEG, "neg", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("void")){
        returnToken = new Token(TokenType.VOID_TYPE, "void", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("var")){
        returnToken = new Token(TokenType.VAR, "var", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("for")){
        returnToken = new Token(TokenType.FOR, "for", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("while")){
        returnToken = new Token(TokenType.WHILE, "while", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("upto")){
        returnToken = new Token(TokenType.UPTO, "upto", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("downto")){
        returnToken = new Token(TokenType.DOWNTO, "downto", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("new")){
        returnToken = new Token(TokenType.NEW, "new", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("from")){
        returnToken = new Token(TokenType.FROM, "from", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("delete")){
        returnToken = new Token(TokenType.DELETE, "delete", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("nil")){
        returnToken = new Token(TokenType.NIL, "nil", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("return")){
        returnToken = new Token(TokenType.RETURN, "return", tokenLine, tokenColumn);
      }
      else if(lexeme.toString().equals("type")){
        returnToken = new Token(TokenType.TYPE, "type", tokenLine, tokenColumn);
      }
      else {
        returnToken = new Token(TokenType.ID, lexeme.toString(), tokenLine, tokenColumn);
      }
    }

  }

  private void checkNumbers() throws MyPLException{
    if(isDigit(currChar)){
      lexeme.append((char)currChar);
      boolean isDouble = false;
      boolean writing = true;

      while(writing){
        if(isDigit(peek())){
          currChar = read();
          column++;
          lexeme.append((char)currChar);
        }
        else if(isSymbol(peek(), '.')){
          if(isDouble){
            error("too many decimal points in double value '" + lexeme +"'", tokenLine, tokenColumn);
          }
          else{
            column++;
            currChar = read();
            lexeme.append((char)currChar);
            isDouble = true;
            if(!isDigit(peek())){
              error("missing decimal digit in double value '" + lexeme + "'", tokenLine, tokenColumn);
            }
          }
        }
        else if(isLetter(peek())){
          error("id cannot begin with a digit", tokenLine, tokenColumn);
        }
        else {
          writing = false;
        }
      }
      if(lexeme.charAt(0) == '0' && lexeme.length() > 1 && isDigit(lexeme.charAt(1)) && lexeme.charAt(1) != '0'){
        error("leading zero in '" + lexeme + "'", tokenLine, tokenColumn);
      }
      else if(isDouble){
        returnToken = new Token(TokenType.DOUBLE_VAL, lexeme.toString(), tokenLine, tokenColumn);
      }
      else {
        returnToken = new Token(TokenType.INT_VAL, lexeme.toString(), tokenLine, tokenColumn);
      }
    }
  }


}


