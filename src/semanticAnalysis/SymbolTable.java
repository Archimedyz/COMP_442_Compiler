package semanticAnalysis;

import java.util.ArrayList;

public class SymbolTable {

	ArrayList<Entry> entries;
	String scope_name;
	SymbolTable parent_scope;
	Entry parent_entry;
	int mId;
	
	// the current pending entry.
	Entry pending;

	private static int next_address = 0;
	private static int next_id = 1;
	
	public SymbolTable(SymbolTable other) {
		this.copy(other);
	}
	
	public SymbolTable(String scope_name) {
		this.mId = next_id++;
		this.entries = new ArrayList<>();
		this.scope_name = scope_name;
		this.parent_scope = null;
		this.parent_entry = null;
		this.pending = null;
	}
	
	/*
	 *  Create an entry in the symbol table
	 */
	public Entry createEntry(String name, String kind, String type, int dimensions, ArrayList<Integer> array_sizes) {
		Entry toAdd = new Entry(name, kind, type, dimensions, array_sizes);
		
		if(kind.equals("parameter")) {
			parent_entry.params.add(toAdd); // redundant on purpose
		} else if(("class function").contains(kind)) {
			toAdd.scope = new SymbolTable(name);
			toAdd.scope.parent_scope = this;
			toAdd.scope.parent_entry = toAdd;
		}
		
		entries.add(toAdd);
		pending = toAdd;
		
		return toAdd;
	}
	
	/*
	 * Finalizes an entry setting <i>defined</i> to <b>true</b>;
	 */
	public void finalize() {
		if(pending != null) {
			pending.defined = true;
			pending = null;
		} 
	}
	
	public Entry getPendingEntry() {
		return pending;
	}
	
	public ArrayList<Entry> getAllEntriesOfKind(String kind) {
		ArrayList<Entry> ret = new ArrayList<Entry>();
		for(Entry e : entries) {
			if(e.kind.equals(kind) && e.defined) {
				ret.add(e);
			}
		}
		return ret;
	}
	
	/*
	 * Search the symbol table for the entry name
	 */
	public boolean search(String name) {
		for(Entry e : entries) {
			if(e.name.equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * Search the symbol table for an entry with a specific name and kind
	 */
	public boolean search(String name, String kind) {
		for(Entry e : entries) {
			if(e.name.equals(name) && e.kind.equals(kind)) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * Return a list of all function entries with the name <i>name</i> which have been successfully defined.
	 */
	public ArrayList<Entry> getDefinedFunctions(String name) {
		ArrayList<Entry> ret = new ArrayList<>();		
		for(Entry e : entries) {
			if(e.name.equals(name) && e.kind.equals("function") && e.defined) {
				ret.add(e);
			}
		}		
		return ret;
	}
	
	/*
	 * Determine the type of the given entry with name <i>name</i>.
	 */
	public void getType(String name, TypeRef type) {
		for(Entry e : entries) {
			if(e.name.equals(name)) {
				type.copy(e);
			}
		}
	}
	
	/*
	 * Return the parent Symbol Table if it exists.
	 */
	public SymbolTable getParentScope() {
		return parent_scope;
	}
	
	/*
	 * Return the Symbol Table name.
	 */
	public String getScopeName() {
		return scope_name;
	}
	
	/*
	 * Return all entries for this symbol table
	 */
	public ArrayList<Entry> getEntries() {
		return entries;
	}
	
	/*
	 * Return the Symbol Table ID.
	 */
	public int getId() {
		return mId;
	}
	
	/*
	 * Return the Symbol Table of the specified entry
	 */
	public SymbolTable getScopeOf(String name) {
		for(Entry e : entries) {
			if(e.name.equals(name)) {
				return e.scope;
			}
		}
		return null;
	}
	
	/*
	 * Copy a Symbol Table.
	 */
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
			table_rep += "(" + e.address + ")[" + e.name + ", " + e.kind + ", " + e.type + dimensionStr(e) + ", " + (e.defined ? "Defined" : "Undefined") + ", " + (e.scope == null ? "NO" : e.scope.getId()) + "]\n";
			if(e.scope != null) {
				sub_table_rep += "\n" + e.scope.toString();
			}
		}
		
		return table_rep + sub_table_rep;		
	}
	
	private String dimensionStr(Entry e) {
		String str = "";
		for(int i : e.array_sizes) {
			str += "[" + i + "]";
		}
		return str;
	}
	
	public class Entry {
		
		public String name;
		public String kind;
		public String type;
		public int dimension;
		public ArrayList<Integer> array_sizes;
		public SymbolTable scope;
		public boolean defined;
		public ArrayList<Entry> params;
		
		int address;
		
		Entry(String name, String kind, String type, int dimension, ArrayList<Integer> array_sizes) {
			this.name = name;
			this.kind = kind;
			this.type = type;
			this.dimension = dimension;
			this.array_sizes = array_sizes == null ? new ArrayList<>() : array_sizes;
			this.address = next_address++;
			this.defined = false;
			this.params = new ArrayList<>();
		}		
	}
}