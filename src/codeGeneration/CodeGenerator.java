package codeGeneration;

import semanticAnalysis.SymbolTable;

public class CodeGenerator {

	private SymbolTable st; // reference to symbol table
	
	public CodeGenerator(SymbolTable st) {
		this.st = st;
		test();
	}
	
	public void test(){
		System.out.println("CG:\n----\n" + st.toString() + "\n----\n");
	}
	
}
