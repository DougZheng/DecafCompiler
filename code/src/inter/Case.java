package inter;

import symbols.Type;
import java.util.Hashtable;

public class Case extends Stmt {

   Expr num; Stmt stmt;
   
   // case num: stmt

   public Case() {
	   num = null; stmt = null;
   }
   
   public void init(Expr n, Stmt s) {
      if(n instanceof Constant && n.type == Type.Int) {
    	  num = n; stmt = s;
      }
      else n.error("constant int required in case"); 
   }
   public void gen(int b, int a, Hashtable h) {
      int label = newlabel();
      emitlabel(label);
      stmt.gen(label, a);
      h.put(num, Integer.valueOf(label));
   }
}
