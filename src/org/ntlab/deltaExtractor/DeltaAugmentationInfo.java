package org.ntlab.deltaExtractor;

import org.ntlab.trace.AugmentationInfo;

public class DeltaAugmentationInfo extends AugmentationInfo {
	private Boolean isCoodinator = false;
	private Boolean isSetter = true;
	private int traceObjectId = 0;

	public void setTraceObjectId(int traceObjectId) {
		this.traceObjectId = traceObjectId;
	}
	
	public int getTraceObjectId() {
		return traceObjectId;
	}

	public void setSetterSide(boolean isSetter) {
		this.isSetter = isSetter;
	}
	
	public boolean isSetterSide() {
		return isSetter;
	}

	public void setCoodinator(boolean isCoodinator) {
		this.isCoodinator = isCoodinator;
	}

	public boolean isCoodinator() {
		return isCoodinator;
	}

}
