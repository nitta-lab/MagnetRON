package org.ntlab.objectGraphAnalyzer;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.ntlab.trace.FieldUpdate;
import org.ntlab.trace.IMethodExecutionVisitor;
import org.ntlab.trace.IStatementVisitor;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.Statement;
import org.ntlab.trace.ThreadInstance;
import org.ntlab.trace.Trace;
import org.ntlab.trace.TraceJSON;

public class ObjectCreationGraphConstructor {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TraceJSON trace = new TraceJSON("traces\\jEditNormal.trace");
		
		// オブジェクト生成木の生成
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
		
		String objectLabel;
		String objectElements[];
		System.out.println("digraph jEditNormal {");
		for (String objectId: classNames.keySet()) {
			objectElements = objectId.split("\\.");
			objectLabel = objectElements[objectElements.length - 1].replace("$", "_");
			System.out.println(objectLabel + " [label=\"" + objectId + ":" + classNames.get(objectId)+ "\"]");
		}
		for (SimpleEntry<String, String> link: links) {
			String srcObjId = link.getKey();
			String dstObjId = link.getValue();
			objectElements = srcObjId.split("\\.");
			objectLabel = objectElements[objectElements.length - 1].replace("$", "_");
			System.out.println(objectLabel + " -> " + dstObjId);
		}
		System.out.println("}");
	}
}
