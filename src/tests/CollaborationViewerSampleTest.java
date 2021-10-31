package tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CollaborationViewerSampleTest {

	public static void main(String[] args) {
		// Build a frame, create a graph, and add the graph to the frame so you can actually see the graph.
		MagnetRONFrameTest frame = new MagnetRONFrameTest(true);
		frame.setVisible(true);
		frame.startAnimation();
	}
}
