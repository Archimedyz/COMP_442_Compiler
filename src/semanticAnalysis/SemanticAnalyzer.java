package semanticAnalysis;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class SemanticAnalyzer {
	
	PrintWriter sem_out;
	PrintWriter sem_err;
	
	public SemanticAnalyzer() {
		try {
			sem_out = new PrintWriter("log/out/sem_out.txt");
			sem_err = new PrintWriter("log/err/sem_err.txt");
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open log files. [sem]");
		}
	}
	
	public void finalize() {
		sem_out.close();
		sem_err.close();
	}
	
	public boolean create(String table_name) {
		
		return true;
	}
	
	public boolean delete(String table_name) {
		
		return true;
	}
	
	public boolean insert(String table_name, String identifier, Object ptr) {
		
		return true;
	}
	
	public boolean search(String table_name, String identifier, Object ptr) {
		
		return true;
	}
	
	
	
}