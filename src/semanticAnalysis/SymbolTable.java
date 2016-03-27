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
	
	public Entry createEntry(String name, String kind, String type, int dimensions, ArrayList<Integer> array_sizes) {
		Entry toAdd = new Entry(name, kind, type, dimensions, array_sizes);
		
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
	
	public void getType(String name, TypeRef type) {
		for(Entry e : entries) {
			if(e.name.equals(name)) {
				type.copy(e);
			}
		}
	}
	
	public SymbolTable getParentScope() {
		return parent_scope;
	}
	
	public String getScopeName() {
		return scope_name;
	}
	
	public SymbolTable getScopeOf(String name) {
		for(Entry e : entries) {
			if(e.name.equals(name)) {
				return e.scope;
			}
		}
		return null;
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
	
	public class Entry {
		
		String name;
		String kind;
		String type;
		int dimension;
		ArrayList<Integer> array_sizes;
		SymbolTable scope;
		
		Entry(String name, String kind, String type, int dimension, ArrayList<Integer> array_sizes) {
			this.name = name;
			this.kind = kind;
			this.type = type;
			this.dimension = dimension;
			this.array_sizes = array_sizes == null ? new ArrayList<>() : array_sizes;
		}
		
		public SymbolTable getScope() {
			return scope;
		}
		
	}
	
}