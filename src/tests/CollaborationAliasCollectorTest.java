package tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.ntlab.deltaExtractor.Alias;
import org.ntlab.deltaExtractor.ExtractedStructure;
import org.ntlab.deltaExtractor.IAliasCollector;
import org.ntlab.deltaViewer.CollaborationAliasCollector;

class CollaborationAliasCollectorTest {

	@Test
	void testMerge() {
		// Change Here!
//		String key = "getterOverlap";
//		String key = "setterOverlap";
		String key = "JHotDrawSelect";
		
		MagnetRONFrameTest magnetRONFrame = new MagnetRONFrameTest();
		Map<ExtractedStructure, IAliasCollector> extractedMultipleDeltas = magnetRONFrame.extractMultipleDeltas(key);
		List<ExtractedStructure> eList = new ArrayList<>(extractedMultipleDeltas.keySet());
		List<IAliasCollector> dacList = new ArrayList<>(extractedMultipleDeltas.values());
		CollaborationAliasCollector cac = null;
		for (IAliasCollector dac: dacList) {
			if (cac == null) {
				cac = new CollaborationAliasCollector(dac);
			} else {
				cac.merge(dac, null);
			}
		}
		System.out.println("mergedAliasList: ");
		for (Alias alias: cac.getAliasList()) {
			System.out.println(alias.getObjectId() + ", " + alias.getMethodSignature() + " l." + alias.getLineNo() + " : " + alias.getAliasType().toString() + ", (" + alias.getMethodExecution().getEntryTime() + ", " + alias.getMethodExecution().getExitTime() + "), " + alias.getTimeStamp());
		}
		assertNotNull(cac);
		for (IAliasCollector dac: dacList) {
			assertTrue(cac.getAliasList().contains(dac.getAliasList().get(0)));
		}
	}
}
