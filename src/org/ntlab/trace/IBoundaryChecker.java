package org.ntlab.trace;

public interface IBoundaryChecker {
	abstract public boolean withinBoundary(String methodSignature);
}
