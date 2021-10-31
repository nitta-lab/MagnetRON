package org.ntlab.deltaViewer;

import java.util.ArrayList;
import java.util.List;

import com.mxgraph.model.mxICell;

/**
 * Object vertex.
 * 
 * @author Nitta Lab.
 */
public class ObjectVertex extends Vertex {
	protected List<MethodExecutionVertex> methodExecutionVertices = new ArrayList<>();

	public ObjectVertex(String label, Object cell) {
		super(label, cell);
	}

	public ObjectVertex(String label, Object cell, double initialX, double initialY) {
		super(label, cell, initialX, initialY);
	}
	
	public ObjectVertex(String label, Object cell, double x, double y, double width, double height) {
		super(label, cell, x, y, width, height);
	}

	public List<MethodExecutionVertex> getMethodExecutionVertices() {
		return methodExecutionVertices;
	}

	public void setMethodExecutionVertices(List<MethodExecutionVertex> methodExecutionVertices) {
		this.methodExecutionVertices = methodExecutionVertices;
	}

	public void addMethodExecutionVertex(MethodExecutionVertex methodExecutionVertex) {
		this.methodExecutionVertices.add(methodExecutionVertex);
	}
}
