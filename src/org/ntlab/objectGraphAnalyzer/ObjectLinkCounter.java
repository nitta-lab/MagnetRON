package org.ntlab.objectGraphAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.AbstractMap.SimpleEntry;

import org.ntlab.trace.FieldUpdate;
import org.ntlab.trace.IMethodExecutionVisitor;
import org.ntlab.trace.IStatementVisitor;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.Statement;
import org.ntlab.trace.ThreadInstance;
import org.ntlab.trace.Trace;
import org.ntlab.trace.TraceJSON;

public class ObjectLinkCounter {

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
		
		// オブジェクト生成木上での各呼び出し距離、呼び出し回数、リンクの有無を計算
		final HashMap<String, HashMap<String, String>> refs = new HashMap<>(); 
		final HashMap<String, HashMap<String, Integer>> callDistances = new HashMap<>();
		final HashMap<String, HashMap<String, Integer>> linkLessCallCounts = new HashMap<>();
		final HashMap<String, HashMap<String, Integer>> linkFullCallCounts = new HashMap<>();
		
		trace.traverseStatementsInTrace(new IStatementVisitor() {
			@Override
			public boolean preVisitStatement(Statement statement) {
				if (statement instanceof FieldUpdate) {
					FieldUpdate f = (FieldUpdate)statement;
					String srcObjId = null;
					String fieldName = null;
					if (!Trace.isNull(f.getContainerObjId())) {
						// 通常のオブジェクト間参照の場合
						srcObjId = f.getContainerObjId();
						fieldName = f.getFieldName();
					} else {
						// static フィールドによる参照の場合
						srcObjId = f.getContainerClassName();
						fieldName = f.getFieldName();
					}
					if (!Trace.isNull(f.getValueObjId())) {
						// null 以外への参照の場合
						if (!Trace.isPrimitive(f.getValueClassName())) {
							// 参照型の場合のみリンク生成
							if (refs.get(srcObjId) == null) {
								refs.put(srcObjId, new HashMap<String, String>());
							}
							refs.get(srcObjId).put(fieldName, f.getValueObjId());
						}
					} else {
						// null 値の代入の場合、リンクを削除
						if (refs.get(srcObjId) != null) {
							refs.get(srcObjId).remove(fieldName);
						}
					}
				} else if (statement instanceof MethodInvocation) {
					MethodInvocation c = (MethodInvocation)statement;
					String srcObjId = c.getThisObjId();
					String dstObjId = c.getCalledMethodExecution().getThisObjId();
					if (Trace.isNull(srcObjId)) {
						srcObjId = c.getThisClassName();
					}
					if (Trace.isNull(dstObjId)) {
						dstObjId = c.getCalledMethodExecution().getThisClassName();
					}
					if (callDistances.get(srcObjId) == null) {
						callDistances.put(srcObjId, new HashMap<String, Integer>());
					}
					HashMap<String, Integer> callDistance = callDistances.get(srcObjId);
					if (callDistance.get(dstObjId) == null) {
						// オブジェクト生成木上での距離を求める
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
						callDistance.put(dstObjId, distance);
					}
					if (refs.get(srcObjId) != null 
							&& refs.get(srcObjId).values().contains(dstObjId)) {
						// 呼び出し時にリンクが存在している
						if (linkFullCallCounts.get(srcObjId) == null) {
							linkFullCallCounts.put(srcObjId, new HashMap<String, Integer>());
						}
						HashMap<String, Integer> callCount = linkFullCallCounts.get(srcObjId);
						if (callCount.get(dstObjId) != null) {
							callCount.put(dstObjId, callCount.get(dstObjId) + 1);
						} else {
							callCount.put(dstObjId, 1);
						}
					} else {
						// 呼び出し時にリンクが存在していない						
						if (linkLessCallCounts.get(srcObjId) == null) {
							linkLessCallCounts.put(srcObjId, new HashMap<String, Integer>());
						}
						HashMap<String, Integer> callCount = linkLessCallCounts.get(srcObjId);
						if (callCount.get(dstObjId) != null) {
							callCount.put(dstObjId, callCount.get(dstObjId) + 1);
						} else {
							callCount.put(dstObjId, 1);
						}
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
		for (String srcObjId: callDistances.keySet()) {
			for (String dstObjId: callDistances.get(srcObjId).keySet()) {
				int callCountWithLink = 0;
				int callCountWithoutLink = 0;
				if (linkFullCallCounts.get(srcObjId) != null && linkFullCallCounts.get(srcObjId).get(dstObjId) != null) {
					callCountWithLink = linkFullCallCounts.get(srcObjId).get(dstObjId);
				}
				if (linkLessCallCounts.get(srcObjId) != null && linkLessCallCounts.get(srcObjId).get(dstObjId) != null) {
					callCountWithoutLink = linkLessCallCounts.get(srcObjId).get(dstObjId);
				}
				System.out.println(srcObjId + ":" + classNames.get(srcObjId) + ":" + dstObjId + ":" + classNames.get(dstObjId) + ":" + callDistances.get(srcObjId).get(dstObjId) + ":" + callCountWithLink + ":" + callCountWithoutLink);
			}
		}
	}

}
