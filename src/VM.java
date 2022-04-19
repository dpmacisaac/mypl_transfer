/*
 * File: VM.java
 * Date: Spring 2022
 * Auth: Dominic MacIsaac
 * Desc: A bare-bones MyPL Virtual Machine. The architecture is based
 *       loosely on the architecture of the Java Virtual Machine
 *       (JVM).  Minimal error checking is done except for runtime
 *       program errors, which include: out of bound indexes,
 *       dereferencing a nil reference, and invalid value conversion
 *       (to int and double).
 */


import java.util.*;


/*----------------------------------------------------------------------

  TODO: Your main job for HW-6 is to finish the VM implementation
        below by finishing the handling of each instruction.

        Note that PUSH, NOT, JMP, READ, FREE, and NOP (trivially) are
        completed already to help get you started. 

        Be sure to look through OpCode.java to get a basic idea of
        what each instruction should do as well as the unit tests for
        additional details regarding the instructions.

        Note that you only need to perform error checking if the
        result would lead to a MyPL runtime error (where all
        compile-time errors are assumed to be found already). This
        includes things like bad indexes (in GETCHR), dereferencing
        and/or using a NIL_OBJ (see the ensureNotNil() helper
        function), and converting from strings to ints and doubles. An
        error() function is provided to help generate a MyPLException
        for such cases.

----------------------------------------------------------------------*/


class VM {

  // set to true to print debugging information
  private boolean DEBUG = false;
  
  // the VM's heap (free store) accessible via object-id
  private Map<Integer,Map<String,Object>> heap = new HashMap<>();
  
  // next available object-id
  private int objectId = 1111;
  
  // the frames for the program (one frame per function)
  private Map<String,VMFrame> frames = new HashMap<>();

  // the VM call stack
  private Deque<VMFrame> frameStack = new ArrayDeque<>();

  
  /**
   * For representing "nil" as a value
   */
  public static String NIL_OBJ = new String("nil");


  /** 
   * Add a frame to the VM's list of known frames
   * @param frame the frame to add
   */
  public void add(VMFrame frame) {
    frames.put(frame.functionName(), frame);
  }

  /**
   * Turn on/off debugging, which prints out the state of the VM prior
   * to each instruction. 
   * @param debug set to true to turn on debugging (by default false)
   */
  public void setDebug(boolean debug) {
    DEBUG = debug;
  }

  /**
   * Run the virtual machine
   */
  public void run() throws MyPLException {

    // grab the main stack frame
    if (!frames.containsKey("main"))
      throw MyPLException.VMError("No 'main' function");
    VMFrame frame = frames.get("main").instantiate();
    frameStack.push(frame);
    
    // run loop (keep going until we run out of frames or
    // instructions) note that we assume each function returns a
    // value, and so the second check below should never occur (but is
    // useful for testing, etc).
    while (frame != null && frame.pc < frame.instructions.size()) {
      // get next instruction
      VMInstr instr = frame.instructions.get(frame.pc);
      // increment instruction pointer
      ++frame.pc;

      // For debugging: to turn on the following, call setDebug(true)
      // on the VM.
      if (DEBUG) {
        System.out.println();
        System.out.println("\t FRAME........: " + frame.functionName());
        System.out.println("\t PC...........: " + (frame.pc - 1));
        System.out.println("\t INSTRUCTION..: " + instr);
        System.out.println("\t OPERAND STACK: " + frame.operandStack);
        System.out.println("\t HEAP ........: " + heap);
        System.out.println("TEST VARIABLE SIZE: ") ;
        int i = 0;
        for(Object vars: frame.variables) {
          System.out.print(i + ":"+vars.toString() + " ");
          i++;
        }
      }
      //------------------------------------------------------------
      // Consts/Vars
      //------------------------------------------------------------

      if (instr.opcode() == OpCode.PUSH) {
        frame.operandStack.push(instr.operand());
      } //DONE - EXAMPLE

      else if (instr.opcode() == OpCode.POP) {
        frame.operandStack.pop();
      } //DONE

      else if (instr.opcode() == OpCode.LOAD) {
        Integer y = (Integer)instr.operand();
        Object x = frame.variables.get(y);
        frame.operandStack.push(x);
      }
        
      else if (instr.opcode() == OpCode.STORE) {
        Object x = frame.operandStack.pop();
        int address = (int)instr.operand();
        try{
          frame.variables.remove(address);
        }catch(Exception e){}

        int count = frame.variables.size();

        while(frame.variables.size() < address) {
          frame.variables.add(count, NIL_OBJ);
          count++;
        }

        frame.variables.add(address, x);
      }

      
      //------------------------------------------------------------
      // Ops
      //------------------------------------------------------------

      else if (instr.opcode() == OpCode.ADD) {
        Object operandX = frame.operandStack.pop();
        Object operandY = frame.operandStack.pop();
        ensureNotNil(frame, operandX);
        ensureNotNil(frame, operandY);
        if(operandX instanceof Integer){
          frame.operandStack.push(((Integer)operandY + (Integer) operandX));
        }
        else if(operandX instanceof Double){
          frame.operandStack.push(((Double)operandY + (Double) operandX));
        }
        else if(operandX instanceof String){
          frame.operandStack.push(((String)operandY + (String) operandX));
        }
        else if(operandX instanceof Character){
          frame.operandStack.push(((String)operandY + (String) operandX));
        }
      } //DONE
      else if (instr.opcode() == OpCode.SUB) {
        Object operandX = frame.operandStack.pop();
        Object operandY = frame.operandStack.pop();
        ensureNotNil(frame, operandX);
        ensureNotNil(frame, operandY);
        if(operandX instanceof Integer){
          frame.operandStack.push(((Integer)operandY - (Integer) operandX));
        }
        else if(operandX instanceof Double){
          frame.operandStack.push(((Double)operandY - (Double) operandX));
        }
      } //DONE
      else if (instr.opcode() == OpCode.MUL) {
        Object operandX = frame.operandStack.pop();
        Object operandY = frame.operandStack.pop();
        ensureNotNil(frame, operandX);
        ensureNotNil(frame, operandY);
        if(operandX instanceof Integer){
          frame.operandStack.push(((Integer)operandY * (Integer) operandX));
        }
        else if(operandX instanceof Double){
          frame.operandStack.push(((Double)operandY * (Double) operandX));
        }
      } //DONE
      else if (instr.opcode() == OpCode.DIV) {
        Object operandX = frame.operandStack.pop();
        Object operandY = frame.operandStack.pop();
        ensureNotNil(frame, operandX);
        ensureNotNil(frame, operandY);
        if(operandX instanceof Integer){
          frame.operandStack.push(((int)operandY / (int) operandX));
        }
        else if(operandX instanceof Double){
          frame.operandStack.push(((double)operandY / (double) operandX));
        }
      } //DONE
      else if (instr.opcode() == OpCode.MOD) {
        Object operandX = frame.operandStack.pop();
        Object operandY = frame.operandStack.pop();
        ensureNotNil(frame, operandX);
        ensureNotNil(frame, operandY);
        frame.operandStack.push(((Integer)operandY % (Integer) operandX));
      } //DONE
      else if (instr.opcode() == OpCode.NEG) {
        Object operandX = frame.operandStack.pop();
        ensureNotNil(frame, operandX);
        if(operandX instanceof Integer){
          frame.operandStack.push((-1 *(Integer)operandX));
        }
        else if(operandX instanceof Double){
          frame.operandStack.push((-1 *(Double)operandX));
        }

      } //DONE

      else if (instr.opcode() == OpCode.AND) {
        Object operandX = frame.operandStack.pop();
        Object operandY = frame.operandStack.pop();
        ensureNotNil(frame, operandX);
        ensureNotNil(frame, operandY);
        frame.operandStack.push(((boolean)operandX && (boolean)operandY));
      } //DONE
      else if (instr.opcode() == OpCode.OR) {
        Object operandX = frame.operandStack.pop();
        Object operandY = frame.operandStack.pop();
        ensureNotNil(frame, operandX);
        ensureNotNil(frame, operandY);
        frame.operandStack.push(((boolean)operandX || (boolean)operandY));
      } //DONE
      else if (instr.opcode() == OpCode.NOT) {
        Object operand = frame.operandStack.pop();
        ensureNotNil(frame, operand);
        frame.operandStack.push(!(boolean)operand);
      } //DONE - EXAMPLE

      else if (instr.opcode() == OpCode.CMPLT) {
        Object operandX = frame.operandStack.pop();
        Object operandY = frame.operandStack.pop();
        ensureNotNil(frame, operandX);
        ensureNotNil(frame, operandY);
        if(operandX instanceof Integer){
          frame.operandStack.push(((Integer)operandY < (Integer) operandX));
        }
        else if(operandX instanceof Double){
          frame.operandStack.push(((Double)operandY < (Double) operandX));
        }
        else if(operandX instanceof String){
          String y = (String) operandY;
          String x = (String) operandX;
          frame.operandStack.push(y.compareTo(x) < 0);
        }
        else if(operandX instanceof Character){
          Character y = (Character) operandY;
          Character x = (Character) operandX;
          frame.operandStack.push(y.compareTo(x) < 0);
        }
      } //DONE
      else if (instr.opcode() == OpCode.CMPLE) {
        Object operandX = frame.operandStack.pop();
        Object operandY = frame.operandStack.pop();
        ensureNotNil(frame, operandX);
        ensureNotNil(frame, operandY);
        if(operandX instanceof Integer){
          frame.operandStack.push(((Integer)operandY <= (Integer) operandX));
        }
        else if(operandX instanceof Double){
          frame.operandStack.push(((Double)operandY <= (Double) operandX));
        }
        else if(operandX instanceof String){
          String y = (String) operandY;
          String x = (String) operandX;
          frame.operandStack.push(y.compareTo(x) <= 0);
        }
        else if(operandX instanceof Character){
          Character y = (Character) operandY;
          Character x = (Character) operandX;
          frame.operandStack.push(y.compareTo(x) <= 0);
        }
      } //DONE
      else if (instr.opcode() == OpCode.CMPGT) {
        Object operandX = frame.operandStack.pop();
        Object operandY = frame.operandStack.pop();
        ensureNotNil(frame, operandX);
        ensureNotNil(frame, operandY);
        if(operandX instanceof Integer){
          frame.operandStack.push(((Integer)operandY > (Integer) operandX));
        }
        else if(operandX instanceof Double){
          frame.operandStack.push(((Double)operandY > (Double) operandX));
        }
        else if(operandX instanceof String){
          String y = (String) operandY;
          String x = (String) operandX;
          frame.operandStack.push(y.compareTo(x) > 0);
        }
        else if(operandX instanceof Character){
          Character y = (Character) operandY;
          Character x = (Character) operandX;
          frame.operandStack.push(y.compareTo(x) > 0);
        }
      } //DONE
      else if (instr.opcode() == OpCode.CMPGE) {
        Object operandX = frame.operandStack.pop();
        Object operandY = frame.operandStack.pop();
        ensureNotNil(frame, operandX);
        ensureNotNil(frame, operandY);
        if(operandX instanceof Integer){
          frame.operandStack.push(((Integer)operandY >= (Integer) operandX));
        }
        else if(operandX instanceof Double){
          frame.operandStack.push(((Double)operandY >= (Double) operandX));
        }
        else if(operandX instanceof String){
          String y = (String) operandY;
          String x = (String) operandX;
          frame.operandStack.push(y.compareTo(x) >= 0);
        }
        else if(operandX instanceof Character){
          Character y = (Character) operandY;
          Character x = (Character) operandX;
          frame.operandStack.push(y.compareTo(x) >= 0);
        }
      } //DONE
      else if (instr.opcode() == OpCode.CMPEQ) {
        Object operandX = frame.operandStack.pop();
        Object operandY = frame.operandStack.pop();
        frame.operandStack.push(operandX.equals(operandY));
      } //DONE
      else if (instr.opcode() == OpCode.CMPNE) {
        Object operandX = frame.operandStack.pop();
        Object operandY = frame.operandStack.pop();
        frame.operandStack.push(!operandX.equals(operandY));
      } //DONE

      
      //------------------------------------------------------------
      // Jumps
      //------------------------------------------------------------
        
      else if (instr.opcode() == OpCode.JMP) {
        frame.pc = (int)instr.operand();
      } //DONE - EXAMPLE
      else if (instr.opcode() == OpCode.JMPF) {
        boolean jump = (boolean)frame.operandStack.pop();
        if(!jump){
          frame.pc = (int)instr.operand();
        }
      } //DONE
        
      //------------------------------------------------------------
      // Functions
      //------------------------------------------------------------

      else if (instr.opcode() == OpCode.CALL) {

        // TODO: 
        // (1) get frame and instantiate a new copy
        VMFrame frameX = frames.get((instr.operand().toString())).instantiate();
        frameStack.push(frameX);
        // (2) Pop argument values off stack and push into the newFrame
        List<Object> args = new ArrayList<>();
        for(int i = 0; i < frameX.argCount(); i++){
          Object x = frame.operandStack.pop();
          args.add(x);
        }
        frame = frameX;
        for(int i = 0; i < frameX.argCount(); i++){
          frame.operandStack.push(args.get(i));
        }

        // (3) Push the new frame onto frame stack
        // (4) Set the new frame as the current frame
      } // ?? DONE
        
      else if (instr.opcode() == OpCode.VRET) {
        // TODO:
        // (1) pop return value off of stack
        Object x = frame.operandStack.pop();
        // (2) remove the frame from the current frameStack
        frameStack.pop();
        // (3) set frame to the frame on the top of the stack
        frame = frameStack.peek();
        // (4) push the return value onto the operand stack of the frame
        if(frame != null) {
          frame.operandStack.push(x);
        }

      } // ?? DONE
        
      //------------------------------------------------------------
      // Built-ins
      //------------------------------------------------------------
        
      else if (instr.opcode() == OpCode.WRITE) {
        Object operand = frame.operandStack.pop();
        System.out.print(operand.toString());
      } //DONE

      else if (instr.opcode() == OpCode.READ) {
        Scanner s = new Scanner(System.in);
        frame.operandStack.push(s.nextLine());
      } //DONE - EXAMPLE

      else if (instr.opcode() == OpCode.LEN) {
        Object operand = frame.operandStack.pop();
        String x = (String)operand;
        frame.operandStack.push(x.length());
      } //DONE

      else if (instr.opcode() == OpCode.GETCHR) {
        // pop (string) x, pop y, push x.substring(y, y+1)
        Object operandString = frame.operandStack.pop();
        Object operandIndex = frame.operandStack.pop();
        String x = (String)operandString;
        int index = (int)operandIndex;
        if(index >= x.length() ||index < 0){
          error("out of bounds in GETCHR",frame);
        }
        x = x.substring(index, index+1);
        frame.operandStack.push(x);
      } //DONE

      else if (instr.opcode() == OpCode.TOINT) {
        Object operand = frame.operandStack.pop();
        Integer x = null;
        if(operand instanceof Double){
          double y  = (double) operand;
          x = (int) y;
        }
        else if(operand instanceof String){
          String y = (String) operand;
          try {
            x = Integer.valueOf(y);
          }catch(Exception e){
            error("nonDouble String", frame);
          }
        }
        frame.operandStack.push(x);
      } //DONE

      else if (instr.opcode() == OpCode.TODBL) {
        Object operand = frame.operandStack.pop();
        Double x = null;
        if(operand instanceof Integer){
          int y  = (int) operand;
          x = Double.valueOf(y);
        }
        else if(operand instanceof String){
          String y = (String) operand;
          try {
            x = Double.valueOf(y);
          }catch(Exception e){
            error("nonDouble String", frame);
          }
        }
        frame.operandStack.push(x);
      } //DONE

      else if (instr.opcode() == OpCode.TOSTR) {
        Object operand = frame.operandStack.pop();
        String x = operand.toString();
        frame.operandStack.push(x);
      } //DONE

      //------------------------------------------------------------
      // Heap related
      //------------------------------------------------------------

      else if (instr.opcode() == OpCode.ALLOC) {
        List<String> fields = (List<String>)instr.operand();
        Map<String,Object> fieldsMap = new HashMap<>();
        for(String fieldName: fields){
          fieldsMap.put(fieldName,NIL_OBJ);
        }
        heap.put(objectId, fieldsMap);
        frame.operandStack.push(objectId);
        objectId++;
      } //DONE

      else if (instr.opcode() == OpCode.FREE) {
        // pop the oid to 
        Object oid = frame.operandStack.pop();
        ensureNotNil(frame, oid);
        // remove the object with oid from the heap
        heap.remove((int)oid);
      } //DONE - EXAMPLE

      else if (instr.opcode() == OpCode.SETFLD) {
        Object val = frame.operandStack.pop();
        Object oid = frame.operandStack.pop();
        Map<String,Object> fieldsMap =  heap.get((int) oid);
        fieldsMap.put(instr.operand().toString(), val);
        heap.put((int) oid ,fieldsMap);
      }

      else if (instr.opcode() == OpCode.GETFLD) {
        //get field f: pop x, push obj(x).f value
        Object operand = frame.operandStack.pop();
        Object got = null;
        try {
           got = heap.get((int) operand).get(instr.operand().toString());
        }catch(Exception e){
          error("Invalid heap access", frame);
        }
        frame.operandStack.push(got);
      }

      //------------------------------------------------------------
      // Special instructions
      //------------------------------------------------------------
        
      else if (instr.opcode() == OpCode.DUP) {
        Object operandX = frame.operandStack.pop();
        frame.operandStack.push(operandX);
        frame.operandStack.push(operandX);
      } //DONE
      else if (instr.opcode() == OpCode.SWAP) {
        Object operandX = frame.operandStack.pop();
        Object operandY = frame.operandStack.pop();
        frame.operandStack.push(operandX);
        frame.operandStack.push(operandY);
      } //DONE
      else if (instr.opcode() == OpCode.NOP) {} //DONE

    }
  }

  // to print the lists of instructions for each VM Frame
  @Override
  public String toString() {
    String s = "";
    for (Map.Entry<String,VMFrame> e : frames.entrySet()) {
      String funName = e.getKey();
      s += "Frame '" + funName + "'\n";
      List<VMInstr> instructions = e.getValue().instructions;      
      for (int i = 0; i < instructions.size(); ++i) {
        VMInstr instr = instructions.get(i);
        s += "  " + i + ": " + instr + "\n";
      }
      // s += "\n";
    }
    return s;
  }

  //----------------------------------------------------------------------
  // HELPER FUNCTIONS
  //----------------------------------------------------------------------

  // error
  private void error(String m, VMFrame f) throws MyPLException {
    int pc = f.pc - 1;
    VMInstr i = f.instructions.get(pc);
    String name = f.functionName();
    m += " (in " + name + " at " + pc + ": " + i + ")";
    throw MyPLException.VMError(m);
  }

  // error if given value is nil
  private void ensureNotNil(VMFrame f, Object v) throws MyPLException {
    if (v == NIL_OBJ)
      error("Nil reference", f);
  }
  
  
}
