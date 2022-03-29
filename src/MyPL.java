/*
 * File: MyPL.java
 * Date: Spring 2022
 * Auth: S. Bowers
 * Desc: Driver program for HW-7
 */

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.Arrays;

public class MyPL {

  public static void main(String[] args) {
    try {

      boolean lexerMode = false;
      boolean parseMode = false;
      boolean printMode = false;
      boolean checkMode = false;
      boolean outIRMode = false;
      int argCount = args.length;
      InputStream input = System.in;

      // check for too many command line args
      if (argCount > 2) {
        displayUsageInfo();
        System.exit(1);
      }
      
      // check if in lexer or print mode
      if (argCount > 0 && args[0].equals("--lex"))
        lexerMode = true;
      else if (argCount > 0 && args[0].equals("--parse"))
        parseMode = true;
      else if (argCount > 0 && args[0].equals("--print"))
        printMode = true;
      else if (argCount > 0 && args[0].equals("--check"))
        checkMode = true;
      else if (argCount > 0 && args[0].equals("--ir"))
        outIRMode = true;

      // to check modes
      boolean specialMode = lexerMode || printMode || parseMode ||
        checkMode || outIRMode;

      // check if incorrect args 
      if (argCount == 2 && !specialMode) {
        displayUsageInfo();
        System.exit(1);
      }

      // grab input file
      String inFile = null;
      if (argCount == 2)
        input = new FileInputStream(args[1]);
      else if (argCount == 1 && !specialMode)
        input = new FileInputStream(args[0]);
      
      // create the lexer
      Lexer lexer = new Lexer(input);

      // run in lexer mode
      if (lexerMode) {
        Token t = lexer.nextToken();
        while (t.type() != TokenType.EOS) {
          System.out.println(t);
          t = lexer.nextToken();
        }
      }
      // run in parser mode
      else if (parseMode) {
        Parser parser = new Parser(lexer);
        parser.parse();
      }
      // run in print mode
      else if (printMode) {
        ASTParser parser = new ASTParser(lexer);
        Program program = parser.parse();
        PrintVisitor visitor = new PrintVisitor(System.out);
        program.accept(visitor);
      }
      // run in static checker mode
      else if (checkMode) {
        ASTParser parser = new ASTParser(lexer);
        Program program = parser.parse();
        TypeInfo typeInfo = new TypeInfo();
        StaticChecker checkVisitor = new StaticChecker(typeInfo);
        program.accept(checkVisitor);
      }
      // run in intermediate-representation mode
      else if (outIRMode) {
        ASTParser parser = new ASTParser(lexer);
        Program program = parser.parse();
        TypeInfo typeInfo = new TypeInfo();
        StaticChecker checkVisitor = new StaticChecker(typeInfo);
        program.accept(checkVisitor);
        VM vm = new VM();
        CodeGenerator genVisitor = new CodeGenerator(typeInfo, vm);
        program.accept(genVisitor);
        System.out.println(vm);
      }
      // run normally
      else {
        ASTParser parser = new ASTParser(lexer);
        Program program = parser.parse();
        TypeInfo typeInfo = new TypeInfo();
        program.accept(new StaticChecker(typeInfo));
        VM vm = new VM();
        CodeGenerator genVisitor = new CodeGenerator(typeInfo, vm);
        program.accept(genVisitor);
        vm.run();
      }
    }
    catch (MyPLException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    catch (FileNotFoundException e) {
      int i = args.length == 1 ? 0 : 1;
      System.err.println("ERROR: Unable to open file '" + args[i] + "'");
      System.exit(1);
    }
  }

  private static void displayUsageInfo() {
    System.out.println("Usage: ./mypl [flag] [script-file]");
    System.out.println("Options:");
    System.out.println("  --lex      Display token information.");
    System.out.println("  --parse    Check for valid syntax.");
    System.out.println("  --print    Pretty print the program.");
    System.out.println("  --check    Statically check program.");
    System.out.println("  --ir       Print intermediate code.");
  }
  
}
