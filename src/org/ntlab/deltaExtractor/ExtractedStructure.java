package org.ntlab.deltaExtractor;

import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.Reference;
import org.ntlab.trace.TracePoint;


public class ExtractedStructure {

	private Delta delta = new Delta();
	private MethodExecution coordinator = null;
	private MethodExecution parent = null;
	private MethodExecution creationCallTree;
	private TracePoint relatedTracePoint = null;

	public Delta getDelta() {
		return delta;
	}

	public MethodExecution getCoordinator() {
		return coordinator;
	}

	/**
	 * âºé¿ëï é¿ëïå„Ç…çÌèúÇ∑ÇÈÇ±Ç∆
	 * @param coordinator
	 */
	public void setCoordinator(MethodExecution coordinator) {
		this.coordinator = coordinator;
	}

	public void createParent(MethodExecution methodExecution) {
		coordinator = methodExecution;
		parent = null;
	}

//	public void addParent(MethodExecution callTree) {
//		if (parent == null)
//			coordinator.addChild(parent = callTree);
//		else
//			parent.addChild(parent = callTree);
//	}
//
//	public void addChild(MethodExecution callTree) {
//		if (parent == null)
//			coordinator.addChild(callTree);
//		else
//			parent.addChild(callTree);
//	}
//	
	public void addSrcSide(Reference reference) {
		delta.addSrcSide(reference);
	}

	public void addDstSide(Reference reference) {
		delta.addDstSide(reference);
	}

	public void changeParent() {
	}

	public void setCreationMethodExecution(MethodExecution callTree) {
		creationCallTree = callTree;
	}

	public MethodExecution getCreationCallTree() {
		return creationCallTree;
	}

	public void setRelatedTracePoint(TracePoint relatedTracePoint) {
		this.relatedTracePoint  = relatedTracePoint;
	}

	public TracePoint getRelatedTracePoint() {
		return relatedTracePoint;
	}
	
}
