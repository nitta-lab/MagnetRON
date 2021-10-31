package org.ntlab.deltaViewer;

import java.util.ArrayList;
import java.util.List;

/**
 * Method execution vertex.
 * 
 * @author Nitta Lab.
 */
public class MethodExecutionVertex extends Vertex {
	protected List<ObjectVertex> locals = new ArrayList<>(); // Return value
	protected List<ObjectVertex> arguments = new ArrayList<>();

	
	public MethodExecutionVertex(String label, Object cell) {
		super(label, cell);
	}

	public MethodExecutionVertex(String label, Object cell, double x, double y, double width, double height) {
		super(label, cell, x, y, width, height);
	}

	public List<ObjectVertex> getLocals() {
		return locals;
	}

	public void setLocals(List<ObjectVertex> locals) {
		this.locals = locals;
	}

	public List<ObjectVertex> getArguments() {
		return arguments;
	}

	public void setArguments(List<ObjectVertex> arguments) {
		this.arguments = arguments;
	}

}
