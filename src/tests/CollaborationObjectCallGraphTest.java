package tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.ntlab.deltaExtractor.ExtractedStructure;
import org.ntlab.deltaExtractor.IAliasCollector;
import org.ntlab.deltaViewer.CollaborationAliasCollector;
import org.ntlab.deltaViewer.CollaborationObjectCallGraph;
import org.ntlab.deltaViewer.DeltaAliasCollector;
import org.ntlab.trace.FieldUpdate;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.Reference;
import org.ntlab.trace.Statement;
import org.ntlab.trace.TracePoint;

class CollaborationObjectCallGraphTest {

	@Test
	void testMerge() {
		// Change Here!
//		String key = "getterOverlap";
//		String key = "setterOverlap";
//		String key = "ArgoUMLDelete";
		String key = "ArgoUMLSelect";
//		String key = "JHotDrawTransform";
//		String key = "JHotDrawSelect";
		MagnetRONFrameTest magnetRONFrame = new MagnetRONFrameTest();
		Map<ExtractedStructure, IAliasCollector> extractedMultipleDeltas = magnetRONFrame.extractMultipleDeltas(key);
		List<ExtractedStructure> eList = new ArrayList<>(extractedMultipleDeltas.keySet());
		List<IAliasCollector> dacList = new ArrayList<>(extractedMultipleDeltas.values());

		CollaborationAliasCollector cac = null;
		Map<MethodExecution, Set<MethodExecution>> newToOldMethodExecMap = new HashMap<>();
		for (IAliasCollector ac: dacList) {
			if (ac instanceof DeltaAliasCollector) {
				DeltaAliasCollector dac = (DeltaAliasCollector) ac;
				newToOldMethodExecMap.putAll(dac.shrink());
				if (cac == null) {
					cac = new CollaborationAliasCollector(dac);
				} else {
					cac.merge(dac, null);
				}
			}
		}

		CollaborationObjectCallGraph cocg = null;
		for (ExtractedStructure e: eList) {
			if (cocg == null) {
				cocg = new CollaborationObjectCallGraph(e);
			} else {
				cocg.merge(e);
			}
		}
		System.out.println("References:");
		for (Reference ref: cocg.getReferences()) {
			System.out.println("\t" + ref.getSrcClassName() + "(" + ref.getSrcObjectId() + ")" + " -> " + ref.getDstClassName() + "(" + ref.getDstObjectId() + "): " + ref.isCollection() + ", " + ref.isCreation());
		}
		System.out.println("StartPoints:");
		for (MethodExecution methodExec: cocg.getStartPoints()) {
			System.out.println("\t" + methodExec.getSignature());
		}
		System.out.println("RelatedPoints:");
		for (TracePoint tp: cocg.getRelatedPoints()) {
			Statement statement = tp.getStatement();
			if(statement instanceof FieldUpdate) {
				FieldUpdate fieldUpdateStatement = (FieldUpdate) statement;
				String fieldName = fieldUpdateStatement.getFieldName();
				String srcObjId = fieldUpdateStatement.getContainerObjId();
				String tgtObjId = fieldUpdateStatement.getValueObjId();
				System.out.println(tgtObjId + " <-- " + fieldName + " -- " + srcObjId);
			}

			if(statement instanceof MethodInvocation) {
				MethodInvocation methodInvStatement = (MethodInvocation) statement;
				MethodExecution calledMethodExec = methodInvStatement.getCalledMethodExecution();
				String methodSignature = calledMethodExec.getSignature();
				String srcClassName = null;
				String srcObjId = null;
				String tgtObjId = null;

				if (calledMethodExec.isCollectionType()
						&& (methodSignature.contains("add(") 
								|| methodSignature.contains("set(") 
								|| methodSignature.contains("put(") 
								|| methodSignature.contains("push(") 
								|| methodSignature.contains("addElement("))) {

					srcClassName = calledMethodExec.getThisClassName();
					srcObjId = calledMethodExec.getThisObjId();
					tgtObjId = calledMethodExec.getArguments().get(0).getId();
				} else {
					// this to another
					srcClassName = methodInvStatement.getThisClassName();
					srcObjId = methodInvStatement.getThisObjId();
					tgtObjId = calledMethodExec.getReturnValue().getId();
				}
				System.out.println(tgtObjId + " <-- " + srcClassName + " -- " + srcObjId);
			}
		}
		assertNotNull(cocg);
		for (ExtractedStructure e: eList) {
			// TODO Test StartPoint.
			assertTrue(cocg.getReferences().containsAll(e.getDelta().getSrcSide()));
			assertTrue(cocg.getReferences().containsAll(e.getDelta().getDstSide()));
			assertTrue(cocg.getRelatedPoints().contains(e.getRelatedTracePoint()));			
		}
		testShrinkAll(true, cocg, newToOldMethodExecMap); // Replaced References and RelatedPoints.
	}

	@Test
	private void testShrinkAll(boolean isShrink, CollaborationObjectCallGraph cocg, Map<MethodExecution, Set<MethodExecution>> newToOldMethodExecMap) {
		if (isShrink) {
			cocg.shrinkAll(newToOldMethodExecMap);
		}
	}
}
