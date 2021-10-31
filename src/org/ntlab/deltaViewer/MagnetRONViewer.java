package org.ntlab.deltaViewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.geom.Dimension2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.JPanel;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.ntlab.animations.MagnetRONAnimation;
import org.ntlab.animations.TranslateAnimation;
import org.ntlab.animations.VertexResizeAnimation;
import org.ntlab.deltaExtractor.Alias;
import org.ntlab.deltaExtractor.IAliasCollector;
import org.ntlab.deltaExtractor.Alias.AliasType;
import org.ntlab.deltaViewer.Edge.TypeName;
import org.ntlab.trace.ArrayUpdate;
import org.ntlab.trace.FieldUpdate;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.ObjectReference;
import org.ntlab.trace.Reference;
import org.ntlab.trace.Statement;
import org.ntlab.trace.TracePoint;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;
import com.mxgraph.shape.mxConnectorShape;
import com.mxgraph.shape.mxIShape;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.view.mxInteractiveCanvas;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;

/**

 * 
 * @author Nitta Lab.
 */
public abstract class MagnetRONViewer extends JPanel {

	private static final long serialVersionUID = -6828987937804142956L;

	// Test code (will be deleted)
	private static final String TAG = MagnetRONViewer.class.getSimpleName();
	
	protected static final Dimension DEFAULT_COMPONENT_SIZE = new Dimension(1300, 600);

	protected static final Dimension DEFAULT_OBJECT_VERTEX_SIZE = new Dimension(70, 70);
	protected static final Dimension DEFAULT_METHOD_EXECUTION_VERTEX_SIZE = new Dimension(55, 20);

	protected IAliasCollector aliasCollector;

	protected Map<String, ObjectVertex> objectToVertexMap = new HashMap<>();
	protected Map<MethodExecution, MethodExecutionVertex> methodExecToVertexMap = new LinkedHashMap<>();
	protected Map<String, Edge> edgeMap = new HashMap<>();

	protected mxGraphComponent mxgraphComponent;
	protected DeltaGraphAdapter mxgraph;
	protected mxICell defaultParent;

	protected int curFrame = 0;
	protected int skipBackFrame = 0; // Use assigned value, when skip back animation.
	
	protected double animationSpeed = DEFAULT_ANIMATION_SPEED;
	protected static final double DEFAULT_ANIMATION_SPEED = 1.0;

	protected ThreadPoolExecutor threadPoolExecutor;
	protected long animationDelayMillis = DEFAULT_ANIMATION_DELAY_MILLIS;
	protected static final long DEFAULT_ANIMATION_DELAY_MILLIS = 250L;
	protected int magnetRONAnimationTotalCycleCount = DEFAULT_MAGNETRON_ANIMATION_TOTAL_CYCLE_COUNT;
	protected static final int DEFAULT_MAGNETRON_ANIMATION_TOTAL_CYCLE_COUNT = 10;
	protected long magnetRONAnimationDelayMillis = DEFAULT_MAGNETRON_ANIMATION_DELAY_MILLIS;
	protected static final long DEFAULT_MAGNETRON_ANIMATION_DELAY_MILLIS = 100L;

	private boolean fSkipBackAnimation = false;
	private boolean fAutoTracking = false;

	public MagnetRONViewer() {
		this.mxgraph = new DeltaGraphAdapter(new DirectedWeightedPseudograph<>(DefaultEdge.class));
		this.defaultParent = (mxCell) mxgraph.getDefaultParent();
		this.mxgraphComponent = new mxGraphComponent(mxgraph) {
			public mxInteractiveCanvas createCanvas() {
				return new CurvedCanvas(this);
			}
		};
		mxgraphComponent.setPreferredSize(DEFAULT_COMPONENT_SIZE);
		this.setLayout(new BorderLayout());
		this.add(mxgraphComponent, BorderLayout.CENTER);
		
		this.threadPoolExecutor = new MagnetRONScheduledThreadPoolExecutor(2);
	}
	
	public mxGraphComponent getGraphComponent() {
		return mxgraphComponent;
	}

	protected mxICell getMxDefaultParent() {
		return defaultParent;
	}
	
	public void clear() {
		this.mxgraph = new DeltaGraphAdapter(new DirectedWeightedPseudograph<>(DefaultEdge.class));
		this.defaultParent = (mxCell) mxgraph.getDefaultParent();
		mxgraphComponent.setGraph(mxgraph);
		curFrame = 0;
		objectToVertexMap.clear();
		methodExecToVertexMap.clear();
		edgeMap.clear();
	}

	abstract public void initAnimation();

	/**
	 * Step to animation of specified alias. 
	 * 
	 * @param alias: alias type and occurrence point etc.
	 */
	abstract public void stepToAnimation(Alias alias);

	/**
	 * Parent : Step to animation of specified numFrame.
	 * 
	 * @param numFrame: current animation frame
	 */
	abstract public void stepToAnimation(int numFrame);

	/**
	 * Do animation from fromFrame to toFrame.
	 * 
	 * @param fromFrame 
	 * @param toFrame
	 */
	protected void doAnimation(int fromFrame, int toFrame) {
		for (int i = fromFrame; i <= toFrame; i++) {
			
			List<Alias> aliasList = new ArrayList<>(aliasCollector.getAliasList());
			Alias alias = aliasList.get(i);
			
			// Test code (will be deleted)
			System.out.println("\r\n" + TAG + ": Frame=" + i  + ", aliasType=" + alias.getAliasType().toString() + ", objectId=" + alias.getObjectId() + ", methodSignature=" + alias.getMethodSignature() + ", l." + alias.getLineNo());
			switch(alias.getAliasType()) {
			case RETURN_VALUE:
				moveObjectVertex(alias);
				update();
				break;
			case METHOD_INVOCATION:
				removeMethodExecutionVertex(alias);
				moveObjectVertex(alias);
				update();
				break;
			case CONSTRACTOR_INVOCATION:
				// TODO: Confirm the program behavior when called after RECEIVER.
				MethodInvocation methodInv = (MethodInvocation) alias.getOccurrencePoint().getStatement();
				String objId = alias.getObjectId();
				if (!objectToVertexMap.containsKey(objId) || objectToVertexMap.get(objId).getCell() == null) {
					createObjectVertexOnConstractor(alias);
				}
				if (!methodExecToVertexMap.containsKey(methodInv.getCalledMethodExecution())) {
					createMethodExecutionVertex(objId, methodInv.getCallerSideMethodName(), methodInv.getCalledMethodExecution());
					update();
				}
				removeMethodExecutionVertex(alias);
				update();
				break;
			case FORMAL_PARAMETER:
				moveObjectVertex(alias);
				update();
				break;
			case ACTUAL_ARGUMENT:
				moveObjectVertex(alias);
				update();
				break;
			case THIS:
				if (curFrame == 0 || alias.getObjectId().startsWith("0:")) {
					createMethodExecutionVertex(alias);
					update();
				}
				break;
			case RECEIVER:
				// Make {@code MethodExecutionVertex} of called method execution.
				MethodExecution calledMethodExec = ((MethodInvocation) alias.getOccurrencePoint().getStatement()).getCalledMethodExecution();
				if (calledMethodExec.isConstructor() 
						&& (!objectToVertexMap.containsKey(alias.getObjectId()) || objectToVertexMap.get(alias.getObjectId()).getCell() == null)) {
					createObjectVertexOnConstractor(alias);
				}
				if (!methodExecToVertexMap.containsKey(calledMethodExec)) {
					MethodExecution methodExec = alias.getMethodExecution();
					if (!methodExecToVertexMap.containsKey(methodExec)
							&& methodExec.getSignature() != calledMethodExec.getSignature() 
							&& objectToVertexMap.containsKey(methodExec.getThisObjId())) {
						createMethodExecutionVertex(methodExec.getThisObjId(), methodExec.getSignature(), methodExec);
					}
					createMethodExecutionVertex(alias.getObjectId(), calledMethodExec.getSignature(), calledMethodExec);
					update();
				}
				break;
			default:
				break;
			}
			curFrame = i + 1;
		}
	}

	/**
	 * Create a {@code mxIcell} of {@code ObjectVertex} while animating {@code TranslateAnimation} 
	 * when {@link Alias#getAliasType()} is CONSTRACTOR_INVOCATION.
	 * 
	 * @param alias
	 */
	protected void createObjectVertexOnConstractor(Alias alias) {
		ObjectVertex objVx = objectToVertexMap.get(alias.getObjectId());
		mxICell objVxCell = null;
		
		// Position of srcCell is start point for ovCell to TranslateAnimation.
		MethodExecution methodExec = alias.getMethodExecution();
		String srcObjId = methodExec.getThisObjId();
		if (srcObjId.matches("0")) {
			srcObjId += ":" + alias.getMethodExecution().getThisClassName();
		}
		mxICell srcObjVxCell = (mxICell) objectToVertexMap.get(srcObjId).getCell();
		double srcObjVxCellWid = srcObjVxCell.getGeometry().getWidth();
		double srcObjVxCellHt = srcObjVxCell.getGeometry().getHeight();
		double overlapWid = srcObjVxCellWid * Math.sqrt(2) * 0.1;
		double overlapHt = srcObjVxCellHt  - (srcObjVxCellHt * Math.sqrt(2) * 0.1);
		Point2D srcObjVxCellAbsPt = getAbsolutePointforCell(srcObjVxCell);
				
		MethodInvocation methodInv;
		String fieldName = null;
		if (!methodExec.isCollectionType() 
				&& alias.getOccurrencePoint().getStatement() != null) {
			methodInv = (MethodInvocation) alias.getOccurrencePoint().getStatement();
			fieldName = methodInv.getCallerSideMethodName();
		}
		
		MagnetRONAnimation.waitAnimationEnd();
		scrollCellAndPointToVisible(srcObjVxCell, objVx.getInitialPoint(), 2);

		mxgraph.getModel().beginUpdate();
		synchronized (mxgraph.getModel()) {
			try {
				// Creates a white mxICell of ObjectVertex. 
				objVxCell = 
						(mxICell) mxgraph.insertDeltaVertex(getMxDefaultParent(), alias.getObjectId(), formatClassName(objVx.getLabel()), 
								srcObjVxCellAbsPt.getX() + overlapWid, srcObjVxCellAbsPt.getY() + overlapHt, 
								DEFAULT_OBJECT_VERTEX_SIZE.getWidth(), DEFAULT_OBJECT_VERTEX_SIZE.getHeight(), 
								"fillColor=white");
				objVx.setCell(objVxCell);
				mxICell edgeCell = (mxICell) mxgraph.insertDeltaEdge(getMxDefaultParent(), fieldName, null, srcObjVxCell, objVxCell);
				edgeMap.put(alias.getObjectId() + "." + fieldName, new Edge(fieldName, TypeName.Create, edgeCell));
				for (String edgeKey: edgeMap.keySet()) {
					Edge edge = edgeMap.get(edgeKey);
					if (edgeKey.startsWith(alias.getObjectId()  + ".") && edge.getTypeName() == Edge.TypeName.PreReference) {
						mxICell dstCell =  (mxICell) objectToVertexMap.get(edge.getDstObjId()).getCell();
						String fieldNames[] = edgeKey.split(".");
						mxICell edgeCell2 = null;
						if (fieldNames.length < 2) {
							edgeCell2 = (mxICell) mxgraph.insertDeltaEdge(getMxDefaultParent(), "", "", objVxCell, dstCell);
						} else {
							edgeCell2 = (mxICell) mxgraph.insertDeltaEdge(getMxDefaultParent(), fieldNames[1], fieldNames[1], objVxCell, dstCell);
						}
						edge.setTypeName(Edge.TypeName.Reference);
						edge.setCell(edgeCell2);
					}
				}
				update();
			} finally {
				mxgraph.getModel().endUpdate();
			}
		}
		MagnetRONAnimation objVxCellAnim = new TranslateAnimation(mxgraph, getGraphComponent());
		objVxCellAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
		objVxCellAnim.setDelay(getMagnetRONAnimationDelayMillis());
		objVxCellAnim.init(objVxCell, objVx.getInitialX(), objVx.getInitialY(), threadPoolExecutor);
		objVxCellAnim.syncPlay();
	}
	
	/**
	 * Create a {@code mxIcell} of {@code ObjectVertex} while animating {@code TranslateAnimation} when {@link Alias#getAliasType()} is CONSTRACTOR_INVOCATION preceded by FORMAL_PARAMETER.
	 * 
	 * @param alias
	 */
	protected void createObjectVertexOnConstractorByFormalParameter(Alias alias) {
		ObjectVertex objVx = objectToVertexMap.get(alias.getObjectId()); // Create mxICell of this ObjectVertex.
		MethodExecution methodExec = alias.getMethodExecution();
		mxICell objVxCell = null;
		
		// Position of srcCell is start point for ovCell to TranslateAnimation.
		String srcObjId = methodExec.getArguments().get(0).getId();
		mxICell srcObjVxCell = (mxICell) objectToVertexMap.get(srcObjId).getCell();
		double srcObjVxCellWid = srcObjVxCell.getGeometry().getWidth();
		double srcObjVxCellHt = srcObjVxCell.getGeometry().getHeight();
		double overlapWidth = srcObjVxCellWid * Math.sqrt(2) * 0.1;
		double overlapHeight = srcObjVxCellHt  - (srcObjVxCellHt * Math.sqrt(2) * 0.1);
		Point2D srcObjVxCellAbsPt = getAbsolutePointforCell(srcObjVxCell);
		
		scrollCellAndPointToVisible(srcObjVxCell, objVx.getInitialPoint(), 2);

		MethodInvocation methodInv;
		String fieldName = null;
		if (!methodExec.isCollectionType() && alias.getOccurrencePoint().getStatement() != null) {
			methodInv = (MethodInvocation)alias.getOccurrencePoint().getStatement();
			fieldName = methodInv.getCallerSideMethodName();
		}
		
		mxgraph.getModel().beginUpdate();
		synchronized (mxgraph.getModel()) {
			try {
				// Creates a white mxIcell of ObjectVertex. 
				objVxCell = 
						(mxICell) mxgraph.insertDeltaVertex(getMxDefaultParent(), objVx.getLabel(), formatClassName(objVx.getLabel()), 
								srcObjVxCellAbsPt.getX() + overlapWidth, srcObjVxCellAbsPt.getY() + overlapHeight, 
								DEFAULT_OBJECT_VERTEX_SIZE.getWidth(), DEFAULT_OBJECT_VERTEX_SIZE.getHeight(), 
								"fillColor=white");
				objVx.setCell(objVxCell);
				mxICell edgeCell = (mxICell) mxgraph.insertDeltaEdge(getMxDefaultParent(), fieldName, null, srcObjVxCell, objVxCell);
				edgeMap.put(alias.getObjectId() + "." + fieldName, new Edge(fieldName, TypeName.Create, edgeCell));
				for (String edgeKey: edgeMap.keySet()) {
					Edge edge = edgeMap.get(edgeKey);
					if (edgeKey.startsWith(alias.getObjectId()  + ".") && edge.getTypeName() == Edge.TypeName.PreReference) {
						mxICell dstCell =  (mxICell) objectToVertexMap.get(edge.getDstObjId()).getCell();
						String fieldNames[] = edgeKey.split(".");
						mxICell edgeCell2 = null;
						if (fieldNames.length < 2) {
							edgeCell2 = (mxICell) mxgraph.insertDeltaEdge(getMxDefaultParent(), "", "", objVxCell, dstCell);
						} else {
							edgeCell2 = (mxICell) mxgraph.insertDeltaEdge(getMxDefaultParent(), fieldNames[1], fieldNames[1], objVxCell, dstCell);
						}
						edge.setTypeName(Edge.TypeName.Reference);
						edge.setCell(edgeCell2);
					}
				}
				update();
			} finally {
				mxgraph.getModel().endUpdate();
			}
		}
		MagnetRONAnimation objVxCellAnim = new TranslateAnimation(mxgraph, getGraphComponent());
		objVxCellAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
		objVxCellAnim.setDelay(getMagnetRONAnimationDelayMillis());
		objVxCellAnim.init(objVxCell, objVx.getInitialX(), objVx.getInitialY(), threadPoolExecutor);
		objVxCellAnim.syncPlay();
	}

	/**
	 * Create edge of {@code ObjectReference}.
	 * 
	 * @param fieldUpdateStatement
	 * @param fieldName
	 */
	protected void createObjectRefrence(FieldUpdate fieldUpdateStatement, String fieldName) {
		// Create edge between source ObjectVertex mxICell and destination ObjectVertex mxICell.
		String srcObjId = fieldUpdateStatement.getContainerObjId();
		mxICell srcObjVxCell = (mxICell) objectToVertexMap.get(srcObjId).getCell();
		
		String dstObjId = fieldUpdateStatement.getValueObjId();
		ObjectVertex dstObjVx = objectToVertexMap.get(dstObjId);
		mxICell dstObjVxCell = (mxICell) dstObjVx.getCell();
		Point2D dstObjVxCellAbsPt = getAbsolutePointforCell(dstObjVxCell);

		scrollCellAndPointToVisible(srcObjVxCell, dstObjVx.getInitialPoint(), 2);
		
		mxgraph.getModel().beginUpdate();
		synchronized (mxgraph.getModel()) {
			try {
				if (!dstObjVxCell.getParent().equals(getMxDefaultParent())) {
					// If parent of ObjectVertex cell isn't mxDefaltParent, reset parent.
					dstObjVxCell.getParent().remove(dstObjVxCell);
					dstObjVxCell.setParent(getMxDefaultParent());
				}
				dstObjVxCell.getGeometry().setX(dstObjVxCellAbsPt.getX());
				dstObjVxCell.getGeometry().setY(dstObjVxCellAbsPt.getY());
				mxICell edgeCell = 
						(mxICell) mxgraph.insertDeltaEdge(getMxDefaultParent(), fieldUpdateStatement.getFieldName(), fieldName, 
								srcObjVxCell, dstObjVxCell);
				edgeCell.setStyle("strokeColor=red;");
//				mxgraph.orderCells(true, new Object[] {edge});
				edgeMap.put(fieldUpdateStatement.getContainerObjId(), new Edge(fieldName, TypeName.Reference, edgeCell));
			} finally {
				mxgraph.getModel().endUpdate();
			}			
		}
		MagnetRONAnimation dstObjVxCellAnim = new TranslateAnimation(mxgraph, getGraphComponent());
		dstObjVxCellAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
		dstObjVxCellAnim.setDelay(getMagnetRONAnimationDelayMillis());
		dstObjVxCellAnim.init(dstObjVxCell, dstObjVx.getInitialX(), dstObjVx.getInitialY(), threadPoolExecutor);
		dstObjVxCellAnim.syncPlay();
		
		// If the animation didn't work to the end.
		mxgraph.getModel().beginUpdate();
		synchronized (mxgraph.getModel()) {
			try {
				dstObjVxCell.getGeometry().setX(dstObjVx.getInitialX());
				dstObjVxCell.getGeometry().setY(dstObjVx.getInitialY());
			} finally {
				mxgraph.getModel().endUpdate();
			}			
		}
	}

	/**
	 *  Create edge of {@code ObjectReference}.
	 *  
	 * @param sourceClassName
	 * @param sourceObjectId
	 * @param destinationObjectId
	 */
	protected void createObjectRefrence(String sourceClassName, String sourceObjectId, String destinationObjectId) {
		// Create edge between source ObjectVertex mxICell and destination ObjectVertex mxICell.
		mxICell srcObjVxCell = (mxICell)objectToVertexMap.get(sourceObjectId).getCell();

		ObjectVertex dstObjVx = objectToVertexMap.get(destinationObjectId);
		mxICell dstObjVxCell = (mxICell) dstObjVx.getCell();
		Point2D dstObjVxCellAbsPt = getAbsolutePointforCell(dstObjVxCell);

		scrollCellAndPointToVisible(srcObjVxCell, dstObjVx.getInitialPoint(), 2);

		// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.   	
		mxgraph.getModel().beginUpdate();
		synchronized (mxgraph.getModel()) {
			try {
				if (!dstObjVxCell.getParent().equals(getMxDefaultParent())) {
					// If parent of ObjectVertex cell isn't mxDefaltParent, reset parent.
					dstObjVxCell.getParent().remove(dstObjVxCell);
					dstObjVxCell.setParent(getMxDefaultParent());
				}
				dstObjVxCell.getGeometry().setX(dstObjVxCellAbsPt.getX());
				dstObjVxCell.getGeometry().setY(dstObjVxCellAbsPt.getY());
				mxICell edgeCell = 
						(mxICell) mxgraph.insertDeltaEdge(getMxDefaultParent(), destinationObjectId, null,
								srcObjVxCell, dstObjVxCell);
				edgeCell.setStyle("strokeColor=red;");
				edgeMap.put(destinationObjectId, new Edge(null, TypeName.Reference, edgeCell));
			} finally {
				mxgraph.getModel().endUpdate();
			}
		}
		MagnetRONAnimation dstObjVxCellAnim = new TranslateAnimation(mxgraph, getGraphComponent());
		dstObjVxCellAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
		dstObjVxCellAnim.setDelay(getMagnetRONAnimationDelayMillis());
		dstObjVxCellAnim.init(dstObjVxCell, dstObjVx.getInitialX(), dstObjVx.getInitialY(), threadPoolExecutor);
		dstObjVxCellAnim.syncPlay();
		
		// If the animation didn't work to the end.
		// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.   	
		mxgraph.getModel().beginUpdate();
		synchronized (mxgraph.getModel()) {
			try {
				dstObjVxCell.getGeometry().setX(dstObjVx.getInitialX());
				dstObjVxCell.getGeometry().setY(dstObjVx.getInitialY());
			} finally {
				mxgraph.getModel().endUpdate();
			}
		}
	}

	/**
	 * Move to position of destination {@code ObjectVertex} from source {@code ObjectVertex}.
	 * 
	 * @param alias
	 */
	protected void moveObjectVertex(Alias alias) {
		// source ObjectVertex
		String objId = alias.getObjectId();
		ObjectVertex srcObjVx = objectToVertexMap.get(objId);
		MethodExecution methodExec = alias.getMethodExecution();
		if (!methodExecToVertexMap.containsKey(methodExec) && methodExec.isStatic()) {
			createMethodExecutionVertex(objId, methodExec.getSignature(), methodExec);
			sleepMainThread(getAnimationDelayMillis());
		}
		// destination ObjectVertex
		MethodExecutionVertex dstMethodExecVx = methodExecToVertexMap.get(methodExec);
		moveObjectVertex(alias, srcObjVx, dstMethodExecVx);
		updateObjectVertices();
	}

	/**
	 * Parent: Move to position of destination {@code ObjectVertex} from source {@code ObjectVertex}.
	 * 
	 * @param alias
	 * @param sourceVertexObject: source {@code ObjectVertex}
	 * @param destinationVertexMethodExec: destination {@code MethodExecutionVertex}
	 */
	private void moveObjectVertex(Alias alias, ObjectVertex sourceObjectVertex, MethodExecutionVertex destinationMethodExecutionVertex) {
		MethodExecution methodExec = alias.getMethodExecution();
		AliasType aliasType = alias.getAliasType();
		if (aliasType.equals(AliasType.RETURN_VALUE) || aliasType.equals(AliasType.METHOD_INVOCATION)) {
			if (sourceObjectVertex.getCell() == null && methodExec.isCollectionType()) {
				if (methodExec.getArguments().isEmpty()) {
					createObjectVertexOnConstractor(alias);
				} else {
					createObjectVertexOnConstractorByFormalParameter(alias);
				}
			}
			if (aliasType.equals(AliasType.RETURN_VALUE)) {
				MagnetRONAnimation.waitAnimationEnd();
			}
			moveLocalObjectVertex(methodExec, sourceObjectVertex, destinationMethodExecutionVertex);
		} else if (aliasType.equals(AliasType.FORMAL_PARAMETER)) {
			moveArgumentObjectVertex(methodExec, sourceObjectVertex, destinationMethodExecutionVertex);
		} else if (aliasType.equals(AliasType.ACTUAL_ARGUMENT)) {
			moveActualArgumentObjectVertex(methodExec, sourceObjectVertex, destinationMethodExecutionVertex);			
		}
	}

	/**
	 * Move to local position of destination {@code MethodExecutionVertex} from caller {@code MethodExecution} of source {@code ObjectVertex}. 
	 * 
	 * @param callerMethodExecution: caller {@code MethodExecution}
	 * @param sourceObjectVertex
	 * @param destinationMethodExecutionVertex
	 */
	private void moveLocalObjectVertex(MethodExecution callerMethodExecution, ObjectVertex sourceObjectVertex, MethodExecutionVertex destinationMethodExecutionVertex) {
		mxICell srcObjVxCell = (mxICell)sourceObjectVertex.getCell();
		mxICell dstMethodExecVxCell = (mxICell) destinationMethodExecutionVertex.getCell();

		if (srcObjVxCell.equals(dstMethodExecVxCell.getParent())) {
			return;
		}

		scrollCellsToVisible(dstMethodExecVxCell.getParent(), srcObjVxCell);

		// Remove source ObjectVertex from Locals and Arguments of caller MethodExecutionVertex.  
		if (methodExecToVertexMap.containsKey(callerMethodExecution)) {
			MethodExecutionVertex callerMethodExecVx = methodExecToVertexMap.get(callerMethodExecution);
			if (callerMethodExecVx.getLocals().contains(sourceObjectVertex)) {
				callerMethodExecVx.getLocals().remove(sourceObjectVertex);
			}
			if (callerMethodExecVx.getArguments().contains(sourceObjectVertex)) {
				callerMethodExecVx.getArguments().remove(sourceObjectVertex);
			}
		}

		// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.   	
		mxgraph.getModel().beginUpdate();
		synchronized (mxgraph.getModel()) {
			try {
				double srcObjVxCellX = srcObjVxCell.getGeometry().getX();
				double srcObjVxCellY = srcObjVxCell.getGeometry().getY();

				// TODO: Confirm why not .equals(getMxDefaultParent()).
				if(srcObjVxCell.getParent().getValue() != null) {
					Point2D srcObjVxCellAbsPt = getAbsolutePointforCell(srcObjVxCell);
					srcObjVxCellX = srcObjVxCellAbsPt.getX();
					srcObjVxCellY = srcObjVxCellAbsPt.getY();
					srcObjVxCell.getParent().remove(srcObjVxCell);
				}
				mxgraph.orderCells(true, new Object[] {srcObjVxCell});
				if (srcObjVxCell.getParent() == null || !srcObjVxCell.getParent().equals(dstMethodExecVxCell.getParent())) {
					// TODO: Confirm why not need following comment out.
//					if (srcCell.getParent() != null) srcCell.getParent().remove(srcCell);
					srcObjVxCell.setParent(dstMethodExecVxCell.getParent());
					dstMethodExecVxCell.getParent().insert(srcObjVxCell);
				}

				Point2D dstObjVxCellAbsPt = getAbsolutePointforCell(srcObjVxCell.getParent());
				srcObjVxCell.getGeometry().setX(srcObjVxCellX - dstObjVxCellAbsPt.getX());
				srcObjVxCell.getGeometry().setY(srcObjVxCellY - dstObjVxCellAbsPt.getY());
			} finally {
				mxgraph.getModel().endUpdate();
			}
		}

		int dstMethodExecVxLocalsSize = destinationMethodExecutionVertex.getLocals().size();
		double srcObjVxCellWid = srcObjVxCell.getGeometry().getWidth();
		double dstMethodExecVxCellHt = dstMethodExecVxCell.getGeometry().getHeight();
		double srcObjVxCellDstX = dstMethodExecVxCell.getGeometry().getX() - (srcObjVxCellWid / Math.sqrt(2.5)) + (srcObjVxCellWid * dstMethodExecVxLocalsSize);
		double srcObjVxCellDstY = dstMethodExecVxCell.getGeometry().getY() + dstMethodExecVxCellHt;

		MagnetRONAnimation srcObjVxCellAnim = new TranslateAnimation(mxgraph, getGraphComponent());
		srcObjVxCellAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
		srcObjVxCellAnim.setDelay(getMagnetRONAnimationDelayMillis());
		srcObjVxCellAnim.init(srcObjVxCell, srcObjVxCellDstX, srcObjVxCellDstY, threadPoolExecutor);
		srcObjVxCellAnim.syncPlay();
		
		// If the animation didn't work to the end.
		// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.   	
		mxgraph.getModel().beginUpdate();
		synchronized (mxgraph.getModel()) {
			try {
				if (!srcObjVxCell.getParent().equals(dstMethodExecVxCell.getParent())) {
					srcObjVxCell.getParent().remove(srcObjVxCell);
					srcObjVxCell.setParent(dstMethodExecVxCell.getParent());
					dstMethodExecVxCell.getParent().insert(srcObjVxCell);
				}
				srcObjVxCell.getGeometry().setX(srcObjVxCellDstX);
				srcObjVxCell.getGeometry().setY(srcObjVxCellDstY);
			} finally {
				mxgraph.getModel().endUpdate();
			}
		}
		destinationMethodExecutionVertex.getLocals().add(sourceObjectVertex);
	}

	/**
	 * Move to position of destination {@code MethodExecutionVertex}'s argument from {@code MethodExecution} of source {@code VertexObject}.
	 * 
	 * @param methodExecution: {@code MethodExecution}
	 * @param sourceVertexObject: moving {@code VertexObject}
	 * @param destinationMethodExecutionVertex
	 */
	protected void moveArgumentObjectVertex(MethodExecution methodExecution, ObjectVertex sourceObjectVertex, MethodExecutionVertex destinationMethodExecutionVertex) {
		mxICell srcObjVxCell = (mxICell)sourceObjectVertex.getCell();
		mxICell dstMethodExecVxCell = (mxICell) destinationMethodExecutionVertex.getCell();

		// Remove source VertexObject from Locals and Arguments of MethodExecutionVertex.  
		MethodExecution callerMethodExec = methodExecution.getCallerMethodExecution();
		if (methodExecToVertexMap.containsKey(callerMethodExec)) {
			MethodExecutionVertex callerMethodExecVx = methodExecToVertexMap.get(callerMethodExec);
			mxICell callerMethodExecVxCell = (mxICell) callerMethodExecVx.getCell();
			scrollCellsToVisible(callerMethodExecVxCell.getParent(), dstMethodExecVxCell.getParent(), 2);
			if (callerMethodExecVx.getLocals().contains(sourceObjectVertex)) {
				callerMethodExecVx.getLocals().remove(sourceObjectVertex);
			}
			if (callerMethodExecVx.getArguments().contains(sourceObjectVertex)) {
				callerMethodExecVx.getArguments().remove(sourceObjectVertex);
			}
		} else {
			scrollCellsToVisible(srcObjVxCell, dstMethodExecVxCell.getParent(), 2);			
		}

		int dstMethodExecVxArgumentsSize = destinationMethodExecutionVertex.getArguments().size();
		double srcObjVxCellX = srcObjVxCell.getGeometry().getX();
		double srcObjVxCellY = srcObjVxCell.getGeometry().getY();

		// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
		mxgraph.getModel().beginUpdate();
		synchronized (mxgraph.getModel()) {
			try {
				if(srcObjVxCell.getParent().getValue() != null) {
					Point2D srcObjVxCellAbsPt = getAbsolutePointforCell(srcObjVxCell);
					srcObjVxCellX = srcObjVxCellAbsPt.getX();
					srcObjVxCellY = srcObjVxCellAbsPt.getY();
					srcObjVxCell.getParent().remove(srcObjVxCell);
				}
			} finally {
				mxgraph.getModel().endUpdate();
			}
		}

		if (!isParent(dstMethodExecVxCell, srcObjVxCell)) {
			// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
			mxgraph.getModel().beginUpdate();
			synchronized (mxgraph.getModel()) {
				try {
					mxgraph.orderCells(true, new Object[] {srcObjVxCell});
					if (srcObjVxCell.getParent() == null || !srcObjVxCell.getParent().equals(dstMethodExecVxCell.getParent())) {
						// TODO: Confirm why not need following comment out.
//						if (srcCell.getParent() != null) srcCell.getParent().remove(srcCell);
						srcObjVxCell.setParent(dstMethodExecVxCell.getParent());
						dstMethodExecVxCell.getParent().insert(srcObjVxCell);
					}
					Point2D srcObjVxCellParentAbsPt = getAbsolutePointforCell(srcObjVxCell.getParent());
					srcObjVxCell.getGeometry().setX(srcObjVxCellX - srcObjVxCellParentAbsPt.getX());
					srcObjVxCell.getGeometry().setY(srcObjVxCellY - srcObjVxCellParentAbsPt.getY());
				} finally {
					mxgraph.getModel().endUpdate();
				}
			}

			double srcObjVxCellWid = srcObjVxCell.getGeometry().getWidth();
			double srcObjVxCellHt = srcObjVxCell.getGeometry().getHeight();
			double overlapWid = srcObjVxCellWid - (srcObjVxCellWid * Math.sqrt(2) * 0.1);
			double overlapHt = srcObjVxCellHt - (srcObjVxCellHt * Math.sqrt(2) * 0.1);
			Point2D srcObjVxCellDstPt = 
					new Point2D.Double(dstMethodExecVxCell.getGeometry().getX() - overlapWid, 
							dstMethodExecVxCell.getGeometry().getY()  - overlapHt + (srcObjVxCellHt * dstMethodExecVxArgumentsSize));
						
			MagnetRONAnimation srcObjVxCellAnim = new TranslateAnimation(mxgraph, getGraphComponent());
			srcObjVxCellAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
			srcObjVxCellAnim.setDelay(getMagnetRONAnimationDelayMillis());
			srcObjVxCellAnim.init(srcObjVxCell, srcObjVxCellDstPt.getX(), srcObjVxCellDstPt.getY(), threadPoolExecutor);
			sleepMainThread(getAnimationDelayMillis());
			srcObjVxCellAnim.syncPlay();

			// If the animation didn't work to the end.
			// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
			mxgraph.getModel().beginUpdate();
			synchronized (mxgraph.getModel()) {
				try {
					srcObjVxCell.getGeometry().setX(srcObjVxCellDstPt.getX());
					srcObjVxCell.getGeometry().setY(srcObjVxCellDstPt.getY());
				} finally {
					mxgraph.getModel().endUpdate();
				}
			}
			destinationMethodExecutionVertex.getArguments().add(sourceObjectVertex);
		} else { // TODO: 仕様上のバグ、ループが発生
			// 元の ObjectVertex
			Point2D srcObjVxCellAbsPt = getAbsolutePointforCell(srcObjVxCell);
			mxICell dstMethodExecVxParentCell = dstMethodExecVxCell.getParent();
			Point2D dstMethodExecVxParentCellAbsPt = getAbsolutePointforCell(dstMethodExecVxParentCell);

			// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
			mxgraph.getModel().beginUpdate();
			synchronized (mxgraph.getModel()) {
				try {
					if ((dstMethodExecVxParentCell != null && dstMethodExecVxParentCell.getParent() != null)
							|| srcObjVxCell.getParent() != null
							|| !dstMethodExecVxParentCell.getParent().equals(getMxDefaultParent())
							|| !srcObjVxCell.getParent().equals(dstMethodExecVxParentCell)) {					
						srcObjVxCell.remove(dstMethodExecVxParentCell);
						dstMethodExecVxParentCell.setParent(getMxDefaultParent());
						// TODO: Confirm why not need following comment out.
//						dstCell.getParent().remove(dstCell);
						// TODO: Confirm why not need following comment out.
//						srcCell.getParent().remove(srcCell);
						srcObjVxCell.setParent(dstMethodExecVxParentCell);
						dstMethodExecVxParentCell.insert(srcObjVxCell);
					}

					dstMethodExecVxParentCell.getGeometry().setX(dstMethodExecVxParentCellAbsPt.getX());
					dstMethodExecVxParentCell.getGeometry().setY(dstMethodExecVxParentCellAbsPt.getY());
					srcObjVxCell.getGeometry().setX(srcObjVxCellAbsPt.getX() - dstMethodExecVxParentCellAbsPt.getX());
					srcObjVxCell.getGeometry().setY(srcObjVxCellAbsPt.getY() - dstMethodExecVxParentCellAbsPt.getY());
					srcObjVxCell.setStyle("opacity=50;shape=ellipse");
				} finally {
					mxgraph.getModel().endUpdate();
				}
			}

			double srcObjVxCellWid = srcObjVxCell.getGeometry().getWidth();
			double srcObjVxCellHt = srcObjVxCell.getGeometry().getHeight();
			double overlapWid = srcObjVxCellWid - (srcObjVxCellWid * Math.sqrt(2) * 0.1);
			double overlapHt = srcObjVxCellHt - (srcObjVxCellHt * Math.sqrt(2) * 0.1);
			double srcObjVxCellDstX = dstMethodExecVxCell.getGeometry().getX() - overlapWid + (srcObjVxCellWid * dstMethodExecVxArgumentsSize);
			double srcObjVxCellDstY = dstMethodExecVxCell.getGeometry().getY()  - overlapHt + (srcObjVxCellHt * dstMethodExecVxArgumentsSize);
					
			MagnetRONAnimation srcObjVxCellAnim = new TranslateAnimation(mxgraph, getGraphComponent());
			srcObjVxCellAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
			srcObjVxCellAnim.setDelay(getMagnetRONAnimationDelayMillis());
			srcObjVxCellAnim.init(srcObjVxCell, srcObjVxCellDstX, srcObjVxCellDstY, threadPoolExecutor);
			srcObjVxCellAnim.syncPlay();

			// If the animation didn't work to the end.
			// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
			mxgraph.getModel().beginUpdate();
			synchronized (mxgraph.getModel()) {
				try {
					srcObjVxCell.getGeometry().setX(srcObjVxCellDstX);
					srcObjVxCell.getGeometry().setY(srcObjVxCellDstY);
				} finally {
					mxgraph.getModel().endUpdate();
				}
			}
			destinationMethodExecutionVertex.getArguments().add(sourceObjectVertex);			
		}
	}

	/**
	 * 
	 * @param methodExecution: {@code MethodExecution}
	 * @param sourceVertexObject: moving {@code VertexObject}
	 * @param destinationVertexMethodExec
	 */

	/**
	 * Move to position of destination {@code MethodExecutionVertex}'s actual argument from {@code MethodExecution} of source {@code VertexObject}.
	 * 
	 * @param methodExecution: {@code MethodExecution}
	 * @param sourceObjectVertex
	 * @param destinationMethodExecutionVertex
	 */
	private void moveActualArgumentObjectVertex(MethodExecution methodExecution, ObjectVertex sourceObjectVertex, MethodExecutionVertex destinationMethodExecutionVertex) {
		mxICell srcObjVxCell = (mxICell)sourceObjectVertex.getCell();
		mxICell dstMethodExecVxCell = (mxICell) destinationMethodExecutionVertex.getCell();

		if (srcObjVxCell.equals(dstMethodExecVxCell.getParent())) {
			return;
		}

		//  Remove source ObjectVertex from Locals and Arguments of MethodExecutionVertex. 
		if (methodExecToVertexMap.containsKey(methodExecution)) {
			if (methodExecToVertexMap.get(methodExecution).getLocals().contains(sourceObjectVertex)) {
				methodExecToVertexMap.get(methodExecution).getLocals().remove(sourceObjectVertex);
			}
			if (methodExecToVertexMap.get(methodExecution).getArguments().contains(sourceObjectVertex)) {
				methodExecToVertexMap.get(methodExecution).getArguments().remove(sourceObjectVertex);
			}
		}
		
		int dstMethodExecVxLocalsSize = destinationMethodExecutionVertex.getLocals().size();
		double srcObjVxCellX = srcObjVxCell.getGeometry().getX();
		double srcObjVxCellY = srcObjVxCell.getGeometry().getY();

		MagnetRONAnimation.waitAnimationEnd();
		scrollCellsToVisible(srcObjVxCell, dstMethodExecVxCell.getParent(), 2);			
		
		// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
		mxgraph.getModel().beginUpdate();
		synchronized (mxgraph.getModel()) {
			try {
				if(srcObjVxCell.getParent().getValue() != null) {
					Point2D srcObjVxCellAbsPt = getAbsolutePointforCell(srcObjVxCell);
					srcObjVxCellX = srcObjVxCellAbsPt.getX();
					srcObjVxCellY = srcObjVxCellAbsPt.getY();
					srcObjVxCell.getParent().remove(srcObjVxCell);
				}

				if (srcObjVxCell.getParent() == null 
						|| !srcObjVxCell.getParent().equals(dstMethodExecVxCell.getParent())) {
					// TODO: Confirm why not need following comment out.
//					if (srcCell.getParent() != null) srcCell.getParent().remove(srcCell);
					srcObjVxCell.setParent(dstMethodExecVxCell.getParent());
					dstMethodExecVxCell.getParent().insert(srcObjVxCell);
				}
				Point2D srcObjVxCellParentAbsPt = getAbsolutePointforCell(srcObjVxCell.getParent());
				srcObjVxCell.getGeometry().setX(srcObjVxCellX - srcObjVxCellParentAbsPt.getX());
				srcObjVxCell.getGeometry().setY(srcObjVxCellY - srcObjVxCellParentAbsPt.getY());
			} finally {
				mxgraph.getModel().endUpdate();
			}
		}

		double srcObjVxCellWid = srcObjVxCell.getGeometry().getWidth();
		double dstMethodExecVxCellHt = dstMethodExecVxCell.getGeometry().getHeight();
		Point2D srcObjVxCellDstPt = new Point2D.Double(dstMethodExecVxCell.getGeometry().getX() - (srcObjVxCellWid / Math.sqrt(3)) + (srcObjVxCellWid * dstMethodExecVxLocalsSize), 
				dstMethodExecVxCell.getGeometry().getY() + dstMethodExecVxCellHt);
				
		MagnetRONAnimation srcObjVxCellAnim = new TranslateAnimation(mxgraph, getGraphComponent());
		srcObjVxCellAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
		srcObjVxCellAnim.setDelay(getMagnetRONAnimationDelayMillis());
		srcObjVxCellAnim.init(srcObjVxCell, srcObjVxCellDstPt.getX(), srcObjVxCellDstPt.getY(), threadPoolExecutor);
		srcObjVxCellAnim.syncPlay();

		// If the animation didn't work to the end.
		// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
		mxgraph.getModel().beginUpdate();
		synchronized (mxgraph.getModel()) {
			try {
				srcObjVxCell.getGeometry().setX(srcObjVxCellDstPt.getX());
				srcObjVxCell.getGeometry().setY(srcObjVxCellDstPt.getY());
			} finally {
				mxgraph.getModel().endUpdate();
			}
		}

		destinationMethodExecutionVertex.getArguments().add(sourceObjectVertex);
	}

	/** 
	 * Update size and position of all {@code ObjectVertex}. 
	 */
	protected void updateObjectVertices() {
		MagnetRONAnimation.waitAnimationEnd();
		for (ObjectVertex objVx: objectToVertexMap.values()) {
			mxICell objVxCell = (mxICell) objVx.getCell();
			if (objVxCell == null) continue;
			Dimension2D curDim = new Dimension((int) objVxCell.getGeometry().getWidth(), (int) objVxCell.getGeometry().getHeight());
			int sizeScale = 0;
			for (int i = 0; i < objVxCell.getChildCount(); i++) {
				if (!objVxCell.getChildAt(i).getId().contains("clone")) sizeScale++;
			}
			if (sizeScale == 0) sizeScale = 1;
			Dimension2D dstDim = 
					new Dimension((int) DEFAULT_OBJECT_VERTEX_SIZE.getWidth() * sizeScale, 
							(int) DEFAULT_OBJECT_VERTEX_SIZE.getHeight() * sizeScale);
			Point2D dstPt = new Point2D.Double(objVxCell.getGeometry().getX(), objVxCell.getGeometry().getY());

			if(!curDim.equals(dstDim)) {
				// Test code (will be deleted)
				System.out.println(TAG + ": Update size of ObjectVertex " + objVxCell.getId() + ". " + curDim.getWidth() + "->" + dstDim.getWidth());
				if (!objVxCell.getParent().equals(getMxDefaultParent()) 
						&& (objVxCell.getChildCount() != 0 || (curDim.getWidth() > dstDim.getWidth() && curDim.getHeight() > dstDim.getHeight()))) {
					double overlapX = (dstDim.getWidth() - curDim.getWidth()) / 2 / Math.sqrt(2);
					double overlapY = (dstDim.getHeight() - curDim.getHeight()) / 2 / Math.sqrt(2);
					dstPt.setLocation(objVxCell.getGeometry().getX() - overlapX, objVxCell.getGeometry().getY() + overlapY);

					// If two and over ObjectVertex are arranged side by side as an argument or local, shift the Y coordinate of ObjectVertex slightly.
					for (MethodExecutionVertex methodExecVertex: methodExecToVertexMap.values()) {
						List<ObjectVertex> locals = methodExecVertex.getLocals();
						if (locals != null && locals.contains(objVx) && locals.indexOf(objVx) >= 1) {
							overlapY = (dstDim.getHeight() - objVxCell.getGeometry().getHeight()) / 2;
							dstPt.setLocation(dstPt.getX(), objVxCell.getGeometry().getY() + overlapY);
							break;
						}
						List<ObjectVertex> arguments = methodExecVertex.getArguments();
						if (arguments != null && arguments.contains(objVx)) {
							dstPt.setLocation(dstPt.getX(), objVxCell.getGeometry().getY() - overlapY);
							break;
						}
					}
				}
				dstPt.setLocation(dstPt.getX() - (dstDim.getWidth() - curDim.getWidth()) / 2, dstPt.getY() - (dstDim.getHeight() - curDim.getHeight()) / 2);
				// Test code (will be deleted)
				System.out.println(TAG + ": Translate " + objVxCell.getId() + ". Current point=" + objVxCell.getGeometry().getPoint() + ", Destination Point=" + dstPt);
				MagnetRONAnimation objVxCellTransAnim = new TranslateAnimation(mxgraph, getGraphComponent());
				objVxCellTransAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
				objVxCellTransAnim.setDelay(getMagnetRONAnimationDelayMillis());
				objVxCellTransAnim.init(objVxCell, dstPt.getX(), dstPt.getY(), threadPoolExecutor);
				objVxCellTransAnim.play();
				MagnetRONAnimation objVxCellResizeAnim = new VertexResizeAnimation(mxgraph, getGraphComponent());
				objVxCellResizeAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
				objVxCellResizeAnim.setDelay(getMagnetRONAnimationDelayMillis());
				objVxCellResizeAnim.init(objVxCell, dstDim.getWidth(), dstDim.getHeight(), threadPoolExecutor);
				objVxCellResizeAnim.play();
				for (int i = 0; i < objVxCell.getChildCount(); i++) {
					mxICell objVxCellChild = objVxCell.getChildAt(i);
					double childCellCurX = objVxCellChild.getGeometry().getX();
					double childCellCurY = objVxCellChild.getGeometry().getY();
					Point2D childDstPt = 
							new Point2D.Double(childCellCurX + (dstDim.getWidth() - curDim.getWidth()) / 2, 
									childCellCurY + (dstDim.getHeight() - curDim.getHeight()) / 2);
					// Test code (will be deleted)
					System.out.println(TAG + ": Translate " + objVxCellChild.getId() + " of " + objVxCell.getId() + ". Current point=" + objVxCellChild.getGeometry().getPoint() + ", Destination Point=" + childDstPt);
					MagnetRONAnimation childCellAnim = new TranslateAnimation(mxgraph, getGraphComponent());
					childCellAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
					childCellAnim.setDelay(getMagnetRONAnimationDelayMillis());
					childCellAnim.init(objVxCellChild, childDstPt.getX(), childDstPt.getY(), threadPoolExecutor);
					childCellAnim.play();
				}
				MagnetRONAnimation.waitAnimationEnd();
			}
		}
	}

	abstract protected void createMethodExecutionVertex(Alias alias);

	/**
	 * Create {@code MethodExecutionVertex} and Call {@link MagnetRONViewer#createEdgesToMethodExecutions()}.
	 * 
	 * @param objectId
	 * @param methodSignature: called or this method signature
	 * @param methodExec: called or this {@code MethodExecution}
	 */
	protected void createMethodExecutionVertex(String objectId, String methodSignature, MethodExecution methodExecution) {
		if (methodSignature == null) {
			methodSignature = methodExecution.getSignature();
		}

		if (methodSignature.matches(".+\\(.*\\)")) {
			methodSignature = formatMethodSignature(methodSignature, methodExecution.getThisClassName());
		}

		if (methodExecution.isStatic() && !objectId.startsWith("0")) {		// Check the object id is a formal parameter's one.
			objectId = methodExecution.getThisObjId();
			if (objectId.matches("0")) {
				objectId += ":" + methodExecution.getThisClassName();
			}
		}

		ObjectVertex parentObjVx = objectToVertexMap.get(objectId);
		mxICell parentObjVxCell = (mxICell) parentObjVx.getCell();
		double coordX = DEFAULT_OBJECT_VERTEX_SIZE.getWidth() * 0.1;
		double coordY = DEFAULT_OBJECT_VERTEX_SIZE.getHeight() * 0.5;
		double stdX = coordX;
		double stdY = 0;
		int methodExecVxSize = parentObjVx.getMethodExecutionVertices().size();
		if (methodExecVxSize >= 1) {
			mxICell stdCell = (mxICell) parentObjVx.getMethodExecutionVertices().get(0).getCell();
			stdX = stdCell.getGeometry().getX();
			stdY = stdCell.getGeometry().getY();
			methodExecVxSize -= 1;
		}

		mxICell methodExecVxCell = null;
		MethodExecutionVertex methodExecVx = null;
		// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
		mxgraph.getModel().beginUpdate();
		synchronized (mxgraph.getModel()) {
			try {
				// Creates a white cell of MethodExecutionVertex. 
				methodExecVxCell = (mxICell) mxgraph.insertDeltaVertex(parentObjVxCell, methodSignature, methodSignature, "fillColor=white");
				mxgraph.orderCells(false, new Object[] {methodExecVxCell});
				methodExecVx = 
						new MethodExecutionVertex(methodSignature, methodExecVxCell, stdX, coordY * (methodExecVxSize + 1) + stdY, 
								DEFAULT_METHOD_EXECUTION_VERTEX_SIZE.getWidth(), DEFAULT_METHOD_EXECUTION_VERTEX_SIZE.getHeight());
				methodExecToVertexMap.put(methodExecution, methodExecVx);
				if(methodExecToVertexMap.size() > 1) {
					// Caution: If synchronized block is split here, {@code cell} is visible instantly until cell#setVisible(false) is executed.
					methodExecVxCell.setVisible(false);
				}
			} finally {
				mxgraph.getModel().endUpdate();
			}
		}
		
		if(methodExecToVertexMap.size() > 1) {
			createEdgesToMethodExecutions();
		}
		parentObjVx.addMethodExecutionVertex(methodExecVx);
		update();
	}

	/**
	 * Remove {@code MethodExecutionVertex} on {@code {@link Alias#getAliasType()} is {@code AliasType.METHOD_INVOCATION}.
	 * 
	 * @param alias
	 */
	private void removeMethodExecutionVertex(Alias alias) {
		// source ObjectVertex
		ObjectVertex srcObjVx = objectToVertexMap.get(alias.getObjectId());
		AliasType aliasType = alias.getAliasType();
		// Quick fix
		if (aliasType == AliasType.CONSTRACTOR_INVOCATION) {
			MethodInvocation methodInv = (MethodInvocation) alias.getOccurrencePoint().getStatement();
			List<Statement> statements = methodInv.getCalledMethodExecution().getStatements();
			if (!statements.isEmpty() && statements.get(0) instanceof MethodInvocation) {
				MethodInvocation calledMethodInv = (MethodInvocation) statements.get(0);
				if (!calledMethodInv.getCalledMethodExecution().getArguments().isEmpty()
						&& objectToVertexMap.containsKey(calledMethodInv.getCalledMethodExecution().getArguments().get(0).getId())) {
					srcObjVx = objectToVertexMap.get(calledMethodInv.getCalledMethodExecution().getArguments().get(0).getId());									
				}
			}
		}
		MethodExecution methodExec = alias.getMethodExecution();

		if(aliasType.equals(AliasType.METHOD_INVOCATION) || aliasType.equals(AliasType.CONSTRACTOR_INVOCATION)) {
			MethodExecution calledMethodExec = ((MethodInvocation) alias.getOccurrencePoint().getStatement()).getCalledMethodExecution();
			List<ObjectVertex> arguments = new ArrayList<>(methodExecToVertexMap.get(calledMethodExec).getArguments());
			List<ObjectVertex> locals = new ArrayList<>(methodExecToVertexMap.get(calledMethodExec).getLocals());
			if (arguments.size() != 0) {
				for (ObjectVertex objVx: arguments) {
					// TODO: Implement equals().
					if (objVx != srcObjVx) {
						mxICell objVxcell = (mxICell)objVx.getCell();
						if (!objVxcell.getParent().equals(getMxDefaultParent())) {
							// If parent of ObjectVertex cell isn't mxDefaltParent, reset parent.
							// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
							mxgraph.getModel().beginUpdate();
							synchronized (mxgraph.getModel()) {
								try {
									Point2D objVxCellAbsPt = getAbsolutePointforCell(objVxcell);
									objVxcell.getParent().remove(objVxcell);
									objVxcell.setParent(getMxDefaultParent());
									objVxcell.getGeometry().setX(objVxCellAbsPt.getX());
									objVxcell.getGeometry().setY(objVxCellAbsPt.getY());
								} finally {
									mxgraph.getModel().endUpdate();
								}
							}
							MagnetRONAnimation objVxCellAnim = new TranslateAnimation(mxgraph, getGraphComponent());
							objVxCellAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
							objVxCellAnim.setDelay(getMagnetRONAnimationDelayMillis());
							objVxCellAnim.init(objVxcell, objVx.getInitialX(), objVx.getInitialY(), threadPoolExecutor);
							objVxCellAnim.play();
							methodExecToVertexMap.get(calledMethodExec).getArguments().remove(objVx);
						}
					}
				}
				if (locals.size() != 0) {
					for (ObjectVertex objVx: locals) {
						if (objVx != srcObjVx) {
							mxICell objVxCell = (mxICell) objVx.getCell();
							// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
							mxgraph.getModel().beginUpdate();
							synchronized (mxgraph.getModel()) {
								try {
									Point2D objVxCellAbsPt = getAbsolutePointforCell(objVxCell);
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
							objVxCellAnim.play();
						}
					}
				}
			}
			MagnetRONAnimation.waitAnimationEnd();
			if (aliasType.equals(AliasType.CONSTRACTOR_INVOCATION)) {
				sleepMainThread(500L);
			}
			removeCalledMethodExecutionVertex(srcObjVx, methodExec, calledMethodExec);
		} else {
			removeMethodExecutionVertex(srcObjVx, methodExec);
		}
	}

	/**
	 * Remove {@code MethodExecutionVertex} on AliasType is {@code AliasType.METHOD_INVOCATION}.
	 * 
	 * @param sourceObjectVertex
	 * @param methodExecution
	 */
	private void removeMethodExecutionVertex(ObjectVertex sourceObjectVertex, MethodExecution methodExecution) {	
		mxgraph.getModel().beginUpdate();
		synchronized (mxgraph.getModel()) {
			try {
				//  Remove source {@code ObjectVertex} from Locals and Arguments of called {@code MethodExecution}'s Vertex.
				if (methodExecToVertexMap.containsKey(methodExecution)) {
					mxCell dstMethodExecVxCell = (mxCell)methodExecToVertexMap.get(methodExecution).getCell();
					if (!dstMethodExecVxCell.getParent().equals(getMxDefaultParent())) {
						// If parent of ObjectVertex cell isn't mxDefaltParent, reset parent.
						dstMethodExecVxCell.getParent().remove(dstMethodExecVxCell);
						dstMethodExecVxCell.setParent(getMxDefaultParent());
					}
					mxgraph.removeCells(new Object[] {dstMethodExecVxCell});
					objectToVertexMap.get(methodExecution.getThisObjId()).getMethodExecutionVertices().remove(methodExecToVertexMap.get(methodExecution));
					methodExecToVertexMap.remove(methodExecution);
					edgeMap.remove(methodExecution.getSignature());
					updateObjectVertices();
				}
			} finally {
				mxgraph.getModel().endUpdate();
			}
		}
	}

	/**
	 * Remove called {@code MethodExecutionVertex} with alias type {@code AliasType#METHOD_INVOCATION}.
	 * 
	 * @param sourceVertexObject: source object vertex that a called method execution has temporarily
	 * @param methodExec: current method execution
	 * @param calledMethodExec: called method execution
	 */
	protected void removeCalledMethodExecutionVertex(ObjectVertex sourceObjectVertex, MethodExecution methodExecution, MethodExecution calledMethodExecution) {	
		MagnetRONAnimation.waitAnimationEnd();

		//  Remove ObjectVertex other than source ObjectVertex from locals and arguments of called MethodExecutionVertex.  
		if (methodExecToVertexMap.containsKey(calledMethodExecution)) {
			MethodExecutionVertex calledMethodExecVx = methodExecToVertexMap.get(calledMethodExecution);

			// TODO: Confirm bug.
			List<ObjectVertex> arguments = new ArrayList<>(calledMethodExecVx.getArguments());
			if (arguments.size() != 0) {
				for (ObjectVertex objVx: arguments) {
					if (objVx != sourceObjectVertex) {
						mxICell objVxCell = (mxICell)objVx.getCell();
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
							} finally {
								mxgraph.getModel().endUpdate();
							}
						}
						if (!objVxCellAbsPt.equals(objVx.getInitialPoint())) {
							// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
							mxgraph.getModel().beginUpdate();
							synchronized (mxgraph.getModel()) {
								try {
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
						}
						methodExecToVertexMap.get(calledMethodExecution).getArguments().remove(objVx);
					}
				}
			}

			List<ObjectVertex> locals = new ArrayList<>(calledMethodExecVx.getLocals());
			if (locals.size() != 0) {
				for (ObjectVertex objVx: locals) {
					if (objVx != sourceObjectVertex) {
						mxICell objVxCell = (mxICell)objVx.getCell();
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
							} finally {
								mxgraph.getModel().endUpdate();
							}
						}
						if (!objVxCellAbsPt.equals(objVx.getInitialPoint())) {
							// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
							mxgraph.getModel().beginUpdate();
							synchronized (mxgraph.getModel()) {
								try {
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
						}
						methodExecToVertexMap.get(calledMethodExecution).getLocals().remove(objVx);
					}
				}
			}

			// Quick fix
			if (methodExecution == null || !methodExecToVertexMap.get(calledMethodExecution).getArguments().isEmpty()) {
				return;
			}

			mxICell srcMethodExecVxCell = (mxICell)methodExecToVertexMap.get(methodExecution).getCell();
			mxICell dstMethodExecVxCell = (mxICell)calledMethodExecVx.getCell();

			scrollCellsToVisible(srcMethodExecVxCell.getParent(), dstMethodExecVxCell.getParent());

			try {
				Point2D srcMethodExecVxCellAbsPt = null;
				Point2D dstMethodExecVxCellAbsPt = null;
				final mxICell[] cloneDstMethodExecVxCell = new mxICell[1];
				// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
				mxgraph.getModel().beginUpdate();
				synchronized (mxgraph.getModel()) {
					try {
						mxgraph.removeCells(mxgraph.getEdgesBetween(srcMethodExecVxCell, dstMethodExecVxCell));
						srcMethodExecVxCellAbsPt = getAbsolutePointforCell(srcMethodExecVxCell);
						dstMethodExecVxCellAbsPt = getAbsolutePointforCell(dstMethodExecVxCell);
						cloneDstMethodExecVxCell[0] = (mxICell) mxgraph.addCell(dstMethodExecVxCell.clone());
						cloneDstMethodExecVxCell[0].getGeometry().setX(dstMethodExecVxCellAbsPt.getX());
						cloneDstMethodExecVxCell[0].getGeometry().setY(dstMethodExecVxCellAbsPt.getY());
						cloneDstMethodExecVxCell[0].setStyle("fillColor=none;strokeColor=none;fontColor=#008000;");
						cloneDstMethodExecVxCell[0].setValue(null);
						mxICell tmpEdgeCell = (mxICell) mxgraph.insertEdge(getMxDefaultParent(), null, null, srcMethodExecVxCell, cloneDstMethodExecVxCell[0]);
						tmpEdgeCell.setStyle("dashed=1;strokeColor=#008000;exitX=0.5;exitY=1;exitPerimeter=1;entryX=0.5;entryY=0;entryPerimeter=1;endArrow=none");
					} finally {
						mxgraph.getModel().endUpdate();
					}
				}

				// Animate an edge to shrink.
				MagnetRONAnimation edgeCellAnim = new TranslateAnimation(mxgraph, getGraphComponent());
				edgeCellAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
				edgeCellAnim.setDelay(getMagnetRONAnimationDelayMillis());
				edgeCellAnim.init(cloneDstMethodExecVxCell[0], srcMethodExecVxCellAbsPt.getX(), srcMethodExecVxCellAbsPt.getY() + srcMethodExecVxCell.getGeometry().getHeight(), threadPoolExecutor);
				edgeCellAnim.setOnFinished(new ActionListener() {
					@Override
					public void actionPerformed(java.awt.event.ActionEvent e) {
						// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
						mxgraph.getModel().beginUpdate();
						synchronized (mxgraph.getModel()) {
							try {
								// Test code (will be deleted)
								System.out.println(TAG + ": Shrink edge animation action performed.");
								mxgraph.removeCells(new Object[]{cloneDstMethodExecVxCell[0]});

								// TODO: Confirm execution order.
								if (!dstMethodExecVxCell.getParent().equals(getMxDefaultParent())) {
									// If parent of ObjectVertex cell isn't mxDefaltParent, reset parent.
									dstMethodExecVxCell.getParent().remove(dstMethodExecVxCell);
									dstMethodExecVxCell.setParent(getMxDefaultParent());
								}
								mxgraph.removeCells(new Object[] {dstMethodExecVxCell});
								update();
							} finally {
								mxgraph.getModel().endUpdate();
							}
						} 
					}
				});
				edgeCellAnim.play();

				if (!calledMethodExecution.isStatic()) {
					objectToVertexMap.get(calledMethodExecution.getThisObjId()).getMethodExecutionVertices().remove(methodExecToVertexMap.get(calledMethodExecution));
				} else {
					// TODO: Confirm why is this object id of the caller method used?
					String objId = calledMethodExecution.getCallerMethodExecution().getThisObjId();
					if (objId.matches("0")) {
						objId += ":" + calledMethodExecution.getCallerMethodExecution().getThisClassName();
					}
					objectToVertexMap.get(objId).getMethodExecutionVertices().remove(methodExecToVertexMap.get(calledMethodExecution));
				}
				methodExecToVertexMap.get(calledMethodExecution).getLocals().remove(sourceObjectVertex);
				methodExecToVertexMap.remove(calledMethodExecution);
				edgeMap.remove(methodExecution.getSignature());						
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			sleepMainThread(getAnimationDelayMillis());
		}
	}
	
	/**
	 * Create an edge between {@code MethodExecutions} while animating the edge to stretch.
	 */
	private void createEdgesToMethodExecutions() {
		List<MethodExecution> methodExecList = new ArrayList<>(methodExecToVertexMap.keySet());

		// TODO: Fix a bug where an edge orientation is reversed.
		for (int i = 0; i < methodExecList.size() - 1; i++) {
			MethodExecution srcMethodExec = methodExecList.get(i);
			MethodExecution dstMethodExec = methodExecList.get(i + 1);
			String methodSig = srcMethodExec.getSignature();
			
			if (!edgeMap.containsKey(methodSig)) {
				MagnetRONAnimation.waitAnimationEnd();

				// Draw an edge from sourceVertexCell to destinationVertexCell.
				mxICell srcMethodExecVxCell = (mxICell)methodExecToVertexMap.get(srcMethodExec).getCell();
				mxICell dstMethodExecVxCell = (mxICell)methodExecToVertexMap.get(dstMethodExec).getCell();
				Point2D srcMethodExecVxCellAbsPt = getAbsolutePointforCell(srcMethodExecVxCell);
				Point2D dstMethodExecVxCellAbsPt = getAbsolutePointforCell(dstMethodExecVxCell);

				scrollCellsToVisible(srcMethodExecVxCell.getParent(), dstMethodExecVxCell.getParent(), 2);
				
				try {
					final mxICell[] cloneDstMethodExecVxCell = new mxICell[1];
					// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
					mxgraph.getModel().beginUpdate();
					synchronized (mxgraph.getModel()) {
						try {
							cloneDstMethodExecVxCell[0] = (mxICell) mxgraph.addCell(dstMethodExecVxCell.clone());
							cloneDstMethodExecVxCell[0].getGeometry().setX(srcMethodExecVxCellAbsPt.getX());
							cloneDstMethodExecVxCell[0].getGeometry().setY(srcMethodExecVxCellAbsPt.getY() + dstMethodExecVxCell.getGeometry().getHeight());
							cloneDstMethodExecVxCell[0].setStyle("fillColor=none;strokeColor=none;fontColor=#008000;");
							cloneDstMethodExecVxCell[0].setValue(null);
							cloneDstMethodExecVxCell[0].setVisible(true);
							mxICell tmpEdgeCell = (mxICell) mxgraph.insertEdge(getMxDefaultParent(), null, null, srcMethodExecVxCell, cloneDstMethodExecVxCell[0]);
							tmpEdgeCell.setStyle("dashed=1;strokeColor=#008000;exitX=0.5;exitY=1;exitPerimeter=1;entryX=0.5;entryY=0;entryPerimeter=1;endArrow=none");
							dstMethodExecVxCell.setVisible(true);
							update();
						} finally {
							mxgraph.getModel().endUpdate();
						}
					}
					
					// Animate an edge to stretch.
					MagnetRONAnimation edgeCellAnim = new TranslateAnimation(mxgraph, getGraphComponent());
					edgeCellAnim.setTotalCycleCount(getMagnetRONAnimationTotalCycleCount());
					edgeCellAnim.setDelay(getMagnetRONAnimationDelayMillis());
					edgeCellAnim.init(cloneDstMethodExecVxCell[0], dstMethodExecVxCellAbsPt.getX(), dstMethodExecVxCellAbsPt.getY(), threadPoolExecutor);
					edgeCellAnim.setOnFinished(new ActionListener() {
						@Override
						public void actionPerformed(java.awt.event.ActionEvent e) {
							// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
							mxgraph.getModel().beginUpdate();
							synchronized (mxgraph.getModel()) {
								try {
									// Test code (will be deleted)
									System.out.println(TAG + ": Stretch edge animation action performed. ");
									mxICell edgeCell = (mxICell) mxgraph.insertDeltaEdge(getMxDefaultParent(), methodSig, null, srcMethodExecVxCell, dstMethodExecVxCell);
									if (!edgeCell.getParent().equals(getMxDefaultParent())) {
										// If parent of Edge cell isn't mxDefaltParent, reset parent.
										edgeCell.getParent().remove(edgeCell);
										edgeCell.setParent(getMxDefaultParent());
									}
									mxgraph.orderCells(false, new Object[] {edgeCell});
									edgeCell.setStyle("exitX=0.5;exitY=1;exitPerimeter=1;entryX=0.5;entryY=0;entryPerimeter=1;");
									edgeMap.put(methodSig, new Edge(methodSig, TypeName.Call, edgeCell));
									mxgraph.removeCells(new Object[]{cloneDstMethodExecVxCell[0]});
									update();
								} finally {
									mxgraph.getModel().endUpdate();
								}
							}
						}
					});
					edgeCellAnim.play();					
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/** 
	 * Styles all cells on the graph. 
	 */
	protected void setCellsStyle() {
		List<Object> objVxs = new ArrayList<>();
		List<Object> staticObjVxs = new ArrayList<>();
		List<Object> alignMidObjVxs = new ArrayList<>();
		List<Object> alignTopObjVxs = new ArrayList<>();
		List<Object> refEdges = new ArrayList<>();
		List<Object> refCreateEdges = new ArrayList<>();
		List<Object> methodExecEdges = new ArrayList<>();
		List<Object> roundEdges = new ArrayList<>();
	
		for (Entry<String, ObjectVertex> objectToVertexEntry: objectToVertexMap.entrySet()) {
			String objId = objectToVertexEntry.getKey();
			ObjectVertex objVx = objectToVertexEntry.getValue();
			if (objId.startsWith("0:")) {
				staticObjVxs.add(objVx.getCell());
			} else {
				objVxs.add(objVx.getCell());
			}
			if(objVx.getMethodExecutionVertices().size() == 0) {
				alignMidObjVxs.add(objVx.getCell());
			} else {
				alignTopObjVxs.add(objVx.getCell());
			}
		}
	
		List<MethodExecutionVertex> methodExecVxList = new ArrayList<>(methodExecToVertexMap.values());
		Collections.reverse(methodExecVxList);
		for (int i = 0; i < methodExecVxList.size(); i++) {
			mxICell methodExecVxCell = (mxICell) methodExecVxList.get(i).getCell();
			if (i == 0) {
				methodExecVxCell.setStyle("fillColor=#ff7fbf");
			} else if (i == 1) {
				methodExecVxCell.setStyle("fillColor=#ff99cc");				
			} else if (i == 2) {
				methodExecVxCell.setStyle("fillColor=#ffb2d8");				
			} else if (i == 3) {
				methodExecVxCell.setStyle("fillColor=#ffcce5");				
			} else if (i == 4) {
				methodExecVxCell.setStyle("fillColor=#ffe0ef");				
			} else {
				break;
			}
		}
	
		for (Edge edge: edgeMap.values()) {
			roundEdges.add(edge.getCell());
			switch(edge.getTypeName()) {
			case Reference:
				refEdges.add(edge.getCell());
				break;
			case Create:
				refEdges.add(edge.getCell());
				refCreateEdges.add(edge.getCell());
				break;
			case Call:
				methodExecEdges.add(edge.getCell());
				break;
			default:
				break;
			}
		}
	
		// Styles ObjectVertex.
		mxgraph.setCellStyles(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE, objVxs.toArray(new Object[objVxs.size()]));
		mxgraph.setCellStyles(mxConstants.STYLE_PERIMETER, mxConstants.PERIMETER_ELLIPSE, objVxs.toArray(new Object[objVxs.size()]));
		mxgraph.setCellStyleFlags(mxConstants.STYLE_FONTSTYLE, mxConstants.FONT_UNDERLINE, true, objVxs.toArray(new Object[objVxs.size()]));
		mxgraph.setCellStyles(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE, alignMidObjVxs.toArray(new Object[alignMidObjVxs.size()]));
		mxgraph.setCellStyles(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_TOP, alignTopObjVxs.toArray(new Object[alignTopObjVxs.size()]));
		// Styles Edge.
		mxgraph.setCellStyles(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_TOPTOBOTTOM, refEdges.toArray(new Object[refEdges.size()]));
		mxgraph.setCellStyleFlags(mxConstants.STYLE_DASHED, 1, true, refCreateEdges.toArray(new Object[refCreateEdges.size()]));
		mxgraph.setCellStyleFlags(mxConstants.STYLE_ROUNDED, 1, true, roundEdges.toArray(new Object[roundEdges.size()]));
		mxgraph.setCellStyles(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_TOP, roundEdges.toArray(new Object[roundEdges.size()]));
		mxgraph.setCellStyles(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_BOTTOM, roundEdges.toArray(new Object[roundEdges.size()]));
		// Styles MethodExecutionVertex.		
		mxgraph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#008000", methodExecEdges.toArray(new Object[methodExecEdges.size()]));
		mxgraph.setCellStyleFlags(mxConstants.STYLE_DASHED, 1, true, methodExecEdges.toArray(new Object[methodExecEdges.size()]));
	}

	protected Point2D getAbsolutePointforCell(mxICell cell) {
		Point2D p1 = new Point2D.Double(cell.getGeometry().getX(), cell.getGeometry().getY());;
		if(cell.getParent() == null 
				|| cell.getParent().getValue() == null
				|| cell.equals(cell.getParent())) {
			return p1;
		}
		Point2D p2 = getAbsolutePointforCell(cell.getParent());
		return new Point2D.Double(p1.getX() + p2.getX(), p1.getY() + p2.getY());
	}
	
 	/**
	 *  Update the graph on the JFrame by styling the cells and refreshing the mxgraphComponent. 
	 */
	protected void update() {
		/* Given a cell, we can change it's style attributes, for example the color. 
		 * NOTE that you have to call the graphComponent.refresh() function, 
		 * otherwise you won't see the difference! */
		setCellsStyle();
		getGraphComponent().refresh();
	}
		
	protected void scrollCellToVisible(mxICell cell, boolean center) {
		if (isAutoTracking()) {
			getGraphComponent().scrollCellToVisible(cell, center);
		}		
	}
	
	private void scrollCellsToVisible(mxICell cell1, mxICell cell2) {
		scrollCellsToVisible(cell1, cell2, 1);
	}
	
	private void scrollCellsToVisible(mxICell cell1, mxICell cell2, int priorityArgumentsIndex) {
		if (isAutoTracking()) {
			Point2D p1 = getAbsolutePointforCell(cell1);
			Point2D p2 = getAbsolutePointforCell(cell2);
			if (p1.getX() <= p2.getX()) {
				if (p1.getY() <= p2.getY()) {
					double p2X = p2.getX() + cell2.getGeometry().getWidth();
					double p2Y = p2.getY() + cell2.getGeometry().getHeight();
					p2.setLocation(p2X, p2Y);
				} else {
					double p1Y = p1.getY() + cell1.getGeometry().getHeight();
					double p2X = p2.getX() + cell2.getGeometry().getWidth();
					p1.setLocation(p1.getX(), p1Y);
					p2.setLocation(p2X, p2.getY());
				}
			} else {
				if (p1.getY() <= p2.getY()) {
					double p1X = p1.getX() + cell1.getGeometry().getWidth();
					double p2Y = p2.getY() + cell2.getGeometry().getHeight();
					p1.setLocation(p1X, p1.getY());
					p2.setLocation(p2.getX(), p2Y);
				} else {
					double p1X = p1.getX() + cell1.getGeometry().getWidth();
					double p1Y = p1.getY() + cell1.getGeometry().getHeight();
					p1.setLocation(p1X, p1Y);
				}
			}
			scrollPointsToVisible(p1, p2, false, priorityArgumentsIndex);
		}
	}
	
	private void scrollCellAndPointToVisible(mxICell cell1, Point2D p2, int priorityArgumentsIndex) {
		if (isAutoTracking()) {
			Point2D p1 = getAbsolutePointforCell(cell1);
			if (p1.getX() <= p2.getX()) {
				if (p1.getY() <= p2.getY()) {
					double p2X = p2.getX() + DEFAULT_OBJECT_VERTEX_SIZE.getWidth();
					double p2Y = p2.getY() + DEFAULT_OBJECT_VERTEX_SIZE.getHeight();
					p2.setLocation(p2X, p2Y);
				} else {
					double p1Y = p1.getY() + cell1.getGeometry().getHeight();
					double p2X = p2.getX() + DEFAULT_OBJECT_VERTEX_SIZE.getWidth();
					p1.setLocation(p1.getX(), p1Y);
					p2.setLocation(p2X, p2.getY());
				}
			} else {
				if (p1.getY() <= p2.getY()) {
					double p1X = p1.getX() + cell1.getGeometry().getWidth();
					double p2Y = p2.getY() + DEFAULT_OBJECT_VERTEX_SIZE.getHeight();
					p1.setLocation(p1X, p1.getY());
					p2.setLocation(p2.getX(), p2Y);
				} else {
					double p1X = p1.getX() + cell1.getGeometry().getWidth();
					double p1Y = p1.getY() + cell1.getGeometry().getHeight();
					p1.setLocation(p1X, p1Y);
				}
			}
			scrollPointsToVisible(p1, p2, false, priorityArgumentsIndex);
		}
	}

	/**
	 * 
	 * 
	 * @param p1
	 * @param p2
	 * @param center
	 * @param priorityArgumentsIndex: 1 is the default value, indicating that 1st argument(p1) of this method should be preferred.
	 */
	private void scrollPointsToVisible(Point2D p1, Point2D p2, boolean center, int priorityArgumentsIndex) {
		if (isAutoTracking()) {
			Rectangle rec = new Rectangle();
			int p1X = (int) p1.getX(), p1Y = (int) p1.getY();
			int p2X = (int) p2.getX(), p2Y = (int) p2.getY();
			if (p1X <= p2X) {
				if (p1Y <= p2Y) {
					rec.setBounds(p1X, p1Y, p2X - p1X, p2Y - p1Y);
					if (rec.getWidth() > super.getWidth() || rec.getHeight() > super.getHeight()) {
						switch(priorityArgumentsIndex) {
							case 1:
								break;
							case 2:
								int x = (int) (rec.getX() + (rec.getWidth() - super.getWidth()));
								int y = (int) (rec.getY() + (rec.getHeight() - super.getHeight()));
								rec.setLocation(x, y);
								break;
						}
						rec.setSize(super.getWidth(), super.getHeight());
					}
				} else {
					rec.setBounds(p1X, p2Y, p2X - p1X, p1Y - p2Y);
					if (rec.getWidth() > super.getWidth() || rec.getHeight() > super.getHeight()) {
						switch(priorityArgumentsIndex) {
							case 1:
								int y = (int) (rec.getY() + (rec.getHeight() - super.getHeight()));
								rec.setLocation((int) rec.getX(), y);
								break;
							case 2:
								int x = (int) (rec.getX() + (rec.getWidth() - super.getWidth()));
								rec.setLocation(x, (int) rec.getY());
								break;
						}
						rec.setSize(super.getWidth(), super.getHeight());
					}
				}
			} else {
				if (p1Y <= p2Y) {
					rec.setBounds(p2X, p1Y, p1X - p2X, p2Y - p1Y);
					if (rec.getWidth() > super.getWidth() || rec.getHeight() > super.getHeight()) {
						switch(priorityArgumentsIndex) {
							case 1:
								int x = (int) (rec.getX() + (rec.getWidth() - super.getWidth()));
								rec.setLocation(x, (int) rec.getY());
								break;
							case 2:
								int y = (int) (rec.getY() + (rec.getHeight() - super.getHeight()));
								rec.setLocation((int) rec.getX(), y);
								break;
						}
						rec.setSize(super.getWidth(), super.getHeight());
					}
				} else {
					rec.setBounds(p2X, p2Y, p1X - p2X, p1Y - p2Y);
					if (rec.getWidth() > super.getWidth() || rec.getHeight() > super.getHeight()) {
						switch(priorityArgumentsIndex) {
							case 1:
								int x = (int) (rec.getX() + (rec.getWidth() - super.getWidth()));
								int y = (int) (rec.getY() + (rec.getHeight() - super.getHeight()));
								rec.setLocation(x, y);																
								break;
							case 2:
								break;
						}
						rec.setSize(super.getWidth(), super.getHeight());
					}
				}
			}
			
			if (center) {
				int x = (int) (rec.getCenterX() - super.getWidth() / 2);
				int y = (int) (rec.getCenterY() - super.getHeight() / 2);
				rec.setBounds(x, y, super.getWidth(), super.getHeight());
			}
			
			scrollRectToVisible(rec);
		}
	}

	@Override
	public void scrollRectToVisible(Rectangle rec) {
		Rectangle visibleRec = getGraphComponent().getGraphControl().getVisibleRect();
		if (isAutoTracking() && !rec.contains(visibleRec)) {
			System.out.println(TAG + ": Before scroll visibleRect=" + getGraphComponent().getGraphControl().getVisibleRect());
			getGraphComponent().getGraphControl().scrollRectToVisible(rec);
			System.out.println(TAG + ": After scroll visibleRect=" + getGraphComponent().getGraphControl().getVisibleRect());
		}
	}

	public boolean isSkipBackAnimation() {
		return this.fSkipBackAnimation;
	}

	private boolean isAutoTracking() {
		return this.fAutoTracking;
	}

	public void setSkipBackAnimation(boolean fSkipBackAnimation) {
		this.fSkipBackAnimation  = fSkipBackAnimation;
	}

	public void setAutoTracking(boolean fAutoTracking) {
		if (fAutoTracking != isAutoTracking()) {
			this.fAutoTracking = fAutoTracking;
		}
	}
	
	protected void setCurrentFrame(int numberFrame) {
		this.curFrame = numberFrame;
	}

	protected void setSkipBackFrame(int numberFrame) {
		this.skipBackFrame = numberFrame;
	}

	public void setAnimationSpeed(double animationSpeed) {
		System.out.println(TAG + ": animationSpeed=" + animationSpeed);
		this.animationSpeed = animationSpeed;
	}

	protected int getCurrentFrame() {
		return this.curFrame;
	}

	public int getSkipBackFrame() {
		return this.skipBackFrame;
	}

	public double getAnimationSpeed() {
		return this.animationSpeed;
	}

	protected long getAnimationDelayMillis() {
		return (long) (this.animationDelayMillis / getAnimationSpeed());
	}

	protected int getMagnetRONAnimationTotalCycleCount() {
		return (int) (this.magnetRONAnimationTotalCycleCount / getAnimationSpeed());
	}

	protected long getMagnetRONAnimationDelayMillis() {
		return (long) (this.magnetRONAnimationDelayMillis / getAnimationSpeed());
	}

	protected static String[] formatFieldName(String fieldName) {
		String fieldNames[] = fieldName.split("\\.");
		String names[] = new String[] {fieldNames[0], fieldNames[fieldNames.length - 1]};
		for(int i = 1; i < fieldNames.length - 1; i++) {
			names[0] += "." + fieldNames[i];
		}
		return names;
	}

	protected String formatClassName(String className) {
		// Step1 : split "."
		String[] classNames = className.split("\\.");
		return classNames[classNames.length - 1];
	}

	protected String formatMethodSignature(String methodSignature, String thisClassName) {
		// TODO: Modify algorithm formatMethodSignature().
		// Step1 : split "("
		methodSignature = methodSignature.substring(0, methodSignature.lastIndexOf('('));
		// Step2 : split " "
		String[] methodSigs = methodSignature.split(" ");
		String tmpMethodSig = methodSigs[methodSigs.length-1];
		// Step3 : split "."
		String[] thisClassNames = thisClassName.split("\\.");
		methodSigs = tmpMethodSig.split("\\.");
		StringBuffer sb = new StringBuffer();
		int i = methodSigs.length - 2;
		int count = methodSignature.split("\\(").length - 1;
		if (count > 0) i -= count;
		if (i >= 0 && !thisClassNames[thisClassNames.length - 1].equals(methodSigs[i])) {
			if (thisClassNames[thisClassNames.length - 1].equals(methodSigs[i + 1]) || methodSigs[i].matches("[0-9]")) i += 1;
			sb.append(methodSigs[i]);
			if (methodSigs.length - i > 1) sb.append(".");
		}
		for (i = i + 1; i < methodSigs.length; i++) {
			sb.append(methodSigs[i]);
			if (methodSigs.length - i > 1) sb.append(".");
		}
		sb.append("()");
		
		String newMethodSignature = sb.toString();
		if (!newMethodSignature.isEmpty()) {
			return newMethodSignature;			
		}
		return methodSignature;
	}

	protected String formatArrayName(String srcClassName) {
		// Step1 : remove "[L"
		StringBuffer sb = new StringBuffer();
		sb.append(srcClassName.substring(2, srcClassName.length()-1));
		sb.append("[]");
		return sb.toString();		
	}

	protected String formatArrayIndex(int index) {
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		sb.append(index);
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Test code (will be deleted)
	 */
	protected void outputLog() {
		for (Object obj: mxgraph.getChildCells(getMxDefaultParent())) {
			System.out.println(obj + " " + obj.hashCode());
			for (int i = 0; i < ((mxICell)obj).getChildCount(); i++) {
				System.out.println("   " + ((mxICell)obj).getChildAt(i) + " " + obj.hashCode());
			}
		}
		System.out.println("\nObject");
		for (Entry<String, ObjectVertex> e: objectToVertexMap.entrySet()) {
			String objId = e.getKey();
			ObjectVertex vo = e.getValue();
			if (vo.getCell() != null) {
				System.out.println(vo.getLabel() + " (" + objId + ")" + " " + vo.getCell().hashCode());				
			} else {
				System.out.println(vo.getLabel() + " (" + objId + ")");								
			}
			for (MethodExecutionVertex vme: vo.getMethodExecutionVertices()) {
				System.out.println("   " + vme.getLabel());
				for (ObjectVertex vmevo: vme.getArguments()) {
					System.out.println("      Argument: " + vmevo.getLabel());					
				}
				for (ObjectVertex vmevo: vme.getLocals()) {
					System.out.println("      Local: " + vmevo.getLabel());					
				}
			}
		}
		System.out.println("\nEdge");
		for (Edge e: edgeMap.values()) {
			System.out.println(e.getLabel() + "(" + ((mxICell)e.getCell()).getId() + ")");
			if (((mxICell)e.getCell()).getParent() != null) {
				System.out.println(" " + ((mxICell)e.getCell()).getParent().getId());
			}
		}
	}
	
	/**
	 * Whether parents of source mxICell contain destination mxICell.
	 * 
	 * @param sourceCell
	 * @param destinationCell
	 * @return
	 */
	private boolean isParent(mxICell sourceCell, mxICell destinationCell) {
		mxICell srcParentCell = sourceCell.getParent();
		if (srcParentCell == null || srcParentCell.getValue() == null || destinationCell == null) {
			return false;
		}
		if (srcParentCell.equals(destinationCell)) {
			return true;
		}
		return isParent(srcParentCell, destinationCell);
	}
	
	public static Map.Entry<Reference, String> getRelatedInformation(TracePoint relatedPoint, IAliasCollector ac) {
		Statement rpStatement = relatedPoint.getStatement();
		String rpSrcObjId = null;
		String rpDstObjId = null;
		String rpSrcClassName = null;
		String rpDstClassName = null;
		String rpFieldName = null;
	
		// Search for relatedPoint objectReference srcClassName, fieldName.
		if (relatedPoint.isMethodEntry()) {
			// this to another (parameter)
			Alias lastAlias = ac.getAliasList().get(ac.getAliasList().size() - 1);
			if (lastAlias.getAliasType() == Alias.AliasType.FORMAL_PARAMETER) {
				rpSrcObjId = relatedPoint.getMethodExecution().getThisObjId();
				rpDstObjId = lastAlias.getObjectId();
				rpFieldName = "";
				rpSrcClassName = relatedPoint.getMethodExecution().getThisClassName();
				for (ObjectReference r: lastAlias.getMethodExecution().getArguments()) {
					if (r.getId().equals(rpDstObjId)) {
						rpDstClassName =  r.getActualType();
						break;
					}
				}
			}			
		}
		if (rpSrcObjId == null || rpDstObjId == null) {
			if (rpStatement instanceof FieldUpdate) {
				// Format fieldName.
				FieldUpdate rpFieldUpdateStatement = (FieldUpdate) rpStatement;
				rpSrcObjId = rpFieldUpdateStatement.getContainerObjId();
				rpDstObjId = rpFieldUpdateStatement.getValueObjId();
				if (rpFieldUpdateStatement.getFieldName() != null) {
					String rpFieldNames[] = formatFieldName(rpFieldUpdateStatement.getFieldName());
					rpSrcClassName = rpFieldNames[0];
					rpFieldName = rpFieldNames[rpFieldNames.length-1];
				} else {
					rpSrcClassName = rpFieldUpdateStatement.getContainerClassName();
					rpFieldName = "";
				}
				rpDstClassName = rpFieldUpdateStatement.getValueClassName();
			} else if (rpStatement instanceof ArrayUpdate) {
				// container to component
				ArrayUpdate rpArrayUpdateStatement = (ArrayUpdate) rpStatement;
				rpSrcObjId = rpArrayUpdateStatement.getArrayObjectId();
				rpDstObjId = rpArrayUpdateStatement.getValueObjectId();
				rpSrcClassName = rpArrayUpdateStatement.getArrayClassName();
				rpDstClassName = rpArrayUpdateStatement.getValueClassName();
				rpFieldName = "[" + rpArrayUpdateStatement.getIndex() + "]";
			} else if(rpStatement instanceof MethodInvocation) {
				MethodInvocation rpMethodInvStatement = (MethodInvocation) rpStatement;
				MethodExecution rpCalledMethodExec = rpMethodInvStatement.getCalledMethodExecution();
				String rpMethodSig = rpCalledMethodExec.getSignature();
	
				//ArrayやListのときだけラベルを付ける（確実に分かっているものとき)getSignature->contains("List.get(") || "Map.get(") <ホワイトリスト>
//					if (rpMethodExec.getSignature().contains("List.add(") ||
//							rpMethodExec.getSignature().contains("Map.put(")) {
				if (rpCalledMethodExec.isCollectionType()
						&& (rpMethodSig.contains("add(") 
								|| rpMethodSig.contains("set(") 
								|| rpMethodSig.contains("put(") 
								|| rpMethodSig.contains("push(") 
								|| rpMethodSig.contains("addElement("))) {
					
					rpSrcClassName = rpCalledMethodExec.getThisClassName();
					rpDstClassName = rpCalledMethodExec.getArguments().get(0).getActualType();
					rpSrcObjId = rpCalledMethodExec.getThisObjId();
					rpDstObjId = rpCalledMethodExec.getArguments().get(0).getId();
				} else {
					// this to another
					rpSrcClassName = rpMethodInvStatement.getThisClassName();
					rpDstClassName = rpCalledMethodExec.getReturnValue().getActualType();
					rpSrcObjId = rpMethodInvStatement.getThisObjId();
					rpDstObjId = rpCalledMethodExec.getReturnValue().getId();
				}
			}
		}
		return new AbstractMap.SimpleEntry<>(new Reference(rpSrcObjId, rpDstObjId, rpSrcClassName, rpDstClassName), rpFieldName);		
	}
	
	public void sleepMainThread(long millis) {
		try {
			// Test code (will be deleted)
			System.out.println(TAG + ": Sleep Main thread " + millis + "millis. ThreadId=" + Thread.currentThread().getId());
			Thread.sleep(millis);
			System.out.println(TAG + ": Resume Main thread.");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}

	
	protected class CurvedCanvas extends mxInteractiveCanvas {
		mxIShape shape = new CurvedConnector();

		public CurvedCanvas(mxGraphComponent mxGraphComponent) {
			super(mxGraphComponent);
		}

		public Object drawCell(mxCellState state) {
			if (!(state.getCell() instanceof mxCell) || !((mxCell)state.getCell()).isEdge() || state.getAbsolutePointCount() == 2) {
				return super.drawCell(state);
			}
			Map<String, Object> style = state.getStyle();

			if (g != null) {
				// Creates a temporary graphics instance for drawing this shape
				float opacity = mxUtils.getFloat(style, mxConstants.STYLE_OPACITY, 100);
				Graphics2D previousGraphics = g;
				g = createTemporaryGraphics(style, opacity, state);
				shape.paintShape(this, state);				
				g.dispose();
				g = previousGraphics;
			}

			return shape;
		}
	}

	protected class CurvedConnector extends mxConnectorShape {
		public void paintShape(mxGraphics2DCanvas canvas, mxCellState state) {
			if (state.getAbsolutePointCount() > 1
					&& configureGraphics(canvas, state, false)) {
				List<mxPoint> pts = new ArrayList<mxPoint>(
						state.getAbsolutePoints());
				Map<String, Object> style = state.getStyle();

				// Paints the markers and updates the points
				// Switch off any dash pattern for markers
				boolean dashed = mxUtils.isTrue(style, mxConstants.STYLE_DASHED);
				Object dashedValue = style.get(mxConstants.STYLE_DASHED);

				if (dashed) {
					style.remove(mxConstants.STYLE_DASHED);
					canvas.getGraphics().setStroke(canvas.createStroke(style));
				}

				translatePoint(pts, 0,
						paintMarker(canvas, state, true));
				translatePoint(
						pts,
						pts.size() - 1,
						paintMarker(canvas, state, false));

				if (dashed) {
					// Replace the dash pattern
					style.put(mxConstants.STYLE_DASHED, dashedValue);
					canvas.getGraphics().setStroke(canvas.createStroke(style));
				}

				// Paints the shape and restores the graphics object
				if (state.getAbsolutePointCount() == 4) {
					double sx = state.getAbsolutePoint(0).getX();
					double sy = state.getAbsolutePoint(0).getY();
//					double tx1 = state.getAbsolutePoint(1).getX();
//					double ty1 = state.getAbsolutePoint(1).getY();
					double tx2 = state.getAbsolutePoint(2).getX();
					double ty2 = state.getAbsolutePoint(2).getY();					
					double ex = state.getAbsolutePoint(3).getX();
					double ey = state.getAbsolutePoint(3).getY();
					Path2D.Double p = new Path2D.Double();
					p.moveTo((int) sx, (int) sy);
					p.quadTo((int) tx2, (int) ty2, (int) ex, (int) ey);
//					p.curveTo((int) tx1, (int) ty1, (int) tx2, (int) ty2, (int) ex, (int) ey);
					canvas.getGraphics().draw(p);
				} else if (state.getAbsolutePointCount() == 3) {
					double sx = state.getAbsolutePoint(0).getX();
					double sy = state.getAbsolutePoint(0).getY();
					double tx = state.getAbsolutePoint(1).getX();
					double ty = state.getAbsolutePoint(1).getY();
					double ex = state.getAbsolutePoint(2).getX();
					double ey = state.getAbsolutePoint(2).getY();
					Path2D.Double p = new Path2D.Double();
					p.moveTo((int) sx, (int) sy);
					p.quadTo((int) tx, (int) ty, (int) ex, (int) ey);
					canvas.getGraphics().draw(p);
				}
			}
		}

		private void translatePoint(List<mxPoint> points, int index, mxPoint offset) {
			if (offset != null) {
				mxPoint pt = (mxPoint) points.get(index).clone();
				pt.setX(pt.getX() + offset.getX());
				pt.setY(pt.getY() + offset.getY());
				points.set(index, pt);
			}
		}
	}
}
