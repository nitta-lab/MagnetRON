package org.ntlab.objectGraphAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.AbstractMap.SimpleEntry;

import org.ntlab.trace.IMethodExecutionVisitor;
import org.ntlab.trace.IStatementVisitor;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.Statement;
import org.ntlab.trace.ThreadInstance;
import org.ntlab.trace.Trace;
import org.ntlab.trace.TraceJSON;

public class ObjectCallCounter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TraceJSON trace = new TraceJSON("traces\\jEditNormal.trace");
		
		// オブジェクト生成木の作成
		final HashMap<String, String> classNames = new HashMap<>();
		final ArrayList<SimpleEntry<String, String>> links = new ArrayList<>();
		final HashMap<String, String> parentOf = new HashMap<>();
		
		trace.traverseMethodEntriesInTraceBackward(new IMethodExecutionVisitor() {
			@Override
			public boolean preVisitThread(ThreadInstance thread) {
				return false;
			}
			@Override
			public boolean preVisitMethodExecution(MethodExecution methodExecution) {
				return false;
			}
			@Override
			public boolean postVisitThread(ThreadInstance thread) {
				return false;
			}
			@Override
			public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
				if (methodExecution.isConstructor() && methodExecution.getCallerMethodExecution() != null) {
					String srcObjId = methodExecution.getCallerMethodExecution().getThisObjId();
					String dstObjId = methodExecution.getThisObjId();
					if (!srcObjId.equals(dstObjId)) {		// 親子クラス間のメソッド呼び出しがあった場合を考慮
						if (Trace.isNull(srcObjId)) {
							srcObjId = methodExecution.getCallerMethodExecution().getThisClassName();
						}
						classNames.put(srcObjId, methodExecution.getCallerMethodExecution().getThisClassName());
						classNames.put(dstObjId, methodExecution.getThisClassName());
						links.add(new SimpleEntry<>(srcObjId, dstObjId));
						parentOf.put(dstObjId, srcObjId);
					}
				}
				return false;
			}
		});
		
		// 各オブジェクトが所属する木のルートノードと深さの計算
		final HashMap<String, String> rootOf = new HashMap<>();
		final HashMap<String, Integer> depthOf = new HashMap<>();
		
		for (String objId: classNames.keySet()) {
			int depth = 0;
			String rootObjId = objId;
			while (parentOf.get(rootObjId) != null) {
				rootObjId = parentOf.get(rootObjId);
				depth++;
				if (depthOf.get(rootObjId) != null) {
					depth += depthOf.get(rootObjId);
					rootObjId = rootOf.get(rootObjId);
				}
			}
			rootOf.put(objId, rootObjId);
			depthOf.put(objId, depth);			
		}
		
		// オブジェクト生成木上での各呼び出し距離、呼び出し回数の計算
		final HashMap<String, Integer> maxCallDistances = new HashMap<>();
		final HashMap<String, Integer> totalCallDistances = new HashMap<>();
		final HashMap<String, HashMap<String, Integer>> callDistances = new HashMap<>();
		final HashMap<String, Integer> callCounts = new HashMap<>();
		
		trace.traverseStatementsInTrace(new IStatementVisitor() {
			@Override
			public boolean preVisitStatement(Statement statement) {
				if (statement instanceof MethodInvocation) {
					MethodInvocation c = (MethodInvocation)statement;
					String srcObjId = c.getThisObjId();
					String dstObjId = c.getCalledMethodExecution().getThisObjId();
					if (Trace.isNull(srcObjId)) {
						srcObjId = c.getThisClassName();
					}
					if (Trace.isNull(dstObjId)) {
						dstObjId = c.getCalledMethodExecution().getThisClassName();
					}						
					int distance;
					if (rootOf.get(srcObjId) == null) {
						rootOf.put(srcObjId, srcObjId);
						depthOf.put(srcObjId, 0);
						classNames.put(srcObjId, c.getThisClassName());
					}
					if (rootOf.get(dstObjId) == null) {
						rootOf.put(dstObjId, dstObjId);
						depthOf.put(dstObjId, 0);
						classNames.put(dstObjId, c.getCalledMethodExecution().getThisClassName());
					}
//					if (rootOf.get(srcObjId).equals(rootOf.get(dstObjId))) {
						ArrayList<String> srcPath = new ArrayList<>();						
						srcPath.add(srcObjId);
						String rootObjId = srcObjId;
						while (parentOf.get(rootObjId) != null) {
							rootObjId = parentOf.get(rootObjId);
							srcPath.add(0, rootObjId);
						}
						
						ArrayList<String> dstPath = new ArrayList<>();
						dstPath.add(dstObjId);
						rootObjId = dstObjId;
						while (parentOf.get(rootObjId) != null) {
							rootObjId = parentOf.get(rootObjId);
							dstPath.add(0, rootObjId);
						}
						
						int srcDepth = depthOf.get(srcObjId);
						int dstDepth = depthOf.get(dstObjId);
						int depth = 0;
						while (srcPath.get(depth).equals(dstPath.get(depth))) {
							depth++;
							if (depth >= srcPath.size() || depth >= dstPath.size()) break;
						}
						distance = srcDepth + dstDepth - 2 * (depth - 1);
//					} else {
//						if (depthOf.get(dstObjId) != null) {
//							distance = depthOf.get(dstObjId) + 2;		// システム全体のルートまでを1、システム全体のルートから各クラスオブジェクトまでを1とする
//						} else {
//							distance = 2;
//						}
//					}
					if (callDistances.get(dstObjId) == null) {
						callDistances.put(dstObjId, new HashMap<String, Integer>());
					}
					HashMap<String, Integer> callDistance = callDistances.get(dstObjId);
					if (callDistance.get(srcObjId) == null) {
						callDistance.put(srcObjId, distance);
					}
					if (totalCallDistances.get(dstObjId) != null) {
						totalCallDistances.put(dstObjId, totalCallDistances.get(dstObjId) + distance);
					} else {
						totalCallDistances.put(dstObjId, distance);
					}
					if (callCounts.get(dstObjId) != null) {
						callCounts.put(dstObjId, callCounts.get(dstObjId) + 1);
					} else {
						callCounts.put(dstObjId, 1);
					}
					if (maxCallDistances.get(dstObjId) == null || distance > maxCallDistances.get(dstObjId)) {
						maxCallDistances.put(dstObjId, distance);
					}
				}
				return false;
			}
			@Override
			public boolean postVisitStatement(Statement statement) {
				return false;
			}
		});
		
		// オブジェクトid、クラス名、総呼び出し回数、総呼び出し距離、最大呼び出し距離、呼び出し元オブジェクト数、呼び出し元オブジェクト単位の呼び出し距離の総和の出力
		for (String objId: callCounts.keySet()) {
			int variation = 0;
			int sum = 0;
			for (String srcObjId: callDistances.get(objId).keySet()) {
				sum += callDistances.get(objId).get(srcObjId);
				variation++;
			}
			System.out.println(objId + ":" + classNames.get(objId) + ":" + callCounts.get(objId) + ":" + totalCallDistances.get(objId) + ":" + maxCallDistances.get(objId)+ ":" + variation + ":" + sum);
		}
		
//		final HashMap<String, String> classNames = new HashMap<>();
//		final HashMap<String, Integer> callCounts = new HashMap<>();
//		
//		for (ThreadInstance thread: trace.getAllThreads().values()) {
//			thread.traverseMethodExecutionsBackward(new IMethodExecutionVisitor() {
//				@Override
//				public boolean preVisitThread(ThreadInstance thread) {
//					return false;
//				}
//				
//				@Override
//				public boolean preVisitMethodExecution(MethodExecution methodExecution) {
//					String objId = methodExecution.getThisObjId();
//					if (!Trace.isNull(objId)) {
//						String key = objId;
//						if (classNames.get(key) == null) {
//							classNames.put(key, methodExecution.getThisClassName());
//						}
//						if (callCounts.get(key) == null) {
//							callCounts.put(key, 1);
//						} else {
//							callCounts.put(key, callCounts.get(key) + 1);
//						}
//					} else {
//						String className = methodExecution.getThisClassName();
//						String key = className;
//						if (classNames.get(key) == null) {
//							classNames.put(key, className);
//						}
//						if (callCounts.get(key) == null) {
//							callCounts.put(key, 1);
//						} else {
//							callCounts.put(key, callCounts.get(key) + 1);
//						}
//					}
//					return false;
//				}
//				
//				@Override
//				public boolean postVisitThread(ThreadInstance thread) {
//					return false;
//				}
//				
//				@Override
//				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
//					return false;
//				}
//			});
//		}
//		
//		for (String objId: callCounts.keySet()) {
//			System.out.println(objId + ":" + classNames.get(objId) + ":" + callCounts.get(objId));
//		}
	}

}
