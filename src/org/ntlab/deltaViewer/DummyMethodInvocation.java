package org.ntlab.deltaViewer;

import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;

public class DummyMethodInvocation extends MethodInvocation {
	private long timeStamp;
	
	public DummyMethodInvocation(MethodInvocation methodInvocation) {
		super(methodInvocation.getCalledMethodExecution(), methodInvocation.getThisClassName(), methodInvocation.getThisObjId(), methodInvocation.getLineNo(), methodInvocation.getThreadNo());
	}

	public DummyMethodInvocation(MethodExecution methodExecution, String thisClassName, String thisObjId, int lineNo, String threadNo) {
		super(methodExecution, thisClassName, thisObjId, lineNo, threadNo);
	}
	
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public long getTimeStamp() {
		return timeStamp;
	}

}
