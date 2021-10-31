package org.ntlab.animations;

import java.awt.Dimension;
import java.awt.geom.Dimension2D;
import java.util.concurrent.ThreadPoolExecutor;

import org.ntlab.deltaViewer.DeltaGraphAdapter;

import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;

/**
 * Animation to resize vertex.
 * 
 * @author Nitta Lab.
 */
public class VertexResizeAnimation extends MagnetRONAnimation {

	// Test code (will be deleted)
	private static final String TAG = VertexResizeAnimation.class.getSimpleName();

	/**
	 * The destination dimension where the sourceCell animates.
	 */
	private Dimension2D destinationDimension;
	/**
	 * The dimension to update for each cycle count.
	 */
	private Dimension2D velocity;

    /**
     * The constructor of {@code VertexResizeAnimation}.
     * 
     * @param mxgraph
     * @param mxgraphComponent
     */
	public VertexResizeAnimation(DeltaGraphAdapter mxgraph, mxGraphComponent mxgraphComponent) {
		super(mxgraph, mxgraphComponent);
	}

	@Override
	protected void setDestination(double x, double y) {
		setDestinationDimension(new Dimension((int) x, (int) y));
	}

	private void setDestinationDimension(Dimension2D destinationDimension) {
		this.destinationDimension = destinationDimension;
	}

	@Override
	protected void setVelocity(double x, double y) {
		setVelocity(new Dimension((int) x, (int) y));
	}

	private void setVelocity(Dimension2D velocity) {
		this.velocity = velocity;
	}

	private Dimension2D getDestinationDimension() {
		return destinationDimension;
	}

	private Dimension2D getVelocity() {
		return velocity;
	}

	public void init(mxICell sourceCell, Dimension2D destinationDimension, ThreadPoolExecutor threadPoolExecutor) {
		init(sourceCell, destinationDimension.getWidth(), destinationDimension.getHeight(), threadPoolExecutor);
	}
	
	/**
	 * See {@code MagnetRONAnimation#init(mxICell, double, double, ThreadPoolExecutor)}
	 * 
	 * @param sourceCell
	 * @param destinationDimension
	 * @param threadPoolExecutor
	 */
	@Override
	public void init(mxICell sourceCell, double width, double height, ThreadPoolExecutor threadPoolExecutor) {
		super.init(sourceCell, width, height, threadPoolExecutor);

		Dimension2D curDim = new Dimension();
		curDim.setSize(sourceCell.getGeometry().getWidth(), sourceCell.getGeometry().getHeight());
		
		// Calculate resize dimension model
		Dimension2D distanceDimension = new Dimension();
		distanceDimension.setSize(destinationDimension.getWidth() - curDim.getWidth(), 
				destinationDimension.getHeight() - curDim.getHeight());
		Dimension2D velocity = new Dimension();
		velocity.setSize(distanceDimension.getWidth() / getTotalCycleCount(), distanceDimension.getHeight() / getTotalCycleCount());
		setVelocity(velocity);
	}

	@Override
	protected void jumpTo(int currentCycleCount) {
		// Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
		mxgraph.getModel().beginUpdate();
		synchronized(mxgraph.getModel()) {
			try {
				getSourceCell().getGeometry().setWidth(getSourceInitialDimension().getWidth() + getVelocity().getWidth() * currentCycleCount);
				getSourceCell().getGeometry().setHeight(getSourceInitialDimension().getHeight() + getVelocity().getHeight() * currentCycleCount);
			} finally {
				  mxgraph.getModel().endUpdate();
			}
	    	mxgraphComponent.refresh();
		}
	}
	
}
