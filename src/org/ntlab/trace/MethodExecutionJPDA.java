package org.ntlab.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class MethodExecutionJPDA {
	private String signature;
	private MethodExecutionJPDA callerMethodExecution = null;
	private ArrayList<MethodExecutionJPDA> children = new ArrayList<MethodExecutionJPDA>();
	private long entryTime = 0L;
	
	public MethodExecutionJPDA(String signature, long enterTime) {
		this.signature = signature;
		this.entryTime  = enterTime;
	}
	
	public String getSignature() {
		return signature;
	}

	public long getEntryTime() {
		return entryTime;
	}
	
	public void addChild(MethodExecutionJPDA child) {
		children.add(child);
	}

	public ArrayList<MethodExecutionJPDA> getChildren() {
		return children;
	}

	public void setCaller(MethodExecutionJPDA callerMethodExecution) {
		this.callerMethodExecution  = callerMethodExecution;
	}

	public MethodExecutionJPDA getParent() {
		return callerMethodExecution;
	}

	public MethodExecutionJPDA getCallerMethodExecution() {
		return callerMethodExecution;
	}
	
	/**
	 * ���̃��\�b�h���s����т��̑S�Ăяo������Ăяo���؂̒��ŋt�����ɒT������(�������Avisitor �� true ��Ԃ��܂�)
	 * @param visitor �r�W�^�[
	 * @return�@true -- �T���𒆒f����, false -- �Ō�܂ŒT������
	 */
	public boolean traverseMethodExecutionsBackward(IMethodExecutionVisitorJPDA visitor) {
		if (visitor.preVisitMethodExecution(this)) return true;
		ArrayList<MethodExecutionJPDA> calledMethodExecutions = getChildren();
		for (int i = calledMethodExecutions.size() - 1; i >= 0; i--) {
			MethodExecutionJPDA child = calledMethodExecutions.get(i);
			if (child.traverseMethodExecutionsBackward(visitor)) return true;
		}		
		if (visitor.postVisitMethodExecution(this, null)) return true;
		return false;
	}
	
	public interface IMethodExecutionVisitorJPDA {
		abstract public boolean preVisitThread(ThreadInstanceJPDA thread);
		abstract public boolean postVisitThread(ThreadInstanceJPDA thread);
		abstract public boolean preVisitMethodExecution(MethodExecutionJPDA methodExecution);
		abstract public boolean postVisitMethodExecution(MethodExecutionJPDA methodExecution, ArrayList<MethodExecutionJPDA> children);
	}
}
