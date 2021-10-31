package org.ntlab.actions;

import java.awt.event.ActionEvent;

import org.ntlab.deltaViewer.MagnetRONViewer;

public class ZoomActualAction extends AbstractViewerAction {
	public ZoomActualAction(MagnetRONViewer viewer) {
		super("100%", viewer);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		viewer.getGraphComponent().zoomActual();
	}

}
