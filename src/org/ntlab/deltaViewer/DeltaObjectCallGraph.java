package org.ntlab.deltaViewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.ntlab.deltaExtractor.ExtractedStructure;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.ObjectReference;
import org.ntlab.trace.Reference;
import org.ntlab.trace.TracePoint;

public class DeltaObjectCallGraph implements IObjectCallGraph {
	private List<Reference> references = new ArrayList<>();
	private List<MethodExecution> startPoints = new ArrayList<>();
	private List<TracePoint> relatedPoints = new ArrayList<>();
	private SortedSet<Long> timeStamps = new TreeSet<>();		// List of time stamps of related points

	public DeltaObjectCallGraph(ExtractedStructure e) {
		references.addAll(e.getDelta().getSrcSide());
		references.addAll(e.getDelta().getDstSide());
		startPoints.add(e.getCoordinator());
		relatedPoints.add(e.getRelatedTracePoint());
		timeStamps.add(getTimeStampOfRelatedPoint(e.getRelatedTracePoint(), e));
	}

	@Override
	public List<Reference> getReferences() {
		return references;
	}

	@Override
	public List<MethodExecution> getStartPoints() {
		return startPoints;
	}

	@Override
	public List<TracePoint> getRelatedPoints() {
		// TODO Auto-generated method stub
		return relatedPoints;
	}

	@Override
	public Map<MethodExecution, List<MethodExecution>> getCallTree() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortedSet<Long> getTimeStamps() {
		return timeStamps;
	}
	
	private Long getTimeStampOfRelatedPoint(TracePoint relatedTracePoint, ExtractedStructure e) {
		ObjectReference targetObj = null;
		if (e.getDelta().getDstSide().size() > 0) {
			targetObj = e.getDelta().getDstSide().get(0).getDstObject();
		} else {
			targetObj = new ObjectReference(e.getCoordinator().getThisObjId(), e.getCoordinator().getThisClassName());
		}
		if (relatedTracePoint.isMethodEntry() && relatedTracePoint.getMethodExecution().getArguments().contains(targetObj)) {
			return relatedTracePoint.getMethodExecution().getEntryTime();
		}
		if (relatedTracePoint.getStatement() instanceof MethodInvocation) {
			return ((MethodInvocation) relatedTracePoint.getStatement()).getCalledMethodExecution().getExitTime();
		} else {
			return relatedTracePoint.getStatement().getTimeStamp();
		}
	}
}
