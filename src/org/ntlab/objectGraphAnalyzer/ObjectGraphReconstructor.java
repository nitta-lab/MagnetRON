package org.ntlab.objectGraphAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;

import org.ntlab.trace.FieldUpdate;
import org.ntlab.trace.IMethodExecutionVisitor;
import org.ntlab.trace.IStatementVisitor;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.Statement;
import org.ntlab.trace.ThreadInstance;
import org.ntlab.trace.Trace;
import org.ntlab.trace.TraceJSON;

public class ObjectGraphReconstructor {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final HashMap<String, Integer> callCounts = new HashMap<>();
		TraceJSON trace = new TraceJSON("traces\\jEditNormal.trace");
		
		for (ThreadInstance thread: trace.getAllThreads().values()) {
			thread.traverseMethodExecutionsBackward(new IMethodExecutionVisitor() {
				@Override
				public boolean preVisitThread(ThreadInstance thread) {
					return false;
				}
				
				@Override
				public boolean preVisitMethodExecution(MethodExecution methodExecution) {
					String objId = methodExecution.getThisObjId();
					if (!Trace.isNull(objId)) {
						String key = objId;
						if (callCounts.get(key) == null) {
							callCounts.put(key, 1);
						} else {
							callCounts.put(key, callCounts.get(key) + 1);
						}
					} else {
						String className = methodExecution.getThisClassName();
						String key = className;
						if (callCounts.get(key) == null) {
							callCounts.put(key, 1);
						} else {
							callCounts.put(key, callCounts.get(key) + 1);
						}
					}
					return false;
				}
				
				@Override
				public boolean postVisitThread(ThreadInstance thread) {
					return false;
				}
				
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}
		
		final HashMap<String, String> classNames = new HashMap<>();
		final HashMap<String, String> links = new HashMap<>(); 
		trace.traverseStatementsInTrace(new IStatementVisitor() {
			@Override
			public boolean preVisitStatement(Statement statement) {
				if (statement instanceof FieldUpdate) {
					FieldUpdate f = (FieldUpdate)statement;
					String linkId = null;
					if (!Trace.isNull(f.getContainerObjId())) {
						// 通常のオブジェクト間参照の場合
						if (classNames.get(f.getContainerObjId()) == null) {
							classNames.put(f.getContainerObjId(), f.getContainerClassName());
						}
						linkId = f.getContainerObjId() + ":" + f.getFieldName();
					} else {
						// static フィールドによる参照の場合
						if (classNames.get(f.getContainerClassName()) == null) {
							classNames.put(f.getContainerClassName(), f.getContainerClassName());
						}						
						linkId = f.getContainerClassName() + ":" + f.getFieldName();
					}
					if (!Trace.isNull(f.getValueObjId())) {
						// null 以外への参照の場合
						if (!Trace.isPrimitive(f.getValueClassName())) {
							// 参照型の場合のみリンク生成
							links.put(linkId, f.getValueObjId());
							if (classNames.get(f.getValueObjId()) == null) {
								classNames.put(f.getValueObjId(), f.getValueClassName());
							}
						}
					} else {
						// null 値の代入の場合、リンクを削除
						links.remove(linkId);
					}
				}
				return false;
			}
			@Override
			public boolean postVisitStatement(Statement statement) {
				return false;
			}
		});
		String objectLabel;
		String objectElements[];
		String linkElements[];
		String fieldElements[];
		System.out.println("digraph jEditNormal {");
		for (String objectId: classNames.keySet()) {
			if (callCounts.get(objectId) != null && callCounts.get(objectId) >= 200) {
				objectElements = objectId.split("\\.");
				objectLabel = objectElements[objectElements.length - 1].replace("$", "_");
				System.out.println(objectLabel + " [label=\"" + objectId + ":" + classNames.get(objectId)+ "\"]");
			}
		}
		String srcObjId;
		String dstObjId;
		for (String linkId: links.keySet()) {
			linkElements = linkId.split(":");
			srcObjId = linkElements[0];
			dstObjId = links.get(linkId);
			if (callCounts.get(srcObjId) != null && callCounts.get(srcObjId) >= 200 
					&& callCounts.get(dstObjId) != null && callCounts.get(dstObjId) >= 200) {
				fieldElements = linkElements[1].split("\\.");
				objectElements = srcObjId.split("\\.");
				objectLabel = objectElements[objectElements.length - 1].replace("$", "_");
				System.out.println(objectLabel + " -> "
									+ dstObjId
									+ " [label=\"" + fieldElements[fieldElements.length - 1] +"\"]");
			}
		}
		System.out.println("}");
	}
}
