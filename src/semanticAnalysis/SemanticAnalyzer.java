package semanticAnalysis;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class SemanticAnalyzer {

	SymbolTable curr_scope;
	
	private PrintWriter sem_out;
	private PrintWriter sem_err;
	
	public SemanticAnalyzer() {
		curr_scope = new SymbolTable("global");

		try {
			sem_out = new PrintWriter("log/out/sem_out.txt");
			sem_err = new PrintWriter("log/err/sem_err.txt");
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open log files. [sem]");
		}
	}
	
	public void openSource() {
		sem_out.close();
		sem_err.close();
		try {
			sem_out = new PrintWriter("log/out/sem_out.txt");
			sem_err = new PrintWriter("log/err/sem_err.txt");
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open log files. [sem]");
		}
	}
	
	public void finalize(){
		sem_out.close();
		sem_err.close();
	}	
	
	public void print() {
		sem_out.write(curr_scope.toString());
	}
	
	public SymbolTable getCurrentScope(){
		return curr_scope;
	}
	
	public boolean addEntry(String name, String kind, TypeRef type) {
		
		if(type.val.equals("class")) {
			type.val = null;
		}
		
		// First determine if the addition is a function, and if it is, determine if it is valid overloading
		if(!kind.equals("function") && curr_scope.search(name)) { // otherwise check for mutiple declaration
			sem_err.println("Semantic Error - (" + type.line + ":" + type.col + "): Multiple declaration: '" + (type.val == null ? "" : (type.val + " ")) + name + "' (" + kind + ").");
			name += " +"; // add a space and then symbol to the name so that matches for it cannot be found in the table, but also so that it is ignored entirely without affecting the compilation
		}
		// add the scope anyway for now.
		SymbolTable next_scope = curr_scope.createEntry(name, kind, type.val, type.dimension, type.array_sizes).getScope();
		if(next_scope != null) { // if the added entry contains a scope, move into it.
			curr_scope = next_scope;
		}
		
		return true;
	}
	
	public boolean finalizeEntry() {
		// determine if the function has been already defined.
		if(kind.equals("function")) { // otherwise check for mutiple declaration
			sem_err.println("Semantic Error - (" + type.line + ":" + type.col + "): Multiple declaration: '" + (type.val == null ? "" : (type.val + " ")) + name + "' (" + kind + ").");
			name += " +"; // add a space and then symbol to the name so that matches for it cannot be found in the table, but also so that it is ignored entirely without affecting the compilation
		}
		
		curr_scope.finalize();
		
		return true;
	}
	
	public boolean popScope() {
		curr_scope = (curr_scope.getParentScope() == null) ? curr_scope : curr_scope.getParentScope();
		return true;
	}
	
	/*
	 * This Function checks whether the variable/array has been declared in the current scope, or super-scopes 
	 */
	public boolean variableCheck(TypeRef name) {
		SymbolTable search_scope = curr_scope;
		while(search_scope != null) {
			if(search_scope.search(name.val, "variable") || search_scope.search(name.val, "array")) {
				return true;
			}
			search_scope = search_scope.getParentScope();
		}
		
		// if the variable was not in the scope, we will be here, and can display an error
		sem_err.println("Semantic Error - (" + name.line + ":" + name.col + "): Undefined variable: '" + name.val + "'.");
		
		return true;
	}
	
	/*
	 * This Function checks whether the class has been declared in the current scope, or super-scopes 
	 */
	public boolean classCheck(TypeRef name) {
		SymbolTable search_scope = curr_scope;
		while(search_scope != null) {
			if(search_scope.search(name.val, "class")) {
				return true;
			}
			search_scope = search_scope.getParentScope();
		}
		
		// if the variable was not in the scope, we will be here, and can display an error
		sem_err.println("Semantic Error - (" + name.line + ":" + name.col + "): Undefined class: '" + name.val + "'.");
		
		return true;
	}
	
	/*
	 * This function check that both types match for an operation.
	 */
	public boolean typeMatch(TypeRef t1, TypeRef t2) {
		
		if(t1.val.equals("_typeerror_") || t2.val.equals("_typeerror_")) {
			// a type error was already detected, so we can ignore this and move on.
			return true;
		}
		
		if(!t1.val.equals(t2.val) || (t1.dimension - t1.indices) != (t2.dimension - t2.indices)) {
			sem_err.println("Semantic Error - (" + t1.line + ":" + t1.col + "): Type mismatch: cannot convert type '" + t2.val + indiceStr(t2.dimension - t2.indices) + "' to type '" + t1.val + indiceStr(t1.dimension - t1.indices) + "'.");
			// change the type of the latter to typeerror
			t2.val = "_typeerror_";
		}
		
		return true;
	}
	
	public String indiceStr(int boxes) {
		String str = "";
		
		while(boxes > 0) {
			str += "[]";
			--boxes;
		}
		
		return str;
	}
	
	public boolean getType(TypeRef name, TypeRef type, SymbolTable... scopes) {
		SymbolTable search_scope = curr_scope;
		if(scopes.length > 0) {
			search_scope = scopes[0];
		}
		while(search_scope != null) {
			if(search_scope.search(name.val)) {
				search_scope.getType(name.val, type);
				type.line = name.line;
				type.col = name.col;
				return true;
			}
			search_scope = search_scope.getParentScope();
		}
		type.val = "_typeerror_";
		return true;
	}
	
	public boolean indexVar(TypeRef name, TypeRef type) {
		if(type.indices < type.dimension) {
			++type.indices;	
		} else {
			sem_err.println("Semantic Error - (" + type.line + ":" + type.col + "): Dimension out of bounds. Identifier '" + name.val + "' has a dimension of " + type.dimension + ".");
		}
		return true;
	}
	
	public boolean checkReturnType(TypeRef type) {
		// make sure we are in a function scope.
		SymbolTable parent_scope = curr_scope.getParentScope();
		if(parent_scope == null) {
			sem_err.println("Parsing Error. Return in global scope.");
		}
		
		TypeRef return_type = new TypeRef();
		parent_scope.getType(curr_scope.getScopeName(), return_type);
		
		if(!return_type.val.equals(type.val)) {
			sem_err.println("Semantic Error - (" + type.line + ":" + type.col + "): Function " + curr_scope.getScopeName() + ": return type must be '" + return_type.val + "'.");
		}
		
		return true;
	}
	
	/*
	 * This function determines whether the attribute/function can be found in the provided type.
	 */
	public boolean getAttributeType(TypeRef type, TypeRef name, TypeRef attr_type) {
		if(type.dimension - type.indices != 0) {
			sem_err.println("Semantic Error - (" + name.line + ":" + name.col + "): Array type cannot have attribute.");
			return true;
		}
		
		
		
		// 1: find the entry for the type being called
		SymbolTable search_scope = curr_scope;
		while(search_scope != null) {
			if(search_scope.search(type.val)) {
				// the correct attribute had been found. now determine if it has the required attribute.
				search_scope = search_scope.getScopeOf(type.val);
				if(search_scope == null) {
					sem_err.println("Semantic Error - (" + name.line + ":" + name.col + "): Identifier '" + type.val + "' has no attributes.");
					attr_type.val = "_typeerror_";
				} else if (!search_scope.search(name.val)) {
					sem_err.println("Semantic Error - (" + name.line + ":" + name.col + "): Undefined attribute/function '" + name.val + "'.");
					attr_type.val = "_typeerror_";
				} else {
					search_scope.getType(name.val, attr_type);
				}
				break;
			}
		}
		
		return true;
	}
	
	public boolean addIndice(TypeRef type, TypeRef size) {
		try {
			int int_size = Integer.parseInt(size.val); 
			if(int_size <= 0) {
				sem_err.println("Semantic Error - (" + type.line + ":" + type.col + "): Array size must be >= 1.");
			} else {
				++type.dimension;
				type.array_sizes.add(int_size);
			}
		} catch(NumberFormatException e) {
			sem_err.println("Parsing Error: (" + type.line + ":" + type.col + "): Could not convert String to int: " + size.val + ".");
		}
		return true;
	}
	
	public boolean functionCheck(TypeRef name, SymbolTable var_scope) {
		
		if(var_scope != null && var_scope.search(name.val, "function")) {
			return true;
		}
		
		sem_err.println("Semantic Error - (" + name.line + ":" + name.col + "): Undefined function: '" + name.val + "'.");
		
		return true;
	}
	
	public boolean updateScope(TypeRef type, SymbolTable var_scope) {
		SymbolTable search_scope = var_scope;
		while(search_scope != null) {
			if(search_scope.search(type.val)) {
				var_scope.copy(search_scope.getScopeOf(type.val));
				return true;
			}
			search_scope = search_scope.getParentScope();
		}
		
		return true;
	}
	
}
