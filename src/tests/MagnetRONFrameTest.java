package tests;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.ntlab.deltaExtractor.Alias;
import org.ntlab.deltaExtractor.DeltaExtractorJSON;
import org.ntlab.deltaExtractor.ExtractedStructure;
import org.ntlab.deltaExtractor.IAliasCollector;
import org.ntlab.deltaExtractor.IAliasTracker;
import org.ntlab.deltaViewer.CollaborationAliasCollector;
import org.ntlab.deltaViewer.CollaborationLayout;
import org.ntlab.deltaViewer.CollaborationObjectCallGraph;
import org.ntlab.deltaViewer.CollaborationViewer;
import org.ntlab.deltaViewer.DeltaAliasCollector;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.ObjectReference;
import org.ntlab.trace.Reference;
import org.ntlab.trace.ThreadInstance;
import org.ntlab.trace.TraceJSON;
import org.ntlab.trace.TracePoint;

/**
 * 
 * 
 * @author Nitta Lab.
 */
class MagnetRONFrameTest extends JFrame {
	
	private static final long serialVersionUID = -230702457071082574L;

	// Delta Extract Type
	protected static final String CONTAINER_COMPONENT = "Container-Component";
	protected static final String CONTAINER_COMPONENT_COLLECTION = "Container-Component(Collection)";
	protected static final String THIS_ANOTHER = "This-Another";

	private static Dimension DEFAULT_SIZE = new Dimension(1300, 700);
	private static String WINDOW_TITLE = "Delta Viewer";

	private CollaborationViewer viewer = null;
	private boolean visible = false;

	public MagnetRONFrameTest() {
	}
	
	public MagnetRONFrameTest(boolean visible) throws HeadlessException {
			super(WINDOW_TITLE);
			setSize(DEFAULT_SIZE);
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setLayout(new BorderLayout());
			this.viewer = new CollaborationViewer();
			getContentPane().add(viewer, BorderLayout.CENTER);
			pack();
			this.visible = true;
	}

	public void startAnimation() {
		if (visible) {
			// Change Here!
//			String key = "ArgoUMLDelete";
			String key = "ArgoUMLSelect";
//			String key = "getterOverlap";
//			String key = "setterOverlap";
			Map<ExtractedStructure, IAliasCollector> extractedMultipleDeltas = extractMultipleDeltas(key);
			List<ExtractedStructure> eList = new ArrayList<>(extractedMultipleDeltas.keySet());
			List<IAliasCollector> dacList = new ArrayList<>(extractedMultipleDeltas.values());
			CollaborationObjectCallGraph cocg = null;
			CollaborationAliasCollector cac = null;
			for (ExtractedStructure e: eList) {
				if (cocg == null) {
					cocg = new CollaborationObjectCallGraph(e);
				} else {
					cocg.merge(e);
				}
			}
			for (IAliasCollector dac: dacList) {
				if (cac == null) {
					cac = new CollaborationAliasCollector(dac);
				} else {
					cac.merge(dac, null);
				}
			}
//			new Thread() {
//				public void run() {
					startCollectionViewer(cocg, cac);						
//				}
//			}.start();
		}
	}

	public Map<ExtractedStructure, IAliasCollector> extractMultipleDeltas(String key) {
		Map<ExtractedStructure, IAliasCollector> extractedMultipleDeltas = new HashMap<>();
		Map<String, String[]> argsMap = new HashMap<>();
		setArgmentsForDeltaExtract(argsMap);
		Map<String, List<String>> argsKeysMap = new HashMap<>();
		setArgsKeysForMultipleDeltasExtract(argsKeysMap, argsMap);
		List<String> argsKeys = argsKeysMap.get(key);
		String firstKey = argsKeys.get(0);
		if (firstKey.contains(",")) {
			String[] splitKeys = firstKey.split(",");
			firstKey = splitKeys[0];
		}
		TraceJSON trace = new TraceJSON(argsMap.get(firstKey)[4]);
		DeltaExtractorJSON s = new DeltaExtractorJSON(trace);

		for (String argsKey: argsKeys) {
			int index = 0;
			if (argsKey.contains(",")) {
				String[] splitKeys = argsKey.split(",");
				argsKey = splitKeys[0];
				index = Integer.parseInt(splitKeys[splitKeys.length - 1]);
			}
			List<ExtractedStructure> eList = new ArrayList<>();
			List<IAliasCollector> dacList = new ArrayList<>();
			if (argsMap.get(argsKey)[5] == CONTAINER_COMPONENT || argsMap.get(argsKey)[5] == CONTAINER_COMPONENT_COLLECTION) {
				HashMap<String, ThreadInstance> threads = trace.getAllThreads();

				if (threads.values().size() == 1) {
					ThreadInstance thread = threads.values().iterator().next(); // 最後のスレッドを見ているだけ…
					TracePoint tp = thread.getRoot().get(thread.getRoot().size() - 1).getExitPoint();
					Reference reference = new Reference(argsMap.get(argsKey)[0], argsMap.get(argsKey)[1], argsMap.get(argsKey)[2], argsMap.get(argsKey)[3]);
					IAliasTracker dac = new DeltaAliasCollector();
					ExtractedStructure e = s.extract(reference, tp, dac);
					eList.add(e);
					dacList.add(dac);
				} else {
					for (ThreadInstance thread: threads.values()) {
						TracePoint tp = thread.getRoot().get(thread.getRoot().size() - 1).getExitPoint();
						Reference reference = new Reference(argsMap.get(argsKey)[0], argsMap.get(argsKey)[1], argsMap.get(argsKey)[2], argsMap.get(argsKey)[3]);
						if (argsMap.get(argsKey)[5] == CONTAINER_COMPONENT_COLLECTION) {
							reference.setCollection(true);						
						}
//						reference.setArray(true);
//						reference.setFinalLocal(true);
//						reference.setCreation(true);
						ExtractedStructure e = null;
						do {
							if (e == null) {
								IAliasTracker dac = new DeltaAliasCollector();
								e = s.extract(reference, tp, dac);
								System.out.println(e);
								if (e != null) {
									eList.add(e);
									dacList.add(dac);
									System.out.println("add" + eList.size() + ", " + dacList.size());
									System.out.println(e);
								}
								System.out.println("---------------------------");
							} else {
								tp = e.getRelatedTracePoint().duplicate();
								tp.stepBackOver();
								IAliasTracker dac = new DeltaAliasCollector();
								e = s.extract(reference, tp, dac);
								System.out.println(e);
								if (e != null) {
									eList.add(e);
									dacList.add(dac);
									System.out.println("add" + eList.size() + ", " + dacList.size());
									System.out.println(e);
								}
								System.out.println("---------------------------");
							}
						} while (e != null);
					}
				}
			} else {
				IAliasTracker dac = new DeltaAliasCollector();
				MethodExecution me = trace.getLastMethodExecution(argsMap.get(argsKey)[2]);
				Map<ObjectReference, TracePoint> refs = me.getObjectReferences(argsMap.get(argsKey)[3]);
				ObjectReference ref = refs.keySet().iterator().next();
				ExtractedStructure e = s.extract(refs.get(ref), ref, dac);
				eList.add(e);
				dacList.add(dac);
			}
			extractedMultipleDeltas.put(eList.get(index), dacList.get(index));
		}
		return extractedMultipleDeltas;
	}
	
	public void startCollectionViewer(CollaborationObjectCallGraph cocg, IAliasCollector cac) {
		List<Alias> aliasList = new ArrayList<>(cac.getAliasList());
		if(cocg != null) {
			List<TracePoint> relatedPoints = cocg.getRelatedPoints();
			TracePoint relatedPoint = relatedPoints.get(relatedPoints.size()-1);
//			if (srcSide.size() >= 1 && dstSide.size() >= 1) {
//				WINDOW_TITLE = "extract delta of:" + e.getDelta().getSrcSide().get(0).getDstClassName() + "(" + e.getDelta().getSrcSide().get(0).getDstObjectId() + ")" + " -> " + e.getDelta().getDstSide().get(0).getDstClassName()  + "(" + e.getDelta().getDstSide().get(0).getDstObjectId() + ")";
//				setTitle(WINDOW_TITLE);			
//			}
		}
		viewer.init(cocg, cac, new CollaborationLayout());
		viewer.initAnimation();
		for (int i = 0; i <= aliasList.size(); i++) {
			viewer.stepToAnimation(i);
		}
	}

	private void setArgmentsForDeltaExtract(Map<String, String[]> map){
		// Samples of Multiple Deltas
		String[] getterOverlap1 = {null, null, "getterOverlap.F","getterOverlap.D", "traces/getterOverlap.trace", CONTAINER_COMPONENT_COLLECTION}; map.put("getterOverlap1", getterOverlap1);
		String[] getterOverlap2 = {null, null, "getterOverlap.G","getterOverlap.D", "traces/getterOverlap.trace", CONTAINER_COMPONENT_COLLECTION}; map.put("getterOverlap2", getterOverlap2);
		String[] setterOverlap1 = {null, null, "setterOverlap.F","setterOverlap.C", "traces/setterOverlap.trace", CONTAINER_COMPONENT_COLLECTION}; map.put("setterOverlap1", setterOverlap1);
		String[] setterOverlap2 = {null, null, "setterOverlap.G","setterOverlap.C", "traces/setterOverlap.trace", CONTAINER_COMPONENT_COLLECTION}; map.put("setterOverlap2", setterOverlap2);
		
		// MagnetRON Experiment
	    String[] ArgoUMLDelete1 = {null, null, "public void org.argouml.uml.diagram.ui.ActionRemoveFromDiagram.actionPerformed(", "org.argouml.uml.diagram.static_structure.ui.FigClass", "traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace", THIS_ANOTHER}; map.put("ArgoUMLDelete1", ArgoUMLDelete1);// ArgoUML 削除機能1 (this to another)
		String[] ArgoUMLDelete2 = {"450474599", "1675174935", "java.util.Vector", "org.argouml.uml.diagram.static_structure.ui.FigClass", "traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace", CONTAINER_COMPONENT_COLLECTION}; map.put("ArgoUMLDelete2", ArgoUMLDelete2); // ArgoUML 削除機能2 (collection)
		String[] ArgoUMLSelect1 = {"125345735", "1672744985", "java.util.ArrayList", "org.argouml.uml.diagram.static_structure.ui.SelectionClass", "traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace", CONTAINER_COMPONENT_COLLECTION}; map.put("ArgoUMLSelect1", ArgoUMLSelect1); // ArgoUML 選択機能 (collection)
	    String[] ArgoUMLSelect2 = {"1672744985", "1675174935", "org.argouml.uml.diagram.static_structure.ui.SelectionClass", "org.argouml.uml.diagram.static_structure.ui.FigClass", "traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace", CONTAINER_COMPONENT}; map.put("ArgoUMLSelect2", ArgoUMLSelect2);// ArgoUML 選択機能2
		String[] JHotDrawTransform = {"176893671", "1952912699", "java.util.HashSet", "org.jhotdraw.draw.RectangleFigure", "traces\\jHotDrawBenchmarkWithMoreStandardClasses.trace", CONTAINER_COMPONENT_COLLECTION}; map.put("JHotDrawTransform", JHotDrawTransform); // JHotDraw 移動機能 (collection)
		String[] JHotDrawSelect1 = {"758826749", "1952912699", "org.jhotdraw.draw.tool.DefaultDragTracker", "org.jhotdraw.draw.RectangleFigure", "traces\\jHotDrawBenchmarkWithMoreStandardClasses.trace", CONTAINER_COMPONENT}; map.put("JHotDrawSelect1", JHotDrawSelect1); // JHotDraw 選択機能1
		String[] JHotDrawSelect2 = {"1378082106", "1952912699", "java.util.HashSet", "org.jhotdraw.draw.RectangleFigure", "traces\\jHotDrawBenchmarkWithMoreStandardClasses.trace", CONTAINER_COMPONENT_COLLECTION}; map.put("JHotDrawSelect2", JHotDrawSelect2); // JHotDraw 選択機能2 (collection)
		String[] JHotDrawSelect3 = {"599587451", "758826749", "org.jhotdraw.draw.tool.DelegationSelectionTool", "org.jhotdraw.draw.tool.DefaultDragTracker", "traces\\jHotDrawBenchmarkWithMoreStandardClasses.trace", CONTAINER_COMPONENT}; map.put("JHotDrawSelect3", JHotDrawSelect3); // JHotDraw 選択機能3 (collection)
		String[] JHotDrawSelect4 = {"1787265837", "1952912699", "java.util.LinkedHashSet", "org.jhotdraw.draw.RectangleFigure", "traces\\jHotDrawBenchmarkWithMoreStandardClasses.trace", CONTAINER_COMPONENT_COLLECTION}; map.put("JHotDrawSelect4", JHotDrawSelect4); // JHotDraw 選択機能4 (collection)
	}
	
	private void setArgsKeysForMultipleDeltasExtract(Map<String, List<String>> argsKeysMap, Map<String, String[]> argsMap){
		// Samples of Multiple Deltas
		argsKeysMap.put("getterOverlap", new ArrayList<String>(Arrays.asList("getterOverlap2", "getterOverlap1")));
		argsKeysMap.put("setterOverlap", new ArrayList<String>(Arrays.asList("setterOverlap1", "setterOverlap2")));
		argsKeysMap.put("ArgoUMLDelete", new ArrayList<String>(Arrays.asList("ArgoUMLDelete1", "ArgoUMLDelete2")));
		argsKeysMap.put("ArgoUMLSelect", new ArrayList<String>(Arrays.asList("ArgoUMLSelect1", "ArgoUMLSelect2")));
		argsKeysMap.put("JHotDrawTransform", new ArrayList<String>(Arrays.asList("JHotDrawTransform")));	
		argsKeysMap.put("JHotDrawSelect", new ArrayList<String>(Arrays.asList("JHotDrawSelect1,1", "JHotDrawSelect2", "JHotDrawSelect3,1", "JHotDrawSelect4")));	
	}
}
