package org.ntlab.trace;

public class ObjectReference {
	private String id;
	private String actualType = null;		// 実際の型
	private String calleeType = null;		// 呼び出し先メソッド内での型(静的に決定できる型)
	private String callerType = null;		// 呼び出し元メソッド内での型(静的に決定できる型)

	public ObjectReference(String id) {
		this.id = id;
		this.actualType = null;
	}

	public ObjectReference(String id, String actualType) {
		this.id = id;
		this.actualType = actualType;
	}
	
	public ObjectReference(String id, String actualType, String calleeType) {
		this.id = id;
		this.actualType = actualType;
		this.calleeType = calleeType;
	}
	
	public ObjectReference(String id, String actualType, String calleeType, String callerType) {
		this.id = id;
		this.actualType = actualType;
		this.calleeType = calleeType;
		this.callerType = callerType;
	}

	public String getId() {
		return id;
	}
	
	public String getActualType() {
		return actualType;
	}

	public String getCalleeType() {
		return calleeType;
	}

	public String getCallerType() {
		return callerType;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setActualType(String actualType) {
		this.actualType = actualType;
	}

	public void setCalleeType(String calleeType) {
		this.calleeType = calleeType;
	}

	public void setCallerType(String callerType) {
		this.callerType = callerType;
	}

	public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof ObjectReference)) return false;
		if (id != null && id.equals(((ObjectReference)other).id)) return true;
		return false;
	}
}
