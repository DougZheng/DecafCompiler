package parser;

import inter.Access;
import inter.And;
import inter.Arith;
import inter.Break;
import inter.Constant;
import inter.Do;
import inter.Else;
import inter.Expr;
import inter.For;
import inter.Id;
import inter.If;
import inter.Not;
import inter.Or;
import inter.Rel;
import inter.Seq;
import inter.Set;
import inter.SetElem;
import inter.Stmt;
import inter.Unary;
import inter.While;
import inter.Switch;
import inter.Cases;
import inter.Case;

import java.io.IOException;

import symbols.Array;
import symbols.Env;
import symbols.Type;
import lexer.Lexer;
import lexer.Tag;
import lexer.Token;
import lexer.Word;
import lexer.Num;

public class Parser {

	   private Lexer lex;    // lexical analyzer for this parser
	   private Token look;   // lookahead tagen
	   Env top = null;       // current or top symbol table
	   int used = 0;         // storage used for declarations

	   public Parser(Lexer l) throws IOException { lex = l; move(); }

	   void move() throws IOException { look = lex.scan(); }

	   void error(String s) { throw new Error("near line "+lex.line+": "+s); }

	   void match(int t) throws IOException {
	      if( look.tag == t ) move();
	      else error("syntax error");
	   }

	   public void program() throws IOException {  // program -> block
	      Stmt s = block();
	      int begin = s.newlabel();  
	      int after = s.newlabel();
	      s.emitlabel(begin); 
	      s.gen(begin, after); 
	      s.emitlabel(after);
	   }

	   Stmt block() throws IOException {  // block -> { decls stmts }
	      match('{');  Env savedEnv = top;  top = new Env(top);
	      decls(); Stmt s = stmts();
	      match('}');  top = savedEnv;
	      return s;
	   }

	   void decls() throws IOException {

	      while( look.tag == Tag.BASIC ) {   // D -> type ID ;
	         Type p = type(); Token tok = look; match(Tag.ID); match(';');
	         if(top.exists(tok)) error(tok.toString() + " redeclared"); // redeclared
	         Id id = new Id((Word)tok, p, used);
	         top.put( tok, id );
	         used = used + p.width;
	      }
	   }

	   Type type() throws IOException {

	      Type p = (Type)look;            // expect look.tag == Tag.BASIC 
	      match(Tag.BASIC);
	      if( look.tag != '[' ) return p; // T -> basic
	      else return dims(p);            // return array type
	   }

	   Type dims(Type p) throws IOException {
	      match('[');  Token tok = look;  match(Tag.NUM);  match(']');
	      if( look.tag == '[' )
	      p = dims(p);
	      return new Array(((Num)tok).value, p);
	   }

	   Stmt stmts() throws IOException {
	      if ( look.tag == '}' ) return Stmt.Null;
	      else return new Seq(stmt(), stmts()); // stmts -> stmts, stmt
	   }

	   Stmt stmt() throws IOException {
	      Expr x;  Stmt s, s1, s2;
	      Stmt savedStmt;         // save enclosing loop for breaks

	      switch( look.tag ) {

	      case ';':
	         move();
	         return Stmt.Null;

	      case Tag.IF:
	         match(Tag.IF); match('('); x = bool(); match(')');
	         s1 = stmt();
	         if( look.tag != Tag.ELSE ) return new If(x, s1); // stmt -> if(bool)stmt
	         match(Tag.ELSE);
	         s2 = stmt();
	         return new Else(x, s1, s2);

	      case Tag.WHILE:
	         While whilenode = new While();
	         savedStmt = Stmt.Enclosing; Stmt.Enclosing = whilenode;
	         match(Tag.WHILE); match('('); x = bool(); match(')');
	         s1 = stmt();
	         whilenode.init(x, s1);
	         Stmt.Enclosing = savedStmt;  // reset Stmt.Enclosing
	         return whilenode; // stmt -> while(bool)stmt

	      case Tag.DO:
	         Do donode = new Do();
	         savedStmt = Stmt.Enclosing; Stmt.Enclosing = donode;
	         match(Tag.DO);
	         s1 = stmt();
	         match(Tag.WHILE); match('('); x = bool(); match(')'); match(';');
	         donode.init(s1, x);
	         Stmt.Enclosing = savedStmt;  // reset Stmt.Enclosing
	         return donode; // stmt -> do stmt while (bool);

	      case Tag.BREAK:
	         match(Tag.BREAK); match(';');
	         return new Break(); // stmt -> break;
	         
	      case Tag.FOR:
	    	 For fornode = new For();
	    	 savedStmt = Stmt.Enclosing; Stmt.Enclosing = fornode;
	    	 match(Tag.FOR);				match('('); 
	    	 Stmt fors1 = forassign(); 		match(';');
	    	 Expr forx = bool(); 			match(';'); 
	    	 Stmt fors2 = forassign();		match(')');
	    	 Stmt fors3 = stmt(); 
	    	 fornode.init(fors1, forx, fors2, fors3);
	    	 Stmt.Enclosing = savedStmt;
	    	 return fornode; // stmt -> for(stmt;bool;stmt)stmt

	      case Tag.SWITCH:
	    	  Switch switchnode = new Switch();
	    	  savedStmt = Stmt.Enclosing; Stmt.Enclosing = switchnode;
	    	  match(Tag.SWITCH); match('(');
	    	  x = expr(); match(')'); match('{');
	    	  Cases c = cases(); // cases
	    	  s = null;
	    	  if(look.tag == Tag.DEFAULT) {
	    		  match(Tag.DEFAULT); match(':'); // default
	    		  s = stmt();
	    	  }
	    	  match('}');
	    	  switchnode.init(x, c, s);
	    	  Stmt.Enclosing = savedStmt;
	    	  return switchnode;
	    	 
	      case '{':
	         return block();

	      default:
	         return assign(); // stmt -> loc = bool;
	      }
	   }
	   
	   Cases cases() throws IOException{
		   Expr x; Stmt s; Cases c = null;
		   do {
			   match(Tag.CASE);
			   x = expr(); match(':'); s = stmt();
			   c = new Cases(x, s, c);
		   }
		   while(look.tag == Tag.CASE);
		   return c;
	   }
	   
	   Stmt forassign() throws IOException {
	      Stmt stmt;  Token t = look;
	      match(Tag.ID);
	      Id id = top.get(t);
	      if( id == null ) error(t.toString() + " undeclared");
	      if( look.tag == '=' ) {       // S -> id = E ;
	         move();  stmt = new Set(id, bool());
	      }
	      else {                        // S -> L = E ;
	         Access x = offset(id);
	         match('=');  stmt = new SetElem(x, bool());
	      }
	      // forassign, no match(";")
	      return stmt;
	   }

	   Stmt assign() throws IOException {
	      Stmt stmt;  Token t = look;
	      match(Tag.ID);
	      Id id = top.get(t); // search t
	      if( id == null ) error(t.toString() + " undeclared");

	      if( look.tag == '=' ) {       // S -> id = E ;
	         move();  stmt = new Set(id, bool());
	      }
	      else {                        // S -> L = E ;
	         Access x = offset(id);
	         match('=');  stmt = new SetElem(x, bool());
	      }
	      match(';');
	      return stmt;
	   }

	   // bool -> bool || join | join
	   // => bool -> join A
	   //    A -> || join A | ¦Å
	   Expr bool() throws IOException {
	      Expr x = join();
	      while( look.tag == Tag.OR ) {
	         Token tok = look;  move();  x = new Or(tok, x, join());
	      }
	      return x;
	   }

	   //    join -> join && equality | equality
	   // => join -> equality A
	   //    A -> && equality A | ¦Å
	   Expr join() throws IOException {
	      Expr x = equality();
	      while( look.tag == Tag.AND ) {
	         Token tok = look;  move();  x = new And(tok, x, equality());
	      }
	      return x;
	   }

	   //    equality -> equality == rel | equality != rel | rel
	   // => equality -> rel B
	   //    A -> == rel | != rel
	   //    B -> AB | ¦Å
	   Expr equality() throws IOException {
	      Expr x = rel();
	      while( look.tag == Tag.EQ || look.tag == Tag.NE ) {
	         Token tok = look;  move();  x = new Rel(tok, x, rel());
	      }
	      return x;
	   }

	   
	   Expr rel() throws IOException {
	      Expr x = expr();
	      switch( look.tag ) { // rel -> expr < expr | expr <= expr | expr >= expr | expr > expr
	      case '<': case Tag.LE: case Tag.GE: case '>':
	         Token tok = look;  move();  return new Rel(tok, x, expr());
	      default: // rel -> expr
	         return x;
	      }
	   }

	   //    expr -> expr + term | expr - term | term
	   // => expr -> term B
	   //    A -> + term | - term
	   //    B -> AB | ¦Å
	   Expr expr() throws IOException {
	      Expr x = term();
	      while( look.tag == '+' || look.tag == '-' ) {
	         Token tok = look;  move();  x = new Arith(tok, x, term());
	      }
	      return x;
	   }

	   //    term -> term * unary | term / unary | unary
	   // => term -> unary B
	   //    A -> * unary | / unary
	   //    B -> AB | ¦Å
	   Expr term() throws IOException {
	      Expr x = unary();
	      while(look.tag == '*' || look.tag == '/' ) {
	         Token tok = look;  move();   x = new Arith(tok, x, unary());
	      }
	      return x;
	   }

	   Expr unary() throws IOException {
	      if( look.tag == '-' ) { // unary -> - unary
	         move();  return new Unary(Word.minus, unary());
	      }
	      else if( look.tag == '!' ) { // unary -> ! unary
	         Token tok = look;  move();  return new Not(tok, unary());
	      }
	      else return factor(); // unary -> factor
	   }

	   Expr factor() throws IOException {
	      Expr x = null;
	      switch( look.tag ) {
	      case '(': // factor -> (bool)
	         move(); x = bool(); match(')');
	         return x;
	      case Tag.NUM: // factor -> num
	         x = new Constant(look, Type.Int);    move(); return x;
	      case Tag.REAL: // factor -> real
	         x = new Constant(look, Type.Float);  move(); return x;
	      case Tag.TRUE: // factor -> true
	         x = Constant.True;                   move(); return x;
	      case Tag.FALSE: // factor -> false
	         x = Constant.False;                  move(); return x;
	      default:
	         error("syntax error");
	         return x;
	      case Tag.ID: // factor -> loc
	         String s = look.toString();
	         Id id = top.get(look);
	         if( id == null ) error(look.toString() + " undeclared");
	         move();
	         if( look.tag != '[' ) return id;
	         else return offset(id);
	      }
	   }

	   Access offset(Id a) throws IOException {   // I -> [E] | [E] I
	      Expr i; Expr w; Expr t1, t2; Expr loc;  // inherit id

	      Type type = a.type;
	      match('['); i = bool(); match(']');     // first index, I -> [ E ]
	      type = ((Array)type).of;
	      w = new Constant(type.width);
	      t1 = new Arith(new Token('*'), i, w);
	      loc = t1;
	      while( look.tag == '[' ) {      // multi-dimensional I -> [ E ] I
	         match('['); i = bool(); match(']');
	         type = ((Array)type).of;
	         w = new Constant(type.width);
	         t1 = new Arith(new Token('*'), i, w);
	         t2 = new Arith(new Token('+'), loc, t1);
	         loc = t2;
	      }

	      return new Access(a, loc, type);
	   }
	}
