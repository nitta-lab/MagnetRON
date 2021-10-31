package org.ntlab.deltaViewer;


import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DirectedWeightedPseudograph;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.util.mxConstants;

/**
 * JGraphX(visualize) to JGraphT(model) DeltaGraphAdapter for MagnetRON. 
 * 
 * @author Nitta Lab.
 */
public class DeltaGraphAdapter extends JGraphXAdapter<Object, Object> {
	protected Graph<Object, Object> graphT;
	private DeltaGraphModel deltaGraphModel;
	
//	private long refreshedTime = 0L;
	
	public DeltaGraphAdapter(DirectedWeightedPseudograph graphT) {
		super(graphT);
		this.graphT = graphT;
		this.deltaGraphModel = new DeltaGraphModel(super.getModel());
	}

	/**
	 * Draw new vertex into the graph.
	 *
	 * @param vertex	Vertex to be added to the graph.
	 */
	private mxICell addJGraphTVertex(String id, Object vertex) {
		getModel().beginUpdate();

		try {
			// create a new JGraphX vertex at position 0
			mxICell cell = (mxICell) super.insertVertex(defaultParent, null, vertex, 0, 0, 0, 0);

			// update cell size so cell isn't "above" graph
			updateCellSize(cell);

			// Save reference between vertex and cell.
			getVertexToCellMap().put(vertex, cell);
			getCellToVertexMap().put(cell, vertex);
//			for(Entry<Object, mxICell> e: getVertexToCellMap().entrySet()) {
//				System.out.println("addJGraphTVertex: " + e.getKey() + ", " + e.getValue());
//			}
//			System.out.println();
//			for(Entry<mxICell, Object> e: getCellToVertexMap().entrySet()) {
//				System.out.println("addJGraphTVertex: " + e.getKey() + ", " + e.getValue());
//			}

			return cell;
		} finally {
			getModel().endUpdate();
		}
	}

	/**
	 * Draw new egde into the graph.
	 * 
	 * @param edge Edge to be added to the graph. Source and target vertices are needed.
	 */
	private mxICell addJGraphTEdge(Object sourceCell, Object targetCell, Object edge) {
		getModel().beginUpdate();

		try {
			// if the one of the vertices is not drawn, don't draw the edge
//			if (!(getVertexToCellMap().containsKey(sourceVertex) && getVertexToCellMap().containsKey(targetVertex))) {
//				return null;
//			}

			// get mxICells
//			Object sourceCell = getVertexToCellMap().get(sourceVertex);
//			Object targetCell = getVertexToCellMap().get(targetVertex);

			// add edge between mxICells
			mxICell cell = (mxICell) super.insertEdge(defaultParent, null, edge, sourceCell, targetCell);

			// update cell size so cell isn't "above" graph
			updateCellSize(cell);

			// Save reference between vertex and cell.
			getEdgeToCellMap().put(edge, cell);
			getCellToEdgeMap().put(cell, edge);
			return cell;
		} finally {
			getModel().endUpdate();
		}
	}

	/**
	 * 
	 * @param parent
	 * @param id	Edge id
	 * @param value	Value to display
	 * @param source
	 * @param target
	 * @return
	 */
	public Object insertDeltaEdge(Object parent, String id, Object value, Object sourceCell, Object targetCell) {
		// find vertices of edge
		Object sourceVertex = getCellToVertexMap().get(sourceCell);
		Object targetVertex = getCellToVertexMap().get(targetCell);

		if (value == null) {
			graphT.addEdge(sourceVertex, targetVertex);
		} else {
			graphT.addEdge(sourceVertex, targetVertex, value);
		}
		mxICell cellEdge = addJGraphTEdge(sourceCell, targetCell, value);
		cellEdge.setId(id);
		return cellEdge;
	}

	/**
	 * @param parent
	 * @param id Vertex id
	 * @param value	Value to display
	 * @param style
	 * @return
	 */
	public Object insertDeltaVertex(Object parent, String id, Object value, String style) {
		return insertDeltaVertex(parent, id, value, 0, 0, 0, 0, style);
	}

	/**
	 * @param parent
	 * @param id Vertex id
	 * @param value Value to display
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param style
	 * @return
	 */
	public Object insertDeltaVertex(Object parent, String id, Object value, double x, double y, double width, double height, String style) {
//		graphT.addVertex(value);
//		mxICell cell = addJGraphTVertex(value);
		graphT.addVertex(value);
		mxICell cell = addJGraphTVertex(id, value);

		cell.setId(id);
		((mxCell) parent).insert(cell);
		cell.setParent((mxICell) parent);

		getCellGeometry(cell).setX(x);
		getCellGeometry(cell).setY(y);
		getCellGeometry(cell).setWidth(width);
		getCellGeometry(cell).setHeight(height);
		String[] styles = style.split(";");
		for(String s: styles) {
			if(s.contains("fillColor")) {
				String[] splitS = s.split("=");
				getCellStyle(cell).put(mxConstants.STYLE_FILLCOLOR, splitS[1]);
			}
		}
		return cell;
	}
	
	public void resetParentCell(mxICell cell, mxICell parentCell) {
		if (cell.getParent() != null && !cell.getParent().equals(parentCell)) {
			// If parent of cell isn't mxDefaltParent, reset parent.
			cell.getParent().remove(cell);
			cell.setParent(parentCell);
		}	
	}

	@Override
	public void refresh() {
//		long curTime = System.currentTimeMillis();
//		if (curTime - refreshedTime > 5L) {
//			super.refresh();
//			refreshedTime = curTime;
//		}
//		if (((DeltaGraphModel) getModel()).getCount() <= 1) {
//			view.reload();
//			repaint();		
//		}
		super.refresh();
	}

	public Graph<Object, Object> getGraph() {
		return graphT;
	}
	
	@Override
	public mxIGraphModel getModel() {
		if (deltaGraphModel == null) return super.getModel();
		return deltaGraphModel;
	}

	public class DeltaGraphModel implements mxIGraphModel {

		private mxIGraphModel model;
		private int count = 0;
		
		public DeltaGraphModel(mxIGraphModel model) {
			this.model = model;
		}

		public int getCount() {
			return count;
		}

		@Override
		public Object getRoot() {
			return model.getRoot();
		}

		@Override
		public Object setRoot(Object root) {
			return model.setRoot(root);
		}

		@Override
		public Object[] cloneCells(Object[] cells, boolean includeChildren) {
			return model.cloneCells(cells, includeChildren);
		}

		@Override
		public boolean isAncestor(Object parent, Object child) {
			return model.isAncestor(parent, child);
		}

		@Override
		public boolean contains(Object cell) {
			return model.contains(cell);
		}

		@Override
		public Object getParent(Object child) {
			return model.getParent(child);
		}

		@Override
		public Object add(Object parent, Object child, int index) {
			return model.add(parent, child, index);
		}

		@Override
		public Object remove(Object cell) {
			return model.remove(cell);
		}

		@Override
		public int getChildCount(Object cell) {
			return model.getChildCount(cell);
		}

		@Override
		public Object getChildAt(Object parent, int index) {
			return model.getChildAt(parent, index);
		}

		@Override
		public Object getTerminal(Object edge, boolean isSource) {
			return model.getTerminal(edge, isSource);
		}

		@Override
		public Object setTerminal(Object edge, Object terminal, boolean isSource) {
			return model.setTerminal(edge, terminal, isSource);
		}

		@Override
		public int getEdgeCount(Object cell) {
			return model.getEdgeCount(cell);
		}

		@Override
		public Object getEdgeAt(Object cell, int index) {
			return model.getEdgeAt(cell, index);
		}

		@Override
		public boolean isVertex(Object cell) {
			return model.isVertex(cell);
		}

		@Override
		public boolean isEdge(Object cell) {
			return model.isEdge(cell);
		}

		@Override
		public boolean isConnectable(Object cell) {
			return model.isConnectable(cell);
		}

		@Override
		public Object getValue(Object cell) {
			return model.getValue(cell);
		}

		@Override
		public Object setValue(Object cell, Object value) {
			return model.setValue(cell, value);
		}

		@Override
		public mxGeometry getGeometry(Object cell) {
			return model.getGeometry(cell);
		}

		@Override
		public mxGeometry setGeometry(Object cell, mxGeometry geometry) {
			return model.setGeometry(cell, geometry);
		}

		@Override
		public String getStyle(Object cell) {
			return model.getStyle(cell);
		}

		@Override
		public String setStyle(Object cell, String style) {
			return model.setStyle(cell, style);
		}

		@Override
		public boolean isCollapsed(Object cell) {
			return model.isCollapsed(cell);
		}

		@Override
		public boolean setCollapsed(Object cell, boolean collapsed) {
			return model.setCollapsed(cell, collapsed);
		}

		@Override
		public boolean isVisible(Object cell) {
			return model.isVisible(cell);
		}

		@Override
		public boolean setVisible(Object cell, boolean visible) {
			return model.setVisible(cell, visible);
		}

		@Override
		public void beginUpdate() {
			if (count == 0) {
				model.beginUpdate();				
			}
			count++;
		}

		@Override
		public void endUpdate() {
			count--;
			if (count == 0) {
				model.endUpdate();				
			}
		}

		@Override
		public void addListener(String eventName, mxIEventListener listener) {
			model.addListener(eventName, listener);
		}

		@Override
		public void removeListener(mxIEventListener listener) {
			model.removeListener(listener);
		}

		@Override
		public void removeListener(mxIEventListener listener, String eventName) {
			model.removeListener(listener, eventName);
		}
		
	}
}
