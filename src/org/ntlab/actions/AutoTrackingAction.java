package org.ntlab.actions;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.ntlab.deltaViewer.MagnetRONViewer;

public class AutoTrackingAction extends AbstractViewerAction implements ChangeListener {
	
	private static final long serialVersionUID = -4780429455781975464L;

	public AutoTrackingAction(MagnetRONViewer viewer) {
		super("Auto Tracking", viewer);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() instanceof JCheckBox) {
			JCheckBox checkBox = (JCheckBox) e.getSource();
			super.viewer.setAutoTracking(checkBox.isSelected());
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
	}
}