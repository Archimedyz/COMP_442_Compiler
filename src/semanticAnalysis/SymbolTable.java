package semanticAnalysis;

import java.util.ArrayList;

public class SymbolTable {

	ArrayList<Entry> entries;
	String scope_name;
	SymbolTable parent_scope;
	int mId;
	

	private static int next_address = 0;
	private static int next_id = 1;
	
	public SymbolTable(SymbolTable other) {
		this.copy(other);
	}
	
	public SymbolTable(String scope_name) {
		mId = next_id++;
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
	
	public int getId() {
		return mId;
	}
	
	public SymbolTable getScopeOf(String name) {
		for(Entry e : entries) {
			if(e.name.equals(name)) {
				return e.scope;
			}
		}
		return null;
	}
	
	public void copy(SymbolTable other) {
		this.parent_scope = other.parent_scope;
		this.scope_name = other.scope_name;
		this.entries = other.entries;
		this.mId = other.mId;
	}
	
	@Override
	public String toString() {
		
		String table_rep = "SCOPE: " + scope_name + " [ID: " + mId + "]\n";
		String sub_table_rep = "";
		for(Entry e : entries) {
			table_rep += "(" + e.address + ")[" + e.name + ", " + e.kind + ", " + e.type + ", " + dimensionStr(e) + ", " + (e.scope == null ? "NO" : e.scope.getId()) + "]\n";
			if(e.scope != null) {
				sub_table_rep += "\n" + e.scope.toString();
			}
		}
		
		return table_rep + sub_table_rep;		
	}
	
	public String dimensionStr(Entry e) {
		String str = "";
		for(int i : e.array_sizes) {
			str += "[" + i + "]";
		}
		return str;
	}
	
	public class Entry {
		
		String name;
		String kind;
		String type;
		int dimension;
		ArrayList<Integer> array_sizes;
		SymbolTable scope;
		
		int address;
		
		Entry(String name, String kind, String type, int dimension, ArrayList<Integer> array_sizes) {
			this.name = name;
			this.kind = kind;
			this.type = type;
			this.dimension = dimension;
			this.array_sizes = array_sizes == null ? new ArrayList<>() : array_sizes;
			address = next_address++;
		}
		
		public SymbolTable getScope() {
			return scope;
		}
		
	}
	
}