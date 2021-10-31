package org.ntlab.deltaViewer;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ntlab.animations.MagnetRONAnimation;
import org.ntlab.animations.TranslateAnimation;
import org.ntlab.deltaExtractor.Alias;
import org.ntlab.deltaExtractor.IAliasCollector;
import org.ntlab.deltaExtractor.Alias.AliasType;
import org.ntlab.deltaViewer.Edge.TypeName;
import org.ntlab.trace.ArrayAccess;
import org.ntlab.trace.FieldAccess;
import org.ntlab.trace.FieldUpdate;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.Reference;
import org.ntlab.trace.Statement;
import org.ntlab.trace.TracePoint;

import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraphView;

public class CollaborationViewer extends MagnetRONViewer {

	private static final long serialVersionUID = 9123813231037494846L;

	// Test code (will be deleted)
	private static final String TAG = CollaborationViewer.class.getSimpleName();

	private IObjectCallGraph objectCallGraph;
	
	public CollaborationViewer() {
		super();
	}
	
	/**
	 * Set {@link IObjectCallGraph} and {@link IAliasCollector}, create {@link VertexObject} and {@link Edge}.
	 * 
	 * @param objectCallGraph
	 * @param aliasCollector
	 */
	public void init(IObjectCallGraph objectCallGraph, IAliasCollector aliasCollector, IObjectLayout layout) {
		this.objectCallGraph = objectCallGraph;
		this.aliasCollector = aliasCollector;
		createObjectVertices(this.objectCallGraph, aliasCollector);
		layout.execute(objectCallGraph, aliasCollector, objectToVertexMap);
		createEdgeToObject(this.objectCallGraph, this.aliasCollector);
	}
		
	/**
	 * Initialize animation.　(再生ボタンを押すとき)
	 */
	public void initAnimation() {
		update();
		
		Dimension graphMaxSize = new Dimension();
		for (ObjectVertex objVx: objectToVertexMap.values()) {
			double objVxInitMaxX = objVx.getInitialX() + DEFAULT_OBJECT_VERTEX_SIZE.getWidth();
			double objVxInitMaxY = objVx.getInitialY() + DEFAULT_OBJECT_VERTEX_SIZE.getHeight();
			if (objVxInitMaxX > graphMaxSize.getWidth()) {
				graphMaxSize.setSize(objVxInitMaxX, graphMaxSize.getHeight());
			}
			if (objVxInitMaxY > graphMaxSize.getHeight()) {
				graphMaxSize.setSize(graphMaxSize.getWidth(), objVxInitMaxY);
			}
		}
		mxgraph.setMinimumGraphSize(new mxRectangle(0, 0, graphMaxSize.getWidth(), graphMaxSize.getHeight()));
	}
	
	/**
	 * Step to animation of specified alias. 
	 * 
	 * @param alias: alias type and occurrence point etc
	 */
	public void stepToAnimation(Alias alias) {
		try {
			stepToAnimation(aliasCollector.getAliasList().indexOf(alias));
		} catch (IndexOutOfBoundsException e) {
			stepToAnimation(-1);	
		}
	}

	/**
	 * Parent: Step to animation of specified numFrame.
	 * 
	 * @param numFrame: current animation frame
	 */
	public void stepToAnimation(int numFrame) {
		// TODO: Fix bug in curFrame.
		System.out.println(TAG + ": Frame=" + curFrame + "->" + numFrame);
//		if (numFrame - curFrame == 1) {
			List<TracePoint> relatedPoints = objectCallGraph.getRelatedPoints();
			List<Alias> aliasList = aliasCollector.getAliasList();
			Alias curFrameAlias = (0 < numFrame && numFrame <= aliasList.size()) ? aliasList.get(numFrame- 1) : null;
			Alias numFrameAlias = (0 <= numFrame && numFrame < aliasList.size()) ? aliasList.get(numFrame) : null;

			if (numFrameAlias != null) {
				if (curFrameAlias != null) {
					int i = 0;
					for (Long rpTime:objectCallGraph.getTimeStamps()) {
						if (curFrameAlias.getTimeStamp() <= rpTime && rpTime < numFrameAlias.getTimeStamp()) {
							System.out.println("\r\n" + TAG + ": Last Animation.");
							TracePoint rp = relatedPoints.get(i);
							doLastAnimation(numFrame, rp);	
							return;
						}
						i++;
					}
				}
				doAnimation(curFrame, numFrame);
			} else if (curFrameAlias != null && numFrameAlias == null) {
				System.out.println("\r\n" + TAG + ": Last Animation.");
				doLastAnimation(numFrame, relatedPoints.get(relatedPoints.size() - 1));	
			} else {
				System.out.println(TAG + ": ERROR Not exist alias.");
			}
//		} else {
			// TODO: Considering fast-forwarding animations. 			
//		}
	}

	private void doLastAnimation(int numFrame, TracePoint relatedPoint) {
		// TODO: Implement doLastAnimation() to support multiple delta.
		setCurrentFrame(numFrame);
		List<Alias> aliasList = aliasCollector.getAliasList();
		Alias prevAlias = aliasList.get(numFrame - 1);
		Alias nextAlias = (numFrame < aliasList.size()) ? aliasList.get(numFrame) : null;

		// Make Edge object and reset position of ObjectVertex, remove MethodExecutionVertex.
		Statement statement = relatedPoint.getStatement();
		MethodExecution prevMethodExec = prevAlias.getMethodExecution();
		boolean fThisAnotherParameter = false;
		if (relatedPoint.isMethodEntry()) {
			if (prevAlias.getAliasType() == Alias.AliasType.FORMAL_PARAMETER) {
				// this to another (parameter)
				MethodExecution calledMethodExec = relatedPoint.getMethodExecution();
				String srcClassName = calledMethodExec.getThisClassName();
				String srcObjId = calledMethodExec.getThisObjId();
				String tgtObjId = prevAlias.getObjectId();
				ObjectVertex tgtObjVx = objectToVertexMap.get(tgtObjId);
				
				createObjectRefrence(srcClassName, srcObjId, tgtObjId);
				
				if (methodExecToVertexMap.containsKey(prevMethodExec)) {
					MethodExecutionVertex prevMethodExecVx = methodExecToVertexMap.get(prevMethodExec);
					if (prevMethodExecVx.getLocals().contains(tgtObjVx)) {
						prevMethodExecVx.getLocals().remove(tgtObjVx);
					} else if (prevMethodExecVx.getArguments().contains(tgtObjVx)) {
						prevMethodExecVx.getArguments().remove(tgtObjVx);
					}
				}
				
				if (nextAlias == null 
						|| (nextAlias != null && !calledMethodExec.getSignature().equals(nextAlias.getMethodSignature()))) {
					removeCalledMethodExecutionVertex(objectToVertexMap.get(srcObjId), prevMethodExec, calledMethodExec);
					updateObjectVertices();
				}
				fThisAnotherParameter = true;
			}
		}
		if (!fThisAnotherParameter) {
			if (statement instanceof FieldUpdate) {
				// Format fieldName.
				FieldUpdate fieldUpdateStatement = (FieldUpdate) statement;
				String fieldName;
				if (fieldUpdateStatement.getFieldName() != null) {
					String fieldNames[] = formatFieldName(fieldUpdateStatement.getFieldName());
					fieldName = fieldNames[fieldNames.length-1];
				} else {
					fieldName = "";
				}
				String srcObjId = fieldUpdateStatement.getContainerObjId();
				
				createObjectRefrence(fieldUpdateStatement, fieldName);
	
				String tgtObjId = fieldUpdateStatement.getValueObjId();
				ObjectVertex tgtObjVx = objectToVertexMap.get(tgtObjId);
	
				if (methodExecToVertexMap.containsKey(prevMethodExec)) {
					MethodExecutionVertex prevMethodExecVx = methodExecToVertexMap.get(prevMethodExec);
					if (prevMethodExecVx.getLocals().contains(tgtObjVx)) {
						prevMethodExecVx.getLocals().remove(tgtObjVx);
					} else if (prevMethodExecVx.getArguments().contains(tgtObjVx)) {
						prevMethodExecVx.getArguments().remove(tgtObjVx);
					}
				}
				
				if (nextAlias == null || (nextAlias != null && !prevMethodExec.getSignature().equals(nextAlias.getMethodSignature()))) {
					removeCalledMethodExecutionVertex(objectToVertexMap.get(srcObjId), prevMethodExec.getCallerMethodExecution(), prevMethodExec);
					updateObjectVertices();
				}
			}
	
			if(statement instanceof MethodInvocation) {
				MethodInvocation methodInvStatement = (MethodInvocation) statement;
				MethodExecution calledMethodExec = methodInvStatement.getCalledMethodExecution();
				String methodSignature = calledMethodExec.getSignature();
				String srcClassName = null;
				String srcObjId = null;
				String tgtObjId = null;
	
				//ArrayやListのときだけラベルを付ける（確実に分かっているものとき)getSignature->contains("List.get(") || "Map.get(") <ホワイトリスト>
//				if (methodExec.getSignature().contains("List.add(") ||
//						methodExec.getSignature().contains("Map.put(")) {
				if (calledMethodExec.isCollectionType()
						&& (methodSignature.contains("add(") 
								|| methodSignature.contains("set(") 
								|| methodSignature.contains("put(") 
								|| methodSignature.contains("push(") 
								|| methodSignature.contains("addElement("))) {
	
					srcClassName = calledMethodExec.getThisClassName();
					srcObjId = calledMethodExec.getThisObjId();
					tgtObjId = calledMethodExec.getArguments().get(0).getId();
					ObjectVertex tgtObjVx = objectToVertexMap.get(tgtObjId);
	
					if (!methodExecToVertexMap.containsKey(calledMethodExec)) {
						createMethodExecutionVertex(calledMethodExec.getThisObjId(), methodInvStatement.getCallerSideMethodName(), calledMethodExec);
						update();
						moveArgumentObjectVertex(calledMethodExec, tgtObjVx, methodExecToVertexMap.get(calledMethodExec));
						update();
						createObjectRefrence(srcClassName, srcObjId, tgtObjId);
						removeCalledMethodExecutionVertex(null, prevMethodExec, calledMethodExec);
					} else {
						createObjectRefrence(srcClassName, srcObjId, tgtObjId);
					}
					
					if (methodExecToVertexMap.containsKey(prevMethodExec)) {
						MethodExecutionVertex prevMethodExecVx = methodExecToVertexMap.get(prevMethodExec);
						if (prevMethodExecVx.getLocals().contains(tgtObjVx)) {
							prevMethodExecVx.getLocals().remove(tgtObjVx);
						} else if (prevMethodExecVx.getArguments().contains(tgtObjVx)) {
							prevMethodExecVx.getArguments().remove(tgtObjVx);
						}
					}
					
					List<MethodExecution> methodExecList = new ArrayList<>(methodExecToVertexMap.keySet());
					if (nextAlias == null || (nextAlias != null && !prevMethodExec.getSignature().equals(nextAlias.getMethodSignature()))) {
						if (methodExecList.size() > 1) {
							removeCalledMethodExecutionVertex(null, prevMethodExec.getCallerMethodExecution(), prevMethodExec);
						} else {
							removeCalledMethodExecutionVertex(null, null, prevMethodExec);						
						}
					}
					updateObjectVertices();
				} else {
					// this to another
					srcClassName = methodInvStatement.getThisClassName();
					srcObjId = methodInvStatement.getThisObjId();
					tgtObjId = calledMethodExec.getReturnValue().getId();
					ObjectVertex tgtObjVx = objectToVertexMap.get(tgtObjId);
					
					createObjectRefrence(srcClassName, srcObjId, tgtObjId);
					
					if (methodExecToVertexMap.containsKey(prevMethodExec)) {
						MethodExecutionVertex prevMethodExecVx = methodExecToVertexMap.get(prevMethodExec);
						if (prevMethodExecVx.getLocals().contains(tgtObjVx)) {
							prevMethodExecVx.getLocals().remove(tgtObjVx);
						} else if (prevMethodExecVx.getArguments().contains(tgtObjVx)) {
							prevMethodExecVx.getArguments().remove(tgtObjVx);
						}
					}
					
					if (nextAlias == null || (nextAlias != null && !calledMethodExec.getSignature().equals(nextAlias.getMethodSignature()))) {
						removeCalledMethodExecutionVertex(objectToVertexMap.get(srcObjId), prevMethodExec, calledMethodExec);
						updateObjectVertices();
					}
				}
	
			}
		}
		
		// Remove MethodExecutionVertex and reset position of ObjectVertex of MethodExecution remaining in methodExecToVertexMap.
		List<MethodExecution> methodExecList = new ArrayList<>(methodExecToVertexMap.keySet());
		Collections.reverse(methodExecList);
		for(int i = 0; i < methodExecList.size(); i++) {
			String objId = methodExecList.get(i).getThisObjId();
			ObjectVertex srcObjVx = objectToVertexMap.get(objId); // sourceVertex
			MethodExecution methodExec = methodExecList.get(i);
			
			if (nextAlias != null && methodExec.getSignature().equals(nextAlias.getMethodSignature())) break;
			
			if (i != methodExecList.size() - 1) {
				for(Statement st: methodExec.getStatements()) {
					if(st instanceof MethodInvocation) {
						MethodExecution calledMethodExec = ((MethodInvocation) st).getCalledMethodExecution();
						String calledObjId = calledMethodExec.getThisObjId();
						if(objectToVertexMap.containsKey(calledObjId)) {
							removeCalledMethodExecutionVertex(srcObjVx, methodExec.getCallerMethodExecution(), methodExec);
							updateObjectVertices();
							break;
						}
					}
				}
			}  else {
				scrollCellToVisible((mxICell) srcObjVx.getCell(), true);

				List<ObjectVertex> arguments = new ArrayList<>(methodExecToVertexMap.get(methodExec).getArguments());
				List<ObjectVertex> locals = new ArrayList<>(methodExecToVertexMap.get(methodExec).getLocals());
				if (arguments.size() != 0) {
					for (ObjectVertex objVx: arguments) {
						mxICell objVxCell = (mxICell) objVx.getCell();
						Point2D objVxCellAbsPt = getAbsolutePointforCell(objVxCell);
						// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
						mxgraph.getModel().beginUpdate();
						synchronized (mxgraph.getModel()) {
							try {
								if (!objVxCell.getParent().equals(getMxDefaultParent())) {
									// If parent of ObjectVertex cell isn't mxDefaltParent, reset parent.
									objVxCell.getParent().remove(objVxCell);
									objVxCell.setParent(getMxDefaultParent());
								}
								objVxCell.getGeometry().setX(objVxCellAbsPt.getX());
								objVxCell.getGeometry().setY(objVxCellAbsPt.getY());
							} finally {
								mxgraph.getModel().endUpdate();
							}
						}

						MagnetRONAnimation pbjVxCellAnim = new TranslateAnimation(mxgraph, getGraphComponent());
						pbjVxCellAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
						pbjVxCellAnim.setDelay(getMagnetRONAnimationDelayMillis());
						pbjVxCellAnim.init(objVxCell, objVx.getInitialX(), objVx.getInitialY(), threadPoolExecutor);
						pbjVxCellAnim.syncPlay();
						methodExecToVertexMap.get(methodExec).getArguments().remove(objVx);
					}
				} else if (locals.size() != 0) {
					for (ObjectVertex objVx: locals) {
						mxICell objVxCell = (mxICell) objVx.getCell();
						Point2D objVxCellAbsPt = getAbsolutePointforCell(objVxCell);
						// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
						mxgraph.getModel().beginUpdate();
						synchronized (mxgraph.getModel()) {
							try {
								if (!objVxCell.getParent().equals(getMxDefaultParent())) {
									// If parent of ObjectVertex cell isn't mxDefaltParent, reset parent.
									objVxCell.getParent().remove(objVxCell);
									objVxCell.setParent(getMxDefaultParent());
								}
								objVxCell.getGeometry().setX(objVxCellAbsPt.getX());
								objVxCell.getGeometry().setY(objVxCellAbsPt.getY());
							} finally {
								mxgraph.getModel().endUpdate();
							}
						}
						MagnetRONAnimation objVxCellAnim = new TranslateAnimation(mxgraph, getGraphComponent());
						objVxCellAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
						objVxCellAnim.setDelay(getMagnetRONAnimationDelayMillis());
						objVxCellAnim.init(objVxCell, objVx.getInitialX(), objVx.getInitialY(), threadPoolExecutor);
						objVxCellAnim.syncPlay();
						methodExecToVertexMap.get(methodExec).getLocals().remove(objVx);
					}
				}
				updateObjectVertices();	    			
			}
		}
		update();
	}

	/**
	 * Create vertices(mxGraph) and OvjectVerticies in {@code objectToVertexMap}. Vertices(mxGraph) coordinate are appropriate. 
	 * 
	 * @param objectCallGraph
	 * @param aliasCollector 
	 */
	private void createObjectVertices(IObjectCallGraph objectCallGraph, IAliasCollector aliasCollector) {
		//Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
		mxgraph.getModel().beginUpdate();
		try {
			// Create vertices(mxGraph) and OvjectVerticies.
			List<Reference> refList = objectCallGraph.getReferences();
			double objVxWid = DEFAULT_OBJECT_VERTEX_SIZE.getWidth();
			double ObjVxHt = DEFAULT_OBJECT_VERTEX_SIZE.getHeight();
			
			{
				MethodExecution coordinator = objectCallGraph.getStartPoints().get(0);
				String coordinatorObjId = coordinator.getThisObjId();
				String coordinatorClassName = coordinator.getThisClassName();
				mxICell coordinatorObjVxCell = (mxICell) mxgraph.insertDeltaVertex(getMxDefaultParent(), coordinatorObjId, formatClassName(coordinatorClassName), 0, 0, DEFAULT_OBJECT_VERTEX_SIZE.getWidth(), DEFAULT_OBJECT_VERTEX_SIZE.getHeight(), "fillColor=white"); //creates a white vertex. 
				objectToVertexMap.put(coordinatorObjId, new ObjectVertex(coordinatorClassName, coordinatorObjVxCell, 0, 0));
			}
			
			for (int i = 0; i < refList.size(); i++) {
				Reference ref = refList.get(i);
				if (!ref.isCreation() && !ref.getSrcObjectId().equals(ref.getDstObjectId())) {
					// srcSide
					if (!objectToVertexMap.containsKey(ref.getSrcObjectId())) {
						String srcObjId = ref.getSrcObjectId();
						String srcClassName = ref.getSrcClassName();
						System.out.println(TAG + ": Source Side(ClassName=" + srcClassName + ", ObjectId=" + srcObjId + ", isCreation=" + ref.isCreation() + ")");
						if (srcClassName.contains("[L")) {
							srcClassName = formatArrayName(srcClassName);
						}
						mxICell srcObjVxCell = (mxICell) mxgraph.insertDeltaVertex(getMxDefaultParent(), srcObjId, formatClassName(srcClassName), 0, 0, objVxWid, ObjVxHt, "fillColor=white"); //creates a white vertex. 
						objectToVertexMap.put(srcObjId, new ObjectVertex(ref.getSrcClassName(), srcObjVxCell, 0, 0));
					}
					// dstSide
					if (!objectToVertexMap.containsKey(ref.getDstObjectId())) {
						String dstObjId = ref.getDstObjectId();
						String dstClassName = ref.getDstClassName();
						System.out.println(TAG + ": Destination Side(ClassName=" + dstClassName + ", ObjectId=" + dstObjId + ")");
						if (dstClassName.contains("[L")) {
							dstClassName = formatArrayName(dstClassName);
						}
						mxICell dstObjVxCell = (mxICell) mxgraph.insertDeltaVertex(getMxDefaultParent(), dstObjId, formatClassName(dstClassName), 0, 0, objVxWid, ObjVxHt, "fillColor=white"); //creates a white vertex. 		
						objectToVertexMap.put(dstObjId, new ObjectVertex(ref.getDstClassName(), dstObjVxCell, 0, 0));							
					}
				} else {
					if (!objectToVertexMap.containsKey(ref.getSrcObjectId())) {
						String srcObjId = ref.getSrcObjectId();
						String srcClassName = ref.getSrcClassName();
						if (srcClassName.contains("[L")) {
							srcClassName = formatArrayName(srcClassName);
						}
						mxICell srcObjVxCell = (mxICell) mxgraph.insertDeltaVertex(getMxDefaultParent(), srcObjId, formatClassName(srcClassName), 0, 0, objVxWid, ObjVxHt, "fillColor=white"); //creates a white vertex. 
						objectToVertexMap.put(srcObjId, new ObjectVertex(ref.getSrcClassName(), srcObjVxCell, 0, 0));
					}
					String dstObjId = ref.getDstObjectId();
					if (!objectToVertexMap.containsKey(dstObjId)) {
						String dstClassName = ref.getDstClassName();
						objectToVertexMap.put(dstObjId, new ObjectVertex(dstClassName, null, 0, 0));							
					} else {
						mxgraph.removeCells(new Object[] {objectToVertexMap.get(dstObjId).getCell()});
						objectToVertexMap.get(dstObjId).setCell(null);
					}
				}
			}
			for (Alias alias: aliasCollector.getAliasList()) {
				if (alias.getAliasType() == Alias.AliasType.THIS) {
					if (!objectToVertexMap.containsKey(alias.getObjectId()) && alias.getMethodExecution().isStatic()) {
						// When both of the calling and called methods are static.
						String thisObjId = alias.getObjectId();
						String thisClassName = alias.getMethodExecution().getThisClassName();
						mxICell thisObjVxCell = (mxICell) mxgraph.insertDeltaVertex(getMxDefaultParent(), thisObjId, formatClassName(thisClassName), 0, 0, objVxWid, ObjVxHt, "fillColor=white"); //creates a white vertex. 
						objectToVertexMap.put(thisObjId, new ObjectVertex(thisClassName, thisObjVxCell, 0, 0));
					}
				} else if (alias.getAliasType() == Alias.AliasType.FORMAL_PARAMETER) {
					String thisClassName = alias.getMethodExecution().getThisClassName();
					String thisObjId = alias.getMethodExecution().getThisObjId();
					if (thisObjId.matches("0")) {
						thisObjId += ":" + thisClassName;
					}
					if (!objectToVertexMap.containsKey(thisObjId)) {
						// When the called method is static.
						mxICell vertex = (mxICell) mxgraph.insertDeltaVertex(getMxDefaultParent(), thisObjId, formatClassName(thisClassName), 0, 0, objVxWid, ObjVxHt, "fillColor=white"); //creates a white vertex. 
						objectToVertexMap.put(thisObjId, new ObjectVertex(thisClassName, vertex, 0, 0));
					}
				}
			}
		} finally {
			mxgraph.getModel().endUpdate();
		}	
	}

	/**
	 * Create {@code MethodExecutionVertex}. 
	 * Be careful to refer {@link IObjectCallGraph#getStartPoints()} when curFrame is 0.
	 * 
	 * @param alias
	 */
	@Override
	public void createMethodExecutionVertex(Alias alias) {
		String objId = alias.getObjectId();
		MethodExecution methodExec = alias.getMethodExecution();
		String methodExecSignature = methodExec.getSignature();

		if (curFrame == 0) {
			MethodExecution callerMethodExec = methodExec.getCallerMethodExecution();
			if (callerMethodExec != null 
					&& methodExecSignature != callerMethodExec.getSignature() 
					&& objectToVertexMap.containsKey(callerMethodExec.getThisObjId()) 
					&& objectCallGraph.getStartPoints().get(0) == callerMethodExec) {
				createMethodExecutionVertex(callerMethodExec.getThisObjId(), callerMethodExec.getSignature(), callerMethodExec);
			}
			createMethodExecutionVertex(objId, methodExecSignature, methodExec);
		} else if (alias.getObjectId().startsWith("0:") && !methodExecToVertexMap.containsKey(methodExec)) {
			createMethodExecutionVertex(objId, methodExecSignature, methodExec);
		}		
	}

	/**
	 * 
	 * @param objectCallGraph
	 * @param aliasCollector
	 */
	private void createEdgeToObject(IObjectCallGraph objectCallGraph, IAliasCollector aliasCollector) {
		List<TracePoint> relatedPoints = objectCallGraph.getRelatedPoints();
		int rpIndex = 0; // relatedPoints index
		List<Alias> aliasList = aliasCollector.getAliasList();

		for (int i = 0; i < aliasList.size()-1; i++) {
			Alias curAlias = aliasList.get(i);
			Alias nextAlias = aliasList.get(i+1);
			String curAliasObjId = curAlias.getObjectId(); // srcObjId
			String nextAliasObjId = nextAlias.getObjectId(); // dstObjId
			String srcClassName = null;
			String fieldName = null;
			
			// Search for objectReference srcClassName, fieldName between curAlias and nexAlias.
			if (curAlias.getAliasType() == AliasType.THIS) {
				Statement statement = nextAlias.getOccurrencePoint().getStatement();
				if (nextAlias.getAliasType() == AliasType.RETURN_VALUE) {
					MethodExecution nextMethodExec = nextAlias.getMethodExecution();
					//ArrayやListのときだけラベルを付ける（確実に分かっているものとき)getSignature->contains("List.get(") || "Map.get(") <ホワイトリスト>
					if (nextMethodExec.getSignature().contains("List.get(")) {
						srcClassName = nextMethodExec.getThisClassName();
						fieldName = nextMethodExec.getArguments().get(0).getId();
						System.out.println(TAG + ": Create List of Edge.(ClassName=" + srcClassName + ", ArgumentId=" + nextMethodExec.getArguments().get(0).getId() + ")");
					} else if (nextMethodExec.getSignature().contains("Map.get(")) {
						srcClassName = nextMethodExec.getThisClassName();
						fieldName = "";
						System.out.println(TAG + ": Create Map of Edge.(ClassName=" + srcClassName + ")");
					} else if (nextMethodExec.getSignature().contains(".next()") || nextMethodExec.getSignature().contains(".iterator()") || nextMethodExec.getSignature().contains(".listIterator()")) {
						srcClassName = nextMethodExec.getThisClassName();
						fieldName = "";
						System.out.println(TAG + ": Create List Iterator of Edge.(ClassName=" + srcClassName + ")");
					}
				} else {
					if (nextAlias.getAliasType() == AliasType.FIELD) { // When final local.
						if (statement != null && statement instanceof MethodInvocation && curAliasObjId.equals(((MethodInvocation)statement).getThisObjId())) {						
							MethodInvocation methodInvocation = (MethodInvocation)statement;
							srcClassName = methodInvocation.getThisClassName();
							fieldName = nextAliasObjId;
						}
					}
					if (statement != null && statement instanceof FieldAccess) {
						FieldAccess fieldAccess = (FieldAccess)statement;
						String containerId = fieldAccess.getContainerObjId();
						if (fieldAccess.getFieldName() != null) {
							String fieldNames[] = formatFieldName(fieldAccess.getFieldName());
							srcClassName = fieldNames[0];
							fieldName = fieldNames[1];
						} else {
							srcClassName = fieldAccess.getContainerClassName();
							fieldName = nextAliasObjId;
						}
						if (containerId.matches("0")) {
							containerId = "0:" + srcClassName;
						}
						if  (!curAliasObjId.equals(containerId)) { // Contains alias type of nextAlias is FIELD.
							srcClassName = null;
							fieldName = null;
						}
					}
				}
				System.out.println(TAG + ": srcClassName=" + srcClassName + "(curObjectId=" + curAliasObjId + ") -- fieldName=" + fieldName + " --> " + "(nextObjectId" + nextAliasObjId + ")");
			}
			
			if(curAlias.getAliasType() == AliasType.ARRAY) {
				Statement statement= nextAlias.getOccurrencePoint().getStatement();
				if(statement instanceof ArrayAccess) {
					srcClassName = ((ArrayAccess)statement).getArrayClassName();
					int index = ((ArrayAccess)statement).getIndex();
					fieldName = formatArrayIndex(index);
					System.out.println(TAG + ": Create Array of Edge. srcClassName=" + srcClassName + "(curObjectId=" + curAliasObjId + ") -- fieldName=" + fieldName + " --> " + "(nextObjectId" + nextAliasObjId + ")");
				}
			}
			
			
			if (srcClassName != null && fieldName != null && !edgeMap.containsKey(curAliasObjId + "." + fieldName)) {
				// Judge AliasList contains relatedPoint. (If contains not to create edge.)
				if (rpIndex < relatedPoints.size() - 1) {
					TracePoint rp = relatedPoints.get(rpIndex);
					Map.Entry<Reference, String> rpInf = getRelatedInformation(rp, aliasCollector);
					if (srcClassName.equals(rpInf.getKey().getSrcClassName()) && fieldName.equals(rpInf.getValue()) && curAliasObjId.equals(rpInf.getKey().getSrcObjectId()) && nextAliasObjId.equals(rpInf.getKey().getDstObjectId())) {
						rpIndex++;
						continue;
					}
				}

				// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
				mxgraph.getModel().beginUpdate();
				try {
					Object srcObjVxCell = objectToVertexMap.get(curAliasObjId).getCell();
					Object dstObjVxCell = objectToVertexMap.get(nextAliasObjId).getCell();
					if (srcObjVxCell != null && dstObjVxCell != null) { // isCreation()
						System.out.println(TAG + ": Create Edge. srcClassName=" + srcClassName + "(curObjectId=" + curAliasObjId + ") -- fieldName=" + fieldName + " --> " + "(nextObjectId" + nextAliasObjId + ")");
						// TODO: Fix bug of NullPointerException.
						mxICell edgeCell = null;
						if (fieldName.equals(nextAliasObjId)) { // If fieldName is objectId.
							edgeCell = (mxICell) mxgraph.insertDeltaEdge(getMxDefaultParent(), "", "", srcObjVxCell, dstObjVxCell);							
						} else {
							edgeCell = (mxICell) mxgraph.insertDeltaEdge(getMxDefaultParent(), fieldName, fieldName, srcObjVxCell, dstObjVxCell);
						}
						edgeMap.put(curAliasObjId + "." + fieldName, new Edge(fieldName, TypeName.Reference, edgeCell));
					} else {
						edgeMap.put(curAliasObjId + "." + fieldName, new Edge(fieldName, TypeName.PreReference, curAliasObjId, nextAliasObjId));
					}
				} finally {
					mxgraph.getModel().endUpdate();
				}
			}
		}		
	}
}
