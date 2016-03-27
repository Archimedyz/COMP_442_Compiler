package semanticAnalysis;

import java.util.ArrayList;

public class SymbolTable {

	ArrayList<Entry> entries;
	String scope_name;
	SymbolTable parent_scope;
	
	public SymbolTable(String scope_name) {
		entries = new ArrayList<>();
		this.scope_name = scope_name;
		this.parent_scope = null;
	}
	
	public Entry createEntry(String name, String kind, String type) {
		Entry toAdd = new Entry(name, kind, type);
		
		if(("class function").contains(kind)) {
			toAdd.scope = new SymbolTable(name);
			toAdd.scope.parent_scope = this;
		}
		
		entries.add(toAdd);
		
		return toAdd;
	}
	
	public boolean search(String name) {
		for(Entry e : entries) {
			if(e.name.equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean search(String name, String kind) {
		for(Entry e : entries) {
			if(e.name.equals(name) && e.kind.equals(kind)) {
				return true;
			}
		}
		return false;
	}
	
	public SymbolTable getParentScope() {
		return parent_scope;
	}
	
	@Override
	public String toString() {
		
		String table_rep = "SCOPE: " + scope_name + "\n";
		String sub_table_rep = "";
		for(Entry e : entries) {
			table_rep += "[" + e.name + ", " + e.kind + ", " + e.type + ", " + (e.scope == null ? "NO" : "YES") + "]\n";
			if(e.scope != null) {
				sub_table_rep += "\n" + e.scope.toString();
			}
		}
		
		return table_rep + sub_table_rep;		
	}
	
	// returns true for a successful creation, otherwise false
	
	
	public class Entry {
		
		String name;
		String kind;
		String type;
		SymbolTable scope;
		
		Entry(String name, String kind, String type) {
			this.name = name;
			this.kind = kind;
			this.type = type;
		}
		
		public SymbolTable getScope() {
			return scope;
		}
		
	}
	
	
	
}