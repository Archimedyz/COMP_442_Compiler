package codeGeneration;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import codeGeneration.CodeFileWriter.FileSection;
import codeGeneration.ExpressionTree.Node;
import codeGeneration.ExpressionTree.NodeType;
import semanticAnalysis.SymbolTable;
import semanticAnalysis.TypeRef;

public class CodeGenerator {

	private SymbolTable global_table; // reference to symbol table

	private CodeFileWriter cod_out;
	private PrintWriter cod_err;
		
	int next_if;
	int next_for;
	int next_var;
	int next_func;
	int next_param;
	int next_register;
	final int maxRegister = 14; // 14 because that is the number of usable registers in the Moon Processor. (R15 is reserved for addressing)

	HashMap<String, String> varMap;
	HashMap<String, String> funcMap;
	HashMap<String, ArrayList<String>> paramMap;
	String in_func_tag;
	
	public static boolean success = true;
	
	public CodeGenerator() {
		
		this.global_table = null;
		
		cod_out = null;
		cod_err = null;

		this.next_if = 1;
		this.next_for = 1;
		this.next_var = 1;
		this.next_func = 1;
		this.next_param = 1;
		this.next_register = 1;
		
		varMap = new HashMap<>();
		funcMap = new HashMap<>();
		paramMap = new HashMap<>();
		in_func_tag = null;
		
		success = true;
	}
	
	public void openSource(int out_num) {

		finalize();
		
		try {
			cod_out = new CodeFileWriter("C:generated_code/cod_out_" + out_num + ".m");
			cod_err = new PrintWriter("log/err/cod_err_" + out_num + ".txt");
		} catch (FileNotFoundException e) {
			success = false;
			System.err.println("Cannot open log files. [cod]");
		}
	}
	
	public void finalize() {
		if(cod_out != null) {
			cod_out.print("\thlt\n", FileSection.PROGRAM);
			cod_out.close();			
		}
		if(cod_err != null) {
			cod_err.close();
		}
	}
	
	public void setGlobalTable(SymbolTable s) {
		global_table = s;
	}
	
	/*
	 * After entering the program body and finalizing the variable declarations, we can enter the program.
	 */
	public boolean beginProg() {	
		
		// add some initial variables to the variable declarations.
		cod_out.print("% Variable Declaration of program variables.\n", FileSection.DECLARATION);
		cod_out.print("stack\tdw 0\n", FileSection.DECLARATION);
		cod_out.print("return\tdw 0\n", FileSection.DECLARATION);
		
		// print the beginning parts for the program.
		cod_out.print("\n\talign\n\tentry\n\n", FileSection.PROGRAM);		

		// initialize the stack variable
		cod_out.print("\t\taddi\tR1,R0,topaddr\n", FileSection.PROGRAM);
		cod_out.print("\t\tsw\t\tstack(R0),R1\n", FileSection.PROGRAM);
		
		return true;
	}	
	
	public boolean addVarDecl(String name, String type) {

		String var_tag = (in_func_tag == null ? "" : (in_func_tag + "_")) + "v" + nextVarNum();
				
		if(in_func_tag != null) {
			name = in_func_tag + "+" + name;
		}
		
		cod_out.print("% Variable Declaration: " + var_tag + "\n", FileSection.DECLARATION);
		cod_out.print(var_tag + "\tdw 0\n", FileSection.DECLARATION);
		
		// add the relation between name and tag for future reference
		varMap.put(name, var_tag);
		
		return true;
	}
	
	public boolean addAssignment(String name, ExpressionTree et) {

		FileSection fs = FileSection.PROGRAM;
		if(in_func_tag != null) {
			name = in_func_tag + "+" + name;
			fs = FileSection.SUBROUTINE;
		}

		cod_out.print("%%% Expression: " + name + " = " + et.toString() + "\n", fs);
		
		int mRegister = nextRegister();
		
		// store the result of the expression in the next available register.
		evaluateExpression(et.getRoot(), mRegister, fs);
				
		String var_tag = varMap.get(name);
		
		// now perform the assignment
		cod_out.print("% Now assign the value in R" + mRegister + " to the identifier " + var_tag + ".\n", fs);
		cod_out.print("\t\tsw\t\t" + var_tag + "(R0),R" + mRegister + "\n\n", fs);		
		
		// decrement the register because it has been released.
		releaseRegister();
		return true;
	}
	
	public void evaluateExpression(Node node, int lRegister, FileSection fs) {
		// first, if the node is a number, load that number into the register.
		if(node.node_type == NodeType.VALUE) {
			int val = 0;
			try {
			val = Integer.parseInt((node.value));
			} catch (NumberFormatException e) {
				cod_err.print("Number Format Exception. Compiler cannot parse or strings. Found '" + node.value + "'.\n");
				success = false;
			}
			if(val < 0) { // if the number is negative, use subtraction, otherwise addition.
				cod_out.print("% Now load the value " + val + " into register R" + lRegister + ".\n", fs);
				cod_out.print("\t\tsubi\tR" + lRegister + ",R0," + (-val) + "\n", fs);		
			} else {
				cod_out.print("% Now load the value " + val + " into register R" + lRegister + ".\n", fs);
				cod_out.print("\t\taddi\tR" + lRegister + ",R0," + val + "\n", fs);
			}
			return;
		}
		// second, if the node is an identifier, load that into the register.
		if(node.node_type == NodeType.IDENTIFIER) {
			String id = (in_func_tag == null ? "" : (in_func_tag + "+")) + node.value;
			String id_tag = varMap.get(id);
			cod_out.print("% Now load the value in " + id_tag + " into register R" + lRegister + ".\n", fs);
			cod_out.print("\t\tlw\t\tR" + lRegister + "," + id_tag + "(R0)\n", fs);
			return;
		}
		// third if the node is a function call, execute the function call.
		if(node.node_type == NodeType.FUNCTION) {
			String func_name = node.value;
			String func_tag = funcMap.get(func_name);
			// save values of current registers
			saveRegisters(lRegister, fs);
			
			// if the function has parameters load their values into memory.
			if(node.args != null) {
				ArrayList<String> params = paramMap.get(func_tag);
				if(params != null && params.size() == node.args.size()) {
					for(int i = 0; i < params.size(); ++i) {
						// evaluate the expression into 
						int pRegister = nextRegister();
						
						evaluateExpression(node.args.get(i).getRoot(), pRegister, fs);
						
						cod_out.print("% Now save the value in R" + pRegister + " into register " + params.get(i)+ ".\n", fs);
						cod_out.print("\t\tsw\t\t" + params.get(i) + "(R0),R" + pRegister + "\n", fs);
						
						releaseRegister();
					}
				}
			}
			
			
			// make the jump to the function
			cod_out.print("% Jump to subroutine: " + func_tag + ".\n", fs);
			cod_out.print("\t\tjl\t\tR15," + func_tag + "\n", fs);
			// load the old values back to registers
			loadRegisters(lRegister, fs);
			// load result to register
			cod_out.print("% Now load the value returned from " + func_tag + " into register R" + lRegister + ".\n", fs);
			cod_out.print("\t\tlw\t\tR" + lRegister + ",return(R0)\n", fs);
			return;
		}
		
		
		// otherwise, the node must have been an operation.
		
		// convert the operator into the correct Moon opcode
		String code = symbolToOpCode(node.value);
		
		if(node.left == null) { // then use R0 as the left operand
			evaluateExpression(node.right, lRegister, fs);
			cod_out.print("% Unary operation '" + code + "' on R" + lRegister + " into itself.\n", fs);
			
			if(code.equals("not")) { // the not operation only uses 2 registers.
				cod_out.print("\t\t" + code + "\t\tR" + lRegister + ",R" + lRegister + "\n", fs);
			} else { // the rest use 3 registers				
				cod_out.print("\t\t" + code + "\t\tR" + lRegister + ",R0,R" + lRegister + "\n", fs);
			}
		} else { // both operands ought to be present, so place the result of the right in a new register
			evaluateExpression(node.left, lRegister, fs);
			int rRegister = nextRegister();
			evaluateExpression(node.right, rRegister, fs);
			cod_out.print("% Binary operation '" + code + "' on R" + lRegister + " and R" + rRegister + " into R" + lRegister + ".\n", fs);
			cod_out.print("\t\t" + code + "\t\tR" + lRegister + ",R" + lRegister + ",R" + rRegister + "\n", fs);
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
			code = symbol; // for 'and', 'or' and 'not'
		}		
		
		return code;
	}
	
	private void saveRegisters(int topRegister, FileSection fs) {
		if(topRegister <= 1) { // no need to add anything
			return;
		}
		
		// first, load the stack counter
		cod_out.print("%%% Save all registers up to: " + (topRegister-1) + ".\n", fs);
		cod_out.print("\t\tlw\t\tR15,stack(R0)\n", fs);
		
		
		// now, foreach register until top_register, save it's value on the stack
		for(int i = 1; i < topRegister; ++i) {
			cod_out.print("\t\tsubi\tR15,R15,4\n", fs);			
			cod_out.print("\t\tsw\t\t0(R15),R" + i + "\n", fs);
			--next_register;
		}
		
		// finally, save the stack variable.
		cod_out.print("\t\tsw\t\tstack(R0),R15\n", fs);				
	}

	
	private void loadRegisters(int topRegister, FileSection fs) {
		if(topRegister <= 1) { // no need to add anything
			return;
		}
		
		// first, load the stack counter
		cod_out.print("%%% Load all registers up to: " + (topRegister-1) + ".\n", fs);
		cod_out.print("\t\tlw\t\tR15,stack(R0)\n", fs);
		
		
		// now, foreach register from top_register, save it's value on the stack
		for(int i = topRegister - 1; i > 0; --i) {
			cod_out.print("\t\tlw\t\tR" + i + ",0(R15)\n", fs);
			cod_out.print("\t\taddi\tR15,R15,4\n", fs);	
			++next_register;
		}
		
		// finally, save the stack variable.
		cod_out.print("\t\tsw\t\tstack(R0),R15\n", fs);				
	}
	
	public void addPut(ExpressionTree et) {
		
		FileSection fs = in_func_tag == null ? FileSection.PROGRAM : FileSection.SUBROUTINE;
				
		cod_out.print("%%% Display the value of Expression: " + et.toString() + ".\n", fs);
		
		// use the expression tree to load the value into the next available register
		int putRegister = nextRegister();
		evaluateExpression(et.getRoot(), putRegister, fs);
		
		// the desired value should now be in the register. So we can now print it after saving it to putv.
		cod_out.print("% put the value of the expression to stdout.\n", fs);
		cod_out.print("\t\tsw\t\tputv(R0),R" + putRegister + "\n", fs);
		cod_out.print("\t\tjl\t\tR15,put\n\n", fs);
		
		releaseRegister();		
	}
	
	public void addGet(String id) {
		
		FileSection fs = FileSection.PROGRAM;
		if(in_func_tag != null) {
			id = in_func_tag + "+" + id;
			fs = FileSection.SUBROUTINE;
		}

		String id_tag = varMap.get(id);
		
		cod_out.print("%%% Get input from the command and store it into " + id_tag + ".\n", fs);
		
		// use the expression tree to load the value into the next available register
		int getRegister = nextRegister();
		
		
		// the desired value should now be in the register. So we can now print it after saving it to putv.
		cod_out.print("% get the value from stdin and place it in " + id_tag + ".\n", fs);
		cod_out.print("\t\tjl\t\tR15,get\n", fs);
		cod_out.print("\t\tlw\t\tR" + getRegister + ",getv(R0)\n", fs);
		cod_out.print("\t\tsw\t\t" + id_tag + "(R0),R" + getRegister + "\n\n", fs);
		
		releaseRegister();	
	}
	
	public boolean beginIf(ExpressionTree et, TypeRef else_name, TypeRef endif_name) {
		
		FileSection fs = in_func_tag == null ? FileSection.PROGRAM : FileSection.SUBROUTINE;
		
		// determine the tags for the else part, and for the endif
		int if_num = nextIfNum();
		else_name.val = "else" + if_num;
		endif_name.val = "endif" + if_num;
		
		cod_out.print("%%% If Statement on Expression: " + et.toString() + ".\n", fs);
		
		int exprRegister = nextRegister();
		evaluateExpression(et.getRoot(), exprRegister, fs);
		
		// add the single line indicating where to go if the expression is false
		cod_out.print("% if condition. goto " + else_name.val + " if false. \n", fs);
		cod_out.print("\t\tbz\t\tR" + exprRegister + "," + else_name.val + "\n", fs);
		
		releaseRegister();
		
		return true;
	}
	
	public boolean endIf(TypeRef endif_name) {
		
		FileSection fs  = in_func_tag == null ? FileSection.PROGRAM : FileSection.SUBROUTINE;
		
		// at the end of an if, add a jump to the end of the if-else statement
		cod_out.print("%%% If (true) body end. Jump to " + endif_name.val + ".\n", fs);
				
		// add the single line indicating where to go once done the true block
		cod_out.print("% if condition body over. jump over else body to "+ endif_name.val + ". \n", fs);
		cod_out.print("\t\tj\t\t" + endif_name.val + "\n", fs);
		
		
		return true;
	}
	
	public boolean beginElse(TypeRef else_name) {

		FileSection fs  = in_func_tag == null ? FileSection.PROGRAM : FileSection.SUBROUTINE;
		
		// at the beginning of an else statement, add a the relevant tag 
		cod_out.print("%%% Beginning of an else statement block: " + else_name.val + ".\n", fs);
		cod_out.print(else_name.val + "\n", fs);
		return true;
	}
	
	public boolean endElse(TypeRef endif_name) {

		FileSection fs  = in_func_tag == null ? FileSection.PROGRAM : FileSection.SUBROUTINE;
		
		// at the end of an if-else statement, add a the relevant tag 
		cod_out.print("%%% End of the if-else statement: " + endif_name.val + ".\n", fs);
		cod_out.print(endif_name.val + "\n\n", fs);
		return true;
	}
	
	public boolean beginFor(TypeRef forbody_name, TypeRef forend_name, TypeRef forcond_name, TypeRef forinc_name) {
		
		FileSection fs  = in_func_tag == null ? FileSection.PROGRAM : FileSection.SUBROUTINE;
		
		int for_int = nextForNum();
		forbody_name.val = "for" + for_int;
		forend_name.val = "endfi" + for_int;
		forcond_name.val = "forc" + for_int;
		forinc_name.val = "fori" + for_int;
		
		// at the beginning of a for statement, add a the relevant tag 
		cod_out.print("%%% Beginning of a for statement block: " + forcond_name.val + ".\n", fs);
		cod_out.print(forcond_name.val + "\n", fs);
		return true;
	}
	
	public boolean addForCondition(ExpressionTree et, TypeRef forbody_name, TypeRef forend_name, TypeRef forinc_name) {
		
		FileSection fs  = in_func_tag == null ? FileSection.PROGRAM : FileSection.SUBROUTINE;
		
		cod_out.print("%%% Beginning of a for condition block.\n", fs);
		
		// acquire a register.
		int forRegister = nextRegister();
		
		// evaluate the expression into the register
		evaluateExpression(et.getRoot(), forRegister, fs);
		
		// add the branching statement for if the condition is false
		cod_out.print("\t\tbz\t\tR" + forRegister + "," + forend_name.val + "\n", fs);
		
		// release the register
		releaseRegister();
		
		// after this, the increment assignment will occur. however we don't want it to occur immediately after the condition check, so we give it a tag and move on for now. 
		cod_out.print("\t\tj\t\t" + forbody_name.val + "\n", fs);
		cod_out.print(forinc_name.val + "\n", fs);
		
		return true;
	}
	
	public boolean endForInc(TypeRef forbody_name, TypeRef forcond_name) {

		FileSection fs  = in_func_tag == null ? FileSection.PROGRAM : FileSection.SUBROUTINE;
		
		// after the end of the increment, we must go check the condition, so jump to it
		cod_out.print("\t\tj\t\t" + forcond_name.val + "\n", fs);
		
		// after this, the for body will begin. 
		cod_out.print(forbody_name.val + "\n", fs);
		
		return true;
	}
	
	public boolean endFor(TypeRef forinc_name, TypeRef forend_name) {

		FileSection fs  = in_func_tag == null ? FileSection.PROGRAM : FileSection.SUBROUTINE;
		
		// at the end of an if-else statement, add a the relevant tag 
		cod_out.print("%%% End of the for statement: " + forend_name.val + ". However, the condition must be evaluated, so jump to " + forinc_name.val + "  for incrementing before evalutation.\n", fs);
		// before the end of the body, we must run the increment statement.
		cod_out.print("\t\tj\t\t" + forinc_name.val + "\n", fs);
		cod_out.print(forend_name.val + "\n\n", fs);
		return true;
	}
	
	public boolean beginFuncDef(TypeRef func_name) {
		
		// grab the next function number
		int func_num = nextFuncNum();
		in_func_tag = "f" + func_num;
		
		// add a variable declaration for the return address for this function.
		cod_out.print("% Variable Declaration for return address for subroutine" + in_func_tag + "\n", FileSection.DECLARATION);
		cod_out.print(in_func_tag + "_ret\tdw 0\n", FileSection.DECLARATION);

		// add the tag line for this function into the code file
		cod_out.print("% Beginning of function: " + func_name.val + ".\n", FileSection.SUBROUTINE);
		cod_out.print(in_func_tag + "\t\tsw\t\t" + in_func_tag + "_ret(R0),R15\n", FileSection.SUBROUTINE);
		
		// create a relation between the func_name and the func_tag. This will be useful for function calls later.
		funcMap.put(func_name.val, in_func_tag);		
		
		return true;
	}
	
	public boolean addFuncParameter(String name, String type) {
		
		// add this parameter to the declaration section, as well as add it to the var mapping

		String param_tag = in_func_tag + "_p" + nextParamNum();
		name = in_func_tag + "+" + name;
		
		cod_out.print("% Parameter Declaration: " + param_tag + "\n", FileSection.DECLARATION);
		cod_out.print(param_tag + "\tdw 0\n", FileSection.DECLARATION);
		
		// add the relation between name and tag for future reference
		varMap.put(name, param_tag);
		if(paramMap.get(in_func_tag) == null) {
			ArrayList<String> toAdd = new ArrayList<>();
			toAdd.add(param_tag);
			paramMap.put(in_func_tag, toAdd);
		} else {
			paramMap.get(in_func_tag).add(param_tag);
		}
		
		return true;
	}
	
	public boolean addReturn(ExpressionTree et) {
		
		int retRegister = nextRegister();
		
		
		// if in program body, terminate program.
		if(in_func_tag == null) {
			evaluateExpression(et.getRoot(), retRegister, FileSection.PROGRAM);
			cod_out.print("\t\tsw\t\treturn(R0),R" + retRegister + "\n", FileSection.PROGRAM);
			cod_out.print("\t\thlt\n", FileSection.PROGRAM);
			
		} else {
			evaluateExpression(et.getRoot(), retRegister, FileSection.SUBROUTINE);
		
		// otherwise save the expression in the return statement to the return value.
		// Then jump back to the address in the function's return address variable.
		cod_out.print("% Return Statement.\n", FileSection.SUBROUTINE);
		cod_out.print("\t\tsw\t\treturn(R0),R" + retRegister + "\n", FileSection.SUBROUTINE);
		cod_out.print("\t\tlw\t\tR15," + in_func_tag + "_ret(R0)\n", FileSection.SUBROUTINE);
		cod_out.print("\t\tjr\t\tR15\n", FileSection.SUBROUTINE);
		}
		
		releaseRegister();
		
		return true;
	}
	
	public boolean endFuncDef() {
		
		// add the jump back line, and return a default value of 0.
		cod_out.print("\t\tsw\t\treturn(R0),R0\n", FileSection.SUBROUTINE);
		cod_out.print("\t\tlw\t\tR15," + in_func_tag + "_ret(R0)\n", FileSection.SUBROUTINE);
		cod_out.print("\t\tjr\t\tR15\n", FileSection.SUBROUTINE);
		cod_out.print("% End of function.\n", FileSection.SUBROUTINE);
		
		in_func_tag = null;
		
		return true;
	}
	
	// TRACKING FUNCTIONS
	private int nextRegister() {
		if(next_register > maxRegister) {
			cod_err.print("Error - Cannot allocate more than " + maxRegister + " registers.");
			success = false;
		}
		return next_register++;
	}
	
	private void releaseRegister() {
		if(next_register == 1) {
			cod_err.print("Error - Cannot release more than " + maxRegister + " registers.");
		} else {
			--next_register;
		}
	}
	
	private int nextIfNum() {
		return next_if++;
	}
	
	private int nextForNum() {
		return next_for++;
	}
	
	private int nextVarNum() {
		return next_var++;
	}
	
	private int nextFuncNum() {
		return next_func++;
	}
	
	private int nextParamNum() {
		return next_param++;
	}
	
	
}
