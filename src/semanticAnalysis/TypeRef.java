package semanticAnalysis;

import java.util.ArrayList;

import semanticAnalysis.SymbolTable.Entry;

public class TypeRef {
	public String val = null;
	public int line = 0;
	public int col = 0;
	public int indices = 0;
	public int dimension = 0;
	public ArrayList<Integer> array_sizes;
	
	public TypeRef() {
		this.array_sizes = new ArrayList<>();		
	}
	public TypeRef(Entry e) {
		copy(e);
	}
	public void copy(Entry e) {
		this.val = e.type;
		this.dimension = e.dimension;
		this.array_sizes = e.array_sizes;
	}
}
