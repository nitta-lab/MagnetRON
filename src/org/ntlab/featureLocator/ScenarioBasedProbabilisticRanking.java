package org.ntlab.featureLocator;

import java.util.ArrayList;
import java.util.HashMap;

import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.Trace;

public class ScenarioBasedProbabilisticRanking {

	public static void main(String[] args) {
		Trace positiveTraces[] = new Trace[4];
		Trace negativeTraces[] = new Trace[4];
		long starts[] = new long[4];
		long ends[] = new long[4];
		positiveTraces[0] = new Trace("documents\\jEdit1578785TC1-2.trace");
		positiveTraces[1] = new Trace("documents\\jEdit1578785TC2-2.trace");
		positiveTraces[2] = new Trace("documents\\jEdit1578785TC3-2.trace");
		positiveTraces[3] = new Trace("documents\\jEdit1578785TC4-2.trace");
		negativeTraces[0] = new Trace("documents\\jEdit1578785TC1-1.trace");
		negativeTraces[1] = new Trace("documents\\jEdit1578785TC2-1.trace");
		negativeTraces[2] = new Trace("documents\\jEdit1578785TC3-1.trace");
		negativeTraces[3] = new Trace("documents\\jEdit1578785TC4-1.trace");
		starts[0] = 53110313578062L;
		ends[0]   = 53114687756752L;
		starts[1] = 53656181043072L;
		ends[1]   = 53662323812706L;
		starts[2] = 56259479424073L;
		ends[2]   = 56263910205683L;
		starts[3] = 56003167357539L;
		ends[3]   = 56009513891913L;
		HashMap<String, Integer> totalExecutionsInPositiveTraces = new HashMap<>();
		HashMap<String, Integer> totalExecutionsInNegativeTraces = new HashMap<>();
		HashMap<String, Integer> totalExecutionsInsideMark = new HashMap<>();
		HashMap<String, Integer> totalExecutionsOutsideMark = new HashMap<>();
		long insideMarkExecutions = 0L;
		long outsideMarkExecutions = 0L;
		for (int n = 0; n < positiveTraces.length; n++) {
			HashMap<String, ArrayList<MethodExecution>> positiveExecutions = positiveTraces[n].getAllMethodExecutions();
			for (String method: positiveExecutions.keySet()) {
				int positive = positiveExecutions.get(method).size();
				if (totalExecutionsInPositiveTraces.get(method) == null) {
					totalExecutionsInPositiveTraces.put(method, positive);
				} else {
					totalExecutionsInPositiveTraces.put(method, positive + totalExecutionsInPositiveTraces.get(method));
				}
			}
			HashMap<String, ArrayList<MethodExecution>> negativeExecutions = negativeTraces[n].getAllMethodExecutions();
			for (String method: negativeExecutions.keySet()) {
				int negatives = negativeExecutions.get(method).size();
				if (totalExecutionsInNegativeTraces.get(method) == null) {
					totalExecutionsInNegativeTraces.put(method, negatives);
					totalExecutionsOutsideMark.put(method, negatives);
				} else {
					totalExecutionsInNegativeTraces.put(method, negatives + totalExecutionsInNegativeTraces.get(method));
					totalExecutionsOutsideMark.put(method, negatives + totalExecutionsOutsideMark.get(method));
				}
				outsideMarkExecutions += negatives;
			}			
			HashMap<String, ArrayList<MethodExecution>> positiveMarkedExecutions = positiveTraces[n].getMarkedMethodExecutions(starts[n], ends[n]);
			for (String method: positiveMarkedExecutions.keySet()) {
				int marked = positiveMarkedExecutions.get(method).size();
				if (totalExecutionsInsideMark.get(method) == null) {
					totalExecutionsInsideMark.put(method, marked);
				} else {
					totalExecutionsInsideMark.put(method, marked + totalExecutionsInsideMark.get(method));
				}
				insideMarkExecutions += marked;
			}
			HashMap<String, ArrayList<MethodExecution>> positiveUnmarkedExecutions = positiveTraces[n].getUnmarkedMethodExecutions(starts[n], ends[n]);
			for (String method: positiveUnmarkedExecutions.keySet()) {
				int unmarked = positiveUnmarkedExecutions.get(method).size();
				if (totalExecutionsOutsideMark.get(method) == null) {
					totalExecutionsOutsideMark.put(method, unmarked);
				} else {
					totalExecutionsOutsideMark.put(method, unmarked + totalExecutionsOutsideMark.get(method));
				}
				outsideMarkExecutions += unmarked;
			}
		}
		
		// An approach to feature location in distributed systems (Edwards‚ç, Journal of Systems and Software 2006)
		System.out.println("=== An approach to feature location in distributed systems ===");
		HashMap<String, Double> relevanceIndexes = new HashMap<>();
		for (String method: totalExecutionsInPositiveTraces.keySet()) {
			if (totalExecutionsInNegativeTraces.get(method) == null) continue;
			double positive = (double)totalExecutionsInPositiveTraces.get(method);
			double negative = (double)totalExecutionsInNegativeTraces.get(method);
			double relevanceIndex = positive / (positive + negative);
			relevanceIndexes.put(method, relevanceIndex);
			System.out.println(method + ":" + relevanceIndex);
		}
		
		// Scenario-Based Probabilistic Ranking (SPR, Antoniol‚ç, ICSM 2006)
		System.out.println("=== Scenario-Based Probabilistic Ranking ===");
		HashMap<String, Double> relevanceIndexesSPR = new HashMap<>();
		for (String method: totalExecutionsInsideMark.keySet()) {
			if (totalExecutionsOutsideMark.get(method) == null) continue;
			double mark = (double)totalExecutionsInsideMark.get(method) / (double)insideMarkExecutions;
			double unmark = (double)totalExecutionsOutsideMark.get(method) / (double)outsideMarkExecutions;
			double relevanceIndexSPR = mark / (mark + unmark);
			relevanceIndexesSPR.put(method, relevanceIndexSPR);
			System.out.println(method + ":" + relevanceIndexSPR);
		}
	}

}
