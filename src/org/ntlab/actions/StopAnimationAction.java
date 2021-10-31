package org.ntlab.actions;

import java.awt.event.ActionEvent;

import org.ntlab.deltaViewer.IMagnetRON;

/**
 * 
 * 
 * @author Nitta Lab.
 */
public class StopAnimationAction extends AbstractMagnetRONAction {

	private static final long serialVersionUID = 8106590890493246008L;

	public StopAnimationAction(IMagnetRON magnetRON) {
		super("Stop", magnetRON);
	}

	public StopAnimationAction(String name, IMagnetRON magnetRON) {
		super(name, magnetRON);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		magnetRON.stopAnimation();						
	}
}
