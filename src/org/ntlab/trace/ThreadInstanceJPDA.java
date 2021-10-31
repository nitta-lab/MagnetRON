package org.ntlab.trace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class ThreadInstanceJPDA {
	private ArrayList<MethodExecutionJPDA> roots = new ArrayList<MethodExecutionJPDA>();
	private MethodExecutionJPDA curMethodExecution = null;
	private String id;
	
	public ThreadInstanceJPDA(String id) {
		this.id = id;
	}
	
	public void addRoot(MethodExecutionJPDA root) {
		this.roots.add(root);
		curMethodExecution = root;
	}
	
	public ArrayList<MethodExecutionJPDA> getRoot() {
		return roots;
	}

	public String getId() {
		return id;
	}
	
	public void callMethod(String signature, long timeStamp) {
		MethodExecutionJPDA newMethodExecution = new MethodExecutionJPDA(signature, timeStamp);
		if (curMethodExecution != null) {
			curMethodExecution.addChild(newMethodExecution);
			newMethodExecution.setCaller(curMethodExecution);
			curMethodExecution = newMethodExecution;
		} else {
			addRoot(newMethodExecution);
		}
	}
	
	public void returnMethod() {
		if (curMethodExecution == null) return; 
		curMethodExecution = curMethodExecution.getParent();
	}
		
	public MethodExecutionJPDA getCuurentMethodExecution() {
		return curMethodExecution;
	}
	
	public void traverseMethodExecutionsBackward(MethodExecutionJPDA.IMethodExecutionVisitorJPDA visitor) {
		visitor.preVisitThread(this);
		for (int i = 0; i < roots.size(); i++) {
			MethodExecutionJPDA root = roots.get(i);
			root.traverseMethodExecutionsBackward(visitor);
		}
		visitor.postVisitThread(this);				
	}
}
