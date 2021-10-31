package org.ntlab.deltaViewer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.ntlab.deltaExtractor.ExtractedStructure;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.ObjectReference;
import org.ntlab.trace.Reference;
import org.ntlab.trace.Statement;
import org.ntlab.trace.TracePoint;

/**
 * CollaborationObjectCallGraph is IObjectCallGraph implementation class to merge ExtractedStructure.
 * 
 * @author Nitta Lab.
 */
public class CollaborationObjectCallGraph implements IObjectCallGraph {
	private Set<Reference> references = new HashSet<>();
	private List<MethodExecution> startPoints = new ArrayList<>(); // Common ancestor point
	private List<TracePoint> relatedPoints = new ArrayList<>();
	private SortedSet<Long> timeStamps = new TreeSet<>();		// List of time stamps of related points

	public CollaborationObjectCallGraph(ExtractedStructure e) {
		references.addAll(e.getDelta().getSrcSide());
		references.addAll(e.getDelta().getDstSide());
		startPoints.add(e.getCoordinator());
		relatedPoints.add(e.getRelatedTracePoint().duplicate());
		timeStamps.add(getTimeStampOfRelatedPoint(e.getRelatedTracePoint(), e));
	}

	@Override
	public List<Reference> getReferences() {
		return new ArrayList<Reference>(references); // Convert to List from Set.
	}

	@Override
	public List<MethodExecution> getStartPoints() {
		return startPoints;
	}

	@Override
	public List<TracePoint> getRelatedPoints() {
		return relatedPoints;
	}

	@Override
	public Map<MethodExecution, List<MethodExecution>> getCallTree() {
		return null;
	}

	@Override
	public SortedSet<Long> getTimeStamps() {
		return timeStamps;
	}
	
	/**
	 * Merge ExtractedStructure not to overlap reference.
	 * 
	 * @param e: ExtractedStructure to be merged into the field.
	 */
	public void merge(ExtractedStructure e) {
//		references.addAll(e.getDelta().getSrcSide());
//		references.addAll(e.getDelta().getDstSide());
		// Prefer isCreatioin() = true references, if references are equal, but isCreation() is different.
		for (Reference srcSideRef: e.getDelta().getSrcSide()) {
			if (references.contains(srcSideRef)) {
				List<Reference> refs = getReferences(); // Convert to List from Set.
				int idx = refs.indexOf(srcSideRef);
				Reference ref = refs.get(idx);
				if (srcSideRef.isCreation() != ref.isCreation()) {
					if (srcSideRef.isCreation()) {
						references.remove(ref);
						references.add(srcSideRef);
					}
				}
				
			} else {
				references.add(srcSideRef);
			}
		}
		for (Reference dstSideRef: e.getDelta().getDstSide()) {
			if (references.contains(dstSideRef)) {
				List<Reference> refs = getReferences(); // Convert to List from Set.
				int idx = refs.indexOf(dstSideRef);
				Reference ref = refs.get(idx);
				if (dstSideRef.isCreation() != ref.isCreation()) {
					if (dstSideRef.isCreation()) {
						references.remove(ref);
						references.add(dstSideRef);
					}
				}
				
			} else {
				references.add(dstSideRef);
			}
		}

		// There may be bug. (Two object has each coordinator like JHotDraw Transform)
		MethodExecution thisStartPoint = startPoints.get(0);
		MethodExecution tmp = thisStartPoint;
		MethodExecution thisRoot = thisStartPoint.getParent();
		MethodExecution otherStartPoint = e.getCoordinator();
		while(thisRoot != null) { // Get Root of thisStartPoint.
			tmp = tmp.getParent();
			thisRoot = tmp.getParent();
		}
		thisRoot = tmp;
		/* lowest common ancestor algorithm */
		MethodExecution lca = lowestCommonAncestor(thisRoot, thisStartPoint, otherStartPoint);
		if (lca == null) {
			if (thisStartPoint.getEntryTime() < otherStartPoint.getEntryTime()) {
				lca = thisStartPoint;
			} else {
				lca = otherStartPoint;
			}
		}
		startPoints.clear();
		startPoints.add(lca);

		TracePoint otherRelatedTp = e.getRelatedTracePoint().duplicate();
		long rpTimeStamp = getTimeStampOfRelatedPoint(otherRelatedTp, e);
		int idx = timeStamps.headSet(rpTimeStamp).size();
		relatedPoints.add(idx, otherRelatedTp);
		timeStamps.add(rpTimeStamp);
//		relatedPoints = sortTracePointByTimeStamp(relatedPoints);
	}

	/**
	 * Search lowest common ancestor(LCA) of p and q.
	 * 
	 * @param root
	 * @param p
	 * @param q
	 * @return LCA MethodExecution
	 */
	public MethodExecution lowestCommonAncestor(MethodExecution root, MethodExecution p, MethodExecution q) {
		if(root == null || root == p || root == q)  return root;
		Set<MethodExecution> pRoots = new HashSet<>();
		MethodExecution pTmp = p;
		while (!root.equals(pTmp) && pTmp != null) {
			pRoots.add(pTmp);
			pTmp = pTmp.getParent();
		}
		pRoots.add(root);
		MethodExecution qTmp = q;
		while (!root.equals(qTmp) && qTmp != null) {
			if (pRoots.contains(qTmp)) return qTmp;
			qTmp = qTmp.getParent();
		}
		return qTmp;
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

	/**
	 * Sort TracePoint in time stamp order.
	 * 
	 * @param tpList: TracePoint list to sort.
	 * @return sorted TracePoint list
	 */
	private List<TracePoint> sortTracePointByTimeStamp(List<TracePoint> tpList) {
		List<TracePoint> cloneTpList = new ArrayList<>(tpList);
		List<TracePoint> sortedTpList = cloneTpList.stream().sorted(new Comparator<TracePoint>() {
			@Override
			public int compare(TracePoint tp1, TracePoint tp2) {
				long tp1TimeStamp = tp1.getStatement().getTimeStamp();
				long tp2TimeStamp = tp2.getStatement().getTimeStamp();
				if (tp1TimeStamp > tp2TimeStamp) return 1;
				else if (tp1TimeStamp < tp2TimeStamp) return -1;
				return 0;
			}
		}).collect(Collectors.toList());
		return sortedTpList;
	}
	
	public void shrinkAll(Map<MethodExecution, Set<MethodExecution>> newToOldMethodExecutionMap) {
		List<Reference> refs = getReferences();
		List<Reference> collectionReferences = collectCollectionReferences(refs);
		List<List<Reference>> collectionChains = collectCollectionChains(collectionReferences, newToOldMethodExecutionMap);
		refs = replaceCollectionChains(refs, collectionChains);
		refs = replaceStaticObjectIds(refs);
		references = new HashSet<>(refs); // Convert to Set from List.
		relatedPoints = replaceRelatedPoints(relatedPoints, newToOldMethodExecutionMap);
		
		// For debug.
		System.out.println("collectionReferences: ");
		for (Reference ref: collectionReferences) {
			System.out.println("\t" + ref.getSrcClassName() + "(" + ref.getSrcObjectId() + ")" + " -> " + ref.getDstClassName() + "(" + ref.getDstObjectId() + "): " + ref.isCollection());
		}
		System.out.println("collectionChains: ");
		for (int i = 0; i < collectionChains.size(); i++) {
			List<Reference> CollectionChain = collectionChains.get(i);
			System.out.println("i = " + i);
			for (Reference ref: CollectionChain) {
				System.out.println("\t" + ref.getSrcClassName() + "(" + ref.getSrcObjectId() + ")" + " -> " + ref.getDstClassName() + "(" + ref.getDstObjectId() + "): " + ref.isCollection());
			}
		}
		System.out.println("replaceCollectionChains: ");
		for (Reference ref: references) {
			System.out.println("\t" + ref.getSrcClassName() + "(" + ref.getSrcObjectId() + ")" + " -> " + ref.getDstClassName() + "(" + ref.getDstObjectId() + "): " + ref.isCollection() + ", " + ref.isCreation());
		}	
	}
	
	private List<Reference> collectCollectionReferences(List<Reference> references) {
		// Collect references that are Collection.
		List<Reference> collectionRefs = new ArrayList<>();
		for (Reference ref: references) {
			if (ref.isCollection()) {
				collectionRefs.add(ref);
			}
		}
		return collectionRefs;
	}
	
	private List<List<Reference>> collectCollectionChains(List<Reference> collectionReferences, Map<MethodExecution, Set<MethodExecution>> newToOldMethodExecutionMap) {
		// Collect follow references.
		List<Reference> collectionRefs = new ArrayList<>(collectionReferences); // Create new instance of coping collectionReference.
		List<List<Reference>> collectionChains = new ArrayList<>();
		// Search first reference.
		int i = 0;
		while (i < collectionRefs.size()) {
			Reference ref = collectionRefs.get(i);
			String srcClassName = ref.getSrcClassName();
			String srcObjId = ref.getSrcObjectId();
			boolean isShrinked = false;
			for (MethodExecution me: newToOldMethodExecutionMap.keySet()) {
				if (srcObjId.equals(me.getThisObjId())) {
					isShrinked = true;
					break;
				}
			}
			boolean isFirstRef = true;
			if (isShrinked) {
				// If dstClassName matches "$[1-9]" and don't shrink References too much(JHotDrawTransform).
				if (!ref.getDstClassName().matches(".*\\$[0-9]+.*")) {
					for (int j = 0; j < collectionReferences.size(); j++) {
						if (collectionReferences.indexOf(ref) != j) {
							Reference compareRef = collectionReferences.get(j);
							if (srcClassName.equals(compareRef.getDstClassName())
									&& srcObjId.equals(compareRef.getDstObjectId())) {
								isFirstRef = false;
								break;
							}
						}
					}				
				}
			}
			if (isShrinked && isFirstRef) {
				List<Reference> collectionChain = new ArrayList<>();
				collectionChain.add(ref);
				collectionChains.add(collectionChain);
				collectionRefs.remove(i);
			} else {
				i++;
			}
		}
		
		// Search references follow first reference.
		for (i = 0; i < collectionChains.size(); i++) {
			List<Reference> collectionChain = collectionChains.get(i);
			int j = 0;
			while (j < collectionChain.size()) {
				Reference ref = collectionChain.get(j);
				String dstClassName = ref.getDstClassName();
				String dstObjId = ref.getDstObjectId();
				j++;
				for (int k = 0; k < collectionRefs.size(); k++) {
					Reference compareRef = collectionRefs.get(k);
					if (dstClassName.equals(compareRef.getSrcClassName())
							&& dstObjId.equals(compareRef.getSrcObjectId())) {
						collectionChain.add(compareRef);
						collectionRefs.remove(k);
						break;
					}
				}
			}
			if (collectionChain.size() == 1) {
				collectionChains.remove(i);
				i--;
			}
		}
		return collectionChains;
	}
	
	private List<Reference> replaceCollectionChains(List<Reference> references, List<List<Reference>> collectionChains) {
		// Replace to shrink Reference in references.
		List<Reference> replacedReferences = new ArrayList<>(references);
		for (List<Reference> collectionChain: collectionChains) {
			// Create shrink new reference.
			Reference firstRef = collectionChain.get(0);
			Reference lastRef = collectionChain.get(collectionChain.size() - 1);
			Reference newRef = new Reference(firstRef.getSrcObjectId(), lastRef.getDstObjectId(), firstRef.getSrcClassName(), lastRef.getDstClassName());
			newRef.setCollection(true);

			// Remove collectionChains from references.
			for (int i = 0; i < collectionChain.size(); i++) {
				Reference ref = collectionChain.get(i);
				int refIdx = replacedReferences.indexOf(ref); // Get index of collection reference in references.
				if (refIdx != - 1) replacedReferences.remove(refIdx);
				else System.out.println("Failed to remove collection reference in references...");
			}
			replacedReferences.add(newRef); // Add new reference.
		}
		return replacedReferences;
	}
	
	/**
	 * Replace objectId of {@code ObjectReference} of static object in references.
	 * 
	 * @param references
	 * @return
	 */
	private List<Reference> replaceStaticObjectIds(List<Reference> references) {
		List<Reference> replacedReferences = new ArrayList<>(references);
		for (Reference ref: replacedReferences) {
			if (ref.getSrcObjectId().matches("0")) {
				ref.setSrcObjectId(ref.getSrcObjectId() + ":" + ref.getSrcClassName());
			}
			if (ref.getDstObjectId().matches("0")) {
				ref.setDstObjectId(ref.getDstObjectId() + ":" + ref.getDstClassName());				
			}
		}
		return replacedReferences;
	}

	/**
	 * Replace called MethodExecution in relatedPoints to new MethodExecution.
	 * 
	 * @param relatedPoints
	 * @param newToOldMethodExecutionMap
	 * @return Replaced related points.
	 */
	private List<TracePoint> replaceRelatedPoints(List<TracePoint> relatedPoints, Map<MethodExecution, Set<MethodExecution>> newToOldMethodExecutionMap) {
		List<TracePoint> replacedRp = new ArrayList<>(relatedPoints);
		for (int i = 0; i < replacedRp.size(); i++) {
			TracePoint rp = replacedRp.get(i);
			Statement st = rp.getStatement();
			if (st instanceof MethodInvocation) {
				MethodInvocation methodInv = (MethodInvocation)st;
				MethodExecution calledMethodExec = methodInv.getCalledMethodExecution();
				for (Entry<MethodExecution, Set<MethodExecution>> entry: newToOldMethodExecutionMap.entrySet()) {
					MethodExecution newMethodExec = entry.getKey();
					Set<MethodExecution> oldMethodExecSet = entry.getValue();
					if (oldMethodExecSet.contains(calledMethodExec)) {
						methodInv.setCalledMethodExecution(newMethodExec);
					}
				}				
			}
			for (Entry<MethodExecution, Set<MethodExecution>> entry: newToOldMethodExecutionMap.entrySet()) {
				MethodExecution newMethodExec = entry.getKey();
				Set<MethodExecution> oldMethodExecSet = entry.getValue();
				if (oldMethodExecSet.contains(rp.getMethodExecution())) {
					DummyTracePoint newTp = new DummyTracePoint(rp, newMethodExec);
					replacedRp.set(i, newTp);
				}
			}
		}
		return replacedRp;
	}

}
