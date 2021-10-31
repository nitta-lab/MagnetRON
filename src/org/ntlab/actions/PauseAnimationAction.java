package org.ntlab.actions;

import java.awt.event.ActionEvent;

import org.ntlab.deltaViewer.IMagnetRON;

/**
 * 
 * 
 * @author Nitta Lab.
 */
public class PauseAnimationAction extends AbstractMagnetRONAction {

	private static final long serialVersionUID = 3866653096063437477L;

	public PauseAnimationAction(IMagnetRON magnetRON) {
		super("Pause", magnetRON);
	}

	public PauseAnimationAction(String name, IMagnetRON magnetRON) {
		super(name, magnetRON);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		magnetRON.pauseAnimation();						
	}
}