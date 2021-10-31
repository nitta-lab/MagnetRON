package org.ntlab.deltaViewer;

import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.Statement;

public class DummyMethodExecution extends MethodExecution {
	public DummyMethodExecution(MethodExecution methodExec) {
		super(methodExec.getSignature(), methodExec.getCallerSideSignature(), methodExec.getThisClassName(), methodExec.getThisObjId(), methodExec.isConstructor(), methodExec.isStatic(), methodExec.getEntryTime());
		setCollectionType(methodExec.isCollectionType());
		setArguments(methodExec.getArguments());
		setReturnValue(methodExec.getReturnValue());
		setCaller(methodExec.getCallerMethodExecution(), methodExec.getCallerStatementExecution());
		for (Statement st: methodExec.getStatements()) {
			addStatement(st);
		}
		setExitTime(methodExec.getExitTime());
	}
}
