package org.ntlab.deltaExtractor;

import java.util.List;

public interface IAliasCollector {

	void addAlias(Alias alias);
	
	List<Alias> getAliasList();

}