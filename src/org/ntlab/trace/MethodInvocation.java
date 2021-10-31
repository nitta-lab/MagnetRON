package org.ntlab.trace;

public class MethodInvocation extends Statement {
	private MethodExecution calledMethodExecution = null;
	protected String thisClassName;
	protected String thisObjId;
	protected String callerSideMethodName = null;
	
	public MethodInvocation(MethodExecution methodExecution, String thisClassName, String thisObjId,
			int lineNo, String threadNo) {
		super(lineNo, threadNo);
		this.calledMethodExecution = methodExecution;
		this.thisClassName = thisClassName;
		this.thisObjId = thisObjId;
	}
		
	public MethodInvocation(String callerSideMethodName, String thisClassName, String thisObjId,
			int lineNo, String threadNo) {
		super(lineNo, threadNo);
		this.callerSideMethodName = callerSideMethodName;
		this.thisClassName = thisClassName;
		this.thisObjId = thisObjId;
	}
	
	public long getTimeStamp() {
		if (calledMethodExecution != null) {
			return calledMethodExecution.getEntryTime();
		}
		return timeStamp;
	}
	
	public void setCalledMethodExecution(MethodExecution calledMethodExecution) {
		this.calledMethodExecution = calledMethodExecution;
	}

	public MethodExecution getCalledMethodExecution() {
		return calledMethodExecution;
	}

	public String getThisClassName() {
		return thisClassName;
	}

	public String getThisObjId() {
		return calledMethodExecution.getCallerMethodExecution().getThisObjId();
	}

	public String getCallerSideMethodName() {
		return callerSideMethodName;
	}
}
