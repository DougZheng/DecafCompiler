package inter;

import java.util.Hashtable;

public class Cases extends Stmt {

   Cases cases; Case cas;

   public Cases(Expr x, Stmt s, Cases cs) {
	   cas = new Case();
	   cas.init(x, s);
	   cases = cs;
   }
   
   public void gen(int b, int a, Hashtable h) {
      if(cases != null) cases.gen(b, a, h);
      cas.gen(b, a, h);
   }
}
