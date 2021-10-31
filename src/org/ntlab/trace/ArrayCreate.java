package org.ntlab.trace;

public class ArrayCreate extends Statement {
	private String arrayClassName;
	private String arrayObjectId;
	private int dimension;

	public ArrayCreate(String arrayClassName, String arrayObjectId, int dimension, int lineNo, String threadNo, long timeStamp) {
		super(lineNo, threadNo, timeStamp);
		this.arrayClassName = arrayClassName;
		this.arrayObjectId = arrayObjectId;
		this.dimension = dimension;
	}

	public String getArrayClassName() {
		return arrayClassName;
	}

	public String getArrayObjectId() {
		return arrayObjectId;
	}

	public int getDimension() {
		return dimension;
	}
}
