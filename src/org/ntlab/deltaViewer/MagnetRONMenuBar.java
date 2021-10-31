package org.ntlab.deltaViewer;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.ntlab.actions.AutoTrackingAction;
import org.ntlab.actions.ExtractAction;
import org.ntlab.actions.OpenAction;
import org.ntlab.actions.ZoomInAction;
import org.ntlab.actions.ZoomOutAction;
import org.ntlab.featureExtractor.Feature;
import org.ntlab.actions.ZoomActualAction;

/**
 * 
 * 
 * @author Nitta Lab.
 */
public class MagnetRONMenuBar extends JMenuBar {
	
	private static final long serialVersionUID = 4967305187008413238L;

	private MagnetRONFrame magnetRON;
	private JMenu extractsMenu;

	private String language;
	
	public MagnetRONMenuBar(MagnetRONFrame magnetRON) {
		super();
		this.magnetRON = magnetRON;
		
		// If you want to change the language, change here!
		Locale.setDefault(Locale.ENGLISH);
//		Locale.setDefault(Locale.JAPANESE);

        Locale local = Locale.getDefault();
        this.language = local.getLanguage();
        String languageFile = null;
        if (language.equals("ja")){
        	languageFile = "/strings/ja.properties";
        } else{
        	languageFile = "/strings/en.properties";
        }
        
        Properties properties = new Properties();
        try {
            InputStream langFileIn = this.getClass().getResourceAsStream(languageFile);
            properties.load(langFileIn);
        } catch (Exception e) {
            System.out.println("Load properties error!");
        }

		JMenu fileMenu = add(new JMenu(properties.getProperty("file")));
		fileMenu.add(new OpenAction(properties.getProperty("open_file"), magnetRON, properties.getProperty("file_desc")));

		extractsMenu = add(new JMenu(properties.getProperty("extract_feature")));

		JMenu animationSettingMenu = add(new JMenu(properties.getProperty("anim_settings")));
		JCheckBox defaultCheck = new JCheckBox(properties.getProperty("normal"), true);
		defaultCheck.addActionListener(new ZoomActualAction(magnetRON.getViewer()));
		JCheckBox autoTrackingCheck = new JCheckBox(properties.getProperty("auto_tracking"), false);
		autoTrackingCheck.addChangeListener(new AutoTrackingAction(magnetRON.getViewer()));
		
		ButtonGroup checkBoxGroup = new ButtonGroup();
		checkBoxGroup.add(defaultCheck);
		checkBoxGroup.add(autoTrackingCheck);
		animationSettingMenu.add(defaultCheck);
		animationSettingMenu.add(autoTrackingCheck);

		JMenu viewMenu = add(new JMenu(properties.getProperty("view")));
		viewMenu.add(new ZoomInAction(properties.getProperty("zoom_in"), magnetRON.getViewer()));
		viewMenu.add(new ZoomOutAction(properties.getProperty("zoom_out"), magnetRON.getViewer()));
		viewMenu.add(new ZoomActualAction(magnetRON.getViewer()));	
	}

	public void updateExtractsMenu(List<Feature> features) {
		extractsMenu.removeAll();
		for (Feature feature: features) {
			extractsMenu.add(new ExtractAction(feature, magnetRON));
		}
	}
	
}
