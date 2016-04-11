package codeGeneration;

public class ExpressionTree {
	
	private Node root;
	private Node curr;
	
	public ExpressionTree() {
		root = null;
		curr = null;
	}
	
	public void pushRoot(String value, boolean isOperation, boolean isNumber, boolean isIdentifier) {
		Node new_root = new Node(value, isOperation, isNumber, isIdentifier, null);
		new_root.left = root;
		root = new_root;
		curr = root;
	}
	
	public boolean addLChild(String value, boolean isOperation, boolean isNumber, boolean isIdentifier) {
		curr.left = new Node(value, isOperation, isNumber, isIdentifier, curr);		
		return true;
	}
	
	public boolean addRChild(String value, boolean isOperation, boolean isNumber, boolean isIdentifier) {
		curr.right = new Node(value, isOperation, isNumber, isIdentifier, curr);
		return true;
	}
	
	public boolean addLChild(ExpressionTree et) {
		curr.left = et.root;
		curr.left.parent = curr;
		return true;
	}
	
	public boolean addRChild(ExpressionTree et) {
		curr.right = et.root;
		curr.right.parent = curr;
		return true;
	}
	
	public Node getRoot() {
		return root;
	}
	
	public Node getCurr() {
		return curr;
	}
	
	public boolean toParent() {
		curr = curr.parent == null ? curr : curr.parent;
		return true;
	}
	
	public boolean toLeft() {
		curr = curr.left == null ? curr : curr.left;
		return true;
	}
	
	public boolean toRight() {
		curr = curr.right == null ? curr : curr.right;
		return true;
	}
	
	public boolean preOrderParse() {
		return true;
	}
	
	public boolean inOrderParse() {
		return true;
	}
	
	public boolean postOrderParse() {
		return true;
	}
	
	/**
	 * In order string representation of the expression.
	 */
	public String toString() {
		return root == null ? "" : root.toString();
	}
	
	public class Node {
		public String value;
		public boolean isOperation;
		public boolean isNumber;
		public boolean isIdentifier;
		
		public Node parent;
		public Node left;
		public Node right;
		
		public Node(String value, boolean isOperation, boolean isNumber, boolean isIdentifier, Node parent) {
			this.value = value;
			this.isOperation = isOperation;
			this.isNumber = isNumber;
			this.isIdentifier = isIdentifier;
		}	
		
		public String toString() {
			return "(" + (left == null ? "" : left.toString()) + " " + value + " " + (right == null ? "" : right.toString()) + ")";
		}
	}
}
