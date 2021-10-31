package org.ntlab.deltaExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.ObjectReference;
import org.ntlab.trace.Reference;
import org.ntlab.trace.ThreadInstance;
import org.ntlab.trace.Trace;
import org.ntlab.trace.TraceJSON;
import org.ntlab.trace.TracePoint;

/**
 * 
 * 
 * @author Nitta Lab.
 */
public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long time = System.nanoTime();
//		TraceJSON trace = new TraceJSON("traces\\sampleCollection.trace");
//		DeltaExtractorJSON s = new DeltaExtractorJSON(trace);
//		HashMap<String, ThreadInstance> threads = trace.getAllThreads();
//		ThreadInstance thread = threads.values().iterator().next();
//		TracePoint tp = thread.getRoot().get(thread.getRoot().size() - 1).getExitPoint();
//		ExtractedStructure e = s.extract(new Reference(null, null, "sampleCollection.D", "sampleCollection.C"), tp, new IAliasTracker() {
//			@Override
//			public void addAlias(Alias alias) {
//				System.out.println(alias.getAliasType().toString() + ":" + alias.getObjectId() +"," + alias.getIndex() + "," +  alias.getMethodSignature() + "," + alias.getLineNo());
//			}
//			@Override
//			public void changeTrackingObject(String from, String to, boolean isSrcSide) {
//				System.out.println("Change:" + from + "　=>　" + to);				
//			}
//			@Override
//			public List<Alias> getAliasList() {
//				return null;
//			}
//		});
		
//		TraceJSON trace = new TraceJSON("traces\\_finalLocal.trace");
//		DeltaExtractorJSON s = new DeltaExtractorJSON(trace);
//		HashMap<String, ThreadInstance> threads = trace.getAllThreads();
//		ThreadInstance thread = threads.values().iterator().next();
//		TracePoint tp = thread.getRoot().get(thread.getRoot().size() - 1).getExitPoint();
//		ExtractedStructure e = s.extract(new Reference(null, null, "finalLocal.Main$1Test", "finalLocal.Main$A"), tp);
		
//		TraceJSON trace = new TraceJSON("traces\\__arraySample.trace");
//		DeltaExtractorJSON s = new DeltaExtractorJSON(trace);
//		HashMap<String, ThreadInstance> threads = trace.getAllThreads();
//		for (ThreadInstance thread: threads.values()) {
//			TracePoint tp = thread.getRoot().get(thread.getRoot().size() - 1).getExitPoint();
//			ExtractedStructure e = s.extract(new Reference(null, null, "arraySample.D", "arraySample.C"), tp);
//			s.extract(e.getDelta().getSrcSide().get(2), e.getCoordinator().getEntryPoint());
//			break;
//		}
		
//		TraceJSON trace = new TraceJSON("traces\\__arraySample.trace");
//		DeltaExtractorJSON s = new DeltaExtractorJSON(trace);
//		HashMap<String, ThreadInstance> threads = trace.getAllThreads();
//		ThreadInstance thread = threads.values().iterator().next();
//		TracePoint tp = thread.getRoot().get(thread.getRoot().size() - 1).getExitPoint();
//		Reference ref = new Reference(null, null, "[Ljava.lang.Object;", "arraySample.B");
//		ref.setArray(true);
//		ExtractedStructure e = s.extract(ref, tp);

//		TraceJSON trace = new TraceJSON("traces\\_threadSample.trace");
//		DeltaExtractor s = new DeltaExtractorJSON(trace);
//		HashMap<String, ThreadInstance> threads = trace.getAllThreads();
//		Iterator<ThreadInstance> it = threads.values().iterator();
//		it.next();
//		it.next();
//		ThreadInstance thread = it.next();
//		TracePoint tp = thread.getRoot().get(thread.getRoot().size() - 1).getExitPoint();
//		ExtractedStructure e = s.extract(new Reference(null, null, "threadSample.D", "threadSample.C"), tp);

		//		DeltaExtractor s = new DeltaExtractor("documents\\finalLocal.trace");
		// ExtractedStructure es = s.extract();
		// s.extract("framework.RWT.RWTFrame3D",
		// "fight3D.CharacterSelectContainer");
//		 s.extract("framework.B", "application.SubC");
		// s.extract("application.SubA", "framework.B");
		// FrameworkUsage usage = extractor.extract("framework.B",
		// "application.SubC");
		// FrameworkUsage usage = extractor.extract("application.SubA",
		// "framework.B");
		// s.extract("framework.model3D.Object3D",
		// "javax.media.j3d.TransformGroup");
		// s.extract("fight3D.Character", "fight3D.WeaponModel");
		// s.extract("test.E","test.C");
//		ExtractedStructure e = s.extract(new Reference("finalLocal.Main$1Test", "finalLocal.Main$A", null, null));

// --------------- Eclipse Core ---------------				
//		DeltaExtractor s = new DeltaExtractor("documents\\eclipse-Core.trace");
//		CallTree m = null;
//		ExtractedStructure e = null;
//		do {
//			if (m == null) {
//				m = s.getCallTreeBackwardly("public boolean java.util.HashSet.add(");
//			} else {
//				m = s.getCallTreeBackwardly("public boolean java.util.HashSet.add(", m.getStartLine() - 1);
//			}
//			if (m != null) {
//				ArrayList<ObjectReference> argments = m.getArguments();
//				System.out.println(m.getSignature() + ":" + argments.size());
//				for (int i = 0; i < argments.size(); i++) {
//					if (argments.get(i).getActualType().equals("org.eclipse.ui.internal.registry.ActionSetDescriptor")) {
//						e = s.extract(m, argments.get(i));
//						break;
//					}
//				}
//			}
//		} while (m != null);
		

// --------------- Eclipse UI ---------------		
//		DeltaExtractor s = new DeltaExtractor("documents\\eclipse-ContextMenu.trace");
//		CallTree m = null;
//		ExtractedStructure e = null;
//		do {
//			if (m == null) {
//				m = s.getCallTreeBackwardly("private void org.eclipse.jface.action.ContributionManager.addToGroup(");
//			} else {
//				m = s.getCallTreeBackwardly("private void org.eclipse.jface.action.ContributionManager.addToGroup(", m.getStartLine() - 1);
//			}
//			if (m != null) {
//				ArrayList<ObjectReference> argments = m.getArguments();
//				System.out.println(m.getSignature() + ":" + argments.size());
//				for (int i = 0; i < argments.size(); i++) {
//					if (argments.get(i).getActualType().equals("org.eclipse.ui.internal.PluginActionCoolBarContributionItem")) {
//						e = s.extract(m, argments.get(i));
//						break;
//					}
//				}
//			}
//		} while (m != null);

		///////////////////////////////////////////////////////////////////////////////////
		//                                                                               //
		//                              ICSME2015投稿用                                                                                 //
		//                                                                               //
		///////////////////////////////////////////////////////////////////////////////////
		
//		// --------------- Eclipse (2014/12/6 プログラム理解実証実験 課題1, 以下の1回目のデルタ) ---------------		
//		DeltaExtractor s = new DeltaExtractor("documents\\eclipse-Console2.trace");
//		ExtractedStructure e = null;
//		do {
//			System.out.println(System.nanoTime() - time);
//			if (e == null) {
//				e = s.extract(new Reference(null, null, "org.eclipse.jface.action.ActionContributionItem", 
//						"org.eclipse.ui.console.actions.ClearOutputAction"));
//			} else {
//				e = s.extract(new Reference(null, null, "org.eclipse.jface.action.ActionContributionItem", 
//						"org.eclipse.ui.console.actions.ClearOutputAction"), e.getCreationCallTree().getEntryPoint());
//			}
//		} while (e != null);

		
// --------------- Eclipse (2014/12/19-20 プログラム理解実証実験 課題2, 以下の2回目のデルタ) ---------------		
//		DeltaExtractor s = new DeltaExtractor("documents\\eclipse-JavaEditor.txt");
//		ExtractedStructure e = null;
//		do {
//			System.out.println(System.nanoTime() - time);
//			if (e == null) {
//				e = s.extract(new Reference(null, null, "org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor$AdaptedSourceViewer", 
//						"org.eclipse.jface.text.contentassist.ContentAssistant"));
//			} else {
//				e = s.extract(new Reference(null, null, "org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor$AdaptedSourceViewer", 
//					"org.eclipse.jface.text.contentassist.ContentAssistant"), e.getCreationCallTree().getEntryPoint());
//			}
//		} while (e != null);

		
//		// --------------- ArgoUML (2014/12/19-20 プログラム理解実証実験 課題3, 以下の1回目のデルタ) ---------------		
////		DeltaExtractor s = new DeltaExtractor("traces\\ArgoUML-3.txt");
//		DeltaExtractorJSON s = new DeltaExtractorJSON("traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace");
//		MethodExecution m = null;
//		ExtractedStructure e = null;
//		do {
//			System.out.println(System.nanoTime() - time);
//			if (m == null) {
//				m = s.getLastMethodExecution("protected void org.tigris.gef.base.SelectionManager.addFig(");
//			} else {
//				TracePoint tp = m.getEntryPoint();
//				tp.stepBackOver();
//				m = s.getLastMethodExecution("protected void org.tigris.gef.base.SelectionManager.addFig(", tp);
//			}
//			if (m != null) {
//				ArrayList<ObjectReference> argments = m.getArguments();
//				System.out.println(m.getSignature() + ":" + argments.size());
//				for (int i = 0; i < argments.size(); i++) {
//					if (argments.get(i).getActualType().equals("org.argouml.uml.diagram.static_structure.ui.FigClass")) {
//						e = s.extract(m.getEntryPoint(), argments.get(i));
//						break;
//					}
//				}
//			}
//		} while (m != null);

		
//		// --------------- ArgoUML (2014/12/19-20 プログラム理解実証実験 課題4, 以下の3回目のデルタ) ---------------		
////		DeltaExtractor s = new DeltaExtractor("traces\\ArgoUML-3.txt");
//		DeltaExtractorJSON s = new DeltaExtractorJSON("traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace");
//		MethodExecution m = null;
//		ExtractedStructure e = null;
//		do {
//			System.out.println(System.nanoTime() - time);
//			if (m == null) {
////				m = s.getLastMethodExecution("public abstract interface boolean java.util.List.add(");
//				m = s.getLastMethodExecution("public boolean java.util.List.add(");
//			} else {
//				TracePoint tp = m.getEntryPoint();
//				tp.stepBackOver();
////				m = s.getLastMethodExecution("public abstract interface boolean java.util.List.add(", tp);
//				m = s.getLastMethodExecution("public boolean java.util.List.add(", tp);
//			}
//			if (m != null) {
//				ArrayList<ObjectReference> argments = m.getArguments();
//				System.out.println(m.getSignature() + ":" + argments.size());
//				for (int i = 0; i < argments.size(); i++) {
//					if (argments.get(i).getActualType().equals("org.argouml.uml.diagram.static_structure.ui.FigClass")) {
//						e = s.extract(m.getEntryPoint(), argments.get(i));
//						break;
//					}
//				}
//			}
//		} while (m != null);

		///////////////////////////////////////////////////////////////////////////////////
		//                                                                               //
		//                              SANER2016投稿用                                                                                 //
		//                                                                               //
		///////////////////////////////////////////////////////////////////////////////////

		// --------------- Eclipse SWT (2015/10/31 アーキテクチャ理解実証実験 課題1) ---------------		
		//  問1(1stデルタ), 以下のデルタ
//		DeltaExtractor s = new DeltaExtractor("documents\\eclipse-Console2.txt");
//		MethodExecution m = null;
//		ExtractedStructure e = null;
//		System.out.println(System.nanoTime() - time);
//		do {
//			if (m == null) {
//				m = s.getLastMethodExecution("public void org.eclipse.jface.action.Action.runWithEvent(");
//			} else {
//				TracePoint nextTp = m.getEntryPoint();
//				nextTp.stepBackOver();
//				m = s.getLastMethodExecution("public void org.eclipse.jface.action.Action.runWithEvent(", nextTp);
//			}
//			if (m != null) {
//				ArrayList<ObjectReference> argments = m.getArguments();
//				for (int i = 0; i < argments.size(); i++) {
//					if (argments.get(i).getActualType().equals("org.eclipse.swt.widgets.Event")) {
//						System.out.println(System.nanoTime() - time);
//						e = s.extract(m.getEntryPoint(), argments.get(i));
//						break;
//					}
//				}
//			}
//		} while (e == null);
//		System.out.println(System.nanoTime() - time);
//		System.out.println("//////////////////////////////////");
//		
//		//   問2,3(2ndデルタ), 問1の続き, 以下のデルタ
//		Reference nextTarget = e.getDelta().getSrcSide().get(6);
//		e = s.extract(nextTarget, e.getCoordinator().getEntryPoint());
		
		
//		// --------------- Eclipse JDT (2015/10/31 アーキテクチャ理解実証実験 課題2) ---------------		
//		//   問1,2(1stデルタ), 以下のデルタ(抽出としては2回分)
//		DeltaExtractor s = new DeltaExtractor("documents\\eclipse-Debug1.txt");
//		MethodExecution m = null;
//
//		System.out.println(System.nanoTime() - time);
//		ExtractedStructure e = null;
//		do {
//			if (m == null) {
//				m = s.getLastMethodExecution("public boolean org.eclipse.jdt.internal.debug.core.breakpoints.JavaBreakpoint.handleBreakpointEvent(");
//			} else {
//				TracePoint nextTp = m.getEntryPoint();
//				nextTp.stepBackOver();
//				m = s.getLastMethodExecution("public boolean org.eclipse.jdt.internal.debug.core.breakpoints.JavaBreakpoint.handleBreakpointEvent(", nextTp);
//			}
//			if (m != null) {
//				ArrayList<ObjectReference> argments = m.getArguments();
//				for (int i = 0; i < argments.size(); i++) {
//					if (argments.get(i).getActualType().equals("org.eclipse.jdi.internal.event.BreakpointEventImpl")) {
//						System.out.println(System.nanoTime() - time);
//						e = s.extract(m.getEntryPoint(), argments.get(i));
//						break;
//					}
//				}
//			}
//		} while (e == null);
//		Reference nextTarget = e.getDelta().getDstSide().get(3);		// EventDispatcher$1 -> EventSetImpl
//		e = s.extract(nextTarget, m.getEntryPoint());
//		System.out.println(System.nanoTime() - time);
//		System.out.println("//////////////////////////////////");
//		
//		//   問3(2ndデルタ), 以下のデルタ
//		MethodExecution m2 = e.getCoordinator().getChildren().get(21);
//		e = s.extract(m2.getExitPoint(), new ObjectReference("859038530", "org.eclipse.jdi.internal.jdwp.JdwpCommandPacket"));
//		System.out.println(System.nanoTime() - time);
//		System.out.println("//////////////////////////////////");
//		
//		
//		//   問4(3rdデルタ), 以下のデルタ
//		m = e.getCoordinator().getChildren().get(0).getChildren().get(1).getChildren().get(4);
//		Reference lastTarget = new Reference(e.getDelta().getDstSide().get(1).getSrcObject(), e.getDelta().getDstSide().get(0).getDstObject());
//		lastTarget.setCollection(true);
//		e = s.extract(lastTarget, m.getExitPoint());
		
		
//		// --------------- ArgoUML + GEF (2014/12/19-20 プログラム理解実証実験 課題3, 以下のデルタ) ---------------		
//		DeltaExtractor s = new DeltaExtractor("documents\\ArgoUML-3.txt");
//		MethodExecution m = null;
//		ExtractedStructure e = null;
//		System.out.println(System.nanoTime() - time);
//		m = s.getLastMethodExecution("public void org.argouml.uml.diagram.ui.ActionRemoveFromDiagram.actionPerformed(");
////		m = s.getLastMethodExecution("public java.util.Vector<org.tigris.gef.presentation.Fig> org.tigris.gef.base.SelectionManager.getFigs(");
//		if (m != null) {
//			System.out.println(System.nanoTime() - time);
//			Reference r = new Reference(null, null, "java.util.ArrayList", "org.argouml.uml.diagram.static_structure.ui.SelectionClass");
//			r.setCollection(true);
//			e = s.extract(r, m.getEntryPoint());
//		}


		/////////////////////////////////////////////////////////
		//                                                                               //
		//                              MagnetRON用                          //
		//                                                                               //
		/////////////////////////////////////////////////////////


		// --------------- Eclipse JDT (2015/10/31 アーキテクチャ理解実証実験 課題2) ---------------		
		//  1回目のデルタ抽出
		//		{
		//			"src": {
		//				"class": "public boolean org.eclipse.jdt.internal.debug.core.breakpoints.JavaBreakpoint.handleBreakpointEvent(",
		//				"id": "666051245"
		//			}, 
		//			"dst": {
		//				"class": "org.eclipse.jdi.internal.event.BreakpointEventImpl",
		//				"id": "907205473"
		//			}, 
		//			"type": "This-Another",
		//			"order": "0"
		//		}
		DeltaExtractor s = new DeltaExtractor("traces\\eclipse-Debug1.txt");
		MethodExecution m = null;

		System.out.println(System.nanoTime() - time);
		ExtractedStructure e = null;
		do {
			if (m == null) {
				m = s.getLastMethodExecution("public boolean org.eclipse.jdt.internal.debug.core.breakpoints.JavaBreakpoint.handleBreakpointEvent(");
			} else {
				TracePoint nextTp = m.getEntryPoint();
				nextTp.stepBackOver();
				m = s.getLastMethodExecution("public boolean org.eclipse.jdt.internal.debug.core.breakpoints.JavaBreakpoint.handleBreakpointEvent(", nextTp);
			}
			if (m != null) {
				ArrayList<ObjectReference> argments = m.getArguments();
				for (int i = 0; i < argments.size(); i++) {
					if (argments.get(i).getActualType().equals("org.eclipse.jdi.internal.event.BreakpointEventImpl")) {
						System.out.println(System.nanoTime() - time);
						e = s.extract(m.getEntryPoint(), argments.get(i));
						break;
					}
				}
			}
		} while (e == null);
		System.out.println(System.nanoTime() - time);
		System.out.println("//////////////////////////////////");
		
		//  2回目のデルタ抽出
		//		{
		//          "src": {
		//            "class": "org.eclipse.jdi.internal.event.EventIteratorImpl",
		//            "id": "239180057"
		//          }, 
		//          "dst": {
		//            "class": "java.util.ArrayList$ListItr",
		//            "id": "316502076"
		//          }, 
		//          "type": "Container-Component",
		//          "order": "0"
		//		},
		Reference nextTarget = e.getDelta().getDstSide().get(1);		// EventIteratorImpl -> ArrayList$ListItr
		ExtractedStructure e2 = s.extract(nextTarget, m.getEntryPoint());
		System.out.println(System.nanoTime() - time);
		System.out.println("//////////////////////////////////");
		
		//   3回目のデルタ抽出
		//		{
		//			"src": {
		//				"class": "java.util.ArrayList",
		//				"id": "1121573201"
		//			}, 
		//			"dst": {
		//				"class": "org.eclipse.jdi.internal.event.BreakpointEventImpl",
		//				"id": "907205473"
		//			}, 
		//			"type": "Container-Component(Collection)",
		//			"order": "0"
		//		},	
		Reference nextnextTarget = new Reference("1121573201", "907205473", null, null);		// ArrayList -> BreakpointEventImpl
		nextnextTarget.setCollection(true);
		e2 = s.extract(nextnextTarget, m.getEntryPoint());
		System.out.println(System.nanoTime() - time);
		System.out.println("//////////////////////////////////");
		
		//  4回目のデルタ抽出
		//		{
		//            "src": {
		//              "class": "public void org.eclipse.jdt.internal.debug.core.EventDispatcher.run(",
		//              "id": "629542817"
		//            }, 
		//            "dst": {
		//              "class": "org.eclipse.jdi.internal.event.EventSetImpl",
		//              "id": "1400795012"
		//            }, 
		//            "type": "This-Another", 
		//            "order": "0"
		//		},		
		Reference nextnextnextTarget = e.getDelta().getDstSide().get(3);		// EventDispatcher$1 -> EventSetImpl
		e2 = s.extract(nextnextnextTarget, m.getEntryPoint());
		System.out.println(System.nanoTime() - time);
		System.out.println("//////////////////////////////////");
				
		//   5回目のデルタ抽出
		//		{
		//			"src": {
		//				"class": "public com.sun.jdi.event.EventSet org.eclipse.jdi.internal.event.EventQueueImpl.remove(",
		//				"id": null
		//			}, 
		//			"dst": {
		//				"class": "org.eclipse.jdi.internal.jdwp.JdwpCommandPacket",
		//				"id": "859038530"
		//			}, 
		//			"type": "This-Another",
		//			"order": "0"
		//		}
		MethodExecution m2 = e2.getCoordinator().getChildren().get(21);
		e2 = s.extract(m2.getExitPoint(), new ObjectReference("859038530", "org.eclipse.jdi.internal.jdwp.JdwpCommandPacket"));
		System.out.println(System.nanoTime() - time);
		System.out.println("//////////////////////////////////");
		
		
//		//   6回目のデルタ抽出(課題には含めない)
//		m = e.getCoordinator().getChildren().get(0).getChildren().get(1).getChildren().get(4);
//		Reference lastTarget = new Reference(e.getDelta().getDstSide().get(1).getSrcObject(), e.getDelta().getDstSide().get(0).getDstObject());
//		lastTarget.setCollection(true);
//		e = s.extract(lastTarget, m.getExitPoint());
//		System.out.println(System.nanoTime() - time);
//		System.out.println("//////////////////////////////////");

//		// s.extractArg(e.getCoodinator(), 123456789);
//		// s.getCallHistory(e.getCoodinator());

	}
}
