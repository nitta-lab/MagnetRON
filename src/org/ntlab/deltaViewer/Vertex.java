package org.ntlab.deltaViewer;

import java.awt.geom.Point2D;

import com.mxgraph.model.mxICell;

/**
 * JGraphX Vertex.
 * 
 * @author Nitta Lab.
 */
public class Vertex {
	String label;
	Object cell;
	double initialX;
	double initialY;
	double x;
	double y;
	double width;
	double height;
	
	public Vertex(String label, Object cell) {
		this.label = label;
		this.cell = cell;
	}

	public Vertex(String label, Object cell, double initialX, double initialY) {
		this.label = label;
		this.cell = cell;
		this.initialX = initialX;
		this.initialY = initialY;
	}

	public Vertex(String label, Object cell, double x, double y, double width, double height) {
		this.label = label;
		this.cell = cell;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		((mxICell)cell).getGeometry().setX(x);
		((mxICell)cell).getGeometry().setY(y);
		((mxICell)cell).getGeometry().setWidth(width);
		((mxICell)cell).getGeometry().setHeight(height);
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Object getCell() {
		return cell;
	}

	public void setCell(Object cell) {
		this.cell = cell;
	}
	
	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
		if (cell != null) ((mxICell)cell).getGeometry().setX(x);
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
		if (cell != null) ((mxICell)cell).getGeometry().setY(y);
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
		((mxICell)cell).getGeometry().setWidth(width);
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
		((mxICell)cell).getGeometry().setHeight(height);
	}
	

	public Point2D getInitialPoint() {
		return new Point2D.Double(initialX, initialY);
	}
	
	public void setInitialPoint(double initX, double initY) {
		initialX = initX;
		initialY = initY;
	}

	public double getInitialX() {
		return initialX;
	}

	public double getInitialY() {
		return initialY;
	}

	public void resetCellPosition() {
		((mxICell)cell).getGeometry().setX(initialX);
		((mxICell)cell).getGeometry().setY(initialY);
	}

	public void resetVertexPosition() {
		if (cell != null) {
			x = initialX = ((mxICell)cell).getGeometry().getX();
			y = initialY = ((mxICell)cell).getGeometry().getY();
		}
	}
}
