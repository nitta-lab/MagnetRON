package org.ntlab.deltaViewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ntlab.deltaExtractor.IAliasCollector;
import org.ntlab.trace.ArrayUpdate;
import org.ntlab.trace.FieldUpdate;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.Reference;
import org.ntlab.trace.Statement;

import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxPoint;

public class DeltaLayout implements IObjectLayout {
	private mxPoint coordinatorPoint = new mxPoint(0, 100);

	@Override
	public void execute(IObjectCallGraph objectCallGraph, IAliasCollector aliasCollector, Map<String, ObjectVertex> objectToVertexMap) {		
		Statement relatedSt = objectCallGraph.getRelatedPoints().get(0).getStatement();
		String srcObjId = null;
		String dstObjId = null;
		if (relatedSt instanceof FieldUpdate) {
			// container to component
			srcObjId = ((FieldUpdate) relatedSt).getContainerObjId();
			dstObjId = ((FieldUpdate) relatedSt).getValueObjId();
		} else if (relatedSt instanceof ArrayUpdate) {
			// container to component
			srcObjId = ((ArrayUpdate) relatedSt).getArrayObjectId();
			dstObjId = ((ArrayUpdate) relatedSt).getValueObjectId();
		} else if (relatedSt instanceof MethodInvocation) {
			MethodInvocation methodInvStatement = (MethodInvocation) relatedSt;
			MethodExecution calledMethodExec = methodInvStatement.getCalledMethodExecution();
			String methodSignature = calledMethodExec.getSignature();
			if (calledMethodExec.isCollectionType()
					&& (methodSignature.contains("add(") 
							|| methodSignature.contains("set(") 
							|| methodSignature.contains("put(") 
							|| methodSignature.contains("push(") 
							|| methodSignature.contains("addElement("))) {
				// container to component
				srcObjId = calledMethodExec.getThisObjId();
				dstObjId = calledMethodExec.getArguments().get(0).getId();				
			} else {
				// this to another
				srcObjId = methodInvStatement.getThisObjId();
				dstObjId = calledMethodExec.getReturnValue().getId();
			}
		} else {
			return;
		}
		
		// reconstruct srcSide and dstSide
		List<Reference> srcSide = new ArrayList<>();
		List<Reference> dstSide = new ArrayList<>();
		List<Reference> references = new ArrayList<>();
		references.addAll(objectCallGraph.getReferences());
		while (references.size() > 0) {
			boolean found;
			do {
				found = false;
				for (Reference r: references) {
					if (r.getDstObjectId().equals(srcObjId)) {
						srcSide.add(r);
						references.remove(r);
						srcObjId = r.getSrcObjectId();
						found = true;
						break;
					}
				}
			} while (found);
			do {
				found = false;
				for (Reference r: references) {
					if (r.getDstObjectId().equals(dstObjId)) {
						dstSide.add(r);
						references.remove(r);
						dstObjId = r.getSrcObjectId();
						found = true;
						break;
					}
				}
			} while (found);
		}
			
		// layout
		double time = 150;
		double padding = 200;
		coordinatorPoint.setX(coordinatorPoint.getX() + (time * dstSide.size()) + padding);

		// ç∂è„(0, 0)
		double xCor = coordinatorPoint.getX();
		double yCor = coordinatorPoint.getY();
		
		//Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
		List<String> doneList = new ArrayList<>();
		
		// Layout vertex object.
		// srcSide
		int srcSideSize = srcSide.size();
		MethodExecution coordinator = objectCallGraph.getStartPoints().get(0);
		String coordinatorObjId = coordinator.getThisObjId();
		String coordinatorClassName = coordinator.getThisClassName();
		for (int i = srcSideSize - 1; i >= 0; i--) {
			Reference ref = srcSide.get(i);
			if (i == srcSideSize - 1 && !coordinatorObjId.equals(ref.getSrcObjectId()) && !coordinatorClassName.equals(ref.getSrcClassName())) {
				System.out.println("coordinator: " + coordinatorClassName + ", " + coordinatorObjId);
				coordinatorPoint.setX(coordinatorPoint.getX() + time * 2);
				xCor += time * 2;
				ObjectVertex vertex = objectToVertexMap.get(coordinatorObjId);
				setVertexCoordinate(vertex, xCor + (time * ((srcSideSize - 1) - i)), yCor + (time * ((srcSideSize - 1) - i)));
				doneList.add(coordinatorObjId);
				srcSideSize++;
			}
			System.out.println("srcSide: " + ref.getSrcClassName() + ", " + ref.isCreation() + ", " + ref.getSrcObjectId());
			if (!ref.isCreation() && !ref.getSrcObjectId().equals(ref.getDstObjectId())) {
				if (!doneList.contains(ref.getSrcObjectId())) {
					ObjectVertex vertex = objectToVertexMap.get(ref.getSrcObjectId());
					setVertexCoordinate(vertex, xCor + (time * ((srcSideSize - 1) - i)), yCor + (time * ((srcSideSize - 1) - i)));
					doneList.add(ref.getSrcObjectId());
				}
				if (!doneList.contains(ref.getDstObjectId())) {
					ObjectVertex vertex = objectToVertexMap.get(ref.getDstObjectId());
					setVertexCoordinate(vertex, xCor + (time * (srcSideSize - i)), yCor + (time * (srcSideSize - i)));
					doneList.add(ref.getDstObjectId());
				}
			} else {
				ObjectVertex vertex = objectToVertexMap.get(ref.getSrcObjectId());
				setVertexCoordinate(vertex, xCor + (time * ((srcSideSize - 1) - i)), yCor + (time * ((srcSideSize - 1) - i)));
				vertex = objectToVertexMap.get(ref.getDstObjectId());
				setVertexCoordinate(vertex, xCor + (time * (srcSideSize - i)), yCor + (time * (srcSideSize - i)));
				doneList.add(ref.getSrcObjectId());
				doneList.add(ref.getDstObjectId());
			}
		}

		// dstSide
		int dstSideSize = dstSide.size();
		int cnt = 0;
		for (int i = dstSideSize - 1; i >= 0; i--) {
			Reference ref = dstSide.get(i);
			if (i == dstSideSize - 1 && srcSideSize == 0 && !coordinatorObjId.equals(ref.getSrcObjectId()) && !coordinatorClassName.equals(ref.getSrcClassName())) {
				System.out.println("coordinator: " + coordinatorClassName + ", " + coordinatorObjId);
				coordinatorPoint.setX(coordinatorPoint.getX() + time * 2);
				xCor += time * 2;
				System.out.println(coordinatorPoint.getX() + ", " + xCor);
				ObjectVertex vertex = objectToVertexMap.get(coordinatorObjId);
				setVertexCoordinate(vertex, xCor - (time * (dstSideSize - i + cnt)), yCor + (time * (dstSideSize - i + cnt)));
				doneList.add(coordinatorObjId);
				dstSideSize++;
			}
			System.out.println("dstSide: " + ref.getSrcClassName() + ", " + ref.getDstClassName() + ", " + ref.isCreation());
			if (!ref.isCreation() && !ref.getSrcObjectId().equals(ref.getDstObjectId())) {
				if (!doneList.contains(ref.getSrcObjectId())) {
					ObjectVertex vertex = objectToVertexMap.get(ref.getSrcObjectId());
					setVertexCoordinate(vertex, xCor - (time * (dstSideSize - i + cnt)), yCor + (time * (dstSideSize - i + cnt)));
					doneList.add(ref.getSrcObjectId());
					cnt++;
				}
				if (!doneList.contains(ref.getDstObjectId())) {
					ObjectVertex vertex = objectToVertexMap.get(ref.getDstObjectId());
					setVertexCoordinate(vertex, xCor - (time * (dstSideSize - i + cnt)), yCor + (time * (dstSideSize - i + cnt)));
					doneList.add(ref.getDstObjectId());
				}
			} else {
				ObjectVertex vertex = objectToVertexMap.get(ref.getDstObjectId());
				setVertexCoordinate(vertex, xCor - (time * (dstSideSize - i + cnt)), yCor + (time * (dstSideSize - i + cnt)));
				doneList.add(ref.getDstObjectId());
			}
		}
	}
	
	private void setVertexCoordinate(ObjectVertex vertex, double x, double y) {
		vertex.setX(x);
		vertex.setY(y);
		vertex.setInitialPoint(x, y);
	}

}
