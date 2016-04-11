package lexicalAnalysis;

import java.util.HashMap;
import java.util.Map;

public class State {

	private boolean is_final;
	//  if the state is a final state, it can return a token name
	private String token_name;
	private Map<Character, Integer> transitions;
	private int else_dest; // save room on the map
	private boolean backtrack;
	private String err_msg;
	
	public State(int id, boolean is_final, String token_name, boolean backtrack, String err_msg) {
		this.is_final = is_final;
		this.token_name = token_name;
		this.transitions = new HashMap<>();
		this.else_dest = -1;
		this.backtrack = backtrack;
		this.err_msg = err_msg;
	}
	
	public void addTransition(int dest, char c) {
		transitions.put(c, dest);
	}
	
	public void addTransition(int dest, char[] criteria) {
		if(criteria == null || criteria.length == 0) {
			else_dest = dest;
			return;
		}
		
		for(char c : criteria) {
			transitions.put(c, dest);
		}
	}
	
	public int getNextState(char input) {
		Integer dest = transitions.get(input);
		if(dest != null) {
			return dest;
		}
		return else_dest;
	}
	
	public String getTokenName() {
		return token_name;
	}
	
	public boolean isFinal() {
		return is_final;
	}
	
	public boolean backtrack() {
		return (is_final && backtrack);
	}
	
	public String getErrorMessage() {
		return err_msg;
	}
	
}
