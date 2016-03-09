package lexicalAnalysis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import core.DFA;

public class LexicalAnalyzer {
	
	private BufferedReader src_file;
	private PrintWriter lex_out;
	private PrintWriter lex_err;
	
	private int curr_line;
	private int curr_col;
	private DFA token_dfa;
	private boolean eof;
	
	public LexicalAnalyzer() {
		
		src_file = null;
		
		try {
			lex_out = new PrintWriter("log/out/lex_out.txt");
			lex_err = new PrintWriter("log/err/lex_err.txt");
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open log files. [lex]");
		}
		
		this.curr_line = 0;
		this.curr_col = 0;
		this.token_dfa = new DFA();
		this.eof = true;
		init();
	}
	
	public boolean openSource(String src_file_path) {
		
		try {
			src_file = new BufferedReader(new FileReader(src_file_path));
		} catch(FileNotFoundException e) {
			return false;
		} catch(Exception e) {
			return false;
		}
		
		curr_line = 0;
		curr_col = 0;
		
		eof = false;
		
		return true;
	}
	
	public void finalize() {
		lex_out.close();
		lex_err.close();
	}
	
	public LexToken nextToken() throws IOException{
		
		String err_msg = "";
		
		String token = "";
		String lexeme = "";
		
		int start_line = curr_line;
		int start_col = curr_col; // add one because the next character will be read

		token_dfa.restart();
		do {
			// mark the index before reading the next character to ensure backtracking.
			src_file.mark(1);
			
			int curr_char_int = src_file.read();
			++curr_col;
			
			char curr_char = (char) curr_char_int;
			
			// check for whitespace if at state 0. if at state 0. ignore it and move on.
			if(token_dfa.getStateID() == 0 && (curr_char == ' ' || curr_char == '\t')) {
				// add to the column count
				start_col += (curr_char == ' ' ? 1 : 4);
				curr_col += (curr_char == ' ' ? 0 : 3); // bcause we added 1 earlier
				continue;
			}
			
			// move states
			token_dfa.moveStates(curr_char);	
			
			// -1 implies EOF
			if(curr_char_int == -1) {
				--curr_col; // because back tracking must be needed next time, when the eof is actually parse.
				break;
			}
			
			// if in the final state, we may need to backtrack
			if(token_dfa.atFinalState()) {
				if(token_dfa.backtrack()) {
					src_file.reset();
					--curr_col;
				} else {
					lexeme += curr_char;
				}
			} else {
				lexeme += curr_char;
			}
			
		} while(!token_dfa.atFinalState());
		
		// empty lexeme iff EOF
		if(lexeme.isEmpty()) {
			token = "_EOF";
			eof = true;
		} else {
			// if the state has a token name, use it, otherwise it is not in a final state (only possible if EOF reached before final state was reached.)
			token = token_dfa.getTokenName();
			err_msg = token_dfa.getErrorMessage();
			if(token == null) {
				token = "_ERROR";
				err_msg = "Unrecognized token.";
			}
		}
		
		// modify the lexeme if it matches a new_line. this in only for printing purposes.
		// also adjust the line:col arguments
		if(lexeme.equals("\r\n")) {
			lexeme = "\\r\\n";
			curr_line++;
			curr_col = 0;
		} else if(lexeme.equals("\n")) {
			lexeme = "\\n";
			curr_line++;
			curr_col = 0;
		}
		
		// finally, before going back, in the event that the token is ID, check to see if it matches a keyword.
		if(token.equals("_ID")) {
			token = keywordCheck(lexeme);
		}
		
		LexToken lex_token = new LexToken(token, lexeme, start_line, start_col, err_msg);
		
		lex_out.write(lex_token.toString());
		
		if(token.equals("_NL")) {
			lex_out.println();
		} else if(token.equals("_ERROR")) {
			lex_err.println("ERROR - (" + start_line + ":" + start_col + ") " + err_msg); 
		}
		
		return lex_token;
	}
	
	public boolean isEOF() {
		return eof;
	}
	
	/*
	 * PRIVATE FUNCTIONS
	 */
		
	private void init() {
		// create and initialize the DFA for tokenisation.
		// order matters, first state added is 0.
		
		String digit_str = "0123456789";
		String nonzero_str = "123456789";
		String letter_str = "abcdefghijklmonpqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String alphanum_str = digit_str + letter_str + "_";
		
		char[] digit = digit_str.toCharArray();
		char[] nonzero = nonzero_str.toCharArray();
		char[] letter = letter_str.toCharArray();
		char[] alphanum = alphanum_str.toCharArray();		
		
		// add the states (35)
		token_dfa.addState(0); // 0
		token_dfa.addState(1); // 1
		token_dfa.addState(2, "_INUM", true, null); // 2
		token_dfa.addState(3); // 3
		token_dfa.addState(4); // 4
		token_dfa.addState(5); // 5
		token_dfa.addState(6, "_FNUM", true, null); // 6
		token_dfa.addState(7); // 7
		token_dfa.addState(8); // 8
		token_dfa.addState(9); // 9
		token_dfa.addState(10, "_NL", false, null); // 10
		token_dfa.addState(11, "_ADDOP", false, null); // 11
		token_dfa.addState(12); // 12
		token_dfa.addState(13); // 13
		token_dfa.addState(14, "_MULTOP", true, null); // 14
		token_dfa.addState(15, "_CMT", false, null); // 15
		token_dfa.addState(16); // 16
		token_dfa.addState(17, "_EQUAL", true, null); // 17
		token_dfa.addState(18); // 18
		token_dfa.addState(19, "_LT", true, null); // 19
		token_dfa.addState(20); // 20
		token_dfa.addState(21, "_GT", true, null); // 21
		token_dfa.addState(22, "_RELOP", false, null); // 22
		token_dfa.addState(23, "_LP", false, null); // 23
		token_dfa.addState(24, "_RP", false, null); // 24
		token_dfa.addState(25, "_LSB", false, null); // 25
		token_dfa.addState(26, "_RSB", false, null); // 26
		token_dfa.addState(27, "_LB", false, null); // 27
		token_dfa.addState(28, "_RB", false, null); // 28
		token_dfa.addState(29, "_DOT", false, null); // 29
		token_dfa.addState(30, "_COMMA", false, null); // 30
		token_dfa.addState(31, "_SCOLON", false, null); // 31
		token_dfa.addState(32); // 32
		token_dfa.addState(33, "_ID", true, null); // 33
		token_dfa.addState(34, "_ERROR", true, "Improper floating-point number. [FloatFormat_Error]"); // 34
		// the keyword states . . .
		token_dfa.addState(81, "_ERROR", false, "Unrecognized character. [NotInAlphabet_Error]"); // 81
		token_dfa.addState(82, "_ERROR", true, "Invalid Newline. [InvalidNL_Error]"); // 82
		token_dfa.addState(83); // 83
		token_dfa.addState(84, "_ERROR", true, "Invalid identifier. [InvalidID_Error]"); // 84
		
		// add the transitions (53)
		token_dfa.addTransistion(0, 1, '0');
		token_dfa.addTransistion(0, 3, nonzero);
		token_dfa.addTransistion(0, 9, '\r');
		token_dfa.addTransistion(0, 10, '\n');
		token_dfa.addTransistion(0, 11, '+');
		token_dfa.addTransistion(0, 11, '-');
		token_dfa.addTransistion(0, 12, '*');
		token_dfa.addTransistion(0, 13, '/');
		token_dfa.addTransistion(0, 16, '=');
		token_dfa.addTransistion(0, 18, '<');
		token_dfa.addTransistion(0, 20, '>');
		token_dfa.addTransistion(0, 23, '(');
		token_dfa.addTransistion(0, 24, ')');
		token_dfa.addTransistion(0, 25, '[');
		token_dfa.addTransistion(0, 26, ']');
		token_dfa.addTransistion(0, 27, '{');
		token_dfa.addTransistion(0, 28, '}');
		token_dfa.addTransistion(0, 29, '.');
		token_dfa.addTransistion(0, 30, ',');
		token_dfa.addTransistion(0, 31, ';');
		token_dfa.addTransistion(0, 32, letter);
		token_dfa.addTransistion(0, 83, '_');
		token_dfa.addTransistion(0, 81, null);
		token_dfa.addTransistion(1, 4, '.');
		token_dfa.addTransistion(1, 2, null);
		token_dfa.addTransistion(3, 4, '.');
		token_dfa.addTransistion(3, 3, digit);
		token_dfa.addTransistion(3, 2, null);
		token_dfa.addTransistion(4, 5, digit);
		token_dfa.addTransistion(4, 34, null);
		token_dfa.addTransistion(5, 7, nonzero);
		token_dfa.addTransistion(5, 8, '0');
		token_dfa.addTransistion(5, 6, null);
		token_dfa.addTransistion(7, 8, '0');
		token_dfa.addTransistion(7, 6, null);
		token_dfa.addTransistion(8, 7, nonzero);
		token_dfa.addTransistion(8, 8, '0');
		token_dfa.addTransistion(8, 34, null);
		token_dfa.addTransistion(9, 10, '\n');
		token_dfa.addTransistion(9, 82, null);
		token_dfa.addTransistion(12, 15, '/');
		token_dfa.addTransistion(12, 14, null);
		token_dfa.addTransistion(13, 15, '*');
		token_dfa.addTransistion(13, 15, '/');
		token_dfa.addTransistion(13, 14, null);
		token_dfa.addTransistion(16, 22, '=');
		token_dfa.addTransistion(16, 17, null);
		token_dfa.addTransistion(18, 22, '=');
		token_dfa.addTransistion(18, 22, '>');
		token_dfa.addTransistion(18, 19, null);
		token_dfa.addTransistion(20, 22, '=');
		token_dfa.addTransistion(20, 21, null);
		token_dfa.addTransistion(32, 32, alphanum);
		token_dfa.addTransistion(32, 33, null);
		token_dfa.addTransistion(83, 83, alphanum);
		token_dfa.addTransistion(83, 84, null);
	}
	
	// checks for keywords and boolean operators
	private String keywordCheck(String lex) {
		if(lex.equals("if")
		|| lex.equals("then")
		|| lex.equals("else")
		|| lex.equals("for")
		|| lex.equals("class")
		|| lex.equals("int")
		|| lex.equals("float")
		|| lex.equals("get")
		|| lex.equals("put")
		|| lex.equals("return")
		|| lex.equals("and")
		|| lex.equals("not")
		|| lex.equals("or")
		|| lex.equals("program")
		) {
			return "_" + lex.toUpperCase(); // Token is then upper cased lexeme
		}
		// return ID if not a keyword.
		return "_ID";
	}
}
