package codeGeneration;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class CodeFileWriter {
	
	enum FileSection {DECLARATION, PROGRAM, SUBROUTINE};

	PrintWriter fout;
	
	String declarations;
	String body;
	String subroutines;
	
	public CodeFileWriter(String file_path) throws FileNotFoundException{
			this.fout = new PrintWriter(file_path);
			this.declarations = "";
			this.body = "";
			this.subroutines = "";
	}
	
	public void print(String s, FileSection fs) {
		if(fs == FileSection.DECLARATION) {
			declarations += s;
		} else if(fs == FileSection.PROGRAM) {
			body += s;
		} else if(fs == FileSection.SUBROUTINE) {
			subroutines += s;
		}
		
	}
	
	public void close() {
		fout.print(declarations + "\n%%%\t\tPROGRAM START" + body + "\n%%%\t\tSUBROUNTINE START\n" + subroutines);
		fout.close();
	}
	
}
