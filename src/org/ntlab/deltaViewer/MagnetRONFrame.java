package org.ntlab.deltaViewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.image.ImageProducer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.JToolBar;

import org.ntlab.actions.FastForwardAnimationAction;
import org.ntlab.actions.PauseAnimationAction;
import org.ntlab.actions.SkipBackAnimationAction;
import org.ntlab.actions.StartAnimationAction;
import org.ntlab.actions.StopAnimationAction;
import org.ntlab.deltaExtractor.Alias;
import org.ntlab.deltaExtractor.DeltaExtractor;
import org.ntlab.deltaExtractor.DeltaExtractorJSON;
import org.ntlab.deltaExtractor.ExtractedStructure;
import org.ntlab.deltaExtractor.IAliasCollector;
import org.ntlab.deltaExtractor.IAliasTracker;
import org.ntlab.featureExtractor.ExpectedExtracts;
import org.ntlab.featureExtractor.ExpectedFeatures;
import org.ntlab.featureExtractor.ExpectedLeftCurlyBraket;
import org.ntlab.featureExtractor.ExpectedRightCurlyBraket;
import org.ntlab.featureExtractor.Extract;
import org.ntlab.featureExtractor.Feature;
import org.ntlab.featureExtractor.MagnetronParser;
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
public class MagnetRONFrame extends JFrame implements IMagnetRON {	

	private static final long serialVersionUID = -7635196578197342313L;
	
	// Test code (will be deleted)
	private static final String TAG = MagnetRONFrame.class.getSimpleName();

	private static Dimension DEFAULT_FRAME_SIZE = new Dimension(1300, 600);
	private static String FRAME_TITLE = "MagnetRON Viewer ";

	private Trace trace = null;
	private DeltaExtractor s = null;
	private List<Feature> features = new ArrayList<>();

	private CollaborationViewer viewer = null;
	private MagnetRONMenuBar menuBar;
	private JToolBar frameToolBar;
	private JSlider slider;

	private Map<String, String[]> argsMap = new HashMap<>();
	private IObjectCallGraph objectCallGraph;
	private IAliasCollector aliasCollector;
	
	private boolean fFeatureChanged = false;
	
	private volatile Thread animationThread;
	private boolean fThreadSuspended = false;
	
	public MagnetRONFrame() throws HeadlessException {
		super(FRAME_TITLE);
		setSize(DEFAULT_FRAME_SIZE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		this.viewer = new CollaborationViewer();
		getContentPane().add(viewer, BorderLayout.CENTER);
		
		menuBar = new MagnetRONMenuBar(this);
		setJMenuBar(menuBar);
	    
		JToolBar toolBar = new JToolBar();

		JToolBar animationToolBar = new JToolBar();
		Image startImage = null;
		Image fastForwardImage = null;
		Image skipBackImage = null;
		Image pauseImage = null;
		Image stopImage = null;
		URL skipBackUrl = this.getClass().getResource("/icons/skip_back_co.png");
		URL startUrl = this.getClass().getResource("/icons/resume_co.png");
		URL fastForwardUrl = this.getClass().getResource("/icons/fast_forwarding_co.png");
		URL pauseUrl = this.getClass().getResource("/icons/suspend_co.png");
		URL stopUrl = this.getClass().getResource("/icons/terminate_co.png");
		try {
			skipBackImage = this.createImage((ImageProducer) skipBackUrl.getContent());
			startImage = this.createImage((ImageProducer) startUrl.getContent());
			fastForwardImage = this.createImage((ImageProducer) fastForwardUrl.getContent());
			pauseImage = this.createImage((ImageProducer) pauseUrl.getContent());
			stopImage = this.createImage((ImageProducer) stopUrl.getContent());
		}catch(Exception e){
			 System.out.println("Load resource error!");
		}
		
		JButton skipBackButton = new JButton(new ImageIcon(skipBackImage, "Skip Back"));
		skipBackButton.addActionListener(new SkipBackAnimationAction(this));
		animationToolBar.add(skipBackButton);
		JButton startButton = new JButton(new ImageIcon(startImage, "Start"));
		startButton.addActionListener(new StartAnimationAction(this));
		animationToolBar.add(startButton);
		JButton fastForwardButton = new JButton(new ImageIcon(fastForwardImage, "Fast-Forward"));
		fastForwardButton.addActionListener(new FastForwardAnimationAction(this));
		animationToolBar.add(fastForwardButton);
		JButton suspendButton = new JButton(new ImageIcon(pauseImage, "Pause"));
		suspendButton.addActionListener(new PauseAnimationAction(this));
		animationToolBar.add(suspendButton);
		JButton terminateButton = new JButton(new ImageIcon(stopImage, "Stop"));
		terminateButton.addActionListener(new StopAnimationAction(this));
		animationToolBar.add(terminateButton);

		frameToolBar = new JToolBar();
	    slider = new JSlider();
	    frameToolBar.add(slider);
	    frameToolBar.setVisible(false);
	    
	    toolBar.add(animationToolBar);
	    toolBar.add(frameToolBar);
		
	    add(toolBar, BorderLayout.NORTH);
		pack();
	}
	
	public MagnetRONViewer getViewer() {
		return viewer;
	}

	public Trace getTrace() {
		return trace;
	}

//	public void startAll() {
//		// Change Here!
//		setArgmentsForDeltaExtract(argsMap);
//		//抽出したいデルタの引数を格納したMapのkey
////		String argsKey1 = "ArgoUMLSelect";
////		String argsKey1 = "ArgoUMLDelete1";
////		String argsKey2 = "ArgoUMLDelete2";
////		String argsKey1 = "JHotDrawTransform";
//		String argsKey1 = "JHotDrawSelect1,1";
//		String argsKey2 = "JHotDrawSelect2";
//		String argsKey3 = "JHotDrawSelect3,1";
//		String argsKey4 = "JHotDrawSelect4";
////		String argsKey = "sampleArray";
////		String argsKey = "sampleCollection";
////		String argsKey = "sampleCreate";
////		String argsKey = "sampleStatic";
////		String argsKey = "delta_eg1";
////		String argsKey = "pre_Exp1";
////		String argsKey1 = "getterOverlap1";
////		String argsKey2 = "getterOverlap2";
////		String argsKey = "setterOverlap1";
////		String argsKey = "worstCase";
////		String argsKey = "sample1";
////		String argsKey = "objTrace5";
//		List<String> keys = new ArrayList<String>();
//		keys.add(argsKey1);
//		keys.add(argsKey2);
//		keys.add(argsKey3);
//		keys.add(argsKey4);
//		Map.Entry<IObjectCallGraph, IAliasCollector> extracted = extractMulti(keys);
//		objectCallGraph = extracted.getKey();
//		aliasCollector = extracted.getValue();
////		new Thread() {
////			public void run() {
////				startDeltaViewer(extracted.getKey(), extracted.getValue());						
////			}
////		}.start();
//	}
	
	@Override
	public List<Feature> open(File file) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			Map<String, Object> magnetJson = MagnetronParser.doParse(reader);
			reader.close();
			if (magnetJson.get("trace") != null) {
				if (magnetJson.get("format") != null && magnetJson.get("format").equals("PlainText")) {
					trace = new Trace(file.getParent() + "\\" + magnetJson.get("trace"));
					s = new DeltaExtractor(trace);
				} else {
					trace = new TraceJSON(file.getParent() + "\\" + magnetJson.get("trace"));
					s = new DeltaExtractorJSON((TraceJSON) trace);
				}
				if (magnetJson.get("features") != null) {
			        features = (List<Feature>) magnetJson.get("features");
					menuBar.updateExtractsMenu(features);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ExpectedLeftCurlyBraket | ExpectedRightCurlyBraket | ExpectedFeatures | ExpectedExtracts e) {
			e.printStackTrace();
		}
		return features;
	}
	
	@Override
	public void doExtract(Feature feature) {
		viewer.clear();
		Map.Entry<IObjectCallGraph, IAliasCollector> extracted = extractMulti(feature);
		if (feature.getName() != null) {
			FRAME_TITLE = feature.getName() + " ";
			setTitle(FRAME_TITLE);
		}
		objectCallGraph = extracted.getKey();
		aliasCollector = extracted.getValue();
		
		fFeatureChanged = true;
		animationThread = null;
		if (objectCallGraph != null && aliasCollector != null) {
			viewer.init(objectCallGraph, aliasCollector, new CollaborationLayout());
			viewer.initAnimation();
		}
	}
	
	private Entry<IObjectCallGraph, IAliasCollector> extractMulti(Feature feature) {
		CollaborationObjectCallGraph cocg = null;
		CollaborationAliasCollector cac = null;
		Map<MethodExecution, Set<MethodExecution>> newToOldMethodExecMap = new HashMap<>();
		
		for(Extract extract: feature.getExtracts()) {
			Map.Entry<ExtractedStructure, IAliasCollector> extracted = extract(extract);
			if (cocg == null) {
				cocg = new CollaborationObjectCallGraph(extracted.getKey());
			} else {
				cocg.merge(extracted.getKey());
			}
			IAliasCollector ac = extracted.getValue();
			DeltaAliasCollector dac = (DeltaAliasCollector) ac;
			newToOldMethodExecMap.putAll(dac.shrink());
			if (cac == null) {
				cac = new CollaborationAliasCollector(dac);
			} else {
				cac.merge(dac, extract);
			}
		}
		cocg.shrinkAll(newToOldMethodExecMap);
		return new AbstractMap.SimpleEntry<IObjectCallGraph, IAliasCollector>(cocg, cac);
	}

	public Map.Entry<IObjectCallGraph, IAliasCollector> extractMulti(List<String> keys) {
		trace = null;
		s = null;
		CollaborationObjectCallGraph cocg = null;
		CollaborationAliasCollector cac = null;
		Map<MethodExecution, Set<MethodExecution>> newToOldMethodExecMap = new HashMap<>();

		for(String key: keys) {
			int index = 0;
			if (key.contains(",")) {
				String[] splitKeys = key.split(",");
				key = splitKeys[0];
				index = Integer.parseInt(splitKeys[splitKeys.length-1]);
				if (trace == null && s == null) {
					trace = new TraceJSON(argsMap.get(key)[4]);
					s = new DeltaExtractorJSON((TraceJSON) trace);
				}
			} else {
				if (trace == null && s == null) {
					trace = new TraceJSON(argsMap.get(key)[4]);
					s = new DeltaExtractorJSON((TraceJSON) trace);
				}
			}
			Map.Entry<ExtractedStructure, IAliasCollector> extracted = extract(key, index);
			if (cocg == null) {
				cocg = new CollaborationObjectCallGraph(extracted.getKey());
			} else {
				cocg.merge(extracted.getKey());
			}
			IAliasCollector ac = extracted.getValue();
			DeltaAliasCollector dac = (DeltaAliasCollector) ac;
			newToOldMethodExecMap.putAll(dac.shrink());
			if (cac == null) {
				cac = new CollaborationAliasCollector(dac);
			} else {
				cac.merge(dac, null);
			}
		}
		cocg.shrinkAll(newToOldMethodExecMap);
		return new AbstractMap.SimpleEntry<IObjectCallGraph, IAliasCollector>(cocg, cac);
	}

	private Entry<ExtractedStructure, IAliasCollector> extract(Extract extract) {
		int index = extract.getOrder();
		if (extract.getType().equals(Extract.CONTAINER_COMPONENT) || extract.getType().equals(Extract.CONTAINER_COMPONENT_COLLECTION)) {
	
			HashMap<String, ThreadInstance> threads = trace.getAllThreads();
			if (threads.values().size() == 1) {
				ThreadInstance thread = threads.values().iterator().next(); // 最後のスレッドを見ているだけ…
				TracePoint tp = thread.getRoot().get(thread.getRoot().size() - 1).getExitPoint();
				Reference reference = new Reference(extract.getSrcId(), extract.getDstId(), extract.getSrcClass(), extract.getDstClass());
				if (extract.getType().equals(Extract.CONTAINER_COMPONENT_COLLECTION)) {
					reference.setCollection(true);						
				}
				ExtractedStructure e = null;
				for (int i = 0; ; i++) {
					IAliasTracker dac = new DeltaAliasCollector();
					e = s.extract(reference, tp, dac);
					if (e == null) break;
					if (i == index) {
						return new AbstractMap.SimpleEntry<ExtractedStructure, IAliasCollector>(e, dac);
					}
					tp = e.getRelatedTracePoint().duplicate();
					tp.stepBackOver();
				}
				return null;
			} else {
				int i = 0;
				for (ThreadInstance thread: threads.values()) {
					TracePoint tp = thread.getRoot().get(thread.getRoot().size() - 1).getExitPoint();
					Reference reference = new Reference(extract.getSrcId(), extract.getDstId(), extract.getSrcClass(), extract.getDstClass());
					if (extract.getType().equals(Extract.CONTAINER_COMPONENT_COLLECTION)) {
						reference.setCollection(true);						
					}
					ExtractedStructure e = null;
					do {
						if (e == null) {
							IAliasTracker dac = new DeltaAliasCollector();
							e = s.extract(reference, tp, dac);
							System.out.println(e);
							if (e != null) {
								if (i == index) {
									return new AbstractMap.SimpleEntry<ExtractedStructure, IAliasCollector>(e, dac);									
								}
								i++;
							}
						} else {
							tp = e.getRelatedTracePoint().duplicate();
							tp.stepBackOver();
							IAliasTracker dac = new DeltaAliasCollector();
							e = s.extract(reference, tp, dac);
							System.out.println(e);
							if (e != null) {
								if (i == index) {
									return new AbstractMap.SimpleEntry<ExtractedStructure, IAliasCollector>(e, dac);									
								}
								i++;
							}
						}
					} while (e != null);
					System.out.println("---------------------------");
				}
			}
			return null;
		} else {
			IAliasTracker dac = new DeltaAliasCollector();
			int i = 0;
			List<MethodExecution> mes = trace.getMethodExecutions(extract.getSrcClass());
			while (mes.size() > 0) {
				MethodExecution lastExec = null;
				long lastExecTime = 0L;
				for (MethodExecution me: mes) {
					if (lastExec == null || me.getEntryTime() > lastExecTime) {
						lastExec = me;
						lastExecTime = me.getEntryTime();
					}
				}
				mes.remove(lastExec);
				Map<ObjectReference, TracePoint> refs = lastExec.getObjectReferences(extract.getDstClass());
				if (refs.size() > 0) {
					for (ObjectReference ref: refs.keySet()) {
						if (extract.getDstId() == null || extract.getDstId().equals(ref.getId())) {
							ExtractedStructure e = s.extract(refs.get(ref), ref, dac);
							if (e != null) {
								if (i == index) {
									return new AbstractMap.SimpleEntry<ExtractedStructure, IAliasCollector>(e, dac);
								}
								i++;
							}
						}
					}
				}
			}
			return null;
		}
	}

	public Map.Entry<ExtractedStructure, IAliasCollector> extract(String argsKey, int index) {
//		DeltaExtractorJSON dex = new DeltaExtractorJSON(trace);
//		HashMap<String, ThreadInstance> threads = trace.getAllThreads();
//		ThreadInstance thread = threads.values().iterator().next(); // 最後のスレッドを見ているだけ…
//		TracePoint tp = thread.getRoot().get(thread.getRoot().size() - 1).getExitPoint();

//		System.out.println("===== All Statements Backward =====");
//		TracePoint tp = null;
//		ExtractedStructure e = null;
//		IAliasTracker dac = new DeltaAliasCollector();
//		long restart = 0L;
//		for(;;) {
//			tp = trace.traverseStatementsInTraceBackward(new IStatementVisitor() {
//				@Override
//				public boolean preVisitStatement(Statement statement) {
//					System.out.println("pre:" + statement.getThreadNo() + ":" + statement.getTimeStamp());
//					if(statement instanceof FieldUpdate) {
//						FieldUpdate fu = (FieldUpdate) statement;
//						if(!Trace.isNull(fu.getValueObjId()) && !Trace.isPrimitive(fu.getValueClassName())) {
//							return true;
//						}
//					}
//					return false;
//				}
//
//				@Override
//				public boolean postVisitStatement(Statement statement) {
//					System.out.println("post:" + statement.getThreadNo() + ":" + statement.getTimeStamp());
//					return false;
//				}
//			}, restart);
//			if(tp == null || !tp.isValid()) break;
//			e = dex.extract(new Reference(jHotDrawSelect3_1[0], jHotDrawSelect3_1[1], jHotDrawSelect3_1[2], jHotDrawSelect3_1[3]), tp, dac);
//			dex.extract(tp);
//			Statement statement = tp.getStatement();
//			if(statement instanceof MethodInvocation) {
//				restart = ((MethodInvocation)tp.getStatement()).getCalledMethodExecution().getExitTime();
//			} else {
//				restart = tp.getStatement().getTimeStamp();
//			}
//		}
//		trace.traverseStatementsInTraceBackward(new IStatementVisitor() {
//			@Override
//			public boolean preVisitStatement(Statement statement) {
//				System.out.println("post:" + statement.getClass().getName() + ":" + statement.getTimeStamp());
//				return false;
//			}
//			@Override
//			public boolean postVisitStatement(Statement statement) {
//				System.out.println("pre:" + statement.getClass().getName() + ":" + statement.getTimeStamp());
//				return false;
//			}
//		});
//	}

//		HashSet<String> marked = trace.getMarkedMethodSignatures(1255991806833871L, 1255991808597322L);
//		HashSet<String> marked = trace.getMarkedMethodSignatures(1699553004208835L, 1699553004739523L);
//		System.out.println("===== Marked Methods =====");
//		for (String method: marked) {
//			System.out.println(method);
//		}
//		HashSet<String> unmarked = trace.getUnmarkedMethodSignatures(1255991806833871L, 1255991808597322L);
//		HashSet<String> unmarked = trace.getUnmarkedMethodSignatures(1699553004208835L, 1699553004739523L);
//		System.out.println("===== Unmarked Methods =====");
//		for (String method: unmarked) {
//			System.out.println(method);
//		}

		// Change!
		// setArray, setCollection(2回目以降は必要なし) , foreach(thread)
		// ArrayListにaddされてない
		// フィールドに代入されていない
//		HashMap<String, ThreadInstance> threads = trace.getAllThreads();
//		for(ThreadInstance ti: threads.values()) {
//			ArrayList<MethodExecution> roots = ti.getRoot();
//			for(MethodExecution root: roots) {
//				traverseMethodExecution(root);
//			}
//		}

//		long time = System.nanoTime();
		if (argsMap.get(argsKey)[5].equals(Extract.CONTAINER_COMPONENT) || argsMap.get(argsKey)[5].equals(Extract.CONTAINER_COMPONENT_COLLECTION)) {
			HashMap<String, ThreadInstance> threads = trace.getAllThreads();

			List<ExtractedStructure> eList = new ArrayList<>();
			List<IAliasCollector> dacList = new ArrayList<>();
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
					if (argsMap.get(argsKey)[5].equals(Extract.CONTAINER_COMPONENT_COLLECTION)) {
						reference.setCollection(true);						
					}
//					reference.setArray(true);
//					reference.setFinalLocal(true);
//					reference.setCreation(true);

					ExtractedStructure e = null;
					do {
//					System.out.println(System.nanoTime() - time);
						if (e == null) {
							IAliasTracker dac = new DeltaAliasCollector();
							e = s.extract(reference, tp, dac);
							System.out.println(e);
							if (e != null) {
								eList.add(e);
								dacList.add(dac);
							}
//						e = s.extract(new Reference(null, null, "org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor$AdaptedSourceViewer", 
//								"org.eclipse.jface.text.contentassist.ContentAssistant"));
						} else {
							tp = e.getRelatedTracePoint().duplicate();
							tp.stepBackOver();
							IAliasTracker dac = new DeltaAliasCollector();
							e = s.extract(reference, tp, dac);
//						e = s.extract(new Reference(null, null, "org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor$AdaptedSourceViewer", 
//							"org.eclipse.jface.text.contentassist.ContentAssistant"), e.getCreationCallTree().getEntryPoint());
							System.out.println(e);
							if (e != null) {
								eList.add(e);
								dacList.add(dac);
								System.out.println("add" + eList.size() + ", " + dacList.size());
							}
						}
					} while (e != null);
//					}
//					long restart = 0L;
//					for(;;) {
//						tp = trace.traverseStatementsInTraceBackward(new IStatementVisitor() {
//							@Override
//							public boolean preVisitStatement(Statement statement) {
//								System.out.println("pre:" + statement.getThreadNo() + ":" + statement.getTimeStamp());
//								if(statement instanceof FieldUpdate) {
//									FieldUpdate fu = (FieldUpdate) statement;
//									if(!Trace.isNull(fu.getValueObjId()) && !Trace.isPrimitive(fu.getValueClassName())) {
//										return true;
//									}
//								}
//								return false;
//							}
//
//							@Override
//							public boolean postVisitStatement(Statement statement) {
//								System.out.println("post:" + statement.getThreadNo() + ":" + statement.getTimeStamp());
//								return false;
//							}
//						}, restart);
//						if(tp == null || !tp.isValid()) break;
//						IAliasTracker dac = new DeltaAliasCollector();
//						ExtractedStructure e = s.extract(reference, tp, dac);
//						System.out.println(e);
//						if (e != null) {
//							eList.add(e);
//							dacList.add(dac);
//							System.out.println("add" + eList.size() + ", " + dacList.size());
//						}
//						if(tp == null || !tp.isValid()) break;
//						s.extract(tp);
//						Statement statement = tp.getStatement();
//						if(statement instanceof MethodInvocation) {
//							restart = ((MethodInvocation)tp.getStatement()).getCalledMethodExecution().getExitTime();
//						} else {
//							restart = tp.getStatement().getTimeStamp();
//						}
//					}

//					IAliasTracker dac = new DeltaAliasCollector();
//					ExtractedStructure e = s.extract(reference, tp, dac);
//					System.out.println(e);
//					if (e != null) {
//						eList.add(e);
//						dacList.add(dac);
//						System.out.println("add" + eList.size() + ", " + dacList.size());
//					}
					System.out.println("---------------------------");
				}
			}
			return new AbstractMap.SimpleEntry<ExtractedStructure, IAliasCollector>(eList.get(index), dacList.get(index));
//			startDeltaViewer(eList.get(0), dacList.get(0));
		} else {
			IAliasTracker dac = new DeltaAliasCollector();
			MethodExecution me = trace.getLastMethodExecution(argsMap.get(argsKey)[2]);
			Map<ObjectReference, TracePoint> refs = me.getObjectReferences(argsMap.get(argsKey)[3]);
			ObjectReference ref = refs.keySet().iterator().next();
			ExtractedStructure e = s.extract(refs.get(ref), ref, dac);
			return new AbstractMap.SimpleEntry<ExtractedStructure, IAliasCollector>(e, dac);
//			startDeltaViewer(e, dac);

		}
	}

	@Override
	public void startAnimation() {
		if (animationThread == null) {
			animationThread = new Thread() {
				public void run() {
					// Build a frame, create a graph, and add the graph to the frame so you can actually see the graph.
					if (objectCallGraph != null && aliasCollector != null) {
						List<Alias> aliasList = new ArrayList<>(aliasCollector.getAliasList());

						if (!fFeatureChanged) {
							viewer.clear();
							viewer.init(objectCallGraph, aliasCollector, new CollaborationLayout());
							viewer.initAnimation();
						}

						if (fFeatureChanged) {
							// Set frame title.
							List<TracePoint> relatedPoints = objectCallGraph.getRelatedPoints();
							TracePoint lastRp = relatedPoints.get(relatedPoints.size() - 1);
							Map.Entry<Reference, String> rpInf = MagnetRONViewer.getRelatedInformation(lastRp, aliasCollector);
							if (FRAME_TITLE.contains("extract delta of")) {
								String[] splits = FRAME_TITLE.split("extract delta of");
								String featureName = splits[0];
								FRAME_TITLE = featureName;
							}
							FRAME_TITLE += "extract delta of:" + rpInf.getKey().getSrcClassName() + "(" + rpInf.getKey().getSrcObjectId() + ")" + " -> " + rpInf.getKey().getDstClassName()+ "(" + rpInf.getKey().getDstObjectId()+ ")";
							setTitle(FRAME_TITLE);

							// Initialize slider.
							slider.setMinimum(0);
							slider.setMaximum(aliasList.size());
							slider.setPaintTicks(true);
						    slider.setMajorTickSpacing(5);
						    slider.setMinorTickSpacing(1);
						    slider.setPaintLabels(true);
							fFeatureChanged = false;
						}

					    frameToolBar.setVisible(true);
						for (int i = 0; i <= aliasList.size(); i++) {
							if (!viewer.isSkipBackAnimation()) {
								slider.setValue(i);
							}
							if (viewer.isSkipBackAnimation() && viewer.getSkipBackFrame() <= viewer.getCurrentFrame()) {
								viewer.setAnimationSpeed(1.0);
								viewer.setVisible(true);
								viewer.setSkipBackAnimation(false);
								slider.setValue(i);
								if (fThreadSuspended) {
									pauseAnimation();
								}
							}
							viewer.stepToAnimation(i);
						}
					}
					animationThread = null;
					viewer.setAnimationSpeed(1.0);
				}
			};
			animationThread.start();
		} else {
			viewer.setAnimationSpeed(1.0);
			resumeAnimation();
		}
	}
	
	public void fastForwardAnimation() {
		if (animationThread != null) {
			double animationSpeed = viewer.getAnimationSpeed();
			if (!fThreadSuspended) {
				if (animationSpeed < 2.0) {
					viewer.setAnimationSpeed(animationSpeed + 0.25);				
				} else if (animationSpeed < 10.0) {
					viewer.setAnimationSpeed(animationSpeed + 1.0);								
				}
			} else {
				viewer.setAnimationSpeed(animationSpeed);
				resumeAnimation();
			}
		}		
	}

	public void skipBackAnimation() {
		if (animationThread != null) {
			stopAnimation();
			viewer.setVisible(false);
			int skipBackFrame = viewer.getCurrentFrame() > 0 ? viewer.getCurrentFrame() - 1 : 0;
			viewer.setSkipBackFrame(skipBackFrame);
			viewer.setSkipBackAnimation(true);
			viewer.setAnimationSpeed(10.0);
			startAnimation();
		}
	}

	@Override
	public void pauseAnimation() {
		if (animationThread != null) {
			fThreadSuspended = true;
			animationThread.suspend();
		}
	}
	
	@Override
	public synchronized void resumeAnimation() {
		if (animationThread != null) {
			animationThread.resume();
			fThreadSuspended = false;
		}
	}

	@Override
	public synchronized void stopAnimation() {
		if (animationThread != null) {
			animationThread.stop();
			animationThread = null;
			viewer.setAnimationSpeed(1.0);
		}
	}

	public void startDeltaViewer(IObjectCallGraph ocg, IAliasCollector ac) {
		// Build a frame, create a graph, and add the graph to the frame so you can actually see the graph.
		if (ocg != null) {
			List<TracePoint> relatedPoints = ocg.getRelatedPoints();
			TracePoint lastRP = relatedPoints.get(relatedPoints.size() - 1);
			Map.Entry<Reference, String> rpInf = MagnetRONViewer.getRelatedInformation(lastRP, ac);
			FRAME_TITLE = "extract delta of:" + rpInf.getKey().getSrcClassName() + "(" + rpInf.getKey().getSrcObjectId() + ")" + " -> " + rpInf.getKey().getDstClassName()+ "(" + rpInf.getKey().getDstObjectId()+ ")";
			setTitle(FRAME_TITLE);
		}
		
		viewer.init(ocg, ac, new CollaborationLayout());
		List<Alias> aliasList = new ArrayList<>(ac.getAliasList());

//		dv.setCoordinatorPoint(1200, 100);
		viewer.initAnimation();
//		for(int i = 0; i < aliasList.size(); i++) {
//			System.out.println(aliasList.get(i).getObjectId() + ", " + aliasList.get(i).getMethodSignature() + " l." + aliasList.get(i).getLineNo() + " : " + aliasList.get(i).getAliasType().toString());
//		}
		for (int i = 0; i <= aliasList.size(); i++) {
			viewer.stepToAnimation(i);
		}
	}

	private void setArgmentsForDeltaExtract(Map<String, String[]> map){

		// one delta extract
		String[] traceSample1 = {null, null, "","", "traces/traceSample1.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("traceSample1", traceSample1);
		String[] traceSample2 = {null, null, "","", "traces/traceSample2.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("traceSample2", traceSample2);
		String[] presenSample = {null, null, "","", "traces/presenSample.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("presenSample", presenSample);
		String[] worstCase = {null, null, "worstCase.P", "worstCase.M", "traces/_worstCase.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("worstCase", worstCase);
		String[] sample1 = {null, null, "sample1.D", "sample1.C", "traces\\sample1.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("sample1", sample1);
		String[] sampleArray = {null, null, "sampleArray.D", "sampleArray.C", "traces\\sampleArray.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("sampleArray", sampleArray);
		String[] sampleCollection = {null, null, "sampleCollection.D", "sampleCollection.C", "traces\\sampleCollection.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("sampleCollection", sampleCollection);
		String[] sampleCreate = {null, null, "sampleCreate.D", "sampleCreate.C", "traces\\sampleCreate.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("sampleCreate", sampleCreate);
		String[] sampleStatic = {null, null, "sampleStatic.D", "sampleStatic.C", "traces\\sampleStatic.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("sampleStatic", sampleStatic);
		String[] delta_eg1 = {null, null, "E","C", "traces/testTrace.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("delta_eg1", delta_eg1);
		String[] delta_eg5 = {null, null, "E","C", "traces/testTrace5.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("delta_eg5", delta_eg5);
		String[] testTrace2 = {null, null, "","", "traces/testTrace2.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("testTrace2", testTrace2);
		String[] testTrace3 = {null, null, "","", "traces/testTrace3.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("testTrace3", testTrace3);
		String[] objTrace5 = {null, null, "A","B", "traces/objTraceSample5.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("objTrace5", objTrace5);

		// 予備実験のときに用いたToy program
		String[] pre_Exp1 = {null, null, "E","C", "traces/pre_Exp1.trace", Extract.CONTAINER_COMPONENT}; map.put("pre_Exp1", pre_Exp1);
		String[] pre_Exp2 = {null, null, "E","C", "traces/pre_Exp2.trace", Extract.CONTAINER_COMPONENT}; map.put("pre_Exp2", pre_Exp2);
		String[] pre_Exp3 = {null, null, "E","C", "traces/pre_Exp3.trace", Extract.CONTAINER_COMPONENT}; map.put("pre_Exp3", pre_Exp3);
		String[] pre_Exp4 = {null, null, "E","C", "traces/pre_Exp4.trace", Extract.CONTAINER_COMPONENT}; map.put("pre_Exp4", pre_Exp4);
		String[] pre_Exp5 = {null, null, "E","C", "traces/pre_Exp5.trace", Extract.CONTAINER_COMPONENT}; map.put("pre_Exp5", pre_Exp5);
		String[] pre_Exp6 = {null, null, "E","C", "traces/pre_Exp6.trace", Extract.CONTAINER_COMPONENT}; map.put("pre_Exp6", pre_Exp6);
		String[] pre_Exp7 = {null, null, "E","C", "traces/pre_Exp7.trace", Extract.CONTAINER_COMPONENT}; map.put("pre_Exp7", pre_Exp7);

		// Samples of Multiple Deltas
		String[] getterOverlap1 = {null, null, "getterOverlap.F","getterOverlap.D", "traces/getterOverlap.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("getterOverlap1", getterOverlap1);
		String[] getterOverlap2 = {null, null, "getterOverlap.G","getterOverlap.D", "traces/getterOverlap.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("getterOverlap2", getterOverlap2);
		String[] setterOverlap1 = {null, null, "setterOverlap.F","setterOverlap.C", "traces/setterOverlap.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("setterOverlap1", setterOverlap1);
		String[] setterOverlap2 = {null, null, "setterOverlap.G","setterOverlap.C", "traces/setterOverlap.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("setterOverlap2", setterOverlap2);
		
		String[] test = {null, null, "org.argouml.uml.diagram.static_structure.ui.FigClass","org.tigris.gef.base.LayerPerspectiveMutable", "traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("test", test);
		String[] testVectorAddElement = {null, null, "java.util.Vector", "org.tigris.gef.base.LayerPerspective", "traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("testVectorAddElement", testVectorAddElement);//trace.javaに"addElement("に対応するように追加
		String[] ArgoUMLBenchmark = {"", "", "", "", "traces/ArgoUMLBenchmark.trace", Extract.CONTAINER_COMPONENT}; map.put("ArgoUMLBenchmark", ArgoUMLBenchmark);
		String[] ArgoUMLBenchmarkWithMoreStandardClasses = {"", "", "", "", "traces/ArgoUMLBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("ArgoUMLBenchmarkWithMoreStandardClasses", ArgoUMLBenchmarkWithMoreStandardClasses);
//		String[] ArgoUMLSelect0_1 = {"1994249754", "1141430801", "java.util.ArrayList", "org.argouml.uml.diagram.static_structure.ui.SelectionClass","traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("ArgoUMLSelect0_1", ArgoUMLSelect0_1);
		String[] ArgoUMLSelect0_1 = {"1994249754", "1141430801", null, null,"traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("ArgoUMLSelect0_1", ArgoUMLSelect0_1);
		/*Vector.addElemnt()に対応済み*/String[] ArgoUMLDelete0_2 = {"1784213708", "1337038091", "java.util.Vector", "org.argouml.uml.diagram.static_structure.ui.FigClass", "traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("ArgoUMLDelete0_2", ArgoUMLDelete0_2);
		/*更に過去mouse.Pressed(), mouseReleased()*/String[] ArgoUMLPlace1_1 = {null, null, "java.util.ArrayList", "org.argouml.uml.diagram.static_structure.ui.FigClass", "traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("ArgoUMLPlace1_1", ArgoUMLPlace1_1);
//		String[] ArgoUMLSelect1_2 = {"1994249754", "1141430801", "java.util.ArrayList", "org.argouml.uml.diagram.static_structure.ui.SelectionClass", "traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("ArgoUMLSelect1_2", ArgoUMLSelect1_2);
//		String[] ArgoUMLSelect1_2 = {"125345735", "1672744985", "java.util.ArrayList", "org.argouml.uml.diagram.static_structure.ui.SelectionClass", "traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("ArgoUMLSelect1_2", ArgoUMLSelect1_2);
		String[] jEditBenchmark = {"", "", "", "", "traces/jEditBenchmark.trace", Extract.CONTAINER_COMPONENT}; map.put("jEditBenchmark", jEditBenchmark);
		String[] jEditSelect2_1 = {"932187140", "1572482885", "java.util.ArrayList", "org.gjt.sp.jedit.textarea.Selection$Range", "traces\\jEditBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("jEditSelect2_1", jEditSelect2_1);
		String[] jHotDrawBenchmark = {"", "", "", "", "traces/jHotDrawBenchmark.trace", Extract.CONTAINER_COMPONENT}; map.put("jHotDrawBenchmark", jHotDrawBenchmark);
		String[] jHotDrawBenchmarkWithMoreStandardClasses = {"", "", "", "", "traces/jHotDrawBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("jHotDrawBenchmarkWithMoreStandardClasses", jHotDrawBenchmarkWithMoreStandardClasses);
		/*List.toArray()に対応させる必要がある？*/String[] jEditDelete2_2 = {null, null, "org.gjt.sp.jedit.buffer.ContentManager", "java.util.ArrayList", "traces\\jEditBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("jEditDelete2_2", jEditDelete2_2);
		String[] jHotDrawSelect3_1 = {"361298449", "212532447", "java.util.LinkedHashSet", "org.jhotdraw.draw.RectangleFigure", "traces\\jHotDrawBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("jHotDrawSelect3_1", jHotDrawSelect3_1);
		String[] jHotDrawMove3_2 = {"778703711", "212532447", "org.jhotdraw.draw.tool.DefaultDragTracker", "org.jhotdraw.draw.RectangleFigure", "traces\\jHotDrawBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("jHotDrawMove3_2", jHotDrawMove3_2);
		/*不明*/String[] jHotDrawMove3_2_1 = {null, null, "java.util.ArrayList", "org.jhotdraw.draw.RectangleFigure", "traces\\jHotDrawBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("jHotDrawMove3_2_1", jHotDrawMove3_2_1);
		String[] jHotDrawPlace4_1 = {"1284329882", "212532447", "java.util.HashMap", "org.jhotdraw.draw.RectangleFigure", "traces\\jHotDrawBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("jHotDrawPlace4_1", jHotDrawPlace4_1);
//		String[] jHotDrawSelect4_2 = {"361298449", "212532447", "java.util.LinkedHashSet", "org.jhotdraw.draw.RectangleFigure", "traces\\jHotDrawBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("jHotDrawSelect4_2", jHotDrawSelect4_2);
		String[] jHotDrawSelect4_2 = {null, null, "java.util.LinkedHashSet", "org.jhotdraw.draw.RectangleFigure", "traces\\jHotDrawBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("jHotDrawSelect4_2", jHotDrawSelect4_2);

		// MagnetRON Experiment
		String[] ArgoUMLSelect = {"125345735", "1672744985", "java.util.ArrayList", "org.argouml.uml.diagram.static_structure.ui.SelectionClass", "traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("ArgoUMLSelect", ArgoUMLSelect); // ArgoUML 選択機能 (collection)
		String[] ArgoUMLDelete1 = {null, null, "public void org.argouml.uml.diagram.ui.ActionRemoveFromDiagram.actionPerformed(", "org.argouml.uml.diagram.static_structure.ui.FigClass", "traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace", Extract.THIS_ANOTHER}; map.put("ArgoUMLDelete1", ArgoUMLDelete1);// ArgoUML 削除機能1 (this to another)
		String[] ArgoUMLDelete2 = {"450474599", "1675174935", "java.util.Vector", "org.argouml.uml.diagram.static_structure.ui.FigClass", "traces\\ArgoUMLBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("ArgoUMLDelete2", ArgoUMLDelete2); // ArgoUML 削除機能2 (collection)
		String[] JHotDrawTransform = {"176893671", "1952912699", "java.util.HashSet", "org.jhotdraw.draw.RectangleFigure", "traces\\jHotDrawBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("JHotDrawTransform", JHotDrawTransform); // JHotDraw 移動機能 (collection)
		String[] JHotDrawSelect1 = {"758826749", "1952912699", "org.jhotdraw.draw.tool.DefaultDragTracker", "org.jhotdraw.draw.RectangleFigure", "traces\\jHotDrawBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("JHotDrawSelect1", JHotDrawSelect1); // JHotDraw 選択機能1
		String[] JHotDrawSelect2 = {"1378082106", "1952912699", "java.util.HashSet", "org.jhotdraw.draw.RectangleFigure", "traces\\jHotDrawBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("JHotDrawSelect2", JHotDrawSelect2); // JHotDraw 選択機能2 (collection)
		String[] JHotDrawSelect3 = {"599587451", "758826749", "org.jhotdraw.draw.tool.DelegationSelectionTool", "org.jhotdraw.draw.tool.DefaultDragTracker", "traces\\jHotDrawBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT}; map.put("JHotDrawSelect3", JHotDrawSelect3); // JHotDraw 選択機能3 (collection)
		String[] JHotDrawSelect4 = {"1787265837", "1952912699", "java.util.LinkedHashSet", "org.jhotdraw.draw.RectangleFigure", "traces\\jHotDrawBenchmarkWithMoreStandardClasses.trace", Extract.CONTAINER_COMPONENT_COLLECTION}; map.put("JHotDrawSelect4", JHotDrawSelect4); // JHotDraw 選択機能4 (collection)
	}
	
}
