package org.ntlab.featureLocator;

import java.util.HashSet;

import org.ntlab.trace.Trace;
import org.ntlab.trace.TraceJSON;

public class SoftwareReconnaissance {

	public static void main(String[] args) {
		TraceJSON positiveTraces[] = new TraceJSON[4];
		TraceJSON negativeTraces[] = new TraceJSON[4];
		long starts[] = new long[4];
		long ends[] = new long[4];
		positiveTraces[0] = new TraceJSON("traces\\jEdit1578785_1.trace");
//		positiveTraces[1] = new TraceJSON("traces\\jEdit1578785_2.trace");
//		positiveTraces[2] = new TraceJSON("traces\\jEdit1578785_3.trace");
//		positiveTraces[3] = new TraceJSON("traces\\jEdit1578785_4.trace");
		negativeTraces[0] = new TraceJSON("traces\\jEdit1578785_1-.trace");
//		negativeTraces[1] = new TraceJSON("traces\\jEdit1578785_2-.trace");
//		negativeTraces[2] = new TraceJSON("traces\\jEdit1578785_3-.trace");
//		negativeTraces[3] = new TraceJSON("traces\\jEdit1578785_4-.trace");
		starts[0] = 3566549911632580L;
		ends[0]   = 3566553144549101L;
		starts[1] = 3567284882609419L;
		ends[1]   = 3567287171623594L;
		starts[2] = 3567564111762342L;
		ends[2]   = 3567566517748884L;
		starts[3] = 3567871983770827L;
		ends[3]   = 3567874335777400L;

		HashSet<String> positiveMethods = positiveTraces[0].getAllMethodSignatures();
		HashSet<String> negativeMethods = negativeTraces[0].getAllMethodSignatures();
		System.out.println("=== Software Reconnaissance ===");
		positiveMethods.removeAll(negativeMethods);
		for (String method: positiveMethods) {
			System.out.println(method);
		}

		HashSet<String> positiveBlocks = positiveTraces[0].getAllBlocks();
		HashSet<String> negativeBlocks = negativeTraces[0].getAllBlocks();
		System.out.println("=== Block-wise Software Reconnaissance ===");
		positiveBlocks.removeAll(negativeBlocks);
		for (String method: positiveBlocks) {
			System.out.println(method);
		}
		
		HashSet<String> positiveFlows = positiveTraces[0].getAllFlows();
		HashSet<String> negativeFlows = negativeTraces[0].getAllFlows();
		System.out.println("=== Flow-wise Software Reconnaissance ===");
		positiveFlows.removeAll(negativeFlows);
		for (String method: positiveFlows) {
			System.out.println(method);
		}
		
//		System.out.println("=== Marked Software Reconnaissance ===");
//		HashSet<String> markedMethods = positiveTraces[0].getMarkedMethodSignatures(starts[0], ends[0]);
//		HashSet<String> unmarkedMethods = negativeTraces[0].getAllMethodSignatures();
//		unmarkedMethods.addAll(negativeTraces[1].getAllMethodSignatures());
//		unmarkedMethods.addAll(negativeTraces[2].getAllMethodSignatures());
//		unmarkedMethods.addAll(negativeTraces[3].getAllMethodSignatures());
//		unmarkedMethods.addAll(positiveTraces[0].getUnmarkedMethodSignatures(starts[0], ends[0]));
//		unmarkedMethods.addAll(positiveTraces[1].getUnmarkedMethodSignatures(starts[1], ends[1]));
//		unmarkedMethods.addAll(positiveTraces[2].getUnmarkedMethodSignatures(starts[2], ends[2]));
//		unmarkedMethods.addAll(positiveTraces[3].getUnmarkedMethodSignatures(starts[3], ends[3]));
//		markedMethods.removeAll(unmarkedMethods);
//		for (String method: markedMethods) {
//			System.out.println(method);
//		}
	}

}
