package syntacticAnalysis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import core.ParseTree;
import lexicalAnalysis.LexToken;
import lexicalAnalysis.LexicalAnalyzer;
import semanticAnalysis.SymbolTable;
import semanticAnalysis.TypeRef;

public class SyntacticAnalyzer {

	private LexicalAnalyzer lexical_analyzer;
	
	private LexToken lookahead;
	
	private PrintWriter syn_out;
	private PrintWriter syn_err;
	
	private PrintWriter sem_out;
	private PrintWriter sem_err;
	
	private boolean fileOpen;
	
	private ParseTree parseTree;
	
	private Map<String, String> firstSets;
	private Map<String, String> followSets;
	
	private SymbolTable curr_scope;
	private TypeRef int_type;
	
	// for error messages
	private Map<String, String> tokenNameToExpectedString;
	
	public SyntacticAnalyzer() {
		// the lexical analyzer
		lexical_analyzer = new LexicalAnalyzer();
		try {
			syn_out = new PrintWriter("log/out/syn_out.txt");
			syn_err = new PrintWriter("log/err/syn_err.txt");
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open log files. [syn]");
		}
		try {
			sem_out = new PrintWriter("log/out/sem_out.txt");
			sem_err = new PrintWriter("log/err/sem_err.txt");
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open log files. [sem]");
		}
		fileOpen = false;
		lookahead = null;
		
		parseTree = new ParseTree();
		
		firstSets = new HashMap<>();
		followSets = new HashMap<>();
		
		curr_scope = null;
		int_type = new TypeRef();
		int_type.val = "int";
		
		tokenNameToExpectedString = new HashMap<>();
		
		init();
	}
	
	public boolean parse() {
		if(!fileOpen) {
			return false;
		}
		
		// initialize the parse tree
		parseTree.reset();
				
		lookahead = nextToken();
		if(prog() && match("_EOF")) {
			syn_out.write(parseTree.toString());
			sem_out.write(curr_scope.toString());
			return true; // compile successful
		}
		return false;
	}
	
	public boolean openSource(String src_file_path) {
		
		syn_out.close();
		syn_err.close();
		try {
			syn_out = new PrintWriter("log/out/syn_out.txt");
			syn_err = new PrintWriter("log/err/syn_err.txt");
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open log files. [syn]");
		}
		
		sem_out.close();
		sem_err.close();
		try {
			sem_out = new PrintWriter("log/out/sem_out.txt");
			sem_err = new PrintWriter("log/err/sem_err.txt");
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open log files. [sem]");
		}
				
		if(!lexical_analyzer.openSource(src_file_path)) {
			fileOpen = false;
			return false;
		}
		fileOpen = true;
		return true;
	}
	
	public void finalize() {
		lexical_analyzer.finalize();
		syn_out.close();
		syn_err.close();
		sem_out.close();
		sem_err.close();
	}
	
	private boolean match(String token_name, TypeRef ... ret) {
				
		if(lookahead.token_name.equals(token_name)) {
			if(ret.length > 0) {
				ret[0].val = lookahead.lexeme;
				ret[0].line = lookahead.line;
				ret[0].col = lookahead.col;
			}
			lookahead = nextToken();
			return true;
		}
		
		syn_err.println("Syntax Error - (" + lookahead.line + ":" + lookahead.col + "): expected " + tokenNameToExpectedString.get(token_name) + ", found '" + lookahead.lexeme + "'.");
		// keep going until the end of the line, or until a semicolon is reached. perhaps the token is close by
		do {
			lookahead = nextTokenAll();
			if(lookahead.token_name == token_name) {
				if(ret.length > 0) {
					ret[0].val = lookahead.lexeme;
					ret[0].line = lookahead.line;
					ret[0].col = lookahead.col;
				}
				lookahead = nextToken();
				return true;
			}
		} while(("_NL _SCOLON").contains(lookahead.token_name)); 
		return false;
	}
	
	private LexToken nextToken() {
		if(lookahead != null && ("_EOF").equals(lookahead.token_name)) {
			return lookahead;
		}
		
		LexToken token = null;
		
		boolean inLineComment = false;
		boolean inBlockComment = false;
		
		while(token == null) { // loop through to skip _NL, _ERROR and Comments
			LexToken lex_token = null;
			try {
				lex_token = lexical_analyzer.nextToken();
			} catch(IOException e) {
				System.out.println("ERROR - IOException while attempting to obtain token.");
				return new LexToken("_ERROR", "error", -1, -1, "IOException while attempting to obain token.");
			}
			
			// if the token is a comment, or in a comment, or the new-line character, skip it			
			if(!inLineComment && !inBlockComment && !lex_token.token_name.equals("_NL") && !lex_token.token_name.equals("_CMT") && !lex_token.token_name.equals("_ERROR")) {
				token = lex_token;
			} else if(inLineComment && lex_token.token_name.equals("_NL")) {
				inLineComment = false;
			} else if(inBlockComment && lex_token.token_name.equals("_CMT") && lex_token.lexeme.equals("*/")) {
				inBlockComment = false;
			} else if(lex_token.token_name.equals("_CMT")) {
				if(lex_token.lexeme.equals("//")) {
					inLineComment = true;
				} else if(lex_token.lexeme.equals("/*")) {
					inBlockComment = true;
				}
			}
		}
		return token;
	}
	
	// same as nextToken, however will return the first token, regardless of what the token is.
	private LexToken nextTokenAll() {
		if(lookahead != null && ("_EOF").equals(lookahead.token_name)) {
			return lookahead;
		}
		
		LexToken lex_token = null;
		
		try {
			lex_token = lexical_analyzer.nextToken();
		}  catch(IOException e) {
			System.out.println("ERROR - IOException while attempting to obtain token.");
			return new LexToken("_ERROR", "error", -1, -1, "IOException while attempting to obain token.");
		}
		
		return lex_token;
	}
	
	private boolean skipErrors(String non_terminal) {
		String first = firstSets.get(non_terminal);
		String follow = followSets.get(non_terminal);
		if(!first.contains(lookahead.token_name) && !(first.contains("_EPSILON") && follow.contains(lookahead.token_name))) {
			syn_err.println("Syntax Error - (" + lookahead.line + ":" + lookahead.col + "): Unexpected token '" + lookahead.lexeme + "'."); // nonTerminalSkipMsg(non_terminal));
			while(!first.contains(lookahead.token_name) && !follow.contains(lookahead.token_name)) {
				lookahead = nextToken();
				if(!first.contains("_EPSILON") && follow.contains(lookahead.token_name)) {
					return false;
				}
			}
		}
		return true;
	}
	
	// Semantic Functions - BEGIN
	
	private boolean addEntry(String name, String kind, TypeRef type) {
		
		if(type.val.equals("class")) {
			type.val = null;
		}
		
		// First determine there is no redefinition within the current_scope
		if(curr_scope.search(name)) {
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
	
	private boolean popScope() {
		curr_scope = (curr_scope.getParentScope() == null) ? curr_scope : curr_scope.getParentScope();
		return true;
	}
	
	/*
	 * This Function checks whether the variable/array has been declared in the current scope, or super-scopes 
	 */
	private boolean variableCheck(TypeRef name) {
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
	private boolean classCheck(TypeRef name) {
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
	private boolean typeMatch(TypeRef t1, TypeRef t2) {
		
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
	
	private String indiceStr(int boxes) {
		String str = "";
		
		while(boxes > 0) {
			str += "[]";
			--boxes;
		}
		
		return str;
	}
	
	private boolean getType(TypeRef name, TypeRef type) {
		SymbolTable search_scope = curr_scope;
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
	
	private boolean indexVar(TypeRef name, TypeRef type) {
		if(type.indices < type.dimension) {
			++type.indices;	
		} else {
			sem_err.println("Semantic Error - (" + type.line + ":" + type.col + "): Dimension out of bounds. Identifier '" + name.val + "' has a dimension of " + type.dimension + ".");
		}
		return true;
	}
	
	private boolean checkReturnType(TypeRef type) {
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
	private boolean getAttributeType(TypeRef type, TypeRef name, TypeRef attr_type) {
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
	
	private boolean addIndice(TypeRef type, TypeRef size) {
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
	
	private boolean functionCheck(TypeRef name, SymbolTable var_scope) {
		
		if(var_scope != null && var_scope.search(name.val)) {
			return true;
		}
		
		sem_err.println("Semantic Error - (" + name.line + ":" + name.col + "): Undefined function: '" + name.val + "'.");
		
		return true;
	}
	
	private boolean updateScope(TypeRef type, SymbolTable var_scope) {
		SymbolTable search_scope = var_scope;
		while(search_scope != null) {
			if(search_scope.search(type.val)) {
				var_scope = search_scope.getScopeOf(type.val);
				return true;
			}
			search_scope = search_scope.getParentScope();
		}
		
		return true;
	}
	// Semantic Functions - END
	
	
	// PRODUCTIONS - BEGIN
	private boolean prog() {
		// skip errors
		if(!skipErrors("prog")) {
			return false;
		}
		
		// create the global symbol table.
		curr_scope = new SymbolTable("global");
		
		// add the node for this production.
		parseDown();
		
		if(("_CLASS _PROGRAM").contains(lookahead.token_name)) {
			if(classDecl() && progBody()) {
				print("<prog> ::= <classDecl> <progBody>");
				parseUp();
				return true;
			}
		}
		parseDestroy();
		return false;
	}

	private boolean classDecl() {
		// skip errors
		if(!skipErrors("classDecl")) {
			return false;
		}
		
		TypeRef class_id = new TypeRef();
		
		// add the node for this production.
		parseDown();
		
		if(("_CLASS").contains(lookahead.token_name)) {
			if(match("_CLASS") && match("_ID", class_id) && addEntry(class_id.val, "class", class_id) && match("_LB") && varThenFunc() && match("_RB") && match("_SCOLON") && popScope() && classDecl()) {
				print("<classDecl> ::= class id { <varThenFunc> } ; <classDecl>");
				parseUp();
				return true;
			}
		} else if (("_ID _FLOAT _INT _PROGRAM").contains(lookahead.token_name)) {
			print("<classDecl> ::= EPSILON");
			parseUp();
			return true;
		}		
		parseDestroy();
		return false;
	}

	private boolean varThenFunc() {
		// skip errors
		if(!skipErrors("varThenFunc")) {
			return false;
		}
		
		TypeRef type = new TypeRef();
		TypeRef name = new TypeRef();
		
		// add the node for this production.
		parseDown();
				
		if(("_ID _FLOAT _INT").contains(lookahead.token_name)) {
			if(type(type) && match("_ID", name) && varOrFunc(type, name)) {
				print("<varThenFunc> ::= <type> id <varOrFunc>");
				parseUp();
				return true;
			}
		} else if (("_RB").contains(lookahead.token_name)) {
			print("<varThenFunc> ::= EPSILON");
			parseUp();
			return true;
		}		
		parseDestroy();
		return false;
	}

	private boolean varOrFunc(TypeRef type, TypeRef name) {
		// skip errors
		if(!skipErrors("varOrFunc")) {
			return false;
		}
				
		// add the node for this production.
		parseDown();
		
		if(("_LSB _SCOLON").contains(lookahead.token_name)) {
			if(arraySize(type) && addEntry(name.val, "variable", type) && match("_SCOLON") && varThenFunc()) {
				print("<varOrFunc> ::= <arraySize> ; <varThenFunc>");
				parseUp();
				return true;
			} 
		} else if(("_LP").contains(lookahead.token_name)){
			if (addEntry(name.val, "function", type) && match("_LP") && fParams() && match("_RP") && funcBody() && match("_SCOLON") && popScope() && funcDef()) {
				print("<varOrFunc> ::= ( <fparams> ) <funcBody> ; <funcDef>");
				parseUp();
				return true;
			}
		}	
		parseDestroy();
		return false;
	}

	private boolean progBody() {
		// skip errors
		if(!skipErrors("progBody")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
		
		if(("_ID _FLOAT _INT _PROGRAM").contains(lookahead.token_name)) {
			if(funcDef() && match("_PROGRAM") && funcBody() && match("_SCOLON")) {
				print("<progBody> ::= <funcDef> program <funcBody> ;");
				parseUp();
				return true;
			}
		}		
		parseDestroy();
		return false;
	}
	
	private boolean funcHead(TypeRef type, TypeRef name) {
		// skip errors
		if(!skipErrors("funcHead")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
		
		if(("_ID _FLOAT _INT").contains(lookahead.token_name)) {
			if(type(type) && match("_ID", name) && match("_LP") && fParams() && match("_RP")) {
				print("<funcHead> ::= <type> id ( <fParams> )");
				parseUp();
				return true;
			}
		}		
		parseDestroy();
		return false;
	}

	private boolean funcDef() {
		// skip errors
		if(!skipErrors("funcDef")) {
			return false;
		}
		
		TypeRef type = new TypeRef();
		TypeRef name = new TypeRef();
		
		// add the node for this production.
		parseDown();
		
		if(("_ID _FLOAT _INT").contains(lookahead.token_name)) {
			if(funcHead(type, name) && addEntry(name.val, "function", type) && funcBody() && match("_SCOLON") && popScope() && funcDef()) {
				print("<funcDef> ::= <funcHead> <funcBody> ; <funcDef>");
				parseUp();
				return true;
			} 
		} else if (("_RB _PROGRAM").contains(lookahead.token_name)) {
			print("<funcDef> ::= EPSILON");
			parseUp();
			return true;
		}	
		parseDestroy();
		return false;
	}

	private boolean funcBody() {
		// skip errors
		if(!skipErrors("funcBody")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
		
		if(("_LB").contains(lookahead.token_name)) {
			if(match("_LB") && varThenStat() && match("_RB")) {
				print("<funcBody> ::= { <varThenStat> }");
				parseUp();
				return true;
			}
		}	
		parseDestroy();
		return false;
	}

	private boolean varThenStat() {
		// skip errors
		if(!skipErrors("varThenStat")) {
			return false;
		}
		
		TypeRef type = new TypeRef();
		
		// add the node for this production.
		parseDown();
		
		if(("_FOR _IF _GET _PUT _RETURN").contains(lookahead.token_name)) {
			if(statementRes() && mStatement()) {
				print("<varThenStat> ::= <statementRes> <mStatement>");
				parseUp();
				return true;
			} 
		} else if(("_FLOAT _INT").contains(lookahead.token_name)) {
			if (primitiveType(type) && varTail(type) && varThenStat()) {
				print("<varThenStat> ::= <pimitiveType> <varTail> <varThenStat>");
				parseUp();
				return true;
			} 
		} else if(("_ID").contains(lookahead.token_name)) {
			if (match("_ID", type) && varOrStat(type)) {
				print("<varThenStat> ::= id <varOrStat>");
				parseUp();
				return true;
			}
		} else if (("_RB").contains(lookahead.token_name)) {
			print("<varThenStat> ::= EPSILON");
			parseUp();
			return true;
		} 	
		parseDestroy();
		return false;
	}

	private boolean varOrStat(TypeRef type) {
		// skip errors
		if(!skipErrors("varOrStat")) {
			return false;
		}
		
		TypeRef right_type = new TypeRef();
		TypeRef name = new TypeRef();
		
		// add the node for this production.
		parseDown();
		
		if(("_ID").contains(lookahead.token_name)) {
			if(classCheck(type) && varTail(type) && varThenStat()) {
				print("<varOrStat> ::= <varTail> <varThenStat>");
				parseUp();
				return true;
			}
		} else if(("_LSB _DOT _EQUAL").contains(lookahead.token_name)) {
			name = type;
			type = new TypeRef();
			if (variableCheck(name) && getType(name, type) && indice(name, type) && idnest(type) && assignOp() && expr(right_type) && match("_SCOLON") && typeMatch(type, right_type) && mStatement()) {
				print("<varOrStat> ::= <indice> <idnest> <assignOp> <expr> ; <mStatement>");
				parseUp();
				return true;
			}
		}		
		parseDestroy();
		return false;
	}
	
	private boolean varTail(TypeRef type) {
		// skip errors
		if(!skipErrors("varTail")) {
			return false;
		}
		
		TypeRef name =  new TypeRef();
		
		// add the node for this production.
		parseDown();		
		
		if(("_ID").contains(lookahead.token_name)) {
			if(match("_ID", name) && arraySize(type) && addEntry(name.val, "variable", type) && match("_SCOLON")) {
				print("<varTail> ::= id <arraySize> ;");
				parseUp();
				return true;
			} 
		}		

		parseDestroy();
		return false;
	}

	private boolean statement() {
		// skip errors
		if(!skipErrors("statement")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
		
		if(("_FOR _IF _GET _PUT _RETURN").contains(lookahead.token_name)) {
			if(statementRes()) {
				print("<statement> ::= <statementRes>");
				parseUp();
				return true;
			} 
		} else if(("_ID").contains(lookahead.token_name)) {
			if (assignStat() && match("_SCOLON")) {
				print("<statement> ::= <assignStat> ;");
				parseUp();
				return true;
			}
		}		
		parseDestroy();
		return false;
	}

	private boolean statementRes() {
		// skip errors
		if(!skipErrors("statementRes")) {
			return false;
		}
		
		TypeRef type = new TypeRef();
		TypeRef right_type = new TypeRef();
		TypeRef name = new TypeRef();
		
		// add the node for this production.
		parseDown();
				
		if(("_RETURN").contains(lookahead.token_name)) {
			if(match("_RETURN") && match("_LP") && expr(type) && match("_RP") && match("_SCOLON") && checkReturnType(type)) {
				print("<statementRes> ::= return ( <expr> ) ;");
				parseUp();
				return true;
			} 
		} else if(("_PUT").contains(lookahead.token_name)) {
			if (match("_PUT") && match("_LP") && expr(type) && match("_RP") && match("_SCOLON")) {
				print("<statementRes> ::= put ( <expr> ) ;");
				parseUp();
				return true;
			} 
		} else if(("_GET").contains(lookahead.token_name)) {
			if (match("_GET") && match("_LP") && variable(type) && match("_RP") && match("_SCOLON")) {
				print("<statementRes> ::= get ( <expr> ) ;");
				parseUp();
				return true;
			} 
		} else if(("_IF").contains(lookahead.token_name)) {
			if (match("_IF") && match("_LP") && expr(type) && typeMatch(type, int_type) && match("_RP") && match("_THEN") && statBlock() && match("_ELSE") && statBlock() && match("_SCOLON")) {
				print("<statementRes> ::= if ( <expr> ) then <statblock> else <statblock> ;");
				parseUp();
				return true;
			} 
		} else if(("_FOR").contains(lookahead.token_name)) {
			if (match("_FOR") && match("_LP") && type(type) && match("_ID", name) && addEntry(name.val, "variable", type) && assignOp() && expr(right_type) && match("_SCOLON") && typeMatch(type, right_type) && relExpr() && match("_SCOLON") && assignStat() && match("_RP") && statBlock() && match("_SCOLON")) {
				print("<statementRes> ::= for ( <type> id <assignOp> <expr> ; relExpr ; assignStat ) statBlock ;");
				parseUp();
				return true;
			}
		}		
		parseDestroy();
		return false;
	}

	private boolean mStatement() {
		// skip errors
		if(!skipErrors("mStatement")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
		
		if(("_ID _FOR _IF _GET _PUT _RETURN").contains(lookahead.token_name)) {
			if(statement() && mStatement()) {
				print("<mStatement> ::= <statement> <mStatement>");
				parseUp();
				return true;
			}
		} else if (("_RB").contains(lookahead.token_name)) {
			print("<mStatement> ::= EPSILON");
			parseUp();
			return true;
		}		
		parseDestroy();
		return false;
	}

	private boolean assignStat() {
		// skip errors
		if(!skipErrors("assignStat")) {
			return false;
		}
		
		TypeRef type1 = new TypeRef();
		TypeRef type2 = new TypeRef();
		
		// add the node for this production.
		parseDown();
		
		if(("_ID").contains(lookahead.token_name)) {
			if(variable(type1) && assignOp() && expr(type2) && typeMatch(type1, type2)) {
				print("<assignStat> ::= <variable> <assingOp> <expr>");
				parseUp();
				return true;
			}
		}		
		parseDestroy();
		return false;
	}
	
	private boolean statBlock() {
		// skip errors
		if(!skipErrors("statBlock")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
		
		if(("_ID _FOR _IF _GET _PUT _RETURN").contains(lookahead.token_name)) {
			if(statement()) {
				print("<statBlock> ::= <statement>");
				parseUp();
				return true;
			} 
		} else if(("_LB").contains(lookahead.token_name)) {
			if(match("_LB") && mStatement() && match("_RB")) {
				print("<statBlock> ::= { <mStatement> }");
				parseUp();
				return true;
			}
		} else if (("_SCOLON _ELSE").contains(lookahead.token_name)) {
			print("<statBlock> ::= EPSILON");
			parseUp();
			return true;
		}		
		parseDestroy();
		return false;
	}

	private boolean expr(TypeRef type) {
		// skip errors
		if(!skipErrors("expr")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
				
		if(("_LP _NOT _ID _FNUM _INUM _ADDOP").contains(lookahead.token_name)) {
			if(arithExpr(type) && relExprPart(type)) {
				print("<expr> ::= <arithExpr> <relExprPart>");
				parseUp();
				return true;
			} 
		}		
		parseDestroy();
		return false;
	}

	private boolean relExprPart(TypeRef left_type) {
		// skip errors
		if(!skipErrors("relExprPart")) {
			return false;
		}
		
		TypeRef right_type = new TypeRef();
		
		// add the node for this production.
		parseDown();
		
		if(("_LT _GT _RELOP").contains(lookahead.token_name)) {
			if(relOp() && arithExpr(right_type) && typeMatch(left_type, right_type)) {
				print("<relExprPart> ::= <relOp> <arithExpr>");
				if(!right_type.val.equals("_typeerror_")) {
					left_type.val = "int"; // convert the relation to int for boolean.
				}
				parseUp();
				return true;
			}
		} else if (("_SCOLON, _RP, _COMMA").contains(lookahead.token_name)) {
			print("<relExprPart> ::= EPSILON");
			parseUp();
			return true;
		}		
		parseDestroy();
		return false;
	}
	
	private boolean relExpr() {
		// skip errors
		if(!skipErrors("relExpr")) {
			return false;
		}
		
		TypeRef left_type = new TypeRef();
		TypeRef right_type = new TypeRef();
		
		// add the node for this production.
		parseDown();
		
		if(("_LP _NOT _ID _FNUM _INUM _ADDOP").contains(lookahead.token_name)) {
			if(arithExpr(left_type) && relOp() && arithExpr(right_type) && typeMatch(left_type, right_type)) {
				print("<relExpr> ::= <arithExpr> <relOp> <arithExpr>");
				parseUp();
				return true;
			}
		}		
		parseDestroy();
		return false;
	}

	private boolean arithExpr(TypeRef type) {
		// skip errors
		if(!skipErrors("arithExpr")) {
			return false;
		}
				
		// add the node for this production.
		parseDown();
				
		if(("_LP _NOT _ID _FNUM _INUM _ADDOP").contains(lookahead.token_name)) {
			if(term(type) && arithExprTail(type)) {
				print("<arithExpr> ::= <term> <arithExprTail>");
				parseUp();
				return true;
			}
		}		
		parseDestroy();
		return false;
	}

	private boolean arithExprTail(TypeRef type) {
		// skip errors
		if(!skipErrors("arithExprTail")) {
			return false;
		}
		
		TypeRef right_type = new TypeRef();
		
		// add the node for this production.
		parseDown();
		
		if(("_ADDOP _OR").contains(lookahead.token_name)) {
			if(addOp() && term(right_type) && arithExprTail(right_type) && typeMatch(type, right_type)) {
				print("<arithExprTail> ::= <addOp> <term> <arithExprTail>");
				parseUp();
				return true;
			}
		} else if (("_SCOLON _RP _COMMA _RSB _LT _GT _RELOP").contains(lookahead.token_name)) {
			print("<arithExprTail> ::= EPSILON");
			parseUp();
			return true;
		}		
		parseDestroy();
		return false;
	}
	
	private boolean sign() {
		// skip errors
		if(!skipErrors("sign")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
		
		// initialize the first set.
		
		if(("_ADDOP").contains(lookahead.token_name)) {
			String lex = lookahead.lexeme;
			if(match("_ADDOP")) {
				print("<sign> ::= " + lex);
				parseUp();
				return true;
			}
		}		
		parseDestroy();
		return false;
	}

	private boolean term(TypeRef type) {
		// skip errors
		if(!skipErrors("term")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
		
		if(("_LP _NOT _ID _FNUM _INUM _ADDOP").contains(lookahead.token_name)) {
			if(factor(type) && termTail(type)) {
				print("<term> ::= <factor> <termTail>");
				parseUp();
				return true;
			}
		}		
		parseDestroy();
		return false;
	}

	private boolean termTail(TypeRef type) {
		// skip errors
		if(!skipErrors("termTail")) {
			return false;
		}
		
		TypeRef right_type = new TypeRef();
		
		// add the node for this production.
		parseDown();
		
		if(("_MULTOP _AND").contains(lookahead.token_name)) {
			if(multOp() && factor(right_type) && termTail(right_type) && typeMatch(type, right_type)) {
				print("<termTail> ::= <multOp> <factor> <termTail>");
				parseUp();
				return true;
			}
		} else if (("_SCOLON _RP _COMMA _RSB _LT _GT _RELOP _ADDOP _OR").contains(lookahead.token_name)) {
			print("<termTail> ::= EPSILON");
			parseUp();
			return true;
		}		
		parseDestroy();
		return false;
	}

	private boolean factor(TypeRef type) {
		// skip errors
		if(!skipErrors("factor")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
		
		if(("_ADDOP").contains(lookahead.token_name)) {
			if(sign() && factor(type)) {
				print("<factor> ::= <sign> <factor>");
				parseUp();
				return true;
			} 
		} else if(("_FNUM _INUM").contains(lookahead.token_name)) {
			if (num(type)) {
				print("<factor> ::= <num>");
				parseUp();
				return true;
			}
		} else if(("_ID").contains(lookahead.token_name)) {
			if (varOrFuncCall(type, curr_scope)) {
				print("<factor> ::= <varOrFuncCall>");
				parseUp();
				return true;
			}
		} else if(("_NOT").contains(lookahead.token_name)) {
			if (match("_NOT") && factor(type) && typeMatch(type, int_type)) {
				print("<factor> ::= not <factor>");
				parseUp();
				return true;
			} 
		} else if(("_LP").contains(lookahead.token_name)) {
			if (match("_LP") && arithExpr(type) && match("_RP")) {
				print("<factor> ::= ( <arithExpr> )");
				parseUp();
				return true;
			}
		}		
		parseDestroy();
		return false;
	}

	private boolean varOrFuncCall(TypeRef type, SymbolTable scope) {
		// skip errors
		if(!skipErrors("varOrFuncCall")) {
			return false;
		}
		
		// maintain reference to current table, as it may change later.
		SymbolTable old_scope = scope;
		
		TypeRef name = new TypeRef();
		
		// add the node for this production.
		parseDown();		
		
		if(("_ID").contains(lookahead.token_name)) {
			if(match("_ID", name) && getType(name, type) && varOrFuncCallTail(name, type, scope)) {
				print("<varOrFuncCall> ::= id <varOrFuncCallTail>");
				parseUp();
				scope = old_scope;
				return true;
			}
		}	
		parseDestroy();	
		scope = old_scope;
		return false;
	}
	
	private boolean varOrFuncCallTail(TypeRef name, TypeRef type, SymbolTable scope) {
		// skip errors
		if(!skipErrors("varOrFuncCallTail")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
				
		if(("_LSB _DOT").contains(lookahead.token_name)) {
			if(indice(name, type) && varOrFuncCallTailTail(type, scope)) {
				print("<varOrFuncCallTail> ::= <indice> <varOrFuncCallTailTail>");
				parseUp();
				return true;
			} 
		} else if(("_LP").contains(lookahead.token_name)) {
			if (functionCheck(name, scope) && match("_LP") && aParams() && match("_RP")) {
				print("<varOrFuncCallTail> ::= ( <aParams> )");
				parseUp();
				return true;
			} 
		} else if (("_SCOLON _RP _COMMA _RSB _LT _GT _RELOP _ADDOP _OR _MULTOP _AND").contains(lookahead.token_name)) {
			print("<varOrFuncCallTail> ::= EPSILON");
			parseUp();
			return true;
		}		
		parseDestroy();
		return false;
	}
	
	private boolean varOrFuncCallTailTail(TypeRef type, SymbolTable scope) {
		// skip errors
		if(!skipErrors("varOrFuncCallTailTail")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
		
		if(("_DOT").contains(lookahead.token_name)) {
			if(match("_DOT") && updateScope(type, scope) && varOrFuncCall(type, scope)) {
				print("<varOrFuncCallTailTail> ::= . <varOrFuncCall>");
				parseUp();
				return true;
			} 
		} else if (("_SCOLON _RP _COMMA _RSB _LT _GT _RELOP _ADDOP _OR _MULTOP _AND").contains(lookahead.token_name)) {
			print("<varOrFuncCallTailTail> ::= EPSILON");
			parseUp();
			return true;
		}		
		parseDestroy();
		return false;
	}

	private boolean variable(TypeRef type) {
		// skip errors
		if(!skipErrors("variable")) {
			return false;
		}
		
		TypeRef name = new TypeRef();
		
		// add the node for this production.
		parseDown();
				
		if(("_ID").contains(lookahead.token_name)) {
			if(match("_ID", name) && variableCheck(name) && getType(name, type) && indice(name, type) && idnest(type)) {
				print("<variable> ::= id <indice> <idnest>");
				parseUp();
				return true;
			}
		}	
		parseDestroy();	
		return false;
	}

	private boolean idnest(TypeRef type) {
		// skip errors
		if(!skipErrors("idnest")) {
			return false;
		}
		
		TypeRef name = new TypeRef();
		
		// add the node for this production.
		parseDown();
		
		if(("_DOT").contains(lookahead.token_name)) {
			if(match("_DOT") && match("_ID", name) && getAttributeType(type, name, type) && indice(name, type) && idnest(type)) {
				print("<idnest> ::= . id <indice> <idnest>");
				parseUp();
				return true;
			} 
		} else if (("_EQUAL _RP").contains(lookahead.token_name)) {
			print("<idnest> ::= EPSILON");
			parseUp();
			return true;
		}		
		parseDestroy();
		return false;
	}

	private boolean indice(TypeRef name, TypeRef type) {
		// skip errors
		if(!skipErrors("indice")) {
			return false;
		}
		
		TypeRef index_type = new TypeRef();
		
		// add the node for this production.
		parseDown();
		
		if(("_LSB").contains(lookahead.token_name)) {
			if(indexVar(name, type) && match("_LSB") && arithExpr(index_type) && typeMatch(index_type, int_type) && match("_RSB") && indice(name, type)) {
				print("<indice> ::= [ <arithExpr> ] <indice>");
				parseUp();
				return true;
			} 
		} else if (("_DOT _EQUAL _RP _SCOLON _COMMA _RSB _LT _GT _RELOP _ADDOP _OR _MULTOP _AND").contains(lookahead.token_name)) {
			print("<indice> ::= EPSILON");
			parseUp();
			return true;
		}		
		parseDestroy();
		return false;
	}

	private boolean arraySize(TypeRef type) {
		// skip errors
		if(!skipErrors("arraySize")) {
			return false;
		}
		
		TypeRef size = new TypeRef();
		
		// add the node for this production.
		parseDown();
		
		if(("_LSB").contains(lookahead.token_name)) {
			if(match("_LSB") && match("_INUM", size) && match("_RSB") && addIndice(type, size) && arraySize(type)) {
				print("<arraySize> ::= [ inum ] <arraySize>");
				parseUp();
				return true;
			}
		} else if (("_SCOLON _RP _COMMA").contains(lookahead.token_name)) {
			print("<arraySize> ::= EPSILON");
			parseUp();
			return true;
		} 		
		parseDestroy();
		return false;
	}
	
	private boolean type(TypeRef type) {
		// skip errors
		if(!skipErrors("type")) {
			return false;
		}
		
		
		
		// add the node for this production.
		parseDown();
				
		if(("_FLOAT _INT").contains(lookahead.token_name)) {
			if(primitiveType(type)) {
				print("<type> ::= <primitiveType>");
				parseUp();
				return true;
			} 
		} else if(("_ID").contains(lookahead.token_name)) {
			if (match("_ID", type) && classCheck(type)) {
				print("<type> ::= id");
				parseUp();
				return true;
			} 
		}		
		parseDestroy();
		return false;
	}

	private boolean primitiveType(TypeRef type) {
		// skip errors
		if(!skipErrors("primitiveType")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
		
		if(("_FLOAT").contains(lookahead.token_name)) {
			if(match("_FLOAT", type)) {
				print("<primitiveType> ::= float");
				parseUp();
				return true;
			} 
		} else if(("_INT").contains(lookahead.token_name)) {
			if (match("_INT", type)) {
				print("<primitiveType> ::= int");
				parseUp();
				return true;
			}
		}		
		parseDestroy();
		return false;
	}

	private boolean fParams() {
		// skip errors
		if(!skipErrors("fParams")) {
			return false;
		}
		
		TypeRef type = new TypeRef();
		TypeRef name = new TypeRef();
		
		// add the node for this production.
		parseDown();
		
		if(("_ID _FLOAT _INT").contains(lookahead.token_name)) {
			if(type(type) && match("_ID", name) && arraySize(type) && addEntry(name.val, "parameter", type) && fParamsTail()) {
				print("<fParams> ::= <type> id <arraySize> <fParamsTail>");
				parseUp();
				return true;
			} 
		} else if (("_RP").contains(lookahead.token_name)) {
			print("<fParams> ::= EPSILON");
			parseUp();
			return true;
		}		
		parseDestroy();
		return false;
	}

	private boolean aParams() {
		// skip errors
		if(!skipErrors("aParams")) {
			return false;
		}
		
		TypeRef type = new TypeRef();
		
		// add the node for this production.
		parseDown();
				
		if(("_LP _NOT _ID _FNUM _INUM _ADDOP").contains(lookahead.token_name)) {
			if(expr(type) && aParamsTail()) {
				print("<aParams> ::= <expr> <aParamsTail>");
				parseUp();
				return true;
			} 
		} else if (("_RP").contains(lookahead.token_name)) {
			print("<aParams> ::= EPSILON");
			parseUp();
			return true;
		}		
		parseDestroy();
		return false;
	}

	private boolean fParamsTail() {
		// skip errors
		if(!skipErrors("fParamsTail")) {
			return false;
		}
		
		TypeRef type = new TypeRef();
		TypeRef name = new TypeRef();
		
		// add the node for this production.
		parseDown();
		
		if(("_COMMA").contains(lookahead.token_name)) {
			if(match("_COMMA") && type(type) && match("_ID", name) && arraySize(type) && addEntry(name.val, "parameter", type) && fParamsTail()) {
				print("<fParamsTail> ::= , <type> id <arraySize> <fParamsTail>");
				parseUp();
				return true;
			} 
		} else if (("_RP").contains(lookahead.token_name)) {
			print("<fParamsTail> ::= EPSILON");
			parseUp();
			return true;
		}		
		parseDestroy();
		return false;
	}
	
	private boolean aParamsTail() {
		// skip errors
		if(!skipErrors("aParamsTail")) {
			return false;
		}
		
		TypeRef type = new TypeRef();
		
		// add the node for this production.
		parseDown();
		
		if(("_COMMA").contains(lookahead.token_name)) {
			if(match("_COMMA") && expr(type) && aParamsTail()) {
				print("<aParamsTail> ::= , <expr> <aParamsTail>");
				parseUp();
				return true;
			}
		} else if (("_RP").contains(lookahead.token_name)) {
			print("<aParamsTail> ::= EPSILON");
			parseUp();
			return true;
		} 		
		parseDestroy();
		return false;
	}

	private boolean assignOp() {
		// skip errors
		if(!skipErrors("assignOp")) {
			return false;
		}
		
		// add the node for this production.
		parseDown(); 
				
		if(("_EQUAL").contains(lookahead.token_name)) {
			if(match("_EQUAL")) {
				print("<assignOp> ::= =");
				parseUp();
				return true;
			}
		}		
		parseDestroy();
		return false;
	}

	private boolean relOp() {
		// skip errors
		if(!skipErrors("relOp")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
						
		if(("_RELOP").contains(lookahead.token_name)) {
			String lex = lookahead.lexeme;
			if(match("_RELOP")) {
				print("<relOp> ::= " + lex);
				parseUp();
				return true;
			} 
		} else if(("_GT").contains(lookahead.token_name)) {
			if (match("_GT")) {
				print("<relOp> ::= >");
				parseUp();
				return true;
			} 
		} else if(("_LT").contains(lookahead.token_name)) {
			if (match("_LT")) {
				print("<relOp> ::= <");
				parseUp();
				return true;
			} 
		}		
		parseDestroy();
		return false;
	}

	private boolean addOp() {
		// skip errors
		if(!skipErrors("addOp")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
				
		if(("_OR").contains(lookahead.token_name)) {
			if(match("_OR")) {
				print("<addOp> ::= or");
				parseUp();
				return true;
			} 
		} else if(("_ADDOP").contains(lookahead.token_name)) {
			String lex = lookahead.lexeme;
			if (match("_ADDOP")) {
				print("<addOp> ::= " + lex);
				parseUp();
				return true;
			}
		}		
		parseDestroy();
		return false;
	}

	private boolean multOp() {
		// skip errors
		if(!skipErrors("multOp")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
				
		if(("_AND").contains(lookahead.token_name)) {
			if(match("_AND")) {
				print("<multOp> ::= and");
				parseUp();
				return true;
			} 
		} else if(("_MULTOP").contains(lookahead.token_name)) {
			String lex = lookahead.lexeme;
			if (match("_MULTOP")) {
				print("<multOp> ::= " + lex);
				parseUp();
				return true;
			} 
		}		
		parseDestroy();
		return false;
	}	
	
	private boolean num(TypeRef type) {
		// skip errors
		if(!skipErrors("num")) {
			return false;
		}
		
		// add the node for this production.
		parseDown();
				
		if(("_INUM").contains(lookahead.token_name)) {
			if(match("_INUM", type)) {
				type.val = "int";
				print("<num> ::= inum");
				parseUp();
				return true;
			} 
		} else if(("_FNUM").contains(lookahead.token_name)) {
			if (match("_FNUM", type)) {
				type.val = "float";
				print("<num> ::= fnum");
				parseUp();
				return true;
			} 
		}		
		parseDestroy();
		return false;
	}
	// PRODUCTIONS - END
	
	// PARSE TREE - BEGIN
	private void parseDown() { // create a leaf
		parseTree.newLeaf();
	}
	
	private void parseUp() { // return to parent
		parseTree.toParent();
	}
	
	private void parseDestroy() { // destroy current node, return to parent
		parseTree.destroyLeaf();
	}
	
	private void print(String s) {
		parseTree.setCurrValue(s);
	}
	// PARSE TREE - END
	
	private void init() {
		// initialize the FIRST Sets
		firstSets.put("prog", "_CLASS _ID _FLOAT _INT _PROGRAM");
		firstSets.put("classDecl", "_EPSILON _CLASS");
		firstSets.put("varThenFunc", "_EPSILON _ID _FLOAT _INT");
		firstSets.put("varOrFunc", "_LSB _SCOLON _LP");
		firstSets.put("progBody", "_ID _FLOAT _INT _PROGRAM");
		firstSets.put("funcHead", "_ID _FLOAT _INT");
		firstSets.put("funcDef", "_EPSILON _ID _FLOAT _INT");
		firstSets.put("funcBody", "_LB");
		firstSets.put("varThenStat", "_EPSILON _FOR _IF _GET _PUT _RETURN _FLOAT _INT _ID");
		firstSets.put("varOrStat", "_ID _LSB _DOT _EQUAL");
		firstSets.put("varTail", "_ID");
		firstSets.put("statement", "_FOR _IF _GET _PUT _RETURN _ID");
		firstSets.put("statementRes", "_RETURN _PUT _GET _IF _FOR");
		firstSets.put("mStatement", "_EPSILON _ID _FOR _IF _GET _PUT _RETURN");
		firstSets.put("assignStat", "_ID");
		firstSets.put("statBlock", "_EPSILON _LB _ID _FOR _IF _GET _PUT _RETURN");
		firstSets.put("expr", "_LP _NOT _ID _FNUM _INUM _ADDOP");
		firstSets.put("relExprPart", "_EPSILON _LT _GT _RELOP");
		firstSets.put("relExpr", "_LP _NOT _ID _FNUM _INUM _ADDOP");
		firstSets.put("arithExpr", "_LP _NOT _ID _FNUM _INUM _ADDOP");
		firstSets.put("arithExprTail", "_EPSILON _OR _ADDOP");
		firstSets.put("sign", "_ADDOP");
		firstSets.put("term", "_LP _NOT _ID _FNUM _INUM _ADDOP");
		firstSets.put("termTail", "_EPSILON _MULTOP _AND");
		firstSets.put("factor", "_ADDOP _FNUM _INUM _ID _NOT _LP");
		firstSets.put("varOrFuncCall", "_ID");
		firstSets.put("varOrFuncCallTail", "_EPSILON _LSB _DOT _LP");
		firstSets.put("varOrFuncCallTailTail", "_EPSILON _DOT");
		firstSets.put("variable", "_ID");
		firstSets.put("idnest", "_EPSILON _DOT");
		firstSets.put("indice", "_EPSILON _LSB");
		firstSets.put("arraySize", "_EPSILON _LSB");
		firstSets.put("type", "_FLOAT _INT _ID");
		firstSets.put("primitiveType", "_INT _FLOAT");
		firstSets.put("fParams", "_EPSILON _ID _FLOAT _INT");
		firstSets.put("aParams", "_EPSILON _LP _NOT _ID _FNUM _INUM _ADDOP");
		firstSets.put("fParamsTail", "_EPSILON _COMMA");
		firstSets.put("aParamsTail", "_EPSILON _COMMA");
		firstSets.put("assignOp", "_EQUALS");
		firstSets.put("relOp", "_LT _GT _RELOP");
		firstSets.put("addOp", "_OR _ADDOP");
		firstSets.put("multOp", "_AND _MULTOP");
		firstSets.put("num", "_INUM _FNUM");
		
		// initialize the FOLLOW Sets (does not include EPSILON)
		followSets.put("prog", "_EOF");
		followSets.put("classDecl", "_ID _FLOAT _INT _PROGRAM");
		followSets.put("varThenFunc", "_RB");
		followSets.put("varOrFunc", "_RB");
		followSets.put("progBody", "_EOF");
		followSets.put("funcHead", "_LB");
		followSets.put("funcDef", "_RB _PROGRAM");
		followSets.put("funcBody", "_SCOLON");
		followSets.put("varThenStat", "_RB");
		followSets.put("varOrStat", "_RB");
		followSets.put("varTail", "_RB");
		followSets.put("statement", "_RB _SCOLON _ELSE");
		followSets.put("statementRes", "_RB _SCOLON _ELSE");
		followSets.put("mStatement", "_RB");
		followSets.put("assignStat", "_SCOLON _RP");
		followSets.put("statBlock", "_SCOLON _ELSE");
		followSets.put("expr", "_SCOLON _RP");
		followSets.put("relExprPart", "_SCOLON _RP _COMMA");
		followSets.put("relExpr", "_SCOLON");
		followSets.put("arithExpr", "_SCOLON _RP _COMMA");
		followSets.put("arithExprTail", "_SCOLON _RP _COMMA _LT _GT _RELOP _RSB");
		followSets.put("sign", "_ADDOP _FNUM _INUM _ID _NOT _LP");
		followSets.put("term", "_SCOLON _RP _COMMA _LT _GT _RELOP _RSB");
		followSets.put("termTail", "_SCOLON _RP _COMMA _LT _GT _RELOP _RSB _ADDOP _OR");
		followSets.put("factor", "_SCOLON _RP _COMMA _LT _GT _RELOP _RSB _ADDOP _OR");
		followSets.put("varOrFuncCall", "_SCOLON _RP _COMMA _LT _GT _RELOP _RSB _ADDOP _OR _MULTOP _AND");
		followSets.put("varOrFuncCallTail", "_SCOLON _RP _COMMA _LT _GT _RELOP _RSB _ADDOP _OR _MULTOP _AND");
		followSets.put("varOrFuncCallTailTail", "_SCOLON _RP _COMMA _LT _GT _RELOP _RSB _ADDOP _OR _MULTOP _AND");
		followSets.put("variable", "_RP _EQUAL");
		followSets.put("idnest", "_RP _EQUAL");
		followSets.put("indice", "_DOT _EQUAL _SCOLON _RP _COMMA _LT _GT _RELOP _RSB _ADDOP _OR _MULTOP _AND");
		followSets.put("arraySize", "_SCOLON _RP _COMMA");
		followSets.put("type", "_ID");
		followSets.put("primitiveType", "_ID");
		followSets.put("fParams", "_RP");
		followSets.put("aParams", "_RP");
		followSets.put("fParamsTail", "_RP");
		followSets.put("aParamsTail", "_RP");
		followSets.put("assignOp", "_LP _FNUM _INUM _ID _NOT _ADDOP");
		followSets.put("relOp", "_LP _FNUM _INUM _ID _NOT _ADDOP");
		followSets.put("addOp", "_LP _FNUM _INUM _ID _NOT _ADDOP");
		followSets.put("multOp", "_ADDOP _FNUM _INUM _ID _NOT _LP");
		followSets.put("num", "_SCOLON _RP _COMMA _LT _GT _RELOP _RSB _ADDOP _OR");
	
		// initialize the token_name to expected_string conversion for match errors
		tokenNameToExpectedString.put("_CLASS", "'class'");
		tokenNameToExpectedString.put("_PROGRAM", "'program'");
		tokenNameToExpectedString.put("_IF", "'if'");
		tokenNameToExpectedString.put("_THEN", "'then'");
		tokenNameToExpectedString.put("_ELSE", "'else'");
		tokenNameToExpectedString.put("_FOR", "'for'");
		tokenNameToExpectedString.put("_INT", "'int'");
		tokenNameToExpectedString.put("_FLOAT", "'float'");
		tokenNameToExpectedString.put("_GET", "'get'");
		tokenNameToExpectedString.put("_PUT", "'put'");
		tokenNameToExpectedString.put("_RETURN", "'return'");
		tokenNameToExpectedString.put("_NOT", "'not'");
		tokenNameToExpectedString.put("_AND", "'and'");
		tokenNameToExpectedString.put("_OR", "'or'");
		tokenNameToExpectedString.put("_CMT", "a comment");
		tokenNameToExpectedString.put("_RB", "'}'");
		tokenNameToExpectedString.put("_LB", "'{'");
		tokenNameToExpectedString.put("_RSB", "']'");
		tokenNameToExpectedString.put("_LSB", "'['");
		tokenNameToExpectedString.put("_RP", "')'");
		tokenNameToExpectedString.put("_LP", "'('");
		tokenNameToExpectedString.put("_RELOP", "a relational operator");
		tokenNameToExpectedString.put("_GT", "'>'");
		tokenNameToExpectedString.put("_LT", "'<'");
		tokenNameToExpectedString.put("_SCOLON", "';'");
		tokenNameToExpectedString.put("_COMMA", "','");
		tokenNameToExpectedString.put("_DOT", "'.'");
		tokenNameToExpectedString.put("_EQUAL", "'='");
		tokenNameToExpectedString.put("_ADDOP", "'+' or '-'");
		tokenNameToExpectedString.put("_MULTOP", "'*' or '/'");
		tokenNameToExpectedString.put("_ID", "an identifier");
		tokenNameToExpectedString.put("_INUM", "an integer value");
		tokenNameToExpectedString.put("_FNUM", "a floating point value");
		tokenNameToExpectedString.put("_NL", "a line break");
		
		// initialize messages for each token when skipping errors.
	
	}	
}
