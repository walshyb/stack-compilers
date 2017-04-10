// Hand-written S3 compiler
import java.io.*;
import java.util.*;
//======================================================
class S3
{  
  public static void main(String[] args) throws 
                                             IOException
  {
    System.out.println("S3 compiler written by Brandon Walsh"); 

    if (args.length != 1)
    {
      System.err.println("Wrong number cmd line args");  
      System.exit(1);
    }

    // set to true to debug token manager
    boolean debug = false;

    // build the input and output file names
    String inFileName = args[0] + ".s";
    String outFileName = args[0] + ".a";

    // construct file objects
    Scanner inFile = new Scanner(new File(inFileName));
    PrintWriter outFile = new PrintWriter(outFileName);

    // identify compiler/author in the output file
    outFile.println("; from S3 compiler written by Brandon Walsh"); 

    // construct objects that make up compiler
    S3SymTab st = new S3SymTab();
    S3TokenMgr tm =  new S3TokenMgr(inFile, outFile, debug);
    S3CodeGen cg = new S3CodeGen(outFile, st);
    S3Parser parser = new S3Parser(st, tm, cg);

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
}                                           // end of S3
//======================================================
interface S3Constants
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
  int STRING = 16;
  int QUOTE = 17;
  int READINT = 18;

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
    "<STRING>",
    "\"\"\"",
    "\"readint\""
  };
}                                  // end of S3Constants
//======================================================
class S3SymTab
{
  private ArrayList<String> symbol;
  //-----------------------------------------
  public S3SymTab()
  {
    symbol = new ArrayList<String>();
  }
  //-----------------------------------------
  public void enter(String s)
  {
    int index = symbol.indexOf(s);

    // if s is not in symbol, then add it 
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
}                                     // end of S3SymTab
//======================================================
class S3TokenMgr implements S3Constants
{
  private Scanner inFile;          
  private PrintWriter outFile;
  private boolean debug;
  private char currentChar;
  private int currentColumnNumber;
  private int currentLineNumber;
  private String inputLine;    // holds 1 line of input
  private Token token;         // holds 1 token
  private StringBuffer buffer; // token image built here
  //-----------------------------------------
  public S3TokenMgr(Scanner inFile, 
                    PrintWriter outFile, boolean debug)
  {
    this.inFile = inFile;
    this.outFile = outFile;
    this.debug = debug;
    currentChar = '\n';        //  '\n' triggers read
    currentLineNumber = 0;
    buffer = new StringBuffer();
  }
  //-----------------------------------------
  public Token getNextToken()
  {
    // skip whitespace
    while (Character.isWhitespace(currentChar))
      getNextChar();

    // construct token to be returned to parser
    token = new Token();   
    token.next = null;

    // save start-of-token position
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
    else 
    if (currentChar == '\"') {
      token.kind = STRING;
      buffer.setLength(0);  // clear buffer

      getNextChar();

      while(true){

        token.endLine = currentLineNumber;
        token.endColumn = currentColumnNumber;
        
        if (currentChar != '"') {
          buffer.append(currentChar);
          
        } else {
          break;
        } 
        getNextChar();
      }

      getNextChar();

      token.image = "\"" + buffer.toString() + "\"";
      
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
      // save buffer as String in token.image
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
      // save buffer as String in token.image
      token.image = buffer.toString();

      // check if keyword
      if (token.image.equals("println"))
        token.kind = PRINTLN;
      else if (token.image.equals("print")) 
        token.kind = PRINT;
      else if (token.image.equals("readint"))
        token.kind = READINT;
      else  // not a keyword so kind is ID
        token.kind = ID;
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
        case '"':
          token.kind = QUOTE;
          break;
        default:
          token.kind = ERROR;
          break;                               
      }

      // save currentChar as String in token.image
      token.image = Character.toString(currentChar);

      // save end-of-token position
      token.endLine = currentLineNumber;
      token.endColumn = currentColumnNumber;

      getNextChar();  // read beyond end of token
    }

    // token trace appears as comments in output file
    if (debug)
      outFile.printf(
        "; kd=%3d bL=%3d bC=%3d eL=%3d eC=%3d im=%s%n",
        token.kind, token.beginLine, token.beginColumn, 
        token.endLine, token.endColumn, token.image);

    return token;     // return token to parser
  }     
  //-----------------------------------------
  private void getNextChar()
  {
    if (currentChar == EOF)
      return;

    if (currentChar == '\n')        // need next line?
    {
      if (inFile.hasNextLine())     // any lines left?
      {
        inputLine = inFile.nextLine();  // get next line
        // output source line as comment
        outFile.println("; " + inputLine);
        inputLine = inputLine + "\n";   // mark line end
        currentColumnNumber = 0;
        currentLineNumber++;   
      }                                
      else  // at end of file
      {
         currentChar = EOF;
         return;
      }
    }

    // get next char from inputLine
    currentChar = 
                inputLine.charAt(currentColumnNumber++);
    // in S3, test for single-line comment goes here
    if(currentChar == '/' && inputLine.charAt(currentColumnNumber) == '/') {

      try {
        if (token.kind != STRING)         // check if current kind is a not a STRING
          currentChar = '\n';
      } catch (NullPointerException e) {  // if it throws a null pointer, then that means it's null and not a STRING
        currentChar = '\n';
      }
      
    }
  }
}                                   // end of S3TokenMgr
//======================================================
class S3Parser implements S3Constants
{
  private S3SymTab st;
  private S3TokenMgr tm;
  private S3CodeGen cg;
  private Token currentToken;
  private Token previousToken; 
  private int labelNumber = 0;
  //-----------------------------------------
  public S3Parser(S3SymTab st, S3TokenMgr tm, 
                                           S3CodeGen cg)
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
      currentToken.beginLine + ", column " + 
      currentToken.beginColumn + "." +
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
    if (currentToken.next != null)
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
    program();   // program is start symbol for grammar
  }
  //-----------------------------------------
  private void program()
  {
    statementList();
    cg.endCode();
    if (currentToken.kind != EOF)  //garbage at end?
      throw genEx("Expecting <EOF>");
  }
  //-----------------------------------------
  private void statementList()
  {
    switch(currentToken.kind)
    {
      case ID:
      case SEMICOLON:
      case PRINTLN:
        statement();
        statementList();
        break;
      case PRINT:
        statement();
        statementList();
        break;
      case READINT:
        statement();
        statementList();
        break;
      case LEFTBRACE:
        consume(LEFTBRACE);
        statementList();
        break;
      case RIGHTBRACE:
        consume(RIGHTBRACE);
        statementList();
        break;
      case EOF:
        ;
        break;
      default:
        throw genEx("Expecting statement or <EOF>");
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
      case READINT:
        readintStatement();
        break;
      case PRINT:
        printStatement();
        break;
      case SEMICOLON:
        nullStatement();
        break;
      case LEFTBRACE:
        consume(LEFTBRACE);
        statementList();
      case RIGHTBRACE:
        consume(RIGHTBRACE);
        statementList();
        break;
      default:         
        throw genEx("Expecting statement");
    }
  }
  //----------------------------------------
  private void readintStatement() {

    

    consume(READINT);
    consume(LEFTPAREN);

    Token t = currentToken;
    consume(ID);

    cg.emitInstruction("pc", t.image); 
    cg.emitInstruction("din");
    cg.emitInstruction("stav");  

    consume(RIGHTPAREN);
    consume(SEMICOLON);

  }
  //----------------------------------------
  private void nullStatement() {
    consume(SEMICOLON);
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
  private void assignmentTail() {

    Token t;

    if (getToken(1).kind == ID && getToken(2).kind == ASSIGN){
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
    else {
      expr();
      consume(SEMICOLON);
    }
    

  }
  //-----------------------------------------
  private void printlnStatement()
  {
    consume(PRINTLN);
    consume(LEFTPAREN);

    switch(currentToken.kind) {
      case STRING:
        printArg();
        consume(STRING);        
        break;
      case RIGHTPAREN:
        ;
        break;
      default:
        expr();
        cg.emitInstruction("dout");  
    }

    cg.emitInstruction("pc", "'\\n'");
    cg.emitInstruction("aout");
    
    consume(RIGHTPAREN);
    consume(SEMICOLON);
  }  
  //-----------------------------------------
  private void printArg() {
    Token t;
    t = currentToken; 
    String label = cg.getLabel ();
    cg.emitInstruction ("pc", label); 
    cg.emitInstruction ("sout"); 
    cg.emitdw ("^" + label, t.image);
  }
  //-----------------------------------------
  private void printStatement()
  {
    consume(PRINT);
    consume(LEFTPAREN);
    switch(currentToken.kind) {
      case STRING:
        printArg();
        consume(STRING);        
        break;
      case RIGHTPAREN:
        ;
        break;
      default:
        expr();
        cg.emitInstruction("dout");  
    }
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
        termList();
        break;
      case RIGHTPAREN:
      case RIGHTBRACE:
      case SEMICOLON:
        ;
        break;
      default:
        throw genEx("Expecting \"+\", \")\", or \";\"");
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
      case RIGHTBRACE:
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
      case PLUS:
        t = currentToken;

        switch(getToken(1).kind){
          case ID:
            consume(ID);
            cg.emitInstruction("pwc", t.image);
            break;
          case UNSIGNED: 
            consume(UNSIGNED);
            cg.emitInstruction("pwc", t.image);
            break;
          case PLUS:
            consume(PLUS);
            factor();
            break;
          case LEFTPAREN:
            consume(LEFTPAREN);
            expr();
            consume(RIGHTPAREN);
            break;
          case MINUS:
            factor();
            break;
        }
        
        break;
      case MINUS:
        consume(MINUS);
        t = currentToken;

        switch(getToken(1).kind) {
          case UNSIGNED:
            consume(UNSIGNED);
            cg.emitInstruction("pwc", "-" + t.image);
            break;
          case ID:
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
            consume(PLUS);
            factor();
            cg.emitInstruction("neg");
            break;
          case MINUS:
            consume(MINUS);
            factor();
            break;
        }
        
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
      case LEFTBRACE:
        consume(LEFTBRACE);
        statementList();
        consume(RIGHTBRACE);
        break;
      case SEMICOLON:
        nullStatement();
        break;
      default:
        throw genEx("Expecting factor");
    }
  }
}                                     // end of S3Parser
//======================================================
class S3CodeGen
{
  private PrintWriter outFile;
  private S3SymTab st;
  private int labelNumber = 0;
  //-----------------------------------------
  public S3CodeGen(PrintWriter outFile, S3SymTab st)
  {
    this.outFile = outFile;
    this.st = st;
  }
  //-----------------------------------------
  public void emitInstruction(String op)
  {
    outFile.printf("          %-4s%n", op); 
  }
  //-----------------------------------------
  public void emitInstruction(String op, String opnd)
  {           
    outFile.printf("          %-4s      %s%n", op,opnd); 
  }
  //-----------------------------------------
  public void emitdw(String label, String value)
  {           
    outFile.printf(
             "%-9s dw        %s%n", label + ":", value);
  }
  //-----------------------------------------
  public String getLabel() {
    return "@L" + labelNumber++;
  }
  //-----------------------------------------
  public void endCode()
  {
    outFile.println();
    emitInstruction("halt");

    int size = st.getSize();
    // emit dw for each symbol in the symbol table
    for (int i=0; i < size; i++) 
      emitdw(st.getSymbol(i), "0");
  }
}                                    // end of S3CodeGen
