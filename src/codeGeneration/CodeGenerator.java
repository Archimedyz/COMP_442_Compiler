package codeGeneration;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import codeGeneration.ExpressionTree.Node;
import semanticAnalysis.SymbolTable;
import semanticAnalysis.SymbolTable.Entry;
import semanticAnalysis.TypeRef;

public class CodeGenerator {

	private SymbolTable global_table; // reference to symbol table

	private CodeFileWriter cod_out;
	private PrintWriter cod_err;
	
	boolean in_prog_body;
		
	int next_if;
	int next_for;
	int next_register;
	final int maxRegister = 14; // 14 because that is the number of usable registers in the Moon Processor. (R15 is reserved for addressing)
	
	public CodeGenerator() {
		
		this.global_table = null;
		
		try {
			cod_out = new CodeFileWriter("C://Users/Awais/Desktop/moon/cod_out.m");
			cod_err = new PrintWriter("log/err/cod_err.txt");
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open log files. [cod]");
		}
		
		this.in_prog_body = false;

		this.next_if = 1;
		this.next_for = 1;
		this.next_register = 1;
	}
	
	public void openSource() {

		finalize();
		
		in_prog_body = false;
		
		try {
			cod_out = new CodeFileWriter("C://Users/Awais/Desktop/moon/cod_out.m");
			cod_err = new PrintWriter("log/err/cod_err.txt");
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open log files. [cod]");
		}
	}
	
	public void finalize() {
		cod_out.printBody("\thlt");
		cod_out.close();
		cod_err.close();
	}
	
	public void setGlobalTable(SymbolTable s) {
		global_table = s;
	}
	
	public boolean progBody() {
		in_prog_body = true;
		return true;
	}
	
	/*
	 * After entering the program body and finalizing the variable declarations, we can enter the program.
	 */
	public boolean finalizeDeclarations() {
		if(global_table == null) {
			System.out.println("Must initialize global table before proceeding.");
			return true;
		}
		if(in_prog_body) {
			// add all variable declarations
			addAllDecls(global_table);			
			
			// print the beginning parts for the program.
			cod_out.printBody("\n\talign\n\tentry\n\n");
		}
		
		return true;
	}	
	
	private void addAllDecls(SymbolTable s) {
		for(Entry e : s.getEntries()) {
			// if there are variables in a sub scope, handle them immediately.
			if(e.scope != null) {
				addAllDecls(e.scope);
			}
			if(e.kind.equals("variable")) {
				addVarDecl(e);
			}
		}
	}
	
	public boolean addVarDecl(String name, String type) {
		if(!in_prog_body) { // do this because all variables aside from those in statements in the program body will be defined at the beginning of program.
			return true;
		}
		// TODO: add support for arrays of variables
		// TODO: add support for type other than int
		
		cod_out.printDecl("% Variable Declaration: " + name + "\n");
		cod_out.printDecl(name + "\tdw 0\n");
		
		return true;
	}
	
	private void addVarDecl(Entry e) {
		// TODO: add support for arrays of variables
		// TODO: add support for type other than int
		cod_out.printDecl("% Variable Declaration: " + e.name + "\n");
		cod_out.printDecl(e.name + "\tdw 0\n");
	}
	
	private void addClassDecl(Entry e) {
		// for now do nothing
	}

	
	private void addFuncDecl(Entry e) {
		// for now do nothing
	}
	
	public boolean addAssignment(String name, ExpressionTree et) {
		if (!in_prog_body) { // for now only consider things in the program body.
			return true;
		}

		cod_out.printBody("%%% Expression: " + name + " = " + et.toString() + "\n");
		
		int mRegister = nextRegister();
		
		// store the result of the expression in the next available register.
		evaluateExpression(et.getRoot(), mRegister);
		
		// TODO: assignment on floats
		
		// now perform the assignment
		cod_out.printBody("% Now assign the value in R" + mRegister + " to the identifier " + name + ".\n");
		cod_out.printBody("\t\tsw\t\t" + name + "(R0),R" + mRegister + "\n\n");		
		
		// decrement the register because it has been released.
		releaseRegister();
		return true;
	}
	
	public void evaluateExpression(Node node, int lRegister) {
		// first, if the node is a number, load that number into the register.
		if(node.isNumber) {
			int val = Integer.parseInt((node.value));
			if(val < 0) { // if the number is negative, use subtraction, otherwise addition.
				cod_out.printBody("% Now load the value " + val + " into register R" + lRegister + ".\n");
				cod_out.printBody("\t\tsubi\tR" + lRegister + ",R0," + (-val) + "\n");		
			} else {
				cod_out.printBody("% Now load the value " + val + " into register R" + lRegister + ".\n");
				cod_out.printBody("\t\taddi\tR" + lRegister + ",R0," + val + "\n");
			}
			return;
		}
		// second, if the node is an identifier, load that into the register.
		if(node.isIdentifier) {
			String id = node.value;
			cod_out.printBody("% Now load the value in " + id + " into register R" + lRegister + ".\n");
			cod_out.printBody("\t\tlw\t\tR" + lRegister + "," + id + "(R0)\n");
			return;
		}
		// otherwise, the node must have been an operation.
		
		// convert the operator into the correct Moon opcode
		String code = symbolToOpCode(node.value);
		
		if(node.left == null) { // then use R0 as the left operand
			evaluateExpression(node.right, lRegister);
			cod_out.printBody("% Unary operation '" + code + "' on R" + lRegister + " into itself.\n");
			
			if(code.equals("not")) { // the not operation only uses 2 registers.
				cod_out.printBody("\t\t" + code + "\t\tR" + lRegister + ",R" + lRegister + "\n");
			} else { // the rest use 3 registers				
				cod_out.printBody("\t\t" + code + "\t\tR" + lRegister + ",R0,R" + lRegister + "\n");
			}
		} else { // both operands ought to be present, so place the result of the right in a new register
			evaluateExpression(node.left, lRegister);
			int rRegister = nextRegister();
			evaluateExpression(node.right, rRegister);
			cod_out.printBody("% Binary operation '" + code + "' on R" + lRegister + " and R" + rRegister + " into R" + lRegister + ".\n");
			cod_out.printBody("\t\t" + code + "\t\tR" + lRegister + ",R" + lRegister + ",R" + rRegister + "\n");
			releaseRegister();
		}
				
	}
	
	private String symbolToOpCode(String symbol) {
		String code = "";
		if(symbol.equals("+")) {
			code = "add";
		} else if(symbol.equals("-")) {
			code = "sub";
		} else if(symbol.equals("*")) {
			code = "mul";
		} else if(symbol.equals("/")) {
			code = "div";
		} else if(symbol.equals("==")) {
			code = "ceq";
		} else if(symbol.equals(">")) {
			code = "cgt";
		} else if(symbol.equals("<")) {
			code = "clt";
		} else if(symbol.equals(">=")) {
			code = "cge";
		} else if(symbol.equals("<=")) {
			code = "cle";
		} else {
			code = symbol; // for and, or and not
		}		
		
		return code;
	}
	
	public void addPut(ExpressionTree et) {
		
		cod_out.printBody("%%% Display the value of Expression: " + et.toString() + ".\n");
		
		// use the expression tree to load the value into the next available register
		int putRegister = nextRegister();
		evaluateExpression(et.getRoot(), putRegister);
		
		// the desired value should now be in the register. So we can now print it after saving it to putv.
		cod_out.printBody("% put the value of the expression to stdout.\n");
		cod_out.printBody("\t\tsw\t\tputv(R0),R" + putRegister + "\n");
		cod_out.printBody("\t\tjl\t\tR15,put\n\n");
		
		releaseRegister();		
	}
	
	public void addGet(String id) {
		cod_out.printBody("%%% Get input from the command and store it into " + id + ".\n");
		
		// use the expression tree to load the value into the next available register
		int getRegister = nextRegister();
		
		// the desired value should now be in the register. So we can now print it after saving it to putv.
		cod_out.printBody("% get the value from stdin and place it in " + id + ".\n");
		cod_out.printBody("\t\tjl\t\tR15,get\n");
		cod_out.printBody("\t\tlw\t\tR" + getRegister + ",getv(R0)\n");
		cod_out.printBody("\t\tsw\t\t" + id + "(R0),R" + getRegister + "\n\n");
		
		releaseRegister();	
		
		
		cod_out.printBody("");
	}
	
	public boolean beginIf(ExpressionTree et, TypeRef else_name, TypeRef endif_name) {
		
		// determine the tags for the else part, and for the endif
		int if_num = nextIfNum();
		else_name.val = "else" + if_num;
		endif_name.val = "endif" + if_num;
		
		cod_out.printBody("%%% If Statement on Expression: " + et.toString() + ".\n");
		
		int exprRegister = nextRegister();
		evaluateExpression(et.getRoot(), exprRegister);
		
		// add the single line indicating where to go if the expression is false
		cod_out.printBody("% if condition. goto " + else_name.val + " if false. \n");
		cod_out.printBody("\t\tbz\t\tR" + exprRegister + "," + else_name.val + "\n");
		
		releaseRegister();
		
		return true;
	}
	
	public boolean endIf(TypeRef endif_name) {
		// at the end of an if, add a jump to the end of the if-else statement
		cod_out.printBody("%%% If (true) body end. Jump to " + endif_name.val + ".\n");
				
		// add the single line indicating where to go once done the true block
		cod_out.printBody("% if condition body over. jump over else body to "+ endif_name.val + ". \n");
		cod_out.printBody("\t\tj\t\t" + endif_name.val + "\n");
		
		
		return true;
	}
	
	public boolean beginElse(TypeRef else_name) {
		
		// at the beginning of an else statement, add a the relevant tag 
		cod_out.printBody("%%% Beginning of an else statement block: " + else_name.val + ".\n");
		cod_out.printBody(else_name.val + "\n");
		return true;
	}
	
	public boolean endElse(TypeRef endif_name) {
		// at the end of an if-else statement, add a the relevant tag 
		cod_out.printBody("%%% End of the if-else statement: " + endif_name.val + ".\n");
		cod_out.printBody(endif_name.val + "\n\n");
		return true;
	}
	
	public boolean beginFor(TypeRef forbody_name, TypeRef forend_name, TypeRef forcond_name, TypeRef forinc_name) {
		
		int for_int = nextForNum();
		forbody_name.val = "for" + for_int;
		forend_name.val = "endfi" + for_int;
		forcond_name.val = "forc" + for_int;
		forinc_name.val = "fori" + for_int;
		
		// at the beginning of a for statement, add a the relevant tag 
		cod_out.printBody("%%% Beginning of a for statement block: " + forcond_name.val + ".\n");
		cod_out.printBody(forcond_name.val + "\n");
		return true;
	}
	
	public boolean addForCondition(ExpressionTree et, TypeRef forbody_name, TypeRef forend_name, TypeRef forinc_name) {
		
		cod_out.printBody("%%% Beginning of a for condition block.\n");
		
		// acquire a register.
		int forRegister = nextRegister();
		
		// evaluate the expression into the register
		evaluateExpression(et.getRoot(), forRegister);
		
		// add the branching statement for if the condition is false
		cod_out.printBody("\t\tbz\t\tR" + forRegister + "," + forend_name.val + "\n");
		
		// release the register
		releaseRegister();
		
		// after this, the increment assignment will occur. however we don't want it to occur immediately after the condition check, so we give it a tag and move on for now. 
		cod_out.printBody("\t\tj\t\t" + forbody_name.val + "\n");
		cod_out.printBody(forinc_name.val + "\n");
		
		return true;
	}
	
	public boolean endForInc(TypeRef forbody_name, TypeRef forcond_name) {

		// after the end of the increment, we must go check the condition, so jump to it
		cod_out.printBody("\t\tj\t\t" + forcond_name.val + "\n");
		
		// after this, the for body will begin. 
		cod_out.printBody(forbody_name.val + "\n");
		
		return true;
	}
	
	public boolean endFor(TypeRef forinc_name, TypeRef forend_name) {
		// at the end of an if-else statement, add a the relevant tag 
		cod_out.printBody("%%% End of the for statement: " + forend_name.val + ". However, the condition must be evaluated, so jump to " + forinc_name.val + "  for incrementing before evalutation.\n");
		// before the end of the body, we must run the increment statement.
		cod_out.printBody("\t\tj\t\t" + forinc_name.val + "\n");
		cod_out.printBody(forend_name.val + "\n\n");
		return true;
	}
	
	
	// TRACKING FUNCTIONS
	public int nextRegister() {
		if(next_register > maxRegister) {
			cod_err.print("Error - (location): Cannot allocate more than " + maxRegister + " registers.");
		}
		return next_register++;
	}
	
	public void releaseRegister() {
		if(next_register == 1) {
			cod_err.print("Error - (location): Cannot release more than " + maxRegister + " registers.");
		} else {
			--next_register;
		}
	}
	
	public int nextIfNum() {
		return next_if++;
	}
	
	public int nextForNum() {
		return next_for++;
	}
	
	
}
