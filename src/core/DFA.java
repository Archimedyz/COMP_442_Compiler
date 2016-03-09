package core;

import java.util.HashMap;
import java.util.Map;

public class DFA {
	
	private Map<Integer, State> states;
	int curr_state;
	
	public DFA() {
		states = new HashMap<>();
		curr_state = 0;
	}
	
	public void addState(int id) {
		states.put(id, new State(states.size()+1, false, null, false, null));
	}
	
	public void addState(int id, String token_name, boolean backtrack, String err_msg) {
		states.put(id, new State(states.size()+1, true, token_name, backtrack, err_msg));
	}
	

	
	public boolean addTransistion(int src_id, int dest_id, char criteria) {
		State state = states.get(src_id);
		if(state == null) {
			return false;
		}
		
		state.addTransition(dest_id, criteria);
		return true;
	}
	
	public boolean addTransistion(int src_id, int dest_id, char[] criteria) {
		State state = states.get(src_id);
		if(state == null) {
			return false;
		}
		
		state.addTransition(dest_id, criteria);
		return true;
	}
	
	public void moveStates(char input) {
		if(states.size() == 0) {
			return;
		}
		int old_state = curr_state;
		curr_state = states.get(curr_state).getNextState(input);
		if(curr_state < 0) {
			System.out.println("ERROR - No 'else' transition set for State ID: " + old_state);
			curr_state = old_state;
		}
	}
	
	public void restart() {
		curr_state = 0;
	}
	
	public int getStateID() {
		return curr_state;
	}
	
	public boolean atFinalState() {
		State state = states.get(curr_state);
		if(state == null) {
			return false;
		}
		return state.isFinal();
	}
	
	public boolean backtrack() {
		State state = states.get(curr_state);
		if(state == null) {
			return false;
		}
		return state.backtrack();
	}
	
	public String getTokenName() {
		State state = states.get(curr_state);
		if(state == null) {
			return null;
		}
		return state.getTokenName();
	}
	
	public String getErrorMessage() {
		State state = states.get(curr_state);
		if(state == null) {
			return null;
		}
		return state.getErrorMessage();
	}
	
}
