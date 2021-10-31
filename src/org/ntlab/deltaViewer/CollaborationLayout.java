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
import org.ntlab.trace.TracePoint;

import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxPoint;

public class CollaborationLayout implements IObjectLayout {
	private static final int angleStep = 30;
	private mxPoint coordinatorPoint = new mxPoint(0, 100);
	private double step;
	private double padding;		

	@Override
	public void execute(IObjectCallGraph objectCallGraph, IAliasCollector aliasCollector, Map<String, ObjectVertex> objectToVertexMap) {
		step = 150;
		padding = 200;
		
		// Extract the bottom reference.
		TracePoint relatedPt = objectCallGraph.getRelatedPoints().get(objectCallGraph.getRelatedPoints().size() - 1);
		Statement relatedSt = relatedPt.getStatement();
		String bottomSrcObjId = null;
		String bottomDstObjId = null;
		if (relatedPt.isMethodEntry()) {
			// this to another (parameter)
			Alias lastAlias = aliasCollector.getAliasList().get(aliasCollector.getAliasList().size() - 1);
			if (lastAlias.getAliasType() == Alias.AliasType.FORMAL_PARAMETER) {
				bottomSrcObjId = relatedPt.getMethodExecution().getThisObjId();
				bottomDstObjId = lastAlias.getObjectId();
			}
		}
		if (bottomSrcObjId == null || bottomDstObjId == null) {
			if (relatedSt instanceof FieldUpdate) {
				// container to component
				bottomSrcObjId = ((FieldUpdate) relatedSt).getContainerObjId();
				bottomDstObjId = ((FieldUpdate) relatedSt).getValueObjId();
			} else if (relatedSt instanceof ArrayUpdate) {
				// container to component
				bottomSrcObjId = ((ArrayUpdate) relatedSt).getArrayObjectId();
				bottomDstObjId = ((ArrayUpdate) relatedSt).getValueObjectId();
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
					bottomSrcObjId = calledMethodExec.getThisObjId();
					bottomDstObjId = calledMethodExec.getArguments().get(0).getId();
				} else {
					// this to another
					bottomSrcObjId = methodInvStatement.getThisObjId();
					bottomDstObjId = calledMethodExec.getReturnValue().getId();
				}
			} else {
				return;
			}
		}
		
		// Extract the reference access history.
		List<Reference> references = new ArrayList<>(objectCallGraph.getReferences());
		Map<Reference, List<Integer>> referenceHistory = new HashMap<>();
		int order = 0;
		for (Alias a: aliasCollector.getAliasList()) {
			int idx = -1;
			if (a.getAliasType() == AliasType.FIELD) {
				Reference ref = null;
				if (a.getOccurrencePoint().getStatement() instanceof FieldAccess) {
					FieldAccess f = (FieldAccess) a.getOccurrencePoint().getStatement();
					ref = new Reference(f.getContainerObjId(), f.getValueObjId(), f.getContainerClassName(), f.getValueClassName());
				} else if (a.getOccurrencePoint().getStatement() instanceof MethodInvocation) {
					// A call to an enclosing instance.
					MethodExecution m = a.getMethodExecution();
					MethodInvocation i = (MethodInvocation) a.getOccurrencePoint().getStatement() ;
					ref = new Reference(m.getThisObjId(), a.getObjectId(), m.getThisClassName(), i.getCalledMethodExecution().getArguments().get(0).getActualType());
				}
				idx = references.indexOf(ref);
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
					Reference r = new Reference(srcObjId, dstObjId, srcClassName, dstClassName);
					idx = references.indexOf(r);
					if (idx < 0) {
						references.add(r);
						idx = references.indexOf(r);
					}
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
			} else if (a.getAliasType() == AliasType.THIS) {
				Statement st = a.getOccurrencePoint().getStatement();
				if (st instanceof MethodInvocation) {
					MethodExecution methodExec = ((MethodInvocation) st).getCalledMethodExecution();
					if (methodExec.isStatic()) {
						// For calls to a static method.
						String dstClassName = methodExec.getThisClassName();
						String srcClassName = methodExec.getCallerMethodExecution().getThisClassName();
						String dstObjId = methodExec.getThisObjId() + ":" + dstClassName;
						String srcObjId = null;
						if (!a.getMethodExecution().isStatic()) {
							srcObjId = methodExec.getCallerMethodExecution().getThisObjId();
						} else {
							srcObjId = methodExec.getCallerMethodExecution().getThisObjId() + ":" + srcClassName;							
						}
						Reference r = new Reference(srcObjId, dstObjId, srcClassName, dstClassName);
						idx = references.indexOf(r);
						if (idx < 0) {
							references.add(r);
							idx = references.indexOf(r);
						}					
					}
				}
			} else if (a.getAliasType() == AliasType.FORMAL_PARAMETER) {
				MethodExecution methodExec = a.getOccurrencePoint().getMethodExecution();
				if (methodExec.isStatic()) {
					// For calls to a static method.
					String dstClassName = methodExec.getThisClassName();
					String srcClassName = methodExec.getCallerMethodExecution().getThisClassName();
					String dstObjId = methodExec.getThisObjId() + ":" + dstClassName;
					String srcObjId = null;
					if (!methodExec.getCallerMethodExecution().isStatic()) {
						srcObjId = methodExec.getCallerMethodExecution().getThisObjId();
					} else {
						srcObjId = methodExec.getCallerMethodExecution().getThisObjId() + ":" + srcClassName;							
					}
					Reference r = new Reference(srcObjId, dstObjId, srcClassName, dstClassName);
					idx = references.indexOf(r);
					if (idx < 0) {
						references.add(r);
						idx = references.indexOf(r);
					}					
				}
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
		Map<String, List<Reference>> preds = new HashMap<>();
		Map<String, List<Reference>> succs = new HashMap<>();
		for (Reference r: references) {
			String dstObjId = r.getDstObjectId();
			List<Reference> pre = preds.get(dstObjId);
			if (pre == null) {
				pre = new ArrayList<Reference>();
				preds.put(dstObjId, pre);
			}
			pre.add(r);
			String srcObjId = r.getSrcObjectId();
			List<Reference> suc = succs.get(srcObjId);
			if (suc == null) {
				suc = new ArrayList<Reference>();
				succs.put(srcObjId, suc);
			}
			suc.add(r);
		}
		
		// Extract the source side and destination side paths.
		String topObjId = objectCallGraph.getStartPoints().get(0).getThisObjId();
		List<Reference> dstSide = new ArrayList<>();
		List<String> dstSideObjects = new ArrayList<>();
		String dstObjId = bottomDstObjId;
		dstSideObjects.add(dstObjId);
		while (!dstObjId.equals(topObjId)) {
			if (preds.get(dstObjId) != null) {
				Reference minRef = getFirstReferece(preds.get(dstObjId), referenceHistory, true);
				dstSide.add(minRef);
				dstObjId = minRef.getSrcObjectId();
			} else {
				dstSide.add(null);
				dstObjId = topObjId;
			}
			dstSideObjects.add(dstObjId);
		}
		
		List<Reference> srcSide = new ArrayList<>();
		List<String> srcSideObjects = new ArrayList<>();
		String srcObjId = bottomSrcObjId;
		srcSideObjects.add(srcObjId);
		while (!srcObjId.equals(topObjId)) {
			if (preds.get(srcObjId) != null) {
				Reference maxRef = getFirstReferece(preds.get(srcObjId), referenceHistory, false);
				srcSide.add(maxRef);
				srcObjId = maxRef.getSrcObjectId();
			} else {
				srcSide.add(null);
				srcObjId = topObjId;
			}
			srcSideObjects.add(srcObjId);
		}
		
		// Fix the confluent points.
		List<Map.Entry<String, Integer>> confluPoints = new ArrayList<>();
		boolean bConfluent = false;
		int hight = 0;
		int width = -1;
		int dstHight = 0;
		int srcHight = 0;
		for (String srcObj: srcSideObjects) {
			if (dstSideObjects.contains(srcObj)) {
				if (!bConfluent) {
					int dstDiff = dstSideObjects.indexOf(srcObj) - dstHight;
					int srcDiff = srcSideObjects.indexOf(srcObj) - srcHight;
					dstHight += dstDiff;
					srcHight += srcDiff;
					if (dstDiff > srcDiff) {
						hight += dstDiff;
					} else {
						hight += srcDiff;
					}
					if (width < 0) width = dstHight;
					confluPoints.add(new AbstractMap.SimpleEntry(srcObj, hight));
					bConfluent = true;
				} else {
					hight++;
				}
			} else {
				bConfluent = false;				
			}
		}
		coordinatorPoint.setX(coordinatorPoint.getX() + step * width + padding);
		double xCor = coordinatorPoint.getX();
		double yCor = coordinatorPoint.getY();
		Set<String> fixed = new HashSet<>();
		for (Map.Entry<String, Integer> confluPoint: confluPoints) {
			setVertexCoordinate(objectToVertexMap.get(confluPoint.getKey()), xCor, yCor + (hight - confluPoint.getValue()) * step);
			fixed.add(confluPoint.getKey());
		}
		
		// Fix the bottom objects.
		Map.Entry<String, Integer> firstConfl = confluPoints.get(0);
		dstHight = hight - firstConfl.getValue() + dstSideObjects.indexOf(firstConfl.getKey());
		srcHight = hight - firstConfl.getValue() + srcSideObjects.indexOf(firstConfl.getKey());
		int width2 = srcSideObjects.indexOf(firstConfl.getKey());
		setVertexCoordinate(objectToVertexMap.get(bottomDstObjId), padding, yCor + dstHight * step);
		fixed.add(bottomDstObjId);
		setVertexCoordinate(objectToVertexMap.get(bottomSrcObjId), xCor + width2 * step, yCor + srcHight * step);
		fixed.add(bottomSrcObjId);
		
		// Fix the destination side, source side and common path.
		int srcIndex = 0;
		int dstIndex = 0;
		double dstDirection = 60;
		double srcDirection = 120;
		for (Map.Entry<String, Integer> confluPoint: confluPoints) {
			String conflObj = confluPoint.getKey();

			// Fix the destination side path.
			List<String> path = dstSideObjects.subList(dstIndex, dstSideObjects.indexOf(conflObj) + 1);
			if (path.size() > 2) fixPath(path, dstDirection, objectToVertexMap, fixed, true);
			dstDirection = 120;
			dstIndex = dstSideObjects.indexOf(conflObj);
					
			// Fix the source side path.
			path = srcSideObjects.subList(srcIndex, srcSideObjects.indexOf(conflObj) + 1);
			if (path.size() > 2) fixPath(path, srcDirection, objectToVertexMap, fixed, true);
			srcDirection = 60;
			srcIndex = srcSideObjects.indexOf(conflObj);
			
			// Fix the common path.
			int commonHight = confluPoint.getValue();
			for (;;) {
				commonHight++;
				srcIndex++;
				dstIndex++;
				if (srcIndex >= srcSideObjects.size() || dstIndex >= dstSideObjects.size()) break;
				if (!srcSideObjects.get(srcIndex).equals(dstSideObjects.get(dstIndex))) break;
				String commonObjId = srcSideObjects.get(srcIndex);
				setVertexCoordinate(objectToVertexMap.get(commonObjId), xCor, yCor + (hight - commonHight) * step);				
				fixed.add(commonObjId);
			}
			srcIndex--;
			dstIndex--;
		}
		
		// Fix the branches from the destination side path.
		for (Reference ref: dstSide) {
			if (ref != null) {
				List<Reference> nextRefs = new ArrayList<>(preds.get(ref.getDstObjectId()));
				nextRefs = new ArrayList<>(nextRefs);
				nextRefs.remove(ref);
				double direction = getDirection(ref, objectToVertexMap) - angleStep;
				while (nextRefs.size() > 0) {
					Reference firstRef = getFirstReferece(nextRefs, referenceHistory, true);
					List<String> path = new ArrayList<>();
					path.add(firstRef.getDstObjectId());
					traversePreds(preds, firstRef, direction, true, referenceHistory, objectToVertexMap, fixed, path);
					nextRefs.remove(firstRef);
					direction = getDirection(firstRef, objectToVertexMap) - angleStep;
				}
			}
		}
		
		// Fix the branches from the source side path.
		for (Reference ref: srcSide) {
			if (ref != null) {
				List<Reference> nextRefs = new ArrayList<>(preds.get(ref.getDstObjectId()));
				nextRefs = new ArrayList<>(nextRefs);
				nextRefs.remove(ref);
				double direction = getDirection(ref, objectToVertexMap) + angleStep;
				while (nextRefs.size() > 0) {
					Reference firstRef = getFirstReferece(nextRefs, referenceHistory, false);
					List<String> path = new ArrayList<>();
					path.add(firstRef.getDstObjectId());
					traversePreds(preds, firstRef, direction, false, referenceHistory, objectToVertexMap, fixed, path);
					nextRefs.remove(firstRef);
					direction = getDirection(firstRef, objectToVertexMap) + angleStep;
				}
			}
		}
		
		// Fix the reverse branches from the destination side path.
		Collections.reverse(dstSide);
		for (Reference ref: dstSide) {
			if (ref != null) {
				List<Reference> nextRefs = new ArrayList<>(succs.get(ref.getSrcObjectId()));
				if (nextRefs.size() > 1) {
					nextRefs = new ArrayList<>(nextRefs);
					List<Reference> nextRefs2 = new ArrayList<>();
					do {
						Reference firstRef = getFirstReferece(nextRefs, referenceHistory, true);
						nextRefs2.add(firstRef);
						nextRefs.remove(firstRef);
					} while (nextRefs.size() > 1);
					nextRefs2.add(nextRefs.remove(0));
					
					if (nextRefs2.indexOf(ref) > 0) {
						nextRefs = nextRefs2.subList(0, nextRefs2.indexOf(ref));
						double direction = getDirection(ref, objectToVertexMap) + 180 - angleStep;
						Collections.reverse(nextRefs);
						for (Reference r: nextRefs) {
							List<String> path = new ArrayList<>();
							path.add(r.getSrcObjectId());
							traverseSuccs(succs, r, direction, true, referenceHistory, objectToVertexMap, fixed, path);
							direction = getDirection(r, objectToVertexMap) + 180 - angleStep;
						}
					}
					
					if (nextRefs2.indexOf(ref) < nextRefs2.size() - 1) {
						nextRefs = nextRefs2.subList(nextRefs2.indexOf(ref) + 1, nextRefs2.size());
						double direction = getDirection(ref, objectToVertexMap) + 180 + angleStep;
						for (Reference r: nextRefs) {
							List<String> path = new ArrayList<>();
							path.add(r.getSrcObjectId());
							traverseSuccs(succs, r, direction, true, referenceHistory, objectToVertexMap, fixed, path);
							direction = getDirection(r, objectToVertexMap) + 180 + angleStep;
						}
					}
				}
//				nextRefs = new ArrayList<>(nextRefs);
//				nextRefs.remove(ref);
//				double direction = getDirection(ref, objectToVertexMap) + 180 + angleStep;
//				while (nextRefs.size() > 0) {
//					Reference firstRef = getFirstReferece(nextRefs, referenceHistory, true);
//					List<String> path = new ArrayList<>();
//					path.add(firstRef.getSrcObjectId());
//					traverseSuccs(succs, firstRef, direction, true, referenceHistory, objectToVertexMap, fixed, path);
//					nextRefs.remove(firstRef);
//					direction = getDirection(firstRef, objectToVertexMap) + 180 + angleStep;
//				}
			}
		}
		
		// Fix the reverse branches from the source side path.
		Collections.reverse(srcSide);
		for (Reference ref: srcSide) {
			if (ref != null) {
				List<Reference> nextRefs = new ArrayList<>(succs.get(ref.getSrcObjectId()));
				if (nextRefs.size() > 1) {
					nextRefs = new ArrayList<>(nextRefs);
					List<Reference> nextRefs2 = new ArrayList<>();
					do {
						Reference firstRef = getFirstReferece(nextRefs, referenceHistory, false);
						nextRefs2.add(firstRef);
						nextRefs.remove(firstRef);
					} while (nextRefs.size() > 1);
					nextRefs2.add(nextRefs.remove(0));
					
					if (nextRefs2.indexOf(ref) > 0) {
						nextRefs = nextRefs2.subList(0, nextRefs2.indexOf(ref));
						double direction = getDirection(ref, objectToVertexMap) + 180 + angleStep;
						Collections.reverse(nextRefs);
						for (Reference r: nextRefs) {
							List<String> path = new ArrayList<>();
							path.add(r.getSrcObjectId());
							traverseSuccs(succs, r, direction, false, referenceHistory, objectToVertexMap, fixed, path);
							direction = getDirection(r, objectToVertexMap) + 180 + angleStep;
						}
					}
					
					if (nextRefs2.indexOf(ref) < nextRefs2.size() - 1) {
						nextRefs = nextRefs2.subList(nextRefs2.indexOf(ref) + 1, nextRefs2.size());
						double direction = getDirection(ref, objectToVertexMap) + 180 - angleStep;
						for (Reference r: nextRefs) {
							List<String> path = new ArrayList<>();
							path.add(r.getSrcObjectId());
							traverseSuccs(succs, r, direction, false, referenceHistory, objectToVertexMap, fixed, path);
							direction = getDirection(r, objectToVertexMap) + 180 - angleStep;
						}
					}
				}
//				List<Reference> nextRefs = new ArrayList<>(succs.get(ref.getSrcObjectId()));
//				nextRefs = new ArrayList<>(nextRefs);
//				nextRefs.remove(ref);
//				double direction = getDirection(ref, objectToVertexMap) + 180 - angleStep;
//				while (nextRefs.size() > 0) {
//					Reference firstRef = getFirstReferece(nextRefs, referenceHistory, false);
//					List<String> path = new ArrayList<>();
//					path.add(firstRef.getSrcObjectId());
//					traverseSuccs(succs, firstRef, direction, false, referenceHistory, objectToVertexMap, fixed, path);
//					nextRefs.remove(firstRef);
//					direction = getDirection(firstRef, objectToVertexMap) + 180 - angleStep;
//				}
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

	private void traversePreds(Map<String, List<Reference>> preds, Reference ref, double direction, boolean bLeftFirst, Map<Reference, List<Integer>> referenceHistory, Map<String, ObjectVertex> objToVtx, Set<String> fixed, List<String> path) {
		String curObj = ref.getSrcObjectId();
		path.add(curObj);
		if (fixed.contains(curObj)) {
			if (path.size() > 2) fixPath(path, direction, objToVtx, fixed, true);
			return;
		}
		List<Reference> nextRefs = preds.get(curObj);
		if (nextRefs == null || nextRefs.size() == 0) {
			if (path.size() >= 2) fixPath(path, direction, objToVtx, fixed, false);
			return;
		}
		nextRefs = new ArrayList<>(nextRefs);
		do {
			Reference firstRef = getFirstReferece(nextRefs, referenceHistory, bLeftFirst);
			traversePreds(preds, firstRef, direction, bLeftFirst, referenceHistory, objToVtx, fixed, path);
			path = new ArrayList<>();
			path.add(curObj);
			nextRefs.remove(firstRef);
			direction = getDirection(firstRef, objToVtx);
			if (bLeftFirst) {
				direction -= angleStep;
			} else {
				direction += angleStep;
			}
		} while (nextRefs.size() > 0);
		return;
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
