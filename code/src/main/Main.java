package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import parser.Parser;
import lexer.Lexer;

public class Main {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		// 获取文件
//		File file = new File("D:/test.txt");
//		Reader reader = null;
//		reader = new InputStreamReader(new FileInputStream(file));
		Reader reader = new InputStreamReader(System.in);
		// 新建一个词法解析器，将文件传给lexer，由它读取文件内容
		Lexer lex = new Lexer(reader);
		Parser parser = new Parser(lex);
		parser.program();
		System.out.print("\n");
	}

}
