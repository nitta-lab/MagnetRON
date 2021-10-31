package org.ntlab.actions;

import java.awt.event.ActionEvent;

import org.ntlab.deltaViewer.IMagnetRON;

/**
 * 
 * 
 * @author Nitta Lab.
 */
public class StartAnimationAction extends AbstractMagnetRONAction {

	private static final long serialVersionUID = -6737903367467763719L;

	public StartAnimationAction(IMagnetRON magnetRON) {
		super("Start", magnetRON);
	}

	public StartAnimationAction(String name, IMagnetRON magnetRON) {
		super(name, magnetRON);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		magnetRON.startAnimation();						
	}
}
