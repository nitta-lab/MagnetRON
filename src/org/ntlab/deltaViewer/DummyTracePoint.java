package org.ntlab.deltaViewer;

import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.Statement;
import org.ntlab.trace.TracePoint;

public class DummyTracePoint extends TracePoint {
	private Statement dummyStatement = null;
	private MethodExecution dummyMethodExecution = null;

	public DummyTracePoint(MethodExecution methodExecution, Statement dummyStatement) {
		super(methodExecution, 0);
		this.dummyStatement = dummyStatement;
	}
	
	public DummyTracePoint(TracePoint tracePoint, MethodExecution dummyMethodExecution) {
		super(tracePoint.getMethodExecution(), tracePoint.getMethodExecution().getStatements().indexOf(tracePoint.getStatement()));
		this.dummyMethodExecution = dummyMethodExecution;
	}

	public Statement getStatement() {
		if (dummyStatement == null) return super.getStatement();
		return dummyStatement;
	}
	
	public MethodExecution getMethodExecution() {
		if (dummyMethodExecution != null) return dummyMethodExecution;
		return super.getMethodExecution();
	}
}
