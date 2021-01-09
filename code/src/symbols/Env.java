package symbols;

import inter.Id;

import java.util.Hashtable;

import lexer.Token;

public class Env {
	private Hashtable table;
	protected Env prev; // 上一层命名空间
	
	public Env(Env n) { table = new Hashtable(); prev=n;}
	
	public void put(Token w, Id i) { // 添加一个符号w
		table.put(w, i);
	}
	
	public Id get(Token w){ // 获取符号w
		for(Env e=this; e!=null; e=e.prev){
			Id found = (Id)(e.table.get(w));
			if(found!=null) return found;
		}
		return null;
	}
	
	public boolean exists(Token w) { // 判定是否重定义
		Id found = (Id)(table.get(w));
		return found!=null;
	}
}
