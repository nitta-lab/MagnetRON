package org.ntlab.trace;

public class FieldUpdate extends Statement {
	private String fieldName;
	private String containerClassName;
	private String containerObjId;
	private String valueClassName;
	private String valueObjId;

	public FieldUpdate(String valueClassName, String valueObjId, String containerClassName, String containerObjId, 
			int lineNo, String threadNo) {
		super(lineNo, threadNo);
		this.containerClassName = containerClassName;
		this.containerObjId = containerObjId;
		this.valueClassName = valueClassName;
		this.valueObjId = valueObjId;		
	}

	public FieldUpdate(String valueClassName, String valueObjId, String containerClassName, String containerObjId, 
			int lineNo, String threadNo, long timeStamp) {
		super(lineNo, threadNo, timeStamp);
		this.containerClassName = containerClassName;
		this.containerObjId = containerObjId;
		this.valueClassName = valueClassName;
		this.valueObjId = valueObjId;		
	}

	public FieldUpdate(String fieldName, String valueClassName, String valueObjId, String containerClassName, String containerObjId, 
			int lineNo, String threadNo, long timeStamp) {
		super(lineNo, threadNo, timeStamp);
		this.fieldName = fieldName;
		this.containerClassName = containerClassName;
		this.containerObjId = containerObjId;
		this.valueClassName = valueClassName;
		this.valueObjId = valueObjId;		
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getContainerClassName() {
		return containerClassName;
	}

	public String getContainerObjId() {
		return containerObjId;
	}

	public String getValueClassName() {
		return valueClassName;
	}

	public String getValueObjId() {
		return valueObjId;
	}

	public Reference getReference() {
		return new Reference(containerObjId, valueObjId, containerClassName, valueClassName);
	}
}
