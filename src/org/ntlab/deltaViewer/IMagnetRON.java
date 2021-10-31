package org.ntlab.deltaViewer;

import java.io.File;
import java.util.List;

import org.ntlab.featureExtractor.Feature;

public interface IMagnetRON {

	List<Feature> open(File file);

	void doExtract(Feature feature);

	void startAnimation();

	void pauseAnimation();
	
	void resumeAnimation();
	
	void stopAnimation();
}