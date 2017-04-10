// Hand-written S4 compiler
import java.io.*;
import java.util.*;
//======================================================
class S4
{  
  public static void main(String[] args) throws 
    IOException
    {
      System.out.println("S4 compiler written by Brandon Walsh");

      boolean debug = false;
      if (args.length >= 1)
        for (int i = 0; i < args.length - 1; i++)
          if (args[0].equalsIgnoreCase("-debug_token_manager"))  
            debug = true;
          else
          {
            System.err.println("Bad command line arg");
            System.exit(1);
          }
      else
      {
        System.err.println("No input file specified");
        System.exit(1);
      }

      // build the input and output file names
      String inFileName = args[args.length - 1] + ".s";
      String outFileName = args[args.length - 1] + ".a";

      // construct file objects
      Scanner inFile = new Scanner(new File(inFileName));
      PrintWriter outFile = new PrintWriter(outFileName);

      // identify compiler/author in the output file
      outFile.println("; from S4 compiler written by Brandon Walsh");

      // construct objects that make up compiler
      S4SymTab st = new S4SymTab();
      S4TokenMgr tm =  new S4TokenMgr(
          inFile, outFile, debug);
      S4CodeGen cg = new S4CodeGen(outFile, st);
      S4Parser parser = new S4Parser(st, tm, cg);

      // parse and translate
      try
      {
        parser.parse();
      }      
      catch (RuntimeException e) 
      {
        System.err.println(e.getMessage());
        outFile.println(e.getMessage());
        outFile.close();
        System.exit(1);
      }

      outFile.close();
    }
}                                           // end of S4
//======================================================
interface S4Constants
{
  // integers that identify token kinds
  int EOF = 0;
  int PRINTLN = 1;
  int UNSIGNED = 2;
  int ID = 3;
  int ASSIGN = 4;
  int SEMICOLON = 5;
  int LEFTPAREN = 6;
  int RIGHTPAREN = 7;
  int PLUS = 8;
  int MINUS = 9;
  int TIMES = 10;
  int ERROR = 11;
  int DIVIDE = 12;
  int LEFTBRACE = 13;
  int RIGHTBRACE = 14;
  int PRINT = 15;
  int READINT = 16;
  int STRING = 17;
  int WHILE = 18;
  int IF = 19;
  int ELSE = 20;
  int DO = 21;

  // tokenImage provides string for each token kind
  String[] tokenImage = 
  {
    "<EOF>",
    "\"println\"",
    "<UNSIGNED>",
    "<ID>",
    "\"=\"",
    "\";\"",
    "\"(\"",
    "\")\"",
    "\"+\"",
    "\"-\"",
    "\"*\"", 
    "<ERROR>",
    "\"/\"",
    "\"{\"",
    "\"}\"",
    "\"print\"",
    "\"readint\"",
    "<STRING>",
    "\"while\"",
    "\"if\"",
    "\"else\"",
    "\"do\""
  };
}                        // end of S4Constants interface
//======================================================
class S4SymTab
{
  private ArrayList<String> symbol;
  //-----------------------------------------
  public S4SymTab()
  {
    symbol = new ArrayList<String>();
  }                                    
  //-----------------------------------------
  public void enter(String s)
  {
    // if s is not in symbol, then add it
    int index = symbol.indexOf(s);
    if (index < 0)
      symbol.add(s);
  }
  //-----------------------------------------
  public String getSymbol(int index)
  {
    return symbol.get(index);
  }
  //-----------------------------------------
  public int getSize()
  {
    return symbol.size();
  }
}                               // end of S4SymTab class
//======================================================
class S4TokenMgr implements S4Constants
{
  private Scanner inFile;          
  private PrintWriter outFile;
  private boolean debug;
  private char currentChar;
  private int currentColumnNumber;
  private int currentLineNumber;
  private String inputLine;     // holds 1 line of input
  private Token token;          // holds 1 token
  private StringBuffer buffer;  // token image built here
  private boolean inString;
  //-----------------------------------------
  public S4TokenMgr(Scanner inFile, 
      PrintWriter outFile, boolean debug)
  {
    this.inFile = inFile;
    this.outFile = outFile;
    this.debug = debug;
    currentChar = '\n';        //  '\n' triggers read
    currentLineNumber = 0;
    buffer = new StringBuffer();
    inString = false;
  }
  //-----------------------------------------
  public Token getNextToken()
  {
    // skip whitespace
    while (Character.isWhitespace(currentChar))
      getNextChar();

    token = new Token();
    token.next = null;
    token.beginLine = currentLineNumber;
    token.beginColumn = currentColumnNumber;

    // check for EOF
    if (currentChar == EOF)
    {
      token.image = "<EOF>";
      token.endLine = currentLineNumber;
      token.endColumn = currentColumnNumber;
      token.kind = EOF;
    }

    else  // check for unsigned int
      if (Character.isDigit(currentChar)) 
      {
        buffer.setLength(0);  // clear buffer
        do  // build token image in buffer
        {
          buffer.append(currentChar);
          token.endLine = currentLineNumber;
          token.endColumn = currentColumnNumber;
          getNextChar();
        } while (Character.isDigit(currentChar));
        token.image = buffer.toString();
        token.kind = UNSIGNED;
      }

      else  // check for identifier
        if (Character.isLetter(currentChar)) 
        { 
          buffer.setLength(0);  // clear buffer
          do  // build token image in buffer
          {
            buffer.append(currentChar);
            token.endLine = currentLineNumber;
            token.endColumn = currentColumnNumber;
            getNextChar();
          } while (Character.isLetterOrDigit(currentChar));
          token.image = buffer.toString();

          // check if keyword
          if (token.image.equals("println"))
            token.kind = PRINTLN;
          else
            if (token.image.equals("print"))
              token.kind = PRINT;
            else
              if (token.image.equals("readint"))
                token.kind = READINT;
              else
                if (token.image.equals("while"))
                  token.kind = WHILE;
                else
                  if (token.image.equals("if"))
                    token.kind = IF;
                  else 
                    if (token.image.equals("do"))
                      token.kind = DO;
                    else
                      if (token.image.equals("else"))
                        token.kind = ELSE;
                      else  // not a keyword so kind is ID
                        token.kind = ID;
        }
        else if (currentChar == '"') {
          boolean done = false;
              inString = true;
              int backslashCounter = 0;
              buffer.setLength(0);  // clear buffer

            while (!done) {
              do  // build token image in buffer
              {
                if (currentChar == '\\'){
                  backslashCounter++;
                }

                buffer.append(currentChar);
                getNextChar();

                if (currentChar != '\\' && currentChar != '"'){
                  backslashCounter = 0;
                }

                try {
                  if (currentChar == '\\' && inputLine.charAt(currentColumnNumber+1) == '\n'){
                    getNextChar();
                  }
                } catch (Exception e) {
                  getNextChar();
                }

                if (currentChar == '\n' || currentChar == '\r') {
                  break;
                }
              } while (currentChar != '"');
              if (currentChar =='"' && backslashCounter % 2 == 0)
              {
                done = true;
                backslashCounter = 0;
                buffer.append(currentChar);
                token.kind = STRING;
              }
              else if (currentChar =='"' && backslashCounter % 2 != 0) {
                backslashCounter = 0;
                continue;
              }
              else
                token.kind = ERROR;
              token.endLine = currentLineNumber;
              token.endColumn = currentColumnNumber;
              getNextChar();
              token.image = buffer.toString();
              inString = false;
            }           
        }
        else  // process single-character token
          {  
            switch(currentChar)
            {
              case '=':
                token.kind = ASSIGN;
                break;                               
              case ';':
                token.kind = SEMICOLON;
                break;                               
              case '(':
                token.kind = LEFTPAREN;
                break;                               
              case ')':
                token.kind = RIGHTPAREN;
                break;                               
              case '+':
                token.kind = PLUS;
                break;                               
              case '-':
                token.kind = MINUS;
                break;                               
              case '*':
                token.kind = TIMES;
                break;                               
              case '/':
                token.kind = DIVIDE;
                break;                               
              case '{':
                token.kind = LEFTBRACE;
                break;                               
              case '}':
                token.kind = RIGHTBRACE;
                break;                               
              default:
                token.kind = ERROR;
                break;                               
            }

            // save currentChar as String in token.image
            token.image = Character.toString(currentChar);

            // save token end location
            token.endLine = currentLineNumber;
            token.endColumn = currentColumnNumber;

            getNextChar();  // read beyond end
          }

    // token trace appears as comments in output file
    if (debug)
      outFile.printf(
          "; kd=%3d bL=%3d bC=%3d eL=%3d eC=%3d im=%s%n",
          token.kind, token.beginLine, token.beginColumn, 
          token.endLine, token.endColumn, token.image);

    return token;
  }     
  //-----------------------------------------
  private void getNextChar()
  {
    if (currentChar == EOF)
      return;

    if (currentChar == '\n')
    {
      if (inFile.hasNextLine())     // any lines left?
      {
        inputLine = inFile.nextLine();  // get next line
        // output source line as comment
        outFile.println("; " + inputLine);
        inputLine = inputLine + "\n";   // mark line end
        currentLineNumber++;
        currentColumnNumber = 0;
      }                                
      else  // at EOF
      {
        currentChar = EOF;
        return;
      }
    }

    // check if single-line comment
    if (!inString &&
        inputLine.charAt(currentColumnNumber) == '/' &&
        inputLine.charAt(currentColumnNumber+1) == '/')
      currentChar = '\n';  // forces end of line
    else
      currentChar = 
        inputLine.charAt(currentColumnNumber++);
  }
}                             // end of S4TokenMgr class
//======================================================
class S4Parser implements S4Constants
{
  private S4SymTab st;
  private S4TokenMgr tm;
  private S4CodeGen cg;
  private Token currentToken;
  private Token previousToken; 
  //-----------------------------
  public S4Parser(S4SymTab st,S4TokenMgr tm,S4CodeGen cg)
  {
    this.st = st;
    this.tm = tm;
    this.cg = cg;   
    // prime currentToken with first token
    currentToken = tm.getNextToken(); 
    previousToken = null;
  }
  //-----------------------------------------
  // Construct and return an exception that contains
  // a message consisting of the image of the current
  // token, its location, and the expected tokens.
  //
  private RuntimeException genEx(String errorMessage)
  {
    return new RuntimeException("Encountered \"" + 
        currentToken.image + "\" on line " + 
        currentToken.beginLine + " column " + 
        currentToken.beginColumn +
        System.getProperty("line.separator") + 
        errorMessage);
  }
  //-----------------------------------------
  // Advance currentToken to next token.
  //
  private void advance()
  {
    previousToken = currentToken; 

    // If next token is on token list, advance to it.
    if (currentToken.next!=null)
      currentToken = currentToken.next;

    // Otherwise, get next token from token mgr and 
    // put it on the list.
    else
      currentToken = 
        currentToken.next = tm.getNextToken();
  }
  //-----------------------------------------
  // getToken(i) returns ith token without advancing
  // in token stream.  getToken(0) returns 
  // previousToken.  getToken(1) returns currentToken.
  // getToken(2) returns next token, and so on.
  //
  private Token getToken(int i)
  {
    if (i <= 0)
      return previousToken;

    Token t = currentToken;
    for (int j = 1; j < i; j++)  // loop to ith token
    {
      // if next token is on token list, move t to it
      if (t.next != null)
        t = t.next;

      // Otherwise, get next token from token mgr and 
      // put it on the list.
      else
        t = t.next = tm.getNextToken();
    }
    return t;
  }
  //-----------------------------------------
  // If the kind of the current token matches the
  // expected kind, then consume advances to the next
  // token. Otherwise, it throws an exception.
  //
  private void consume(int expected)
  {
    if (currentToken.kind == expected)
      advance();
    else
      throw genEx("Expecting " + tokenImage[expected]);
  }
  //-----------------------------------------
  public void parse()
  {
    program();
  }
  //-----------------------------------------
  private void program()
  {
    statementList();
    cg.endCode();
    if (currentToken.kind != EOF)
      throw genEx("Expecting <EOF>");
  }
  //-----------------------------------------
  private void statementList()
  {
    switch(currentToken.kind)
    {
      case ID:
      case PRINTLN:
      case PRINT:
      case SEMICOLON:
      case LEFTBRACE:
      case READINT:
      case WHILE:
      case DO:
      case IF:
        statement();
        statementList();
        break;
      case EOF:
      case RIGHTBRACE:
        ;
        break;
      default:
        throw 
          genEx("Expecting statement, \"}\", or <EOF>");
    }
  }
  //-----------------------------------------
  private void statement()
  {
    switch(currentToken.kind)
    {
      case ID: 
        assignmentStatement(); 
        break;
      case PRINTLN:    
        printlnStatement(); 
        break;
      case PRINT:
        printStatement();
        break;
      case SEMICOLON:
        nullStatement();
        break;
      case LEFTBRACE:
        compoundStatement();
        break;
      case READINT:
        readintStatement();
        break;
      case WHILE:
        whileStatement();
        break;
      case IF:
        ifStatement();
        break;
      case DO:
        doStatement();
        break;
      default:         
        throw genEx("Expecting statement");
    }
  }
  //-----------------------------------------
  private void assignmentStatement()
  {
    Token t;

    t = currentToken;
    consume(ID);
    st.enter(t.image);
    cg.emitInstruction("pc", t.image);
    consume(ASSIGN);
    assignmentTail();
    cg.emitInstruction("stav");
  }
  //-----------------------------------------
  private void whileStatement() {
    Token t = currentToken;

    consume(WHILE);
    String label1 = cg.getLabel();
    cg.emitLabel(label1);
    consume(LEFTPAREN);
    expr();
    consume(RIGHTPAREN);
    String label2 = cg.getLabel();
    cg.emitInstruction("jz", label2);
    statement();
    cg.emitInstruction("ja", label1);
    cg.emitLabel(label2);
  }
  //-----------------------------------------
  private void doStatement() {
    Token t = currentToken;

    consume(DO);

    String label1 = cg.getLabel();
    String label2 = cg.getLabel();

    cg.emitLabel(label1);

    statement();



    consume(WHILE);
    consume(LEFTPAREN);
    expr();


    cg.emitInstruction("jnz", label1);


    consume(RIGHTPAREN);
    consume(SEMICOLON);
  }
  //-----------------------------------------
  private void ifStatement() {
    Token t = currentToken;

    consume(IF);
    consume(LEFTPAREN);
    expr();
    consume(RIGHTPAREN);

    String label1 = cg.getLabel();
    cg.emitInstruction("jz", label1);

    statement();

    elsePart(label1);
  }
  //-----------------------------------------
  private void elsePart(String label1) {
    switch (currentToken.kind) {
      case ELSE:
        consume(ELSE);
        String label2 = cg.getLabel();

        cg.emitInstruction("ja", label2);
        cg.emitLabel(label1);

        statement();

        cg.emitLabel(label2);

        break;

      default:
        cg.emitLabel(label1);

    }

  }
  //-----------------------------------------
  private void assignmentTail()
  {
    Token t;
    if (getToken(1).kind == ID &&
        getToken(2).kind == ASSIGN)
    {
      t = currentToken;
      consume(ID);
      st.enter(t.image);
      cg.emitInstruction("pc", t.image);
      consume(ASSIGN);
      assignmentTail();
      cg.emitInstruction("dupe");
      cg.emitInstruction("rot");
      cg.emitInstruction("stav");
    }
    else
    {
      expr();
      consume(SEMICOLON);
    }
  }
  //-----------------------------------------
  private void printlnStatement()
  {
    consume(PRINTLN);
    consume(LEFTPAREN);
    if (currentToken.kind != RIGHTPAREN)
      printArg();
    cg.emitInstruction("pc", "'\\n'");
    cg.emitInstruction("aout");
    consume(RIGHTPAREN);
    consume(SEMICOLON);
  }
  //-----------------------------------------
  private void printArg()
  {     
    Token t;
    String label;

    if (currentToken.kind != STRING)
    {
      expr();
      cg.emitInstruction("dout");
    }
    else
    {
      t = currentToken;
      consume(STRING);
      label = cg.getLabel();
      cg.emitInstruction("pc", label);
      cg.emitInstruction("sout");
      cg.emitdw("^" + label, t.image);
    }
  }
  //-----------------------------------------
  private void printStatement()
  {
    consume(PRINT);
    consume(LEFTPAREN);
    printArg();
    consume(RIGHTPAREN);
    consume(SEMICOLON);
  }
  //-----------------------------------------
  private void nullStatement()
  {
    consume(SEMICOLON);
  }
  //-----------------------------------------
  private void compoundStatement()
  {
    consume(LEFTBRACE);
    statementList();
    consume(RIGHTBRACE);
  } 
  //-----------------------------------------
  private void readintStatement()
  {
    Token t;

    consume(READINT);
    consume(LEFTPAREN);
    t = currentToken;
    consume(ID);
    st.enter(t.image);
    cg.emitInstruction("pc", t.image);
    cg.emitInstruction("din");
    cg.emitInstruction("stav");
    consume(RIGHTPAREN);
    consume(SEMICOLON);
  } 
  //-----------------------------------------
  private void expr()
  {
    term();
    termList();
  }
  //-----------------------------------------
  private void termList()
  {
    switch(currentToken.kind)
    {
      case PLUS:
        consume(PLUS);
        term();
        cg.emitInstruction("add");
        termList();
        break;
      case MINUS:
        consume(MINUS);
        term();
        cg.emitInstruction("sub");
      case RIGHTPAREN:
      case SEMICOLON:
        ;
        break;
      default:
        throw 
          genEx("Expecting \"+\", \"-\", \")\", or \";\"");
    }
  }
  //-----------------------------------------
  private void term()
  {
    factor();
    factorList();
  }
  //-----------------------------------------
  private void factorList()
  {
    switch(currentToken.kind)
    {
      case TIMES:
        consume(TIMES);
        factor();
        cg.emitInstruction("mult");
        factorList();
        break;
      case DIVIDE:  
        consume(DIVIDE);
        factor();
        cg.emitInstruction("div");
        factorList();
        break;
      case PLUS:
      case MINUS:
      case RIGHTPAREN:
      case SEMICOLON:
        ;
        break;
      default:
        throw genEx("Expecting op, \")\", or \";\"");
    }
  }
  //-----------------------------------------
  private void factor()
  {  
    Token t;

    switch(currentToken.kind)
    {
      case UNSIGNED:
        t = currentToken;
        consume(UNSIGNED);
        cg.emitInstruction("pwc", t.image);
        break;
      case ID:
        t = currentToken;
        consume(ID);
        st.enter(t.image);
        cg.emitInstruction("p", t.image);
        break;
      case LEFTPAREN:
        consume(LEFTPAREN);
        expr();
        consume(RIGHTPAREN);
        break;
      case PLUS:
        consume(PLUS);
        factor();
        break;
      case MINUS:
        consume(MINUS);
        switch(currentToken.kind)
        {
          case UNSIGNED:
            t = currentToken;
            consume(UNSIGNED);
            cg.emitInstruction("pwc", "-" + t.image);
            break;
          case ID:
            t = currentToken;
            consume(ID);
            st.enter(t.image);
            cg.emitInstruction("p", t.image);
            cg.emitInstruction("neg");
            break;
          case LEFTPAREN:
            consume(LEFTPAREN);
            expr();
            consume(RIGHTPAREN);
            cg.emitInstruction("neg");
            break;
          case PLUS:
            do
            {
              consume(PLUS);
            }
            while (currentToken.kind == PLUS);
            if (currentToken.kind == MINUS)
            {
              consume(MINUS);
              factor();
            }
            else
            {
              factor();
              cg.emitInstruction("neg");
            }
            break;
          case MINUS:
            consume(MINUS);
            factor();
            break;
        }
        break;
      default:
        throw genEx("Expecting factor");
    }
  }
}                               // end of S4Parser class
//======================================================
class S4CodeGen
{
  private PrintWriter outFile;
  private S4SymTab st;
  private int labelNumber;
  //-----------------------------------------
  public S4CodeGen(PrintWriter outFile, S4SymTab st)
  {
    this.outFile = outFile;
    this.st = st;
    labelNumber = 0;
  }
  //-----------------------------------------
  public void emitInstruction(String op)
  {
    outFile.printf("          %-4s%n", op); 
  }
  //-----------------------------------------
  public void emitInstruction(String op, String opnd)
  {           
    outFile.printf(
        "          %-4s      %s%n", op, opnd); 
  }
  //-----------------------------------------
  public void emitdw(String label, String value)
  {           
    outFile.printf(
        "%-9s dw        %s%n", label + ":", value);
  }
  //-----------------------------------------
  public void emitLabel(String label) {
    outFile.printf(
        "%-9s %n", label + ":");
  }
  //-----------------------------------------
  public void endCode()
  {
    outFile.println();
    emitInstruction("halt");

    int size = st.getSize();
    // emit dw stmt for each symbol in the symbol table
    for (int i=0; i < size; i++) 
      emitdw(st.getSymbol(i), "0");
  }
  //-----------------------------------------
  public String getLabel()
  {
    return "@L" + labelNumber++;
  }
}                        // end of S4CodeGen class
