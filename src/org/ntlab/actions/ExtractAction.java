package org.ntlab.actions;

import java.awt.event.ActionEvent;

import org.ntlab.deltaViewer.IMagnetRON;
import org.ntlab.featureExtractor.Feature;

/**
 * 
 * 
 * @author Nitta Lab.
 */
public class ExtractAction extends AbstractMagnetRONAction {

	private static final long serialVersionUID = 2665164940207790978L;

	private Feature feature = null;
	
	public ExtractAction(Feature feature, IMagnetRON magnetRON) {
		super(feature.getName(), magnetRON);
		this.feature = feature;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		magnetRON.doExtract(feature);
	}
}
