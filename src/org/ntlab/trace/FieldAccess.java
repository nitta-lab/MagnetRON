package org.ntlab.trace;

public class FieldAccess extends Statement {
	private String fieldName;
	private String containerClassName;
	private String containerObjId;
	private String valueClassName;
	private String valueObjId;
	protected String thisClassName;
	protected String thisObjId;
	
	public FieldAccess(String valueClassName, String valueObjId, String containerClassName,
			String containerObjId, String thisClassName, String thisObjId, 
			int lineNo, String threadNo) {
		super(lineNo, threadNo);
		this.containerClassName = containerClassName;
		this.containerObjId = containerObjId;
		this.valueClassName = valueClassName;
		this.valueObjId = valueObjId;
		this.thisClassName = thisClassName;
		this.thisObjId = thisObjId;
	}
	
	public FieldAccess(String valueClassName, String valueObjId, String containerClassName,
			String containerObjId, String thisClassName, String thisObjId, 
			int lineNo, String threadNo, long timeStamp) {
		super(lineNo, threadNo, timeStamp);
		this.containerClassName = containerClassName;
		this.containerObjId = containerObjId;
		this.valueClassName = valueClassName;
		this.valueObjId = valueObjId;
		this.thisClassName = thisClassName;
		this.thisObjId = thisObjId;
	}
	
	public FieldAccess(String fieldName, String valueClassName, String valueObjId, String containerClassName,
			String containerObjId, String thisClassName, String thisObjId, 
			int lineNo, String threadNo, long timeStamp) {
		super(lineNo, threadNo, timeStamp);
		this.fieldName = fieldName;
		this.containerClassName = containerClassName;
		this.containerObjId = containerObjId;
		this.valueClassName = valueClassName;
		this.valueObjId = valueObjId;
		this.thisClassName = thisClassName;
		this.thisObjId = thisObjId;
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

	public String getThisClassName() {
		return thisClassName;
	}

	public String getThisObjId() {
		return thisObjId;
	}

	public Reference getReference() {
		return new Reference(containerObjId, valueObjId, containerClassName, valueClassName);
	}
}
