package org.ntlab.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.ntlab.deltaViewer.IMagnetRON;

/**
 * 
 * 
 * @author Nitta Lab.
 */
public class AbstractMagnetRONAction extends AbstractAction {

	private static final long serialVersionUID = -6043745804857798325L;
	
	protected IMagnetRON magnetRON = null;
	
	public AbstractMagnetRONAction() {
	}

	public AbstractMagnetRONAction(String name, IMagnetRON magnetRON) {
		super(name);
		this.magnetRON = magnetRON;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

	}

}
