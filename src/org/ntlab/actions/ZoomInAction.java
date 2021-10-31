package org.ntlab.actions;

import java.awt.event.ActionEvent;

import org.ntlab.deltaViewer.MagnetRONViewer;

/**
 * 
 * 
 * @author Nitta Lab.
 */
public class ZoomInAction extends AbstractViewerAction {

	private static final long serialVersionUID = -1538971870320664372L;

	public ZoomInAction(MagnetRONViewer viewer) {
		super("Zoom In", viewer);
	}

	public ZoomInAction(String name, MagnetRONViewer viewer) {
		super(name, viewer);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		viewer.getGraphComponent().zoomIn();
	}

}
