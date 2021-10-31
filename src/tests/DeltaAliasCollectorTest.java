package tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.ntlab.deltaExtractor.ExtractedStructure;
import org.ntlab.deltaExtractor.IAliasCollector;
import org.ntlab.deltaViewer.CollaborationAliasCollector;
import org.ntlab.deltaViewer.DeltaAliasCollector;

class DeltaAliasCollectorTest {

	@Test
	void testShrink() {
		String key = "JHotDrawSelect";
		MagnetRONFrameTest magnetRONFrame = new MagnetRONFrameTest();
		Map<ExtractedStructure, IAliasCollector> extractedMultipleDeltas = magnetRONFrame.extractMultipleDeltas(key);
		List<ExtractedStructure> eList = new ArrayList<>(extractedMultipleDeltas.keySet());
		List<IAliasCollector> dacList = new ArrayList<>(extractedMultipleDeltas.values());
		CollaborationAliasCollector cac = null;
		for (IAliasCollector dac: dacList) {
			if (dac instanceof DeltaAliasCollector) {
				DeltaAliasCollector deltaAliasCollector = (DeltaAliasCollector)dac;
				deltaAliasCollector.shrink();
				assertNotNull(deltaAliasCollector);
			}
		}
	}
}
