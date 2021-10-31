package org.ntlab.actions;

import java.awt.event.ActionEvent;

import org.ntlab.deltaViewer.IMagnetRON;
import org.ntlab.deltaViewer.MagnetRONFrame;

public class FastForwardAnimationAction extends AbstractMagnetRONAction {

	private static final long serialVersionUID = -7485589008525481164L;

	public FastForwardAnimationAction(IMagnetRON magnetRON) {
		super("Fast-Forward", magnetRON);
	}

	public FastForwardAnimationAction(String name, IMagnetRON magnetRON) {
		super(name, magnetRON);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (super.magnetRON instanceof MagnetRONFrame) {
			MagnetRONFrame magnetRONFrame = (MagnetRONFrame) super.magnetRON;
			magnetRONFrame.fastForwardAnimation();			
		}
	}

}
