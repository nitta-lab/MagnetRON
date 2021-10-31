package org.ntlab.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import org.ntlab.deltaViewer.IMagnetRON;

/**
 * 
 * 
 * @author Nitta Lab.
 */
public class OpenAction extends AbstractMagnetRONAction {
	
	private static final long serialVersionUID = -3935721814268952040L;

	private String lastDir = null;
	private String fileDesc = "MagnetRON File";
	public OpenAction(IMagnetRON magnetRON) {
		super("Open File...", magnetRON);
	}

	public OpenAction(String name, IMagnetRON magnetRON) {
		super(name, magnetRON);
	}

	public OpenAction(String name, IMagnetRON magnetRON, String fileDesc) {
		super(name, magnetRON);
		this.fileDesc = fileDesc;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String wd = (lastDir  != null) ? lastDir : System.getProperty("user.dir");
		UIManager.put("FileChooser.acceptAllFileFilterText", "All files");
		UIManager.put("FileChooser.lookInLabelText", "Location");
		UIManager.put("FileChooser.cancelButtonText", "Cancel");
		UIManager.put("FileChooser.cancelButtonToolTipText", "Cancel");
		UIManager.put("FileChooser.openButtonText", "Open");
		UIManager.put("FileChooser.openButtonToolTipText", "Open File");
		UIManager.put("FileChooser.filesOfTypeLabelText", "Type");
		UIManager.put("FileChooser.fileNameLabelText", "File");
		UIManager.put("FileChooser.listViewButtonToolTipText", "List"); 
		UIManager.put("FileChooser.listViewButtonAccessibleName", "List"); 
		UIManager.put("FileChooser.detailsViewButtonToolTipText", "Details");
		UIManager.put("FileChooser.detailsViewButtonAccessibleName", "Details");
		UIManager.put("FileChooser.upFolderToolTipText", "Up one level"); 
		UIManager.put("FileChooser.upFolderAccessibleName", "Up one level"); 
		UIManager.put("FileChooser.homeFolderToolTipText", "Workplace"); 
		UIManager.put("FileChooser.homeFolderAccessibleName", "Workplace"); 
		UIManager.put("FileChooser.fileNameHeaderText", "Name"); 
		UIManager.put("FileChooser.fileSizeHeaderText", "Size"); 
		UIManager.put("FileChooser.fileTypeHeaderText", "Type"); 
		UIManager.put("FileChooser.fileDateHeaderText", "Date"); 
		UIManager.put("FileChooser.fileAttrHeaderText", "Attributes"); 
		UIManager.put("FileChooser.openDialogTitleText","Open file");
		UIManager.put("FileChooser.readOnly", Boolean.TRUE);
		JFileChooser fc = new JFileChooser(wd);
		FileFilter defaultFilter = new FileFilter() {
			public boolean accept(File file) {
				String lcase = file.getName().toLowerCase();
				return lcase.endsWith(".magnet");
			}
			@Override
			public String getDescription() {
				return fileDesc;
			}
		};
		fc.addChoosableFileFilter(defaultFilter);
		int rc = fc.showOpenDialog(null);
		if (rc == JFileChooser.APPROVE_OPTION) {
			lastDir = fc.getSelectedFile().getParent();
			magnetRON.open(fc.getSelectedFile());
		}
	}

}
