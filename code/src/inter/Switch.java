package inter;

import symbols.Type;
import java.util.Hashtable;
import java.util.Iterator;

public class Switch extends Stmt {

   Expr expr; Cases cases; Stmt stmt;
   
   // switch(expr) { 
   // cases
   // default: stmt }

   public Switch() {
	   expr = null; cases = null; stmt = null;
   }
   
   public void init(Expr x, Cases c, Stmt s) {
      expr = x; cases = c; stmt = s;
      if(Type.max(expr.type, Type.Int) != Type.Int) expr.error("int or char required in switch");
   }
//   public void gen(int b, int a) {
//      after = a;                // save label a
//      expr.jumping(0, a);		// iffalse expr goto La
//      int label = newlabel();   // label for stmt
//      emitlabel(label); stmt.gen(label, b);
//      emit("goto L" + b); // goto Lb (continue while)
//   }
   public void gen(int b, int a) {
	   after = a;
	   Expr x = expr.reduce();
	   int testlabel = newlabel();
	   emit("goto L" + testlabel); // goto test
	   
	   Hashtable h = new Hashtable(); // mapping expr to label
	   cases.gen(b, a, h); // cases
	   
	   int defaultlabel = newlabel(); // default
	   emitlabel(defaultlabel);
	   if(stmt != null) stmt.gen(b, a);
	   emit("goto L" + a);
	   
	   emitlabel(testlabel); // test
	   Iterator it = h.keySet().iterator();
	   while(it.hasNext()) {
		   Expr expr = (Expr)it.next();
		   Integer label = (Integer)h.get(expr);
		   emit("if " + x.toString() + " == " + expr.toString() + " goto L" + label);
	   }
	   emit("goto L" + defaultlabel);
	   
   }
}
