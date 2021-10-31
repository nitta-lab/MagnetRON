package org.ntlab.trace;

import java.util.ArrayList;

public interface IMethodExecutionVisitor {
	abstract public boolean preVisitThread(ThreadInstance thread);
	abstract public boolean postVisitThread(ThreadInstance thread);
	abstract public boolean preVisitMethodExecution(MethodExecution methodExecution);
	abstract public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children);
}
