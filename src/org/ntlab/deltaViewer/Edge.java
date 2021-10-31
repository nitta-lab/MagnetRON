package org.ntlab.deltaViewer;

/**
 * JGraphX Edge.
 * 
 * @author Nitta Lab.
 */
public class Edge {
	String label;
	TypeName typeName;
	Object cell;			// edge cell
	String srcObjId;	// source object id to create the edge later
	String dstObjId;	// destination object id to create the edge later
	
	protected enum TypeName {
		PreReference,	// object will be referred later
		Reference, // object reference
		Create, // create object reference
		Call // method call
	}
	
	/**
	 * @param label	No display label
	 * @param typeName
	 * @param cell
	 */
	public Edge(String label, TypeName typeName, Object cell) {
		this.label = label;
		this.typeName = typeName;
		this.cell = cell;
	}
	
	public Edge(String label, TypeName typeName, String srcObjId, String dstObjId) {
		this.label = label;
		this.typeName = typeName;
		this.srcObjId = srcObjId;
		this.dstObjId = dstObjId;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public TypeName getTypeName() {
		return typeName;
	}

	public void setTypeName(TypeName typeName) {
		this.typeName = typeName;
	}

	public Object getCell() {
		return cell;
	}

	public void setCell(Object cell) {
		this.cell = cell;
	}

	public String getSrcObjId() {
		return srcObjId;
	}

	public void setSrcObjId(String srcObjId) {
		this.srcObjId = srcObjId;
	}

	public String getDstObjId() {
		return dstObjId;
	}

	public void setDstObjId(String dstObjId) {
		this.dstObjId = dstObjId;
	}
}
