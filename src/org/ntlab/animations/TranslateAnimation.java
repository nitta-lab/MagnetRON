package org.ntlab.animations;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.concurrent.ThreadPoolExecutor;

import org.ntlab.deltaViewer.DeltaGraphAdapter;

import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;

/**
 * Animation of edge stretching and shrinking, vertex translating.
 * 
 * @author Nitta Lab.
 */
public class TranslateAnimation extends MagnetRONAnimation {
	
	// Test code (will be deleted)
	private static final String TAG = TranslateAnimation.class.getSimpleName();

	/**
	 * The destination point where the sourceCell animates.
	 */
	private Point2D destinationPoint;
	/**
	 * The point to update for each cycle count.
	 */
	private Point2D velocity;

    /**
     * The constructor of {@code TranslateAnimation}.
     * 
     * @param mxgraph
     * @param mxgraphComponent
     */
	public TranslateAnimation(DeltaGraphAdapter mxgraph, mxGraphComponent mxgraphComponent) {
		super(mxgraph, mxgraphComponent);
	}
	
	@Override
	protected void setDestination(double x, double y) {
		setDestinationPoint(new Point2D.Double(x, y));
	}
	
	private void setDestinationPoint(Point2D destinationPoint) {
		this.destinationPoint = destinationPoint;
	}

	@Override
	protected void setVelocity(double x, double y) {
		setVelocity(new Point2D.Double(x, y));
	}

	private void setVelocity(Point2D velocity) {
		this.velocity = velocity;
	}

	private Point2D getDestinationPoint() {
		return destinationPoint;
	}

	private Point2D getVelocity() {
		return velocity;
	}

	public void init(mxICell sourceCell, Point2D destinationPoint, ThreadPoolExecutor threadPoolExecutor) {
		init(sourceCell, destinationPoint.getX(), destinationPoint.getY(), threadPoolExecutor);
	}
	
	/**
	 * See {@code MagnetRONAnimation#init(mxICell, double, double, ThreadPoolExecutor)}
	 * 
	 * @param sourceCell
	 * @param destinationPoint
	 * @param threadPoolExecutor
	 */
	@Override
	public void init(mxICell sourceCell, double x, double y, ThreadPoolExecutor threadPoolExecutor) {
		super.init(sourceCell, x, y, threadPoolExecutor);

		Point2D curPt = new Point(sourceCell.getGeometry().getPoint());
		
		// Calculate resize line model
		Point2D distancePoint = new Point();
		distancePoint.setLocation(destinationPoint.getX() - curPt.getX(),
				destinationPoint.getY() - curPt.getY());
		Point2D velocity = new Point2D.Double();
		velocity.setLocation(distancePoint.getX() / getTotalCycleCount(), distancePoint.getY() / getTotalCycleCount());
		setVelocity(velocity);
	}
	
	@Override
	protected void jumpTo(int currentCycleCount) {
		// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
		mxgraph.getModel().beginUpdate();
		synchronized(mxgraph.getModel()) {
			try {
				getSourceCell().getGeometry().setX(getSourceInitialPoint().getX() + getVelocity().getX() * currentCycleCount);
				getSourceCell().getGeometry().setY(getSourceInitialPoint().getY() + getVelocity().getY() * currentCycleCount);
			} finally {
				  mxgraph.getModel().endUpdate();
			}
	    	mxgraphComponent.refresh();
		}
	}

}
