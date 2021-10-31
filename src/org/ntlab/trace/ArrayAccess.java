package org.ntlab.trace;

public class ArrayAccess extends Statement {
	private String arrayClassName;
	private String arrayObjectId;
	private int index;
	private String valueClassName;
	private String valueObjectId;

	public ArrayAccess(String arrayClassName, String arrayObjectId, int index, String valueClassName, String valueObjectId, 
			int lineNo, String threadNo, long timeStamp) {
		super(lineNo, threadNo, timeStamp);
		this.arrayClassName = arrayClassName;
		this.arrayObjectId = arrayObjectId;
		this.index = index;
		this.valueClassName = valueClassName;
		this.valueObjectId = valueObjectId;
	}

	public String getArrayClassName() {
		return arrayClassName;
	}

	public String getArrayObjectId() {
		return arrayObjectId;
	}

	public int getIndex() {
		return index;
	}

	public String getValueClassName() {
		return valueClassName;
	}

	public String getValueObjectId() {
		return valueObjectId;
	}
}
