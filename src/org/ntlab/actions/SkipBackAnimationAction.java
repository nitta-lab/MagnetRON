package org.ntlab.actions;

import java.awt.event.ActionEvent;

import org.ntlab.deltaViewer.IMagnetRON;
import org.ntlab.deltaViewer.MagnetRONFrame;

public class SkipBackAnimationAction extends AbstractMagnetRONAction {

	private static final long serialVersionUID = -7485589008525481164L;

	public SkipBackAnimationAction(IMagnetRON magnetRON) {
		super("Skip Back", magnetRON);
	}

	public SkipBackAnimationAction(String name, IMagnetRON magnetRON) {
		super(name, magnetRON);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (super.magnetRON instanceof MagnetRONFrame) {
			MagnetRONFrame magnetRONFrame = (MagnetRONFrame) super.magnetRON;
			magnetRONFrame.skipBackAnimation();			
		}
	}

}
