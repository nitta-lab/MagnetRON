package org.ntlab.deltaViewer;

import java.util.Map;

import org.ntlab.deltaExtractor.IAliasCollector;

public interface IObjectLayout {
	
	public void execute(IObjectCallGraph objectCallGraph, IAliasCollector aliasCollector, Map<String, ObjectVertex> objectToVertexMap);
	
}
