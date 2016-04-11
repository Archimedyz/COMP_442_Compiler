package syntacticAnalysis;

import java.util.ArrayList;

public class ParseTree {
	
	Node root;
	Node curr;
	
	public ParseTree() {
		root = null;
		curr = null;
	}
	
	public void reset() {
		root = null;
		curr = null;
	}
	
	public boolean addRoot() {
		if(root == null) {
			root = new Node(null);
			curr = root;
			return true;
		}
		return false;
	}
	
	public boolean newLeaf() {
		if(root == null) {
			addRoot();
			return true;
		}
		if(curr == null) {
			return false;
		}
		Node leaf = new Node(curr);
		curr.children.add(leaf);
		curr = leaf;
		return true;
	}
	
	public boolean destroyLeaf() {
		if(curr == null) {
			return false;
		}
		Node theParent = curr.parent;
		if(curr.parent == null) { // at root, so just reset
			reset();
			return true;
		}
		curr.parent = null;
		curr = theParent;
		curr.children.remove(curr.children.size()-1);
		curr.children.trimToSize();
		return true;
	}
	
	public boolean toParent() {
		if(curr == null || curr == root) {
			return false;
		}
		curr = curr.parent;
		return true;
	}
	
	public boolean setCurrValue(String value) {
		if(curr == null) {
			return false;
		}
		curr.value = value;
		return true;
	}
	
	@Override
	public String toString() {
		if(root == null) {
			return "";
		}
		return root.toString();
	}
	
	private class Node {
		
		String value;
		ArrayList<Node> children;
		Node parent;
		
		Node(Node parent) {
			this.value = null;
			this.children = new ArrayList<>();
			this.parent = parent;
		}

		public String toString() {
			String str = value;
			for(Node c : children) {
				str += "\r\n" + c.toString();
			}
			return str;
		}
	}
	
}
