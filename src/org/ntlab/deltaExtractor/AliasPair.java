package org.ntlab.deltaExtractor;

import java.util.AbstractMap.SimpleEntry;

public class AliasPair {
	private SimpleEntry<Alias, Alias> aliasPair;
	private boolean isSrcSideChanged;

	public AliasPair(boolean isSrcSideChanged) {
		this.isSrcSideChanged = isSrcSideChanged;
	}

	public AliasPair(Alias fromAlias, Alias toAlias, boolean isSrcSideChanged) {
		this(isSrcSideChanged);
		this.aliasPair = new SimpleEntry<>(fromAlias, toAlias);
	}

	public SimpleEntry<Alias, Alias> setAliasPair(Alias fromAlias, Alias toAlias) {
		return this.aliasPair = new SimpleEntry<>(fromAlias, toAlias);
	}
	
	public SimpleEntry<Alias, Alias> getAliasPair() {
		return this.aliasPair;
	}

	public boolean getIsSrcSideChanged() {
		return this.isSrcSideChanged;
	}
}