package org.ntlab.actions;

import java.awt.event.ActionEvent;

import org.ntlab.deltaViewer.MagnetRONViewer;

/**
 * 
 * 
 * @author Nitta Lab.
 */
public class ZoomOutAction extends AbstractViewerAction {

	private static final long serialVersionUID = 166649313967590302L;

	public ZoomOutAction(MagnetRONViewer viewer) {
		super("Zoom Out", viewer);
	}

	public ZoomOutAction(String name, MagnetRONViewer viewer) {
		super(name, viewer);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		viewer.getGraphComponent().zoomOut();
	}

}
