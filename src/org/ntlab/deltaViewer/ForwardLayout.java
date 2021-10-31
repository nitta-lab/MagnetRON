package org.ntlab.deltaViewer;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ntlab.deltaExtractor.Alias;
import org.ntlab.deltaExtractor.Alias.AliasType;
import org.ntlab.deltaExtractor.IAliasCollector;
import org.ntlab.trace.ArrayAccess;
import org.ntlab.trace.ArrayCreate;
import org.ntlab.trace.ArrayUpdate;
import org.ntlab.trace.FieldAccess;
import org.ntlab.trace.FieldUpdate;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.Reference;
import org.ntlab.trace.Statement;

import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxPoint;

public class ForwardLayout implements IObjectLayout {
	private static int angleStep = 15;
	private mxPoint coordinatorPoint = new mxPoint(0, 100);
	private double step;
	private double padding;		

	@Override
	public void execute(IObjectCallGraph objectCallGraph, IAliasCollector aliasCollector, Map<String, ObjectVertex> objectToVertexMap) {
		step = 250;
		padding = 200;
				
		// Extract the reference access history.
		List<Reference> references = objectCallGraph.getReferences();
		Map<Reference, List<Integer>> referenceHistory = new HashMap<>();
		int order = 0;
		for (Alias a: aliasCollector.getAliasList()) {
			int idx = -1;
			if (a.getAliasType() == AliasType.FIELD) {
				FieldAccess f = (FieldAccess) a.getOccurrencePoint().getStatement();
				idx = references.indexOf(new Reference(f.getContainerObjId(), f.getValueObjId(), f.getContainerClassName(), f.getValueClassName()));
			} else if (a.getAliasType() == AliasType.ARRAY_ELEMENT) {
				ArrayAccess aa = (ArrayAccess) a.getOccurrencePoint().getStatement();
				idx = references.indexOf(new Reference(aa.getArrayObjectId(), aa.getValueObjectId(), aa.getArrayClassName(), aa.getValueClassName()));
			} else if (a.getAliasType() == AliasType.RETURN_VALUE) {
				MethodExecution methodExec = a.getMethodExecution();
				if (methodExec.getSignature().contains("List.get(") ||
						methodExec.getSignature().contains("Map.get(") || 
						methodExec.getSignature().contains(".next()") ||
						methodExec.getSignature().contains(".iterator()") ||
						methodExec.getSignature().contains(".listIterator()")) {
					String srcObjId = methodExec.getThisObjId();
					String dstObjId = methodExec.getReturnValue().getId();
					String srcClassName = methodExec.getThisClassName();
					String dstClassName = methodExec.getReturnValue().getActualType();
					idx = references.indexOf(new Reference(srcObjId, dstObjId, srcClassName, dstClassName));
				}				
			} else if (a.getAliasType() == AliasType.CONSTRACTOR_INVOCATION) {
				MethodInvocation c = (MethodInvocation) a.getOccurrencePoint().getStatement();
				String srcObjId = a.getMethodExecution().getThisObjId();
				String dstObjId = c.getCalledMethodExecution().getThisObjId();
				String srcClassName = a.getMethodExecution().getThisClassName();
				String dstClassName = c.getCalledMethodExecution().getThisClassName();
				idx = references.indexOf(new Reference(srcObjId, dstObjId, srcClassName, dstClassName));
			} else if (a.getAliasType() == AliasType.ARRAY_CREATE) {
				ArrayCreate ac = (ArrayCreate) a.getOccurrencePoint().getStatement();
				String srcObjId = a.getMethodExecution().getThisObjId();
				String srcClassName = a.getMethodExecution().getThisClassName();
				idx = references.indexOf(new Reference(srcObjId, ac.getArrayObjectId(), srcClassName, ac.getArrayClassName()));
			}
			if (idx >= 0) {
				Reference r = references.get(idx);
				List<Integer> rHIstory = referenceHistory.get(r);
				if (rHIstory == null) {
					rHIstory = new ArrayList<>();
					referenceHistory.put(r, rHIstory);
				}
				rHIstory.add(order);
			}
			order++;
		}
		
		// Construct the object graph.
		Map<String, List<Reference>> succs = new HashMap<>();
		for (Reference r: references) {
			String srcObjId = r.getSrcObjectId();
			List<Reference> suc = succs.get(srcObjId);
			if (suc == null) {
				suc = new ArrayList<Reference>();
				succs.put(srcObjId, suc);
			}
			suc.add(r);
		}
		
		// Fix the all paths from the top object.
		double width = 1;
		coordinatorPoint.setX(coordinatorPoint.getX() + step * width + padding);
		double xCor = coordinatorPoint.getX();
		double yCor = coordinatorPoint.getY();
		Set<String> fixed = new HashSet<>();
		
		String topObjId = objectCallGraph.getStartPoints().get(0).getThisObjId();
		setVertexCoordinate(objectToVertexMap.get(topObjId), xCor, yCor);
		fixed.add(topObjId);
		
		String curObjId = topObjId;
		List<Reference> nextRefs = new ArrayList<>(succs.get(curObjId));
		if (nextRefs.size() > 0) {
			List<Reference> nextRefs2 = new ArrayList<>();
			do {
				Reference firstRef = getFirstReferece(nextRefs, referenceHistory, true);
				nextRefs2.add(firstRef);
				nextRefs.remove(firstRef);
			} while (nextRefs.size() > 1);
			nextRefs2.add(nextRefs.remove(0));
			double direction = 225;
			for (Reference r: nextRefs2) {
				List<String> path = new ArrayList<>();
				path.add(r.getSrcObjectId());
				traverseSuccs(succs, r, direction, true, referenceHistory, objectToVertexMap, fixed, path);
				if (nextRefs2.size() > 1) direction = getDirection(r, objectToVertexMap) + 180 + 90 / (nextRefs2.size() - 1);
			}
		}
	}

	private double getDirection(Reference ref, Map<String, ObjectVertex> objToVtx) {
		ObjectVertex src = objToVtx.get(ref.getSrcObjectId());
		ObjectVertex dst = objToVtx.get(ref.getDstObjectId());
		return Math.toDegrees(Math.atan2(dst.getY() - src.getY(), src.getX() - dst.getX()));
	}

	private Reference getFirstReferece(List<Reference> refs, Map<Reference, List<Integer>> referenceHistory, boolean bLeftFirst) {
		double first = 0.0;
		Reference firstRef = null;
		for (Reference ref: refs) {
			double avgOrder = 0.0;
			if (referenceHistory.get(ref) != null) {
				for (int ord: referenceHistory.get(ref)) {
					avgOrder += (double) ord;
				}
				avgOrder /= ((double) referenceHistory.get(ref).size());
			}
			if (firstRef == null || (bLeftFirst && avgOrder < first) || (!bLeftFirst && avgOrder > first)) {
				firstRef = ref;
				first = avgOrder;
			}
		}
		return firstRef;
	}	

	private void traverseSuccs(Map<String, List<Reference>> succs, Reference ref, double direction, boolean bLeftFirst, Map<Reference, List<Integer>> referenceHistory, Map<String, ObjectVertex> objToVtx, Set<String> fixed, List<String> path) {
		String curObj = ref.getDstObjectId();
		path.add(curObj);
		if (fixed.contains(curObj)) {
			if (path.size() > 2) fixPath(path, direction, objToVtx, fixed, true);
			return;
		}
		List<Reference> nextRefs = succs.get(curObj);
		if (nextRefs == null || nextRefs.size() == 0) {
			if (path.size() >= 2) fixPath(path, direction, objToVtx, fixed, false);
			return;
		}
		nextRefs = new ArrayList<>(nextRefs);
		do {
			Reference firstRef = getFirstReferece(nextRefs, referenceHistory, bLeftFirst);
			traverseSuccs(succs, firstRef, direction, bLeftFirst, referenceHistory, objToVtx, fixed, path);
			path = new ArrayList<>();
			path.add(curObj);
			nextRefs.remove(firstRef);
			direction = getDirection(firstRef, objToVtx) + 180;
			if (bLeftFirst) {
				direction += angleStep;
			} else {
				direction -= angleStep;
			}
		} while (nextRefs.size() > 0);
		return;
	}
	
	private void fixPath(List<String> path, double direction, Map<String, ObjectVertex> objToVtx, Set<String> fixed, boolean bClose) {
		if (path.size() < 2) return;
		ObjectVertex start = objToVtx.get(path.get(0));
		double x = start.getX();
		double y = start.getY();
		double dirY = -Math.sin(Math.toRadians(direction));
		double dirX = Math.cos(Math.toRadians(direction));
		if (!bClose) {
			// straight line
			for (int i = 1; i < path.size(); i++) {
				x += step * dirX;
				y += step * dirY;
				setVertexCoordinate(objToVtx.get(path.get(i)), x, y);
				fixed.add(path.get(i));
			}
		} else {
			ObjectVertex end = objToVtx.get(path.get(path.size() - 1));
			double diffX = end.getX() - start.getX();
			double diffY = end.getY() - start.getY();
			double distance = Math.sqrt(diffX * diffX + diffY * diffY);
			diffX /= distance;
			diffY /= distance;
			double outer = dirX * diffY - dirY * diffX;
			double centerX, centerY, vecX, vecY;
			double theta = Math.acos(diffX * dirX + diffY * dirY);
			double radius = distance / 2 / Math.sin(theta);
			if (outer > 0) {
				vecX = -dirY;
				vecY = dirX;				
				theta = theta * 2.0 / (double) (path.size() - 1);
			} else if (outer < 0) {
				vecX = dirY;
				vecY = -dirX;				
				theta = -theta * 2.0 / (double) (path.size() - 1);
			} else {
				// straight line
				diffX *= distance / (double) (path.size() - 1);
				diffY *= distance / (double) (path.size() - 1);
				for (int i = 1; i < path.size(); i++) {
					x += diffX;
					y += diffY;
					setVertexCoordinate(objToVtx.get(path.get(i)), x, y);
					fixed.add(path.get(i));
				}
				return;
			}
			// arc
			centerX = x + radius * vecX;
			centerY = y + radius * vecY;
			for (int i = 1; i < path.size() - 1; i++) {
				double tmp = vecX * Math.cos(theta) - vecY * Math.sin(theta);
				vecY = vecX * Math.sin(theta) + vecY * Math.cos(theta);
				vecX = tmp;
				setVertexCoordinate(objToVtx.get(path.get(i)), centerX - radius * vecX, centerY - radius * vecY);
				fixed.add(path.get(i));
			}
		}
	}

	private void setVertexCoordinate(ObjectVertex vertex, double x, double y) {
		vertex.setX(x);
		vertex.setY(y);
		vertex.setInitialPoint(x, y);
	}
}
