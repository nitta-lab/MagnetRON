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
	 * このメソッド実行およびその全呼び出し先を呼び出し木の中で逆向きに探索する(ただし、visitor が true を返すまで)
	 * @param visitor ビジター
	 * @return　true -- 探索を中断した, false -- 最後まで探索した
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
