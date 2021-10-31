package org.ntlab.trace;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

public class TraceJPDA {
	protected HashMap<String, ThreadInstanceJPDA> threads = new HashMap<String, ThreadInstanceJPDA>();
	
	/**
	 * 指定したJSONトレースファイルを解読して Trace オブジェクトを生成する
	 * @param traceFile トレースファイルのパス
	 */
	public TraceJPDA(BufferedReader file) {
		try {
			readJPDA(file);
			file.close();		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 指定したJSONトレースファイルを解読して Trace オブジェクトを生成する
	 * @param traceFile トレースファイルのパス
	 */
	public TraceJPDA(String traceFile) {
		BufferedReader file;
		try {
			file = new BufferedReader(new FileReader(traceFile));
			readJPDA(file);
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readJPDA(BufferedReader file) throws IOException {
		// トレースファイル読み込み
		String line = null;
		String[] columns;
		String[] columns2;
		String[] stack;
		String[] signature;
		String threadId;
		String threadName;
		HashMap<String, Integer> threadCallDepths = new HashMap<>();
		int depth = 0;
		long timeStamp = 0L;
		ThreadInstanceJPDA thread = null;
		while ((line = file.readLine()) != null) {
			// トレースファイルの解析
			columns = line.split(":");
			if (columns.length < 4) continue;
			threadName = columns[0];
			threadId = columns[1];
			stack = columns[2].split(" ");
			columns2 = columns[3].split("\t");
			if (columns2.length < 2) continue;
			timeStamp = Integer.parseInt(stack[stack.length - 1]) * 60 
					+ Integer.parseInt(columns2[0]);
			depth = stack.length - 1;
			signature = columns2[1].split("  --  ");
			thread = threads.get(threadName);
			if (thread == null) {
				thread = new ThreadInstanceJPDA(threadName);
				threads.put(threadName, thread);
				threadCallDepths.put(threadName, 0);
			}
			if (signature.length < 2) continue;
			for (int i = 0; i < threadCallDepths.get(threadName) - depth + 1; i++) {
				thread.returnMethod();
			}
			thread.callMethod(signature[1] + "." + signature[0] + "()", timeStamp);
			threadCallDepths.put(threadName, depth);
		}
	}
	
	/**
	 * メソッド毎に全メソッド実行を全てのスレッドから取り出す
	 * @return メソッドシグニチャからメソッド実行のリストへのHashMap
	 */
	public HashMap<String, ArrayList<MethodExecutionJPDA>> getAllMethodExecutions() {
		final HashMap<String, ArrayList<MethodExecutionJPDA>> results = new HashMap<>();
		for (ThreadInstanceJPDA thread: threads.values()) {
			thread.traverseMethodExecutionsBackward(new MethodExecutionJPDA.IMethodExecutionVisitorJPDA() {
				@Override
				public boolean preVisitThread(ThreadInstanceJPDA thread) {
					return false;
				}
				@Override
				public boolean postVisitThread(ThreadInstanceJPDA thread) {
					return false;
				}
				@Override
				public boolean preVisitMethodExecution(MethodExecutionJPDA methodExecution) {
					ArrayList<MethodExecutionJPDA> executions = results.get(methodExecution.getSignature());
					if (executions == null) {
						executions = new ArrayList<MethodExecutionJPDA>();
						results.put(methodExecution.getSignature(), executions);
					}
					executions.add(methodExecution);
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecutionJPDA methodExecution, ArrayList<MethodExecutionJPDA> children) {
					return false;
				}
			});
		}	
		return results;		
	}

	/**
	 * 全メソッドのシグニチャを取得する
	 * @return 全メソッドシグニチャ
	 */
	public HashSet<String> getAllMethodSignatures() {
		final HashSet<String> results = new HashSet<String>();
		for (ThreadInstanceJPDA thread: threads.values()) {
			thread.traverseMethodExecutionsBackward(new MethodExecutionJPDA.IMethodExecutionVisitorJPDA() {
				@Override
				public boolean preVisitThread(ThreadInstanceJPDA thread) {
					return false;
				}
				@Override
				public boolean postVisitThread(ThreadInstanceJPDA thread) {
					return false;
				}
				@Override
				public boolean preVisitMethodExecution(MethodExecutionJPDA methodExecution) {
					results.add(methodExecution.getSignature());
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecutionJPDA methodExecution, ArrayList<MethodExecutionJPDA> children) {
					return false;
				}
				
			});
		}
		return results;
	}
	
	public void traverseMethodExecutionsBackward(MethodExecutionJPDA.IMethodExecutionVisitorJPDA visitor) {
		for (ThreadInstanceJPDA thread: threads.values()) {
			visitor.preVisitThread(thread);
			thread.traverseMethodExecutionsBackward(visitor);
			visitor.postVisitThread(thread);
		}		
	}
}
