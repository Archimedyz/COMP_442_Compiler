package lexicalAnalysis;

public class LexToken {
	public String token_name;
	public String lexeme;
	public int line;
	public int col;
	public String err_msg;
	
	public LexToken(String token_name, String lexeme, int line, int col, String err_msg) {
		this.token_name = token_name;
		this.lexeme = lexeme;
		this.line = line;
		this.col = col;
		this.err_msg = err_msg;
	}
	
	@Override
	public String toString() {
		return "<" + token_name + ", " + line + ":" + col + ", \"" + lexeme + "\">";
	}
}
