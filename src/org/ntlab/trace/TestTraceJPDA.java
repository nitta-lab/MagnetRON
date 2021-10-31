package org.ntlab.trace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class TestTraceJPDA {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			final HashMap<String, Integer> methodTraceCounts = new HashMap<>();		// そのメソッドの実行を含むトレースの数
			final HashMap<String, Integer> methodExecCountSums = new HashMap<>();	// そのメソッドの全実行トレースにおける総実行回数
			final HashMap<String, Integer> methodExecDepthSums = new HashMap<>();	// そのメソッドの全実行トレースにおける全実行の呼び出し深さの総和
			File dir;
			dir = new File("traces");
			for (File file: dir.listFiles()) {
				if (file.getName().endsWith(".log")) {
					TraceJPDA traceJPDA = new TraceJPDA(new BufferedReader(new FileReader(file)));
					final HashMap<String, Integer> methodExecCounts = new HashMap<>();
					final HashMap<String, Integer> methodExecDepthSum = new HashMap<>();
					MethodExecutionJPDA.IMethodExecutionVisitorJPDA visitor = new MethodExecutionJPDA.IMethodExecutionVisitorJPDA() {
						private int depth = 0;
						
						@Override
						public boolean preVisitThread(ThreadInstanceJPDA thread) {
							depth = 0;
							return false;
						}
						@Override
						public boolean postVisitThread(ThreadInstanceJPDA thread) {
							return false;
						}
						@Override
						public boolean preVisitMethodExecution(MethodExecutionJPDA methodExecution) {
							String signature = methodExecution.getSignature();
							if (methodExecCounts.get(signature) == null) {
								methodExecCounts.put(signature, 1);
							} else {
								methodExecCounts.put(signature, methodExecCounts.get(signature) + 1); 
							}
							if (methodExecDepthSum.get(signature) == null) {
								methodExecDepthSum.put(signature, depth);
							} else {
								methodExecDepthSum.put(signature, methodExecDepthSum.get(signature) + depth); 
							}
							depth++;
							return false;
						}
						@Override
						public boolean postVisitMethodExecution(MethodExecutionJPDA methodExecution, ArrayList<MethodExecutionJPDA> children) {
							depth--;
							return false;
						}
					};
					traceJPDA.traverseMethodExecutionsBackward(visitor);
					
					for (String signature: methodExecCounts.keySet()) {
						if (methodExecCounts.get(signature) != null) {
							if (methodTraceCounts.get(signature) == null) {
								methodTraceCounts.put(signature, 1);
							} else {
								methodTraceCounts.put(signature, methodTraceCounts.get(signature) + 1);
							}
							if (methodExecCountSums.get(signature) == null) {
								methodExecCountSums.put(signature, methodExecCounts.get(signature));
							} else {
								methodExecCountSums.put(signature, methodExecCountSums.get(signature) + methodExecCounts.get(signature));
							}
						}
						if (methodExecDepthSum.get(signature) != null) {
							if (methodExecDepthSums.get(signature) == null) {
								methodExecDepthSums.put(signature, methodExecDepthSum.get(signature));
							} else {
								methodExecDepthSums.put(signature, methodExecDepthSums.get(signature) + methodExecDepthSum.get(signature));
							}
						}
					}
				}
			}
			for (String signature: methodTraceCounts.keySet()) {
				int traceCount = methodTraceCounts.get(signature);
				int execCount = methodExecCountSums.get(signature); 
				System.out.println(signature + ":" + traceCount + ":" 
				+ ((double)execCount / (double)traceCount) + ":"
				+ ((double)methodExecDepthSums.get(signature) / (double)execCount));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		TraceJPDA trace150938 = new TraceJPDA("traces\\trace1500938.log");
//		System.out.println("===== JPDA Trace Methods =====");
//		for (String method: trace150938.getAllMethodSignatures()) {
//			System.out.println(method);
//		}
/*
 * 正しい結果
 * 
===== JPDA Trace Methods =====
org.gjt.sp.jedit.bsh.Parser.jj_3R_51()
org.gjt.sp.jedit.textarea.TextArea.getHorizontalOffset()
org.gjt.sp.jedit.bsh.Types.getTypes()
org.gjt.sp.jedit.EBMessage.toString()
org.gjt.sp.jedit.bsh.Parser.jj_3R_84()
org.gjt.sp.jedit.bsh.Reflect.invokeObjectMethod()
org.gjt.sp.jedit.TextUtilities.findMatchingBracket()
org.gjt.sp.jedit.textarea.TextArea.getLastPhysicalLine()
org.gjt.sp.jedit.textarea.TextAreaPainter.isWrapGuidePainted()
org.gjt.sp.jedit.bsh.Parser.MultiplicativeExpression()
org.gjt.sp.jedit.textarea.SelectionManager.getSelectionStartAndEnd()
org.gjt.sp.jedit.Buffer.toString()
org.gjt.sp.jedit.bsh.Parser.jj_3R_86()
org.gjt.sp.jedit.textarea.TextArea.getPainter()
org.gjt.sp.jedit.bsh.BshClassManager$SignatureKey.equals()
org.gjt.sp.jedit.bsh.Parser.jj_3R_44()
org.gjt.sp.jedit.bsh.Parser.jj_3R_142()
org.gjt.sp.jedit.textarea.SelectionManager.addToSelection()
org.gjt.sp.jedit.gui.StatusBar$2.actionPerformed()
org.gjt.sp.jedit.bsh.JavaCharStream.getEndLine()
org.gjt.sp.jedit.bsh.Parser.jj_ntk()
org.gjt.sp.util.WorkThreadPool.fireProgressChanged()
org.gjt.sp.jedit.textarea.TextArea.getVisibleLines()
     :
 */
	}
}
