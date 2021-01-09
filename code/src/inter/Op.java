package inter;

import symbols.Type;
import lexer.Token;

public class Op extends Expr{
	public Op(Token tok, Type p) { super(tok,p); }
	
	public Expr reduce()
	{
		Expr x = gen(); // gen x
		Temp t = new Temp(type);
		emit(t.toString()+" = "+x.toString()); // t = x;
		return t;
	}
}
