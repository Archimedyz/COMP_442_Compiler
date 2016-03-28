import java.io.IOException;

import lexicalAnalysis.LexToken;
import syntacticAnalysis.SyntacticAnalyzer;

public class Compiler {

	private SyntacticAnalyzer syntactic_analyzer;
	
	private String err_msg;
	
	public Compiler() {
		// initialize th syntactic analyzer
		syntactic_analyzer = new SyntacticAnalyzer();
	}
	
	public boolean compileFile(String src_file_path) {
		boolean compilation_error = false;
		
		// initialize the lexical analyzer
		if(!syntactic_analyzer.openSource(src_file_path)) {
			err_msg = "File not Found.";
			return false;
		}
		
		compilation_error = !syntactic_analyzer.parse();
		syntactic_analyzer.finalize();
		// return false if there was an error.
		return !compilation_error;
	}
	
	public String getPreviousError() {
		return err_msg;
	}
	
	public static void main(String[] args) {
		Compiler compiler = new Compiler();
		
		boolean success = compiler.compileFile("test_files/test_file_3.txt");
		
		System.out.println("Compile " + (success ? "Successful" : "Unsuccessful") + ".");
	}

}
