package org.ntlab.deltaViewer;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ntlab.animations.MagnetRONAnimation;
import org.ntlab.animations.TranslateAnimation;
import org.ntlab.deltaExtractor.Alias;
import org.ntlab.deltaExtractor.Delta;
import org.ntlab.deltaExtractor.ExtractedStructure;
import org.ntlab.deltaExtractor.IAliasCollector;
import org.ntlab.deltaViewer.Edge.TypeName;
import org.ntlab.deltaExtractor.Alias.AliasType;
import org.ntlab.trace.ArrayAccess;
import org.ntlab.trace.FieldAccess;
import org.ntlab.trace.FieldUpdate;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.Reference;
import org.ntlab.trace.Statement;

import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraphView;

/**
 * Make and display JGraph of extracted delta.
 * 
 * @author Nitta Lab.
 */
//Careful: Parent
//TODO BUG: edge drawing order. -> parent
//TODO BUG: methodExecution drawing order. -> parent
public class DeltaViewer extends MagnetRONViewer {
	private ExtractedStructure eStructure;
	private mxPoint coordinatorPoint = new mxPoint(0, 100);

	private double scale = 1;

	public DeltaViewer() {
		super();
	}
	
	/**
	 * Set extractedStructure and aliasCollector, create vertex object and edge object.
	 * @param extractedStructure
	 * @param aliasCollector
	 */
	public void init(ExtractedStructure extractedStructure, IAliasCollector aliasCollector) {
		this.eStructure = extractedStructure;
		this.aliasCollector = aliasCollector;
		createObjectVertices(eStructure);
		createEdgeToObject(this.aliasCollector.getAliasList());
	}

	/**
	 * Initialize animation.　(再生ボタンを押すとき)
	 */
	public void initAnimation() {
		// Fit graph size in visible JFrame.
		mxGraphView view = mxgraphComponent.getGraph().getView();
		int componentWidth = mxgraphComponent.getWidth() - 25;
		double viewWidth = (double) view.getGraphBounds().getWidth();

//		Object component = mxgraph.insertDeltaVertex(mxDefaultParent, "component", "component", "fillColor=white"); //creates a white vertex.
//		((mxICell)component).getGeometry().setX(mxgraphComponent.getWidth() - 30);
//		((mxICell)component).getGeometry().setY(10);
//		Object vertex = mxgraph.insertDeltaVertex(mxDefaultParent, "view", "view", "fillColor=white"); //creates a white vertex.
//		((mxICell)vertex).getGeometry().setX(view.getGraphBounds().getWidth());
//		((mxICell)vertex).getGeometry().setY(10);

		System.out.print("Scale " + componentWidth + ", " + viewWidth + ", " + coordinatorPoint.getX());
		if (viewWidth < coordinatorPoint.getX()) {
			viewWidth += coordinatorPoint.getX();
		}
//		scale = (double)componentWidth/viewWidth;
		System.out.println(", " + scale);
//		scale = 1.5;
		view.setScale(scale);
//		deltaAnimation.setScale(scale);
		update();
	}

	/**
	 * Step to animation of specified alias. 
	 * 
	 * @param alias Alias type and occurrence point etc.
	 */
	public void stepToAnimation(Alias alias) {
		try {
			stepToAnimation(aliasCollector.getAliasList().indexOf(alias));
		} catch (IndexOutOfBoundsException e) {
			stepToAnimation(-1);	
		}
	}

	/**
	 * Parent : Step to animation of specified numFrame.
	 * 
	 * @param numFrame Current animation frame.
	 */
	public void stepToAnimation(int numFrame) {
		if (aliasCollector.getAliasList().size() > numFrame) {
			doAnimation(curFrame, numFrame);
		} else if (aliasCollector.getAliasList().size() == numFrame){
			System.out.println("\r\nLast Animation.");
			doLastAnimation(numFrame);	
		} else {
			System.out.println("ERROR : Not exist alias.");
		}
	}

	/**
	 * Make last animation of extracted delta.
	 * 
	 * @param numFrame Index of aliasList size.
	 */
	private void doLastAnimation(int numFrame) {
		outputLog();
		curFrame = numFrame;	    	
		Alias alias = aliasCollector.getAliasList().get(numFrame - 1);

		// Make ObjectEdge and reset position of vertexObject, remove vertexMethodExecution.
//		for(Statement statement: alias.getMethodExecution().getStatements()) 
		{
			Statement statement = eStructure.getRelatedTracePoint().getStatement();
			MethodExecution methodExec = alias.getMethodExecution();
			if(statement instanceof FieldUpdate) {
				// Format fieldName.
				FieldUpdate fieldUpdateStatement = (FieldUpdate) statement;
				String fieldName;
				if (fieldUpdateStatement.getFieldName() != null) {
					String fieldNames[] = formatFieldName(fieldUpdateStatement.getFieldName());
					fieldName = fieldNames[fieldNames.length-1];
				} else {
					fieldName = "";
				}
				String sourceObjectId = fieldUpdateStatement.getContainerObjId();

				createObjectRefrence(fieldUpdateStatement, fieldName);

				// Change!
				String targetObjectId = fieldUpdateStatement.getValueObjId();
				ObjectVertex targetObjectVertex = objectToVertexMap.get(targetObjectId);

				if (methodExecToVertexMap.containsKey(methodExec)) {
					if (methodExecToVertexMap.get(methodExec).getLocals().contains(targetObjectVertex)) {
						methodExecToVertexMap.get(methodExec).getLocals().remove(targetObjectVertex);
						System.out.println(methodExecToVertexMap.get(methodExec).getLabel() + " :removeLocal: " + targetObjectVertex.getLabel());
					} else if (methodExecToVertexMap.get(methodExec).getArguments().contains(targetObjectVertex)) {
						methodExecToVertexMap.get(methodExec).getArguments().remove(targetObjectVertex);
						System.out.println(methodExecToVertexMap.get(methodExec).getLabel() + " :removeArgument: " + targetObjectVertex.getLabel());
					}
				}

				removeCalledMethodExecutionVertex(objectToVertexMap.get(sourceObjectId), methodExec.getCallerMethodExecution(), methodExec);
				updateObjectVertices();
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
					ObjectVertex tgtObjectVertex = objectToVertexMap.get(tgtObjId);

					createObjectRefrence(srcClassName, srcObjId, tgtObjId);
					if (methodExecToVertexMap.containsKey(methodExec)) {
						if (methodExecToVertexMap.get(methodExec).getLocals().contains(tgtObjectVertex)) {
							methodExecToVertexMap.get(methodExec).getLocals().remove(tgtObjectVertex);
							System.out.println(methodExecToVertexMap.get(methodExec).getLabel() + " :removeLocal: " + tgtObjectVertex.getLabel());
						} else if (methodExecToVertexMap.get(methodExec).getArguments().contains(tgtObjectVertex)) {
							methodExecToVertexMap.get(methodExec).getArguments().remove(tgtObjectVertex);
							System.out.println(methodExecToVertexMap.get(methodExec).getLabel() + " :removeArgument: " + tgtObjectVertex.getLabel());
						}
					}
					List<MethodExecution> methodExecList = new ArrayList<>(methodExecToVertexMap.keySet());
					System.out.println(methodExecList.size());
					if (methodExecList.size() > 1) {
						removeCalledMethodExecutionVertex(null, methodExec.getCallerMethodExecution(), methodExec);
					} else {
						removeCalledMethodExecutionVertex(null, null, methodExec);						
					}
					updateObjectVertices();
				} else {
					// this to another
					srcClassName = methodInvStatement.getThisClassName();
					srcObjId = methodInvStatement.getThisObjId();
					tgtObjId = calledMethodExec.getReturnValue().getId();
					ObjectVertex tgtObjectVertex = objectToVertexMap.get(tgtObjId);

					createObjectRefrence(srcClassName, srcObjId, tgtObjId);
					if (methodExecToVertexMap.containsKey(methodExec)) {
						if (methodExecToVertexMap.get(methodExec).getLocals().contains(tgtObjectVertex)) {
							methodExecToVertexMap.get(methodExec).getLocals().remove(tgtObjectVertex);
							System.out.println(methodExecToVertexMap.get(methodExec).getLabel() + " :removeLocal: " + tgtObjectVertex.getLabel());
						} else if (methodExecToVertexMap.get(methodExec).getArguments().contains(tgtObjectVertex)) {
							methodExecToVertexMap.get(methodExec).getArguments().remove(tgtObjectVertex);
							System.out.println(methodExecToVertexMap.get(methodExec).getLabel() + " :removeArgument: " + tgtObjectVertex.getLabel());
						}
					}
					removeCalledMethodExecutionVertex(objectToVertexMap.get(srcObjId), methodExec, calledMethodExec);
					updateObjectVertices();
				}

			}
		}

//		MethodExecution tempMethodExec = alias.getMethodExecution();
//		//ArrayやListのときだけラベルを付ける（確実に分かっているものとき)getSignature->contains("List.get(") || "Map.get(") <ホワイトリスト>
//		if (tempMethodExec.getSignature().contains("List.add(") ||
//				tempMethodExec.getSignature().contains("Map.put(")) {
//			String srcClassName = tempMethodExec.getThisClassName();
//			String fieldName = tempMethodExec.getArguments().get(0).getId();
//			System.out.println("rTHIS " + srcClassName + ", " + fieldName);
//		}

//		Statement tempStatement = alias.getOccurrencePoint().getStatement(); -> MethodInvocation
//		if(tempStatement instanceof FieldAccess) {
//			FieldAccess fieldAccessStatement = (FieldAccess) tempStatement;
//			String fieldNames[] = formatFieldName(fieldAccessStatement.getFieldName());
//			String srcClassName = fieldNames[0];
//			String fieldName = fieldNames[1];
//			String sourceObjectId = fieldAccessStatement.getContainerObjId();
//			System.out.println(fieldName);
//			createObjectRefrence(fieldAccessStatement, fieldName);
//			removeCalledMethodExecutionVertex(objectToVertexMap.get(sourceObjectId), alias.getMethodExecution().getCallerMethodExecution(), alias.getMethodExecution());
//			updateObjectVertices();
//		}

		// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
		mxgraph.getModel().beginUpdate();
		try {
			List<MethodExecution> methodExecList = new ArrayList<>(methodExecToVertexMap.keySet());
			Collections.reverse(methodExecList);
			System.out.println(methodExecList.size());
			for(int i = 0; i < methodExecList.size(); i++) {
				String objectId = methodExecList.get(i).getThisObjId();
				ObjectVertex sourceVertexObject = objectToVertexMap.get(objectId); // sourceVertex
				MethodExecution methodExec = methodExecList.get(i);
				if (i != methodExecList.size()-1) {
					for(Statement statement: methodExec.getStatements()) {
						if(statement instanceof MethodInvocation) {
							MethodExecution calledMethodExec = ((MethodInvocation) statement).getCalledMethodExecution();
							String calledObjectId = calledMethodExec.getThisObjId();
							System.out.println(calledObjectId);
							if(objectToVertexMap.containsKey(calledObjectId)) {
								mxICell calledCell = (mxICell)objectToVertexMap.get(calledObjectId).getCell();
								Point2D absolutePointCalledCell = getAbsolutePointforCell(calledCell);
								System.out.println(objectId + ", " + methodExec.getSignature());
								//	    	    		objectToVertexMap.get(calledObjectId).resetCellPosition();
//								if (methodExecToVertexMap.get(methodExec).getArguments().contains(objectToVertexMap.get(calledObjectId)) || methodExecToVertexMap.get(methodExec).getLocals().contains(objectToVertexMap.get(calledObjectId))) {
//									calledCell.getParent().remove(calledCell);
//									calledCell.setParent(mxDefaultParent);
//									calledCell.getGeometry().setX(absolutePointCalledCell.getX());
//									calledCell.getGeometry().setY(absolutePointCalledCell.getY());
//									deltaAnimation.setVertexAnimation(calledCell, new mxPoint(objectToVertexMap.get(calledObjectId).getInitialX(), objectToVertexMap.get(calledObjectId).getInitialY()));
//									deltaAnimation.startVertexAnimation();
//								}
								removeCalledMethodExecutionVertex(sourceVertexObject, methodExec.getCallerMethodExecution(), methodExec);
								updateObjectVertices();
								//			    		removeVertexMethodExecution(sourceVertexObject, methodExec);
								//	    				update();
								break;
							}
						}
					}
				}  else {
					outputLog();

					// Change!
					List<ObjectVertex> arguments = new ArrayList<>(methodExecToVertexMap.get(methodExec).getArguments());
					List<ObjectVertex> locals = new ArrayList<>(methodExecToVertexMap.get(methodExec).getLocals());
					if (arguments.size() != 0) {
						for (ObjectVertex vo: arguments) {
							mxICell cell = (mxICell)vo.getCell();
							Point2D absolutePointCell = getAbsolutePointforCell(cell);
							cell.getParent().remove(cell);
							cell.setParent(getMxDefaultParent());
							cell.getGeometry().setX(absolutePointCell.getX());
							cell.getGeometry().setY(absolutePointCell.getY());
//							deltaAnimation.setVertexAnimation(cell, new mxPoint(vo.getInitialX(), vo.getInitialY()));
//							deltaAnimation.startVertexAnimation();
							MagnetRONAnimation vertexAnim = new TranslateAnimation(mxgraph, getGraphComponent());
							vertexAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
							vertexAnim.setDelay(getMagnetRONAnimationDelayMillis());
							vertexAnim.init(cell, vo.getInitialX(), vo.getInitialY(), threadPoolExecutor);
							vertexAnim.play();
							methodExecToVertexMap.get(methodExec).getArguments().remove(vo);
						}
					}else if (locals.size() != 0) {
						for (ObjectVertex vo: locals) {
							mxICell cell = (mxICell)vo.getCell();
							Point2D absolutePointCell = getAbsolutePointforCell(cell);
							cell.getParent().remove(cell);
							cell.setParent(getMxDefaultParent());
							cell.getGeometry().setX(absolutePointCell.getX());
							cell.getGeometry().setY(absolutePointCell.getY());
//							deltaAnimation.setVertexAnimation(cell, new mxPoint(vo.getInitialX(), vo.getInitialY()));
//							deltaAnimation.startVertexAnimation();
							MagnetRONAnimation vertexAnim = new TranslateAnimation(mxgraph, getGraphComponent());
							vertexAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
							vertexAnim.setDelay(getMagnetRONAnimationDelayMillis());
							vertexAnim.init(cell, vo.getInitialX(), vo.getInitialY(), threadPoolExecutor);
							vertexAnim.play();
							methodExecToVertexMap.get(methodExec).getLocals().remove(vo);
						}
					}
					updateObjectVertices();	    			
				}
			}
		} finally {
			mxgraph.getModel().endUpdate();
		}
		update();
	}

	/**
	 * Create vertices(mxGraph) and objectVertices in objectToVertexMap.
	 * @param eStructure
	 */
	private void createObjectVertices(ExtractedStructure eStructure) {
		Delta delta = eStructure.getDelta();
		double time = 150;
		double padding = 200;
		coordinatorPoint.setX(coordinatorPoint.getX() + (time * delta.getDstSide().size()) + padding);

		// 左上(0, 0)
		double xCor = coordinatorPoint.getX();
		double yCor = coordinatorPoint.getY();

		//Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
		mxgraph.getModel().beginUpdate();
		try {
			// Draw vertex object.
			// srcSide
			int srcSideSize = delta.getSrcSide().size();
			MethodExecution coordinator = eStructure.getCoordinator();
			String coordinatorObjId = coordinator.getThisObjId();
			String coordinatorClassName = coordinator.getThisClassName();
			for (int i = srcSideSize - 1; i >= 0; i--) {
				Reference ref = delta.getSrcSide().get(i);
				if (i == srcSideSize - 1 && !coordinatorObjId.equals(ref.getSrcObjectId()) && !coordinatorClassName.equals(ref.getSrcClassName())) {
					System.out.println("coordinator: " + coordinatorClassName + ", " + coordinatorObjId);
					coordinatorPoint.setX(coordinatorPoint.getX() + time * 2);
					xCor += time * 2;
					Object vertex = mxgraph.insertDeltaVertex(getMxDefaultParent(), coordinatorObjId, coordinatorClassName, xCor + (time * ((srcSideSize - 1) - i)), yCor + (time * ((srcSideSize - 1) - i)), DEFAULT_OBJECT_VERTEX_SIZE.getWidth(), DEFAULT_OBJECT_VERTEX_SIZE.getHeight(), "fillColor=white"); //creates a white vertex. 
					objectToVertexMap.put(coordinatorObjId, new ObjectVertex(coordinatorClassName, vertex, xCor + (time * ((srcSideSize - 1) - i)), yCor + (time * ((srcSideSize - 1) - i))));
					srcSideSize++;
				}
				System.out.println("srcSide: " + ref.getSrcClassName() + ", " + ref.isCreation() + ", " + ref.getSrcObjectId());
				if (!ref.isCreation() && !ref.getSrcObjectId().equals(ref.getDstObjectId())) {
					if (!objectToVertexMap.containsKey(ref.getSrcObjectId())) {
						String srcClassName = ref.getSrcClassName();
						if (srcClassName.contains("[L")) {
							srcClassName = formatArrayName(srcClassName);
						}
						Object vertex = mxgraph.insertDeltaVertex(getMxDefaultParent(), ref.getSrcObjectId(), srcClassName, xCor + (time * ((srcSideSize - 1) - i)), yCor + (time * ((srcSideSize - 1) - i)), DEFAULT_OBJECT_VERTEX_SIZE.getWidth(), DEFAULT_OBJECT_VERTEX_SIZE.getHeight(), "fillColor=white"); //creates a white vertex. 
						objectToVertexMap.put(ref.getSrcObjectId(), new ObjectVertex(ref.getSrcClassName(), vertex, xCor + (time * ((srcSideSize - 1) - i)), yCor + (time * ((srcSideSize - 1) - i))));
					}
					if (!objectToVertexMap.containsKey(ref.getDstObjectId())) {
						System.out.println(ref.getDstClassName() + ", " + ref.isCreation());
						String dstClassName = ref.getDstClassName();
						if (dstClassName.contains("[L")) {
							dstClassName = formatArrayName(dstClassName);
						}
						Object vertex = mxgraph.insertDeltaVertex(getMxDefaultParent(), ref.getDstObjectId(), dstClassName, xCor + (time * (srcSideSize - i)), yCor + (time * (srcSideSize - i)), DEFAULT_OBJECT_VERTEX_SIZE.getWidth(), DEFAULT_OBJECT_VERTEX_SIZE.getHeight(), "fillColor=white"); //creates a white vertex. 		
						objectToVertexMap.put(ref.getDstObjectId(), new ObjectVertex(ref.getDstClassName(), vertex, xCor + (time * (srcSideSize - i)), yCor + (time * (srcSideSize - i))));
					}
				} else {
					String srcClassName = ref.getSrcClassName();
					if (srcClassName.contains("[L")) {
						srcClassName = formatArrayName(srcClassName);
					}
					Object vertex = mxgraph.insertDeltaVertex(getMxDefaultParent(), ref.getSrcObjectId(), srcClassName, xCor + (time * ((srcSideSize - 1) - i)), yCor + (time * ((srcSideSize - 1) - i)), DEFAULT_OBJECT_VERTEX_SIZE.getWidth(), DEFAULT_OBJECT_VERTEX_SIZE.getHeight(), "fillColor=white"); //creates a white vertex. 
					objectToVertexMap.put(ref.getSrcObjectId(), new ObjectVertex(ref.getSrcClassName(), vertex, xCor + (time * ((srcSideSize - 1) - i)), yCor + (time * ((srcSideSize - 1) - i))));
					objectToVertexMap.put(ref.getDstObjectId(), new ObjectVertex(ref.getDstClassName(), null, xCor + (time * (srcSideSize - i)), yCor + (time * (srcSideSize - i))));
				}
			}

			// dstSide
			int dstSideSize = delta.getDstSide().size();
			int cnt = 0;
			for (int i = dstSideSize - 1; i >= 0; i--) {
				Reference ref = delta.getDstSide().get(i);
				if (i == dstSideSize - 1 && srcSideSize == 0 && !coordinatorObjId.equals(ref.getSrcObjectId()) && !coordinatorClassName.equals(ref.getSrcClassName())) {
					System.out.println("coordinator: " + coordinatorClassName + ", " + coordinatorObjId);
					coordinatorPoint.setX(coordinatorPoint.getX() + time * 2);
					xCor += time * 2;
					System.out.println(coordinatorPoint.getX() + ", " + xCor);
					Object vertex = mxgraph.insertDeltaVertex(getMxDefaultParent(), coordinatorObjId, coordinatorClassName, xCor - (time * (dstSideSize - i + cnt)), yCor + (time * (dstSideSize - i + cnt)), DEFAULT_OBJECT_VERTEX_SIZE.getWidth(), DEFAULT_OBJECT_VERTEX_SIZE.getHeight(), "fillColor=white"); //creates a white vertex. 
					objectToVertexMap.put(coordinatorObjId, new ObjectVertex(coordinatorClassName, vertex, xCor - (time * (dstSideSize - i + cnt)), yCor + (time * (dstSideSize - i + cnt))));
					dstSideSize++;
				}
				System.out.println("dstSide: " + ref.getSrcClassName() + ", " + ref.getDstClassName() + ", " + ref.isCreation());
				if (!ref.isCreation() && !ref.getSrcObjectId().equals(ref.getDstObjectId())) {
					if (!objectToVertexMap.containsKey(ref.getSrcObjectId())) {
						String srcClassName = ref.getSrcClassName();
						if (srcClassName.contains("[L")) {
							srcClassName = formatArrayName(srcClassName);
						}
						Object vertex = mxgraph.insertDeltaVertex(getMxDefaultParent(), ref.getSrcObjectId(), srcClassName, xCor - (time * (dstSideSize - i + cnt)), yCor + (time * (dstSideSize - i + cnt)), DEFAULT_OBJECT_VERTEX_SIZE.getWidth(), DEFAULT_OBJECT_VERTEX_SIZE.getHeight(), "fillColor=white"); //creates a white vertex. 
						objectToVertexMap.put(ref.getSrcObjectId(), new ObjectVertex(ref.getSrcClassName(), vertex, xCor - (time * (dstSideSize - i + cnt)), yCor + (time * (dstSideSize - i + cnt))));
						cnt++;
					}
					if (!objectToVertexMap.containsKey(ref.getDstObjectId())) {
						String dstClassName = ref.getDstClassName();
						if (dstClassName.contains("[L")) {
							dstClassName = formatArrayName(dstClassName);
						}
						Object vertex = mxgraph.insertDeltaVertex(getMxDefaultParent(), ref.getDstObjectId(), dstClassName, xCor - (time * (dstSideSize - i + cnt)), yCor + (time * (dstSideSize - i + cnt)), DEFAULT_OBJECT_VERTEX_SIZE.getWidth(), DEFAULT_OBJECT_VERTEX_SIZE.getHeight(), "fillColor=white"); //creates a white vertex. 
						objectToVertexMap.put(ref.getDstObjectId(), new ObjectVertex(ref.getDstClassName(), vertex, xCor - (time * (dstSideSize - i + cnt)), yCor + (time * (dstSideSize - i + cnt))));
					}
				} else {
					objectToVertexMap.put(ref.getDstObjectId(), new ObjectVertex(ref.getDstClassName(), null, xCor - (time * (dstSideSize - i + cnt)), yCor + (time * (dstSideSize - i + cnt))));
				}
			}
		} finally {
			mxgraph.getModel().endUpdate();
		}
	}

	//	private void moveInitialObjectVertex(MethodExecution methodExecution) {
	//		for(Statement statement: methodExecution.getStatements()) {
	//			if(statement instanceof MethodInvocation) {
	//				//				moveInitialVertexObject((MethodInvocation) statement);
	//				MethodExecution calledMethodExec = ((MethodInvocation)statement).getCalledMethodExecution();
	//				String calledObjectId = calledMethodExec.getThisObjId();
	//				mxICell calledCell = (mxICell)objectToVertexMap.get(calledObjectId).getCell();
	//				Point absolutePointCalledCell = getAbsolutePointforCell(calledCell);
	//				//				System.out.println(objectId + ", " + methodExec.getSignature());
	//				//		    	    		objectToVertexMap.get(calledObjectId).resetCellPosition();
	//				if (methodExecToVertexMap.get(methodExecution).getArguments().contains(objectToVertexMap.get(calledObjectId)) || methodExecToVertexMap.get(methodExecution).getLocals().contains(objectToVertexMap.get(calledObjectId))) {
	//					calledCell.getParent().remove(calledCell);
	//					calledCell.setParent(mxDefaultParent);
	//					calledCell.getGeometry().setX(absolutePointCalledCell.getX());
	//					calledCell.getGeometry().setY(absolutePointCalledCell.getY());
	//					deltaAnimation.setVertexAnimation(calledCell, new mxPoint(objectToVertexMap.get(calledObjectId).getInitialX(), objectToVertexMap.get(calledObjectId).getInitialY()));
	//					deltaAnimation.startVertexAnimation();
	//					break;
	//				}
	//			}
	//		}
	//	}

	/**
	 * Update VertexObject of targetMethodExecCell size have sourceObjectCell.
	 * 
	 * @param sourceObjectCell
	 * @param targetMethodExecCell
	 */
	//	private void updateVertexObjectSize(mxICell sourceObjectCell, mxICell targetMethodExecCell) {
	//		mxICell parentTargetMethodExecCell = targetMethodExecCell.getParent();	    	
	//
	//		//Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
	//		mxgraph.getModel().beginUpdate();
	//		try {
	//			double preX = parentTargetMethodExecCell.getGeometry().getX();
	//			double preY = parentTargetMethodExecCell.getGeometry().getY();
	//			double preCenterX = parentTargetMethodExecCell.getGeometry().getCenterX();
	//			double preCenterY = parentTargetMethodExecCell.getGeometry().getCenterY();
	//			parentTargetMethodExecCell.getGeometry().setWidth(parentTargetMethodExecCell.getGeometry().getWidth() * 1.8);
	//			parentTargetMethodExecCell.getGeometry().setHeight(parentTargetMethodExecCell.getGeometry().getHeight() * 1.8);
	//			parentTargetMethodExecCell.getGeometry().setX(preX - (parentTargetMethodExecCell.getGeometry().getCenterX() - preCenterX));
	//			parentTargetMethodExecCell.getGeometry().setY(preY - (parentTargetMethodExecCell.getGeometry().getCenterY() - preCenterY));
	//		} finally {
	//			mxgraph.getModel().endUpdate();
	//		}
	//	}

	/**
	 * Update VertexObject of targetMethodExecCell size have sourceObjectCell.
	 * 
	 * @param sourceObjectCell
	 * @param targetMethodExecCell
	 */
	//	private void updateVertexObjectSize(mxICell sourceObjectCell, mxICell targetMethodExecCell) {
	//		mxICell parentTargetMethodExecCell = targetMethodExecCell.getParent();	    	
	//
	//		//Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
	//		mxgraph.getModel().beginUpdate();
	//		try {
	//			double preX = parentTargetMethodExecCell.getGeometry().getX();
	//			double preY = parentTargetMethodExecCell.getGeometry().getY();
	//			double preCenterX = parentTargetMethodExecCell.getGeometry().getCenterX();
	//			double preCenterY = parentTargetMethodExecCell.getGeometry().getCenterY();
	//			parentTargetMethodExecCell.getGeometry().setWidth(parentTargetMethodExecCell.getGeometry().getWidth() * 1.8);
	//			parentTargetMethodExecCell.getGeometry().setHeight(parentTargetMethodExecCell.getGeometry().getHeight() * 1.8);
	//			parentTargetMethodExecCell.getGeometry().setX(preX - (parentTargetMethodExecCell.getGeometry().getCenterX() - preCenterX));
	//			parentTargetMethodExecCell.getGeometry().setY(preY - (parentTargetMethodExecCell.getGeometry().getCenterY() - preCenterY));
	//		} finally {
	//			mxgraph.getModel().endUpdate();
	//		}
	//	}

	/**
	 * Create MethodExecutionVertex. Be careful to refer eStructure#getCoordinator() when curFrame is 0.
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
			if (callerMethodExec != null && methodExecSignature != callerMethodExec.getSignature() && objectToVertexMap.containsKey(callerMethodExec.getThisObjId()) && eStructure.getCoordinator() == callerMethodExec) {
				createMethodExecutionVertex(callerMethodExec.getThisObjId(), callerMethodExec.getSignature(), callerMethodExec);
			}
			createMethodExecutionVertex(objId, methodExecSignature, methodExec);
		} else if (alias.getObjectId().startsWith("0:") && !methodExecToVertexMap.containsKey(methodExec)) {
			createMethodExecutionVertex(objId, methodExecSignature, methodExec);
		}
	}

	/** Make edge object in JGraphT and draw this in JGraphX.
	 * 
	 * @param aliasList
	 */
	private void createEdgeToObject(List<Alias> aliasList) {
		for (int i = 0; i < aliasList.size()-1; i++) {
			Alias curAlias = aliasList.get(i);
			Alias nextAlias = aliasList.get(i+1);
			String srcClassName = null;
			String fieldName = null;

			if (curAlias.getAliasType().equals(AliasType.THIS) /*&& nextAlias.getAliasType().equals(AliasType.FIELD)*/) {
				if (nextAlias.getAliasType().equals(AliasType.RETURN_VALUE)) {
					MethodExecution nextMethodExec = nextAlias.getMethodExecution();
					//ArrayやListのときだけラベルを付ける（確実に分かっているものとき)getSignature->contains("List.get(") || "Map.get(") <ホワイトリスト>
					if (nextMethodExec.getSignature().contains("List.get(") ||
							nextMethodExec.getSignature().contains("Map.get(") || 
							nextMethodExec.getSignature().contains(".next()") ||
							nextMethodExec.getSignature().contains(".iterator()") ||
							nextMethodExec.getSignature().contains(".listIterator()")) {
						srcClassName = nextMethodExec.getThisClassName();
						if (nextMethodExec.getArguments().size() > 0) {
							fieldName = nextMethodExec.getArguments().get(0).getId();
						} else {
							fieldName = "";
						}
						System.out.println("rTHIS " + srcClassName + ", " + fieldName);
					}
				} else {
					Statement statement = nextAlias.getOccurrencePoint().getStatement();
					if(statement instanceof FieldAccess && curAlias.getObjectId().equals(((FieldAccess)statement).getContainerObjId())) {
						if (((FieldAccess)statement).getFieldName() != null) {
							String fieldNames[] = formatFieldName(((FieldAccess)statement).getFieldName());
							srcClassName = fieldNames[0];
							fieldName = fieldNames[1];
						} else {
							srcClassName = ((FieldAccess)statement).getContainerClassName();
							fieldName = "";
						}
					}
				}
				System.out.println("THIS " + srcClassName + "(" + curAlias.getObjectId() + ") -> " + "(" + nextAlias.getObjectId() + "), "+ fieldName);
			}
			if(curAlias.getAliasType().equals(AliasType.ARRAY)) {
				Statement statement= nextAlias.getOccurrencePoint().getStatement();
				if(statement instanceof ArrayAccess) {
					srcClassName = ((ArrayAccess)statement).getArrayClassName();
					int index = ((ArrayAccess)statement).getIndex();
					fieldName = formatArrayIndex(index);
					System.out.println("ARRAY " + srcClassName + "(" + curAlias.getObjectId() + ") -> " + "(" +  nextAlias.getObjectId() + "),  " + fieldName);
				}
			}
			if (srcClassName != null && fieldName != null && srcClassName != null && !edgeMap.containsKey(curAlias.getObjectId() + "." + fieldName)) {
				// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
				mxgraph.getModel().beginUpdate();
				try {
					Object srcCell = objectToVertexMap.get(curAlias.getObjectId()).getCell();
					Object dstCell = objectToVertexMap.get(nextAlias.getObjectId()).getCell();
					if (srcCell != null && dstCell != null) { // isCreation()
						System.out.println("makeEdgeObject: " + fieldName + ", " + srcClassName + " (" + srcCell.hashCode() + "), " + " (" + dstCell.hashCode() + ")"/* + ", " + dstClassName*/);
						// BUG:NullPointerException
						Object edge = mxgraph.insertDeltaEdge(getMxDefaultParent(), fieldName, fieldName, srcCell, dstCell);
						edgeMap.put(curAlias.getObjectId() + "." + fieldName, new Edge(fieldName, TypeName.Reference, edge));
					}
				} finally {
					mxgraph.getModel().endUpdate();
				}
			}
		}
	}

	public void setExtractedStructure(ExtractedStructure extractedStructure) {
		this.eStructure = extractedStructure;
	}

	public void setDeltaAliasCollector(IAliasCollector aliasCollector) {
		this.aliasCollector = aliasCollector;
	}

	public void setCoordinatorPoint(double x, double y) {
		coordinatorPoint.setX(x);
		coordinatorPoint.setY(y);
	}


//	private double getXForCell(String id) {
//		double res = -1;
//		if (objectToVertexMap.containsKey(id)) {
//			Object cell = objectToVertexMap.get(id).getCell();
//			res = mxgraph.getCellGeometry(cell).getX();
//		}
//		return res;
//	}

//	private double getYForCell(String id) {
//		double res = -1;
//		if (objectToVertexMap.containsKey(id)) {
//			Object cell = objectToVertexMap.get(id).getCell();
//			res = mxgraph.getCellGeometry(cell).getY();
//		}
//		return res;
//	}

//	private mxICell getRootParentCell(Object object) {
//		mxICell cell = (mxICell) object;
//		if(cell.getParent().getValue() == null) {
//			return cell;
//		}
//		return getRootParentCell(cell.getParent());
//	}

}
