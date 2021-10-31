package org.ntlab.deltaViewer;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxPoint;

/**
 * Delta animation for MagnetRON.
 * 
 * @author Nitta Lab.
 */
public class DeltaAnimation {

	private DeltaGraphAdapter mxgraph;
	private mxGraphComponent mxgraphComponent;

	private Graphics2D graphics2D;
	private static int FINAL_STEP_COUNT = 10;

	private mxICell sourceCell;

	GeneralPath p = new GeneralPath();

	// X, Y
	private mxPoint sourcePoint;
	private mxPoint targetPoint;
	private mxPoint curPoint;
	private mxPoint updatePoint = new mxPoint();

	// Width, Height
	private Dimension targetDimension;
	private Dimension curDimension = new Dimension();
	private Dimension updateDimension = new Dimension();

	private double scale = 1;
	
	/**
	 * @param mxgraph
	 * @param mxgraphComponent
	 */
	public DeltaAnimation(DeltaGraphAdapter mxgraph, mxGraphComponent mxgraphComponent) {
		this.mxgraph = mxgraph;
		this.mxgraphComponent = mxgraphComponent;
		graphics2D = (Graphics2D)mxgraphComponent.getGraphics();
	}

	/**
	 * @param zoomLevel	Zoom level of MagnetRON view.
	 */
	public void setScale(double zoomLevel) {
		this.scale = zoomLevel;
	}
	
	/**
	 * Set to move animation source cell vertex to targetPoint.
	 * 
	 * @param sourceCell	Vertex.
	 * @param targetPoint XY coordinates.
	 */
	public void setVertexAnimation(mxICell sourceCell, mxPoint targetPoint) {
		this.sourceCell = sourceCell;
		this.targetPoint = targetPoint;
		curPoint = new mxPoint(sourceCell.getGeometry().getX(), sourceCell.getGeometry().getY());
		calculateResizeLineModel();
	}

	/**
	 * Set stretch(expand) animation of edge from sourcePoint to targetPoint.
	 * 
	 * @param cloneTargetVertexCell	Edge sourcePoint.
	 * @param targetPoint	Edge targetPoint.
	 */
	public void setExpandEdgeAnimation(mxICell sourceCell, mxPoint targetPoint) {
		this.sourceCell = sourceCell;
		this.targetPoint = targetPoint;
		curPoint = new mxPoint(sourceCell.getGeometry().getX(), sourceCell.getGeometry().getY());
		System.out.println("sourcePoint : " + sourceCell.getGeometry().getPoint());
		System.out.println("targetPoint : " + targetPoint);
		calculateResizeLineModel();
	}

	/**
	 * Set to move animation source cell vertex clone to targetPoint, reduce edge length.
	 * 
	 * @param sourceCell Remove sourceCell vertex clone.
	 * @param targetPoint
	 */
	public void setReductionEdgeAnimation(mxICell sourceCell, mxPoint targetPoint) {
		this.sourceCell = sourceCell;
		this.targetPoint = targetPoint;
		curPoint = new mxPoint(sourceCell.getGeometry().getX(), sourceCell.getGeometry().getY());
//		System.out.println("sourcePoint : " + sourceCell.getGeometry().getPoint());
//		System.out.println("targetPoint : " + targetPoint);
		calculateResizeLineModel();
	}

	/**
	 * Set animation resize vertex.
	 * 
	 * @param sourceCell
	 * @param targetDimension Vertex (Width, Height)
	 */
	public void setResizeVertexAnimation(mxICell sourceCell, Dimension targetDimension) {
		this.sourceCell = sourceCell;
		this.targetDimension = targetDimension;
		curDimension.setSize(sourceCell.getGeometry().getWidth(), sourceCell.getGeometry().getHeight());
		calculateResizeVertexModel();
	}

	/**
	 * Calculate updatePoint every second from curPoint and targetPoint.
	 */
	public void calculateResizeLineModel() {
		mxPoint distancePoint = new mxPoint();
		distancePoint.setX(targetPoint.getX() - curPoint.getX()); 
		distancePoint.setY(targetPoint.getY() - curPoint.getY());
		updatePoint.setX(distancePoint.getX() / FINAL_STEP_COUNT);
		updatePoint.setY(distancePoint.getY() / FINAL_STEP_COUNT);
		System.out.println(updatePoint);
	}

	/**
	 * Calculate updateDimension every second from curPoint and targetPoint.
	 */
	public void calculateResizeVertexModel() {
		Dimension distanceDimension = new Dimension();
		distanceDimension.setSize(targetDimension.getWidth() - curDimension.getWidth(), targetDimension.getHeight() - curDimension.getWidth());
		updateDimension.setSize(distanceDimension.getWidth() / FINAL_STEP_COUNT, distanceDimension.getHeight() / FINAL_STEP_COUNT);
	}

	/**
	 * Start animation to move source cell vertex to targetPoint for 10sec.
	 */
	public void startVertexAnimation() {
		ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
		scheduledThreadPoolExecutor.scheduleWithFixedDelay(new TimerTask() {
			int stepCount = 0;

			@Override
			public void run() {
				if(stepCount < FINAL_STEP_COUNT) {
					updateVertexAnimation();
					System.out.println("updateVertexAnimation: " + stepCount + " " + curPoint.getX());
					stepCount++;
					if(stepCount >= FINAL_STEP_COUNT){
						scheduledThreadPoolExecutor.shutdown();
					}
				}
			}
		}, 0, 100, TimeUnit.MILLISECONDS);
//		sleepThread(doThreadSleep);
//		try {
//			Thread.sleep(1001);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}

	/**
	 * Start stretch(expand) animation of edge from sourcePoint to targetPoint for 10sec.
	 */
	public void startExpandEdgeAnimation() {
		ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
		scheduledThreadPoolExecutor.scheduleWithFixedDelay(new TimerTask() {
			int stepCount = 0;

			@Override
			public void run() {
				if(stepCount < FINAL_STEP_COUNT) {
					updateExpandEdgeAnimation();
					System.out.println("updateExpandEdgeAnimation: " + stepCount + " " + curPoint.getX());
					stepCount++;
					if(stepCount >= FINAL_STEP_COUNT){
						scheduledThreadPoolExecutor.shutdown();
					}
				}
			}
		}, 0, 100, TimeUnit.MILLISECONDS);
//		sleepThread(doThreadSleep);
//		try {
//			Thread.sleep(1001);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}

	/**
	 * Start move animation source cell vertex clone to targetPoint, reduce edge length for 10sec.
	 */
	public void startReductionEdgeAnimation() {
		ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
		scheduledThreadPoolExecutor.scheduleWithFixedDelay(new TimerTask() {
			int stepCount = 0;

			@Override
			public void run() {
				if(stepCount < FINAL_STEP_COUNT) {
					updateReductionEdgeAnimation();
					System.out.println(stepCount + ": " + curPoint.getX());
					stepCount++;
					if(stepCount >= FINAL_STEP_COUNT){
						scheduledThreadPoolExecutor.shutdown();
					}
				}
			}
		}, 0, 100, TimeUnit.MILLISECONDS);
//		sleepThread(doThreadSleep);
//		try {
//			System.out.println("Thread.sleep()");
//			Thread.sleep(1001);
//			System.out.println("Thread.start()");
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}

	/**
	 * Start animation resize vertex for 10sec.
	 */
	public void startResizeVertexAnimation() {
		ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
		scheduledThreadPoolExecutor.scheduleWithFixedDelay(new TimerTask() {
			int stepCount = 0;

			@Override
			public void run() {
				if(stepCount < FINAL_STEP_COUNT) {
					updateResizeVertexAnimation();
					System.out.println(stepCount + ": " + curDimension.width);
					stepCount++;
					if(stepCount >= FINAL_STEP_COUNT){
						scheduledThreadPoolExecutor.shutdown();
					}
				}
			}
		}, 0, 100, TimeUnit.MILLISECONDS);
//		sleepThread(doThreadSleep);
//		try {
//			System.out.println("Thread.sleep()");
//			Thread.sleep(1001);
//			System.out.println("Thread.start()");
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}

	/**
	 * Update animation to move source cell vertex to targetPoint every second.
	 */
	private void updateVertexAnimation() {
		// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
		mxgraph.getModel().beginUpdate();
		try {
			System.out.println(sourceCell);
			curPoint.setX(sourceCell.getGeometry().getX());
			curPoint.setY(sourceCell.getGeometry().getY());
			sourceCell.getGeometry().setX(curPoint.getX() + updatePoint.getX());
			sourceCell.getGeometry().setY(curPoint.getY() + updatePoint.getY());
//			System.out.println(sourceCell.getGeometry().getPoint());
		} finally {
			mxgraph.getModel().endUpdate();
		}
		mxgraphComponent.refresh();
	}

	/**
	 * Update stretch(expand) animation of edge length updatePoint every second.
	 */
	private void updateExpandEdgeAnimation() {
		// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
		mxgraph.getModel().beginUpdate();
		try {
			curPoint.setX(sourceCell.getGeometry().getX());
			curPoint.setY(sourceCell.getGeometry().getY());
			sourceCell.getGeometry().setX(curPoint.getX() + updatePoint.getX());
			sourceCell.getGeometry().setY(curPoint.getY() + updatePoint.getY());
//			System.out.println(sourceCell.getGeometry().getPoint());
		} finally {
			  mxgraph.getModel().endUpdate();
		}
    	mxgraphComponent.refresh();
	}

	/**
	 * Update move animation sourcell vertex clone to targetPoint, reduce edge length updatePoint every second.
	 */
	private void updateReductionEdgeAnimation() {
		// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
		mxgraph.getModel().beginUpdate();
		try {
			curPoint.setX(sourceCell.getGeometry().getX());
			curPoint.setY(sourceCell.getGeometry().getY());
			sourceCell.getGeometry().setX(curPoint.getX() + updatePoint.getX());
			sourceCell.getGeometry().setY(curPoint.getY() + updatePoint.getY());
//			System.out.println(sourceCell.getGeometry().getPoint());
		} finally {
			mxgraph.getModel().endUpdate();
		}
		mxgraphComponent.refresh();
	}

	/**
	 * Update animation resize vertex every second.
	 */
	private void updateResizeVertexAnimation() {
		// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.	
		mxgraph.getModel().beginUpdate();
		try {
			double preCenterX = sourceCell.getGeometry().getCenterX();
			double preCenterY = sourceCell.getGeometry().getCenterY();

			curDimension.setSize(sourceCell.getGeometry().getWidth(), sourceCell.getGeometry().getHeight());
			sourceCell.getGeometry().setWidth(curDimension.getWidth() + updateDimension.getWidth());
			sourceCell.getGeometry().setHeight(curDimension.getHeight() + updateDimension.getHeight());

			double curCenterX = sourceCell.getGeometry().getCenterX();
			double curCenterY = sourceCell.getGeometry().getCenterY();
			double distanceX = (curCenterX - preCenterX);
			double distanceY = (curCenterY - preCenterY);
			double curX = sourceCell.getGeometry().getX();
			double curY = sourceCell.getGeometry().getY();

			sourceCell.getGeometry().setX(curX - distanceX);
			sourceCell.getGeometry().setY(curY - distanceY);

			for (int i = 0; i < sourceCell.getChildCount(); i++) {
				mxICell childCell = sourceCell.getChildAt(i);
				curX = childCell.getGeometry().getX();
				curY = childCell.getGeometry().getY();
				childCell.getGeometry().setX(curX + distanceX);
				childCell.getGeometry().setY(curY + distanceY);
			}
		} finally {
			mxgraph.getModel().endUpdate();
		}
		mxgraphComponent.refresh();
	}
	
	public void sleepThread(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
