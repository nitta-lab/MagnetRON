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
		
		// �I�u�W�F�N�g�����؂̍쐬
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
					if (!srcObjId.equals(dstObjId)) {		// �e�q�N���X�Ԃ̃��\�b�h�Ăяo�����������ꍇ���l��
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
		
		// �e�I�u�W�F�N�g����������؂̃��[�g�m�[�h�Ɛ[���̌v�Z
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
		
		// �I�u�W�F�N�g�����؏�ł̊e�Ăяo�������A�Ăяo���񐔂̌v�Z
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
//							distance = depthOf.get(dstObjId) + 2;		// �V�X�e���S�̂̃��[�g�܂ł�1�A�V�X�e���S�̂̃��[�g����e�N���X�I�u�W�F�N�g�܂ł�1�Ƃ���
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
		
		// �I�u�W�F�N�gid�A�N���X���A���Ăяo���񐔁A���Ăяo�������A�ő�Ăяo�������A�Ăяo�����I�u�W�F�N�g���A�Ăяo�����I�u�W�F�N�g�P�ʂ̌Ăяo�������̑��a�̏o��
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
