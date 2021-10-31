package org.ntlab.deltaExtractor;

public interface IAliasTracker extends IAliasCollector {
	
	void changeTrackingObject(String from, String to, boolean isSrcSide);
	
}