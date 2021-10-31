package org.ntlab.deltaExtractor;

import java.util.ArrayList;

import org.ntlab.trace.Reference;

public class Delta {

	private ArrayList<Reference> srcSide = new ArrayList<Reference>();
	private ArrayList<Reference> dstSide = new ArrayList<Reference>();

	public void addSrcSide(Reference r){
		getSrcSide().add(r);
	}
	
	public void addDstSide(Reference r){
		getDstSide().add(r);
	}

	public ArrayList<Reference> getSrcSide() {
		return srcSide;
	}

	public ArrayList<Reference> getDstSide() {
		return dstSide;
	}
}
