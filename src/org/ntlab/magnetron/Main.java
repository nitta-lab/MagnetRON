package org.ntlab.magnetron;

import org.ntlab.deltaViewer.MagnetRONFrame;

/**
 * Delta viewer sample for MagnetRON.
 * 
 * @author Nitta Lab.
 */
public class Main {
	public static void main(String[] args) {
		// Build a frame, create a graph, and add the graph to the frame so you can actually see the graph.
		MagnetRONFrame frame = new MagnetRONFrame();
		frame.setVisible(true);
//		frame.startAll();
	}
}


