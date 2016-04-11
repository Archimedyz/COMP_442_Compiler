package codeGeneration;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class CodeFileWriter {

	PrintWriter fout;
	
	String declarations;
	String body;
	
	public CodeFileWriter(String file_path) throws FileNotFoundException{
			this.fout = new PrintWriter(file_path);
			this.declarations = "";
			this.body = "";
	}
	
	public void printBody(String s) {
		body += s;
	}
	
	public void printDecl(String s) {
		declarations += s;
	}
	
	public void close() {
		fout.print(declarations + body);
		fout.close();
	}
	
}
