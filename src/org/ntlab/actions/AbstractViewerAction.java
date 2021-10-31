package org.ntlab.actions;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import org.ntlab.deltaViewer.MagnetRONViewer;

public abstract class AbstractViewerAction extends AbstractAction {

	protected MagnetRONViewer viewer;

	public AbstractViewerAction(String name, MagnetRONViewer viewer) {
		super(name);
		this.viewer = viewer;
	}

}