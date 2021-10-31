package org.ntlab.trace;

abstract public class AbstractTracePointVisitor implements IStatementVisitor {
	@Override
	public boolean preVisitStatement(Statement statement) {		// not called
		return false;
	}
	@Override
	public boolean postVisitStatement(Statement statement) {	// not called
		return false;
	}
	abstract public boolean preVisitStatement(Statement statement, TracePoint tp);
	abstract public boolean postVisitStatement(Statement statement, TracePoint tp);
}
