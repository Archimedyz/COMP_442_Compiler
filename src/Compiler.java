import java.util.Scanner;

import codeGeneration.CodeGenerator;
import lexicalAnalysis.LexicalAnalyzer;
import semanticAnalysis.SemanticAnalyzer;
import syntacticAnalysis.SyntacticAnalyzer;

public class Compiler {
	
	static boolean is_success = true;

	protected SyntacticAnalyzer syn;	

	private String err_msg;
	
	public Compiler() {
		// initialize the syntactic analyzer
		syn = new SyntacticAnalyzer();
	}
	
	public boolean compileFile(int out_num) {
		return compileFile("test_files/test_file_" + out_num + ".txt", out_num);
	}
	
	public boolean compileFile(String src_file_path, int out_num) {
		boolean compilation_error = false;
		
		syn = new SyntacticAnalyzer();
		
		System.out.println("\nCompiling file " + src_file_path + " . . .");
				
		// initialize the lexical analyzer
		if(!syn.openSource(src_file_path, out_num)) {
			System.out.println("File not Found!\n");
			return false;
		}
		
		syn.parse();
		syn.finalize();
		
		boolean lex_success = LexicalAnalyzer.success;
		boolean syn_success = SyntacticAnalyzer.success;
		boolean sem_success = SemanticAnalyzer.success;
		boolean cod_success = CodeGenerator.success;
		
		// Report if compilation was successful
		if(lex_success && syn_success && sem_success && cod_success) {
			System.out.println("Compile Successful.\n");
		} else {
			System.out.println("Compile Unsuccessful. Compilation failed for the following modules:");
			if(!lex_success) {
				System.out.println("\t> lex");
			}
			if(!syn_success) {
				System.out.println("\t> syn");
			}
			if(!sem_success) {
				System.out.println("\t> sem");
			}
			if(!cod_success) {
				System.out.println("\t> cod");
			}
		}
		
		// return false if there was an error.
		return !compilation_error;
	}
	
	public String getPreviousError() {
		return err_msg;
	}
	
	public static void main(String[] args) {
		Compiler compiler = new Compiler();
		
		boolean exit = false;

		Scanner keyin = new Scanner(System.in);
		System.out.println("-----------------------------------------------------");
		System.out.println("\tCOMP 442 - COMPILER");
		System.out.println("-----------------------------------------------------");
		System.out.println("Select a file number to run.");
		System.out.println("You may either select a single value(#), or a range of values(#-#).");
		System.out.println("To compile a custom file, specify the location using the command -c(i.e. '-c {file_path}').");
		System.out.println("Type 'exit' to terminate the application.\n");
		
		while(exit == false) {
			System.out.print("> ");
			
			String res = keyin.next();
			if(res.trim().equalsIgnoreCase("exit")) {
				exit = true;
				continue;
			}
			
			if(res.trim().matches("^\\d+-\\d+$")) {		
				// parse several files
				String[] range = res.trim().split("-");
				
				int start = Integer.parseInt(range[0]);
				int end = Integer.parseInt(range[1]);
				
				for(int i = start; i <= end; ++i) {
					compiler.compileFile(i);
				}
				
			} else if (res.trim().matches("^\\d+$")){				
				//parse the single file.
				int file_num = Integer.parseInt(res.trim());
				
				compiler.compileFile(file_num);
			} else if(res.equals("-c")) {
				// parse custom file into out 0.
				
				String path = keyin.next();

				compiler.compileFile(path, 0);
			} else {
				System.out.println("Invalid input format.\nPlease Choose a single file number, or range of file_numbers.\nOptinally choose another file, using the '-c' command.");
			}
			
			System.out.println("\nPress enter to continue . . . ");
			
			keyin.nextLine();
			keyin.nextLine();
			
		}
		
		System.out.println("\n\tPROGRAM TERMINATED\n");
		
	}
	public static void error() {
		is_success = false;
	}

}
