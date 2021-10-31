package org.ntlab.featureExtractor;

import java.util.ArrayList;
import java.util.List;

public class Feature {

	private String name = null;
	private List<Extract> extracts = new ArrayList<>();

	public Feature() {		
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Extract> getExtracts() {
		return extracts;
	}

	public void setExtracts(List<Extract> extracts) {
		this.extracts = extracts;
	}

}
