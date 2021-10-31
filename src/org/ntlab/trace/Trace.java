package org.ntlab.trace;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

public class Trace {
	protected static final boolean EAGER_DETECTION_OF_ARRAY_SET = false;		// 配列要素への代入の検出を多く見積もるか?(多く見積もるとFalse Positiveになる可能性がある)
	protected static Trace theTrace = null;
	protected HashMap<String, ThreadInstance> threads = new HashMap<String, ThreadInstance>();

	protected Trace() {
	}
	
	/**
	 * 指定したPlainTextのトレースファイルを解読して Trace オブジェクトを生成する
	 * @param file トレースファイル
	 */
	public Trace(BufferedReader file) {
		try {
			read(file);
			file.close();		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 指定したPlainTextのトレースファイルを解読して Trace オブジェクトを生成する
	 * @param traceFile トレースファイルのパス
	 */
	public Trace(String traceFile) {
		BufferedReader file;
		try {
			file = new BufferedReader(new FileReader(traceFile));
			read(file);
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void read(BufferedReader file) throws IOException {
		// トレースファイル読み込み
		String line, prevLine = null;
		String signature;
		String callerSideSignature;
		String threadNo = null;
		String[] methodData;
		String[] argData;
		String[] returnData;
		String[] accessData;
		String[] updateData;
		String thisObjectId;
		String thisClassName;
		boolean isConstractor = false;
		boolean isCollectionType = false;
		boolean isStatic = false;
		long timeStamp = 0L;
		ThreadInstance thread = null;
		HashMap<String, Stack<String>> stacks = new HashMap<String, Stack<String>>();
		while ((line = file.readLine()) != null) {
			// トレースファイルの解析
			if (line.startsWith("Method")) {
				// メソッド呼び出し（コンストラクタ呼び出しも含む）
				methodData = line.split(":");
				int n = methodData[0].indexOf(',');
				signature = methodData[0].substring(n + 1);
				threadNo = getThreadNo(line);
//				threadNo = methodData[methodData.length - 1].split(" ")[1];
				if (threadNo != null) {
					thisObjectId = methodData[1];
					thisClassName = methodData[0].substring(0, n).split(" ")[1];
					isConstractor = false;
					isStatic = false;
					if (signature.contains("static ")) {
						isStatic = true;
					}
					callerSideSignature = signature;
					timeStamp = Long.parseLong(methodData[methodData.length - 2]);
					if (prevLine != null) {
						if (prevLine.startsWith("New")) {
							isConstractor = true;							
						} else if (prevLine.startsWith("Invoke")) {
							callerSideSignature = prevLine.split(":")[1];
						}
					}
					thread = threads.get(threadNo);
					Stack<String> stack;
					if (thread == null) {
						thread = new ThreadInstance(threadNo);
						threads.put(threadNo, thread);
						stack = new Stack<String>();
						stacks.put(threadNo, stack);
					} else {
						stack = stacks.get(threadNo);
					}
					stack.push(line);
					thread.callMethod(signature, callerSideSignature, thisClassName, thisObjectId, isConstractor, isStatic, timeStamp);
				}
			} else if (line.startsWith("Args")) {
				// メソッド呼び出しの引数
				argData = line.split(":");
				threadNo = getThreadNo(line);
//				threadNo = argData[argData.length - 1].split(" ")[1];
				if (threadNo != null) {
					thread = threads.get(threadNo);
					ArrayList<ObjectReference> arguments = new ArrayList<ObjectReference>();
					for (int k = 1; k < argData.length - 2; k += 2) {
						arguments.add(new ObjectReference(argData[k+1], argData[k]));
					}
					thread.setArgments(arguments);
				}
			} else if (line.startsWith("Return")) {
				// メソッドからの復帰
				returnData = line.split(":");
				threadNo = getThreadNo(line);
//				threadNo = returnData[returnData.length - 1].split(" ")[1];
				if (threadNo != null) {
					Stack<String> stack = stacks.get(threadNo);
					if (!stack.isEmpty()) {
						String line2 = stack.peek();
						if (line2.split("\\(")[0].endsWith(line.split("\\(")[1])) {
							stack.pop();
						} else {
							do {
								stack.pop();
								thread.terminateMethod();
								if (!stack.isEmpty()) line2 = stack.peek();
							} while (!stack.isEmpty() && !line2.split("\\(")[0].endsWith(line.split("\\(")[1]));
							if (!stack.isEmpty()) stack.pop();
						}
						thread = threads.get(threadNo);
						ObjectReference returnValue = new ObjectReference(returnData[2], returnData[1]);					
						thisObjectId = returnData[2];
						isCollectionType = false;
						String curLine = returnData[0];
						if(curLine.contains("Return call(List")
								|| curLine.contains("Return call(Vector")
								|| curLine.contains("Return call(Iterator")
								|| curLine.contains("Return call(ListIterator")
								|| curLine.contains("Return call(ArrayList")
								|| curLine.contains("Return call(Stack")
								|| curLine.contains("Return call(Hash")
								|| curLine.contains("Return call(Map")
								|| curLine.contains("Return call(Set")
								|| curLine.contains("Return call(Linked")
								|| curLine.contains("Return call(Collection")
								|| curLine.contains("Return call(Arrays")
								|| curLine.contains("Return call(Thread")) {
							isCollectionType = true;
						}
						thread.returnMethod(returnValue, thisObjectId, isCollectionType);
					}
				}
			} else if (line.startsWith("get")) {
				// フィールドアクセス
				accessData = line.split(":");
				if (accessData.length >= 9) {
					threadNo = getThreadNo(line);
//					threadNo = accessData[8].split(" ")[1];
					if (threadNo != null) {
						thread = threads.get(threadNo);
						timeStamp++;				// 仮のタイムスタンプ(実行順を保持するため)
						if (thread != null) thread.fieldAccess(accessData[5], accessData[6], accessData[3], accessData[4], accessData[1], accessData[2], 0, timeStamp);
					}
				}
			} else if (line.startsWith("set")) {
				// フィールド更新
				updateData = line.split(":");
				if (updateData.length >= 7) {
					threadNo = getThreadNo(line);
//					threadNo = updateData[6].split(" ")[1];
					if (threadNo != null) {
						thread = threads.get(threadNo);
						timeStamp++;				// 仮のタイムスタンプ(実行順を保持するため)
						if (thread != null) thread.fieldUpdate(updateData[3], updateData[4], updateData[1], updateData[2], 0, timeStamp);
					}
				}
			}
			prevLine = line;
		}
	}
	
	private String getThreadNo(String line) {
		int tidx = line.indexOf("ThreadNo ");
		if (tidx == -1) return null;
		String threadNo = line.substring(tidx + 9);
		try {
			Integer.parseInt(threadNo);
		} catch (NumberFormatException e) {
			for (int i = 1; i <= threadNo.length(); i++) {
				try {
					Integer.parseInt(threadNo.substring(0, i));
				} catch (NumberFormatException e2) {
					threadNo = threadNo.substring(0, i - 1);
					break;
				}
			}
		}
		return threadNo;
	}
	
	/**
	 * オンライン解析用シングルトンの取得
	 * @return オンライン解析用トレース
	 */
	public static Trace getInstance() {
		if (theTrace == null) {
			theTrace = new Trace();
		}
		return theTrace;
	}
	
	/**
	 * スレッドIDを指定してスレッドインスタンスを取得する(オンライン解析用)
	 * @param threadId
	 * @return スレッドインスタンス
	 */
	public static ThreadInstance getThreadInstance(String threadId) {
		return getInstance().threads.get(threadId);
	}
	
	/**
	 * 指定したスレッド上で現在実行中のメソッド実行を取得する(オンライン解析用)
	 * @param thread 対象スレッド
	 * @return thread 上で現在実行中のメソッド実行
	 */
	public static MethodExecution getCurrentMethodExecution(Thread thread) {
		ThreadInstance t = getInstance().threads.get(String.valueOf(thread.getId()));
		return t.getCurrentMethodExecution();
	}
	
	/**
	 * 指定したスレッド上で現在実行中のトレースポイントを取得する(オンライン解析用)
	 * @param thread 対象スレッド
	 * @return thread 上で現在実行中の実行文のトレースポイント
	 */
	public static TracePoint getCurrentTracePoint(Thread thread) {
		ThreadInstance t = getInstance().threads.get(String.valueOf(thread.getId()));
		return t.getCurrentTracePoint();
	}
	
	/**
	 * 全スレッドを取得する
	 * @return スレッドIDからスレッドインスタンスへのマップ
	 */
	public HashMap<String, ThreadInstance> getAllThreads() {
		return threads;
	}

	/**
	 * メソッド毎に全メソッド実行を全てのスレッドから取り出す
	 * @return メソッドシグニチャからメソッド実行のリストへのHashMap
	 */
	public HashMap<String, ArrayList<MethodExecution>> getAllMethodExecutions() {
		Iterator<String> threadsIterator = threads.keySet().iterator();
		final HashMap<String, ArrayList<MethodExecution>> results = new HashMap<>();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.traverseMethodExecutionsBackward(new IMethodExecutionVisitor() {
				@Override
				public boolean preVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean preVisitMethodExecution(MethodExecution methodExecution) {
					String signature = methodExecution.getSignature();
					ArrayList<MethodExecution> executions = results.get(signature);
					if (executions == null) {
						executions = new ArrayList<>();
						results.put(signature, executions);
					}
					executions.add(methodExecution);
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
		return results;		
	}
	
	/**
	 * 全メソッドのシグニチャを取得する
	 * @return 全メソッドシグニチャ
	 */
	public HashSet<String> getAllMethodSignatures() {
		final HashSet<String> signatures = new HashSet<String>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.traverseMethodExecutionsBackward(new IMethodExecutionVisitor() {
				@Override
				public boolean preVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean postVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean preVisitMethodExecution(MethodExecution methodExecution) {
					signatures.add(methodExecution.getSignature());
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}	
		return signatures;
	}
	
	/**
	 * メソッド名が methodSignature　に前方一致するメソッド実行を全てのスレッドから取り出す
	 * @param methodSignature 検索文字列
	 * @return 一致した全メソッド実行
	 */
	public ArrayList<MethodExecution> getMethodExecutions(final String methodSignature) {
		Iterator<String> threadsIterator = threads.keySet().iterator();
		final ArrayList<MethodExecution> results = new ArrayList<MethodExecution>();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.traverseMethodExecutionsBackward(new IMethodExecutionVisitor() {
				@Override
				public boolean preVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean preVisitMethodExecution(MethodExecution methodExecution) {
					if (methodExecution.getSignature().startsWith(methodSignature)) {
						results.add(methodExecution);
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
		return results;		
	}

	/**
	 * methodSignature に前方一致するメソッド名を持つメソッドの最後の実行
	 * @param methodSignature メソッド名(前方一致で検索する)
	 * @return 該当する最後のメソッド実行
	 */
	public MethodExecution getLastMethodExecution(final String methodSignature) {
		return traverseMethodEntriesInTraceBackward(new IMethodExecutionVisitor() {
			@Override
			public boolean preVisitThread(ThreadInstance thread) { return false; }
			@Override
			public boolean postVisitThread(ThreadInstance thread) { return false; }
			@Override
			public boolean preVisitMethodExecution(MethodExecution methodExecution) { return false; }
			@Override
			public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
				if (methodExecution.getSignature().startsWith(methodSignature)) return true;
				return false;
			}
		});
	}

	/**
	 * methodSignature に前方一致するメソッド名を持つメソッドの before 以前の最後の実行
	 * @param methodSignature メソッド名(前方一致で検索する)
	 * @param before　探索開始トレースポイント(これより以前を探索)
	 * @return　該当する最後のメソッド実行
	 */
	public MethodExecution getLastMethodExecution(final String methodSignature, TracePoint before) {
		return traverseMethodEntriesInTraceBackward(new IMethodExecutionVisitor() {
			@Override
			public boolean preVisitThread(ThreadInstance thread) { return false; }
			@Override
			public boolean postVisitThread(ThreadInstance thread) { return false; }
			@Override
			public boolean preVisitMethodExecution(MethodExecution methodExecution) { return false; }
			@Override
			public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
				if (methodExecution.getSignature().startsWith(methodSignature)) return true;
				return false;
			}
		}, before);
	}
	
	/**
	 * マーク内で実行が開始された全メソッドのシグニチャを取得する
	 * @param markStart マークの開始時刻
	 * @param markEnd マークの終了時刻
	 * @return 該当するメソッドシグニチャ
	 */
	public HashSet<String> getMarkedMethodSignatures(final long markStart, final long markEnd) {
		final HashSet<String> signatures = new HashSet<String>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.traverseMarkedMethodExecutions(new IMethodExecutionVisitor() {
				@Override
				public boolean preVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean postVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean preVisitMethodExecution(MethodExecution methodExecution) {
					signatures.add(methodExecution.getSignature());
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			}, markStart, markEnd);
		}	
		return signatures;
	}
	
	/**
	 * マーク内で実行が開始された全メソッド実行をメソッド毎に取得する
	 * @param markStart マークの開始時刻
	 * @param markEnd マークの終了時刻
	 * @return メソッドシグニチャから該当するメソッド実行のリストへのHashMap
	 */
	public HashMap<String, ArrayList<MethodExecution>> getMarkedMethodExecutions(final long markStart, final long markEnd) {
		final HashMap<String, ArrayList<MethodExecution>>allExecutions = new HashMap<>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.traverseMarkedMethodExecutions(new IMethodExecutionVisitor() {
				@Override
				public boolean preVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean postVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean preVisitMethodExecution(MethodExecution methodExecution) {
					ArrayList<MethodExecution> executions = allExecutions.get(methodExecution.getSignature());
					if (executions == null) {
						executions = new ArrayList<>();
						allExecutions.put(methodExecution.getSignature(), executions);
					}
					executions.add(methodExecution);
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			}, markStart, markEnd);
		}	
		return allExecutions;
	}
	
	/**
	 * マーク外で実行が開始された全メソッドのシグニチャを取得する
	 * @param markStart マークの開始時刻
	 * @param markEnd マークの終了時刻
	 * @return 該当するメソッドシグニチャ
	 */
	public HashSet<String> getUnmarkedMethodSignatures(long markStart, long markEnd) {
		HashSet<String> signatures = new HashSet<String>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.getUnmarkedMethodSignatures(signatures, markStart, markEnd);
		}	
		return signatures;
	}
	
	/**
	 * マーク外で実行が開始された全メソッド実行を取得する
	 * @param markStart マークの開始時刻
	 * @param markEnd マークの終了時刻
	 * @return メソッドシグニチャから該当するメソッド実行のリストへのHashMap
	 */
	public HashMap<String, ArrayList<MethodExecution>> getUnmarkedMethodExecutions(long markStart, long markEnd) {
		HashMap<String, ArrayList<MethodExecution>> executions = new HashMap<>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.getUnmarkedMethodExecutions(executions, markStart, markEnd);
		}	
		return executions;
	}
	
	
	protected TracePoint getLastMethodEntryInThread(ArrayList<MethodExecution> rootExecutions) {
		MethodExecution lastExecution = rootExecutions.remove(rootExecutions.size() - 1);
		return getLastMethodEntryInThread(rootExecutions, lastExecution.getExitOutPoint());
	}

	protected TracePoint getLastMethodEntryInThread(ArrayList<MethodExecution> rootExecutions, TracePoint start) {
		return getLastMethodEntryInThread(rootExecutions, start, -1L);
	}
	
	/**
	 * 
	 * @param rootExecutions
	 * @param start
	 * @param before
	 * @return
	 */
	protected TracePoint getLastMethodEntryInThread(ArrayList<MethodExecution> rootExecutions, TracePoint start, final long before) {
		final TracePoint cp[] = new TracePoint[1];
		cp[0] = start;
		for (;;) {
			if (!cp[0].isStepBackOut() && traverseMethodExecutionsInCallTreeBackward (
					new IMethodExecutionVisitor() {
						@Override
						public boolean preVisitThread(ThreadInstance thread) { return false; }
						@Override
						public boolean preVisitMethodExecution(MethodExecution methodExecution) { return false; }
						@Override
						public boolean postVisitThread(ThreadInstance thread) { return false; }
						@Override
						public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
							if (methodExecution.getEntryTime() < before || before == -1L) {
								cp[0] = methodExecution.getEntryPoint();
								return true;
							}
							return false;
						}
					}, cp[0])) {
				return cp[0];
			}
			if (rootExecutions.size() == 0) break;
			MethodExecution lastExecution = rootExecutions.remove(rootExecutions.size() - 1);
			cp[0] = lastExecution.getExitOutPoint();
		}
		return null;
	}
	
	public boolean getLastStatementInThread(String threadId, final TracePoint[] start, final IStatementVisitor visitor) {
		return getLastStatementInThread((ArrayList<MethodExecution>) threads.get(threadId).getRoot().clone(), start, start[0].getStatement().getTimeStamp(), visitor);
	}
	
	protected boolean getLastStatementInThread(ArrayList<MethodExecution> rootExecutions, final TracePoint[] start, final long before, final IStatementVisitor visitor) {
		final boolean[] bArrived = new boolean[] {
			false
		};
		for (;;) {
			if (start[0].isValid() && traverseStatementsInCallTreeBackward(
					new IStatementVisitor() {
						@Override
						public boolean preVisitStatement(Statement statement) {
							if (statement instanceof MethodInvocation) {
								MethodExecution methodExecution = ((MethodInvocation) statement).getCalledMethodExecution();
								if ((!methodExecution.isTerminated() && methodExecution.getExitTime() < before) || before == -1L) {
									if (visitor.preVisitStatement(statement)) return true;
									bArrived[0] = true;
									return true;
								}
							} else {
								if (statement.getTimeStamp() < before || before == -1L) {
									if (visitor.preVisitStatement(statement)) return true;
									bArrived[0] = true;
									return true;
								}
							}
							return visitor.preVisitStatement(statement); 
						}
						@Override
						public boolean postVisitStatement(Statement statement) {
							return visitor.postVisitStatement(statement);
						}
					}, start[0])) {
				return !bArrived[0];
			}
			if (rootExecutions.size() == 0) break;
			MethodExecution lastExecution = rootExecutions.remove(rootExecutions.size() - 1);
			start[0] = lastExecution.getExitPoint();
		}
		start[0] = null;
		return false;
	}
	
	public TracePoint getLastTracePoint() {
		TracePoint tp = null;
		for (ThreadInstance thread: threads.values()) {
			if (tp == null || thread.getLastTracePoint().getStatement().getTimeStamp() > tp.getStatement().getTimeStamp()) {
				tp = thread.getLastTracePoint();
			}
		}
		return tp;
	}

	public TracePoint getCreationTracePoint(final ObjectReference newObjectId, TracePoint before) {
		if (before != null) before = before.duplicate();
		before = traverseStatementsInTraceBackward(
				new IStatementVisitor() {
					@Override
					public boolean preVisitStatement(Statement statement) {
						if (statement instanceof MethodInvocation) {
							MethodInvocation mi = (MethodInvocation)statement;
							if (mi.getCalledMethodExecution().isConstructor() 
									&& mi.getCalledMethodExecution().getReturnValue().equals(newObjectId)) {
								return true;
							}
						}
						return false;
					}
					@Override
					public boolean postVisitStatement(Statement statement) { return false; }
				}, before);
		if (before != null) {
			return before;
		}
		return null;
	}

	public TracePoint getFieldUpdateTracePoint(final Reference ref, TracePoint before) {
		if (before != null) before = before.duplicate();
		final String srcType = ref.getSrcClassName();
		final String dstType = ref.getDstClassName();
		final String srcObjId = ref.getSrcObjectId();
		final String dstObjId = ref.getDstObjectId();
		
		before = traverseStatementsInTraceBackward(new IStatementVisitor() {
			@Override
			public boolean preVisitStatement(Statement statement) {
				if (statement instanceof FieldUpdate) {
					FieldUpdate fu = (FieldUpdate)statement;
					if (fu.getContainerObjId().equals(srcObjId) 
							&& fu.getValueObjId().equals(dstObjId)) {
						// オブジェクトIDが互いに一致した場合
						return true;
					} else if ((srcObjId == null || isNull(srcObjId)) && fu.getContainerClassName().equals(srcType)) {
						if ((dstObjId == null || isNull(dstObjId)) && fu.getValueClassName().equals(dstType)) {
							// ref にオブジェクトIDを指定していなかった場合
							ref.setSrcObjectId(fu.getContainerObjId());
							ref.setDstObjectId(fu.getValueObjId());
							return true;
						} else if (fu.getValueObjId().equals(dstObjId)) {
							// クラス変数への代入の場合
							ref.setSrcObjectId(srcObjId);
							ref.setDstClassName(dstType);
							return true;
						}
					}
				}
				return false;
			}
			@Override
			public boolean postVisitStatement(Statement statement) { return false; }
		}, before);
		if (before != null) {
			return before;			
		}
		return null;
	}

	public TracePoint getCollectionAddTracePoint(final Reference ref, TracePoint before) {
		final TracePoint[] result = new TracePoint[1];
		if (traverseMethodEntriesInTraceBackward(new IMethodExecutionVisitor() {
			@Override
			public boolean preVisitThread(ThreadInstance thread) {
				return false;
			}
			@Override
			public boolean postVisitThread(ThreadInstance thread) {
				return false;
			}			
			@Override
			public boolean preVisitMethodExecution(MethodExecution methodExecution) {
				return false;
			}
			@Override
			public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
				String srcType = ref.getSrcClassName();
				String dstType = ref.getDstClassName();
				String srcObjId = ref.getSrcObjectId();
				String dstObjId = ref.getDstObjectId();
				if (methodExecution.isCollectionType() && isCollectionAdd(methodExecution.getSignature())) {
					if (dstObjId != null && methodExecution.getThisObjId().equals(srcObjId)) {
						ArrayList<ObjectReference> args = methodExecution.getArguments();
						for (int i = 0; i < args.size(); i++) {
							ObjectReference arg = args.get(i);
							if (arg.getId().equals(dstObjId)) {
								ref.setSrcClassName(methodExecution.getThisClassName());
								ref.setDstClassName(arg.getActualType());
								result[0] = methodExecution.getCallerTracePoint();
								return true;								
							}
						}
					} else if (dstObjId == null && methodExecution.getThisClassName().equals(srcType)) {
						ArrayList<ObjectReference> args = methodExecution.getArguments();						
						for (int i = 0; i < args.size(); i++) {
							ObjectReference arg = args.get(i);
							if (arg.getActualType().equals(dstType)) {
								ref.setSrcObjectId(methodExecution.getThisObjId());
								ref.setDstObjectId(arg.getId());
								result[0] = methodExecution.getCallerTracePoint();
								return true;								
							}
						}
					}
				}
				return false;
			}
		}, before) != null) {
			return result[0];
		}
		return null;
	}

	public TracePoint getArraySetTracePoint(final Reference ref, TracePoint before) {
		final TracePoint start = (before == null) ? null : before.duplicate();
		before = traverseStatementsInTraceBackward(new IStatementVisitor() {
				@Override
				public boolean preVisitStatement(Statement statement) {
					if (statement instanceof FieldAccess) {
						if (isArraySet(ref, start)) {
							return true;
						}
					}
					return false;
				}
				@Override
				public boolean postVisitStatement(Statement statement) { return false; }
			}, start);
		if (before != null) {
			return before;
		}
		return null;
	}
	
	private boolean isCollectionAdd(String methodSignature) {
		return (methodSignature.contains("add(") || methodSignature.contains("set(") || methodSignature.contains("put(") || methodSignature.contains("push(") || methodSignature.contains("addElement("));
	}
	
	private boolean isArraySet(Reference ref, TracePoint fieldAccessPoint) {
		FieldAccess fieldAccess = (FieldAccess)fieldAccessPoint.getStatement();
		String srcObjId = ref.getSrcObjectId();
		String dstObjId = ref.getDstObjectId();
		if (fieldAccess.getValueClassName().startsWith("[L") 
				&& fieldAccess.getValueObjId().equals(srcObjId)) {
			// srcId は配列
			// メソッド実行開始から fieldAccessPoint までの間のメソッド実行中で dstId が出現したか?
			TracePoint p = fieldAccessPoint.duplicate();
			while (p.stepBackOver()) {
				Statement statement = p.getStatement();
				if (statement instanceof MethodInvocation) {
					MethodExecution calledMethod = ((MethodInvocation)statement).getCalledMethodExecution();
					if (calledMethod.getReturnValue().getId().equals(dstObjId)) {
						// dstId は戻り値として出現
						ref.setSrcClassName(fieldAccess.getValueClassName());
						ref.setDstClassName(calledMethod.getReturnValue().getActualType());
						return true;
					} else if (dstObjId == null || isNull(dstObjId) && calledMethod.getReturnValue().getActualType().equals(ref.getDstClassName())) {
						// dstClassName は戻り値の型として出現
						ref.setSrcObjectId(fieldAccess.getValueObjId());
						ref.setDstObjectId(calledMethod.getReturnValue().getId());
						return true;								
					}				
				}
				if (EAGER_DETECTION_OF_ARRAY_SET) {
					if (statement instanceof FieldAccess) {
						if (((FieldAccess)statement).getContainerObjId().equals(dstObjId)) {
							// dstId はフィールドに出現
							ref.setSrcClassName(fieldAccess.getValueClassName());
							ref.setDstClassName(((FieldAccess)statement).getContainerClassName());
							return true;
						} else if (dstObjId == null || isNull(dstObjId) && ((FieldAccess)statement).getContainerClassName().equals(ref.getDstClassName())) {
							// dstClassName はフィールドの型として出現					
							ref.setSrcObjectId(fieldAccess.getValueObjId());
							ref.setDstObjectId(((FieldAccess)statement).getContainerObjId());
							return true;
						}
					}
				}
			}
			ArrayList<ObjectReference> args = fieldAccessPoint.getMethodExecution().getArguments();
			int argindex = args.indexOf(new ObjectReference(dstObjId));
			if (argindex != -1) {
				// dstId は引数に出現
				ref.setSrcClassName(fieldAccess.getValueClassName());
				ref.setDstClassName(args.get(argindex).getActualType());
				return true;
			} else if (dstObjId == null || isNull(dstObjId)) {
				for (int j = 0; j < args.size(); j++) {
					if (args.get(j).getActualType().equals(ref.getDstClassName())) {
						// dstClassName は引数の型に出現							
						ref.setSrcObjectId(fieldAccess.getValueObjId());
						ref.setDstObjectId(args.get(j).getId());
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * トレース中の全メソッド実行の開始地点を逆向きに探索する
	 * @param visitor メソッド実行のビジター(postVisitMethodExecution()しか呼び返さないので注意)
	 * @return 中断したメソッド実行
	 */
	public MethodExecution traverseMethodEntriesInTraceBackward(IMethodExecutionVisitor visitor) {
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadLastPoints = new HashMap<String, TracePoint>();
		// 各スレッドにおいて一番最後に開始したメソッド実行を探す
		long traceLastTime = 0;
		String traceLastThread = null;
		long traceLastTime2 = 0;
		String traceLastThread2 = null;
		for (String threadId: threads.keySet()) {
			ThreadInstance thread = threads.get(threadId);
			ArrayList<MethodExecution> rootExecutions = (ArrayList<MethodExecution>)thread.getRoot().clone();
			threadRoots.put(threadId, rootExecutions);
			TracePoint threadLastTp = getLastMethodEntryInThread(rootExecutions);
			threadLastPoints.put(threadId, threadLastTp);
			if (threadLastTp != null) {
				long threadLastTime = threadLastTp.getMethodExecution().getEntryTime();
				if (traceLastTime < threadLastTime) {
					traceLastTime2 = traceLastTime;
					traceLastThread2 = traceLastThread;
					traceLastTime = threadLastTime;
					traceLastThread = threadId;
				} else if (traceLastTime2 < threadLastTime) {
					traceLastTime2 = threadLastTime;
					traceLastThread2 = threadId;
				}
			}
		}
		return traverseMethodEntriesInTraceBackwardSub(visitor, threadRoots, threadLastPoints, traceLastThread, traceLastThread2, traceLastTime2);
	}
	
	/**
	 * 指定した実行時点以前に実行が開始されたメソッド実行の開始時点を逆向きに探索する
	 * @param visitor メソッド実行のビジター(postVisitMethodExecution()しか呼び返さないので注意)
	 * @param before 探索開始時点
	 * @return 中断したメソッド実行
	 */
	public MethodExecution traverseMethodEntriesInTraceBackward(IMethodExecutionVisitor visitor, TracePoint before) {
		if (before == null) {
			return traverseMethodEntriesInTraceBackward(visitor);
		}		
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadLastPoints = new HashMap<String, TracePoint>();
		String traceLastThread = null;
		long traceLastTime2 = 0;
		String traceLastThread2 = null;
		Statement st = before.getStatement();
		if (st == null) {
			st = before.getMethodExecution().getCallerTracePoint().getStatement();
		}
		ThreadInstance thread = threads.get(st.getThreadNo());
		ArrayList<MethodExecution> rootExecutions = (ArrayList<MethodExecution>)thread.getRoot().clone();		
		for (int n = rootExecutions.size() - 1; n >= 0; n--) {
			MethodExecution root = rootExecutions.get(n);
			if (root.getEntryTime() > before.getMethodExecution().getEntryTime()) {
				rootExecutions.remove(n);
			} else {
				break;
			}
		}
		if (rootExecutions.size() > 0) {
			rootExecutions.remove(rootExecutions.size() - 1);
		}
		before = getLastMethodEntryInThread(rootExecutions, before);
		for (String threadId: threads.keySet()) {
			ThreadInstance t = threads.get(threadId);
			if (t == thread) {
				threadRoots.put(threadId, rootExecutions);
				traceLastThread = threadId;
				threadLastPoints.put(threadId, before);
			} else {
				ArrayList<MethodExecution> rootExes = (ArrayList<MethodExecution>)t.getRoot().clone();
				threadRoots.put(threadId, rootExes);
				MethodExecution threadLastExecution = rootExes.remove(rootExes.size() - 1);
				TracePoint threadBeforeTp = getLastMethodEntryInThread(rootExes, threadLastExecution.getExitOutPoint(), before.getMethodExecution().getEntryTime());
				threadLastPoints.put(threadId, threadBeforeTp);
				if (threadBeforeTp != null) {
					long threadLastTime = threadBeforeTp.getMethodExecution().getEntryTime();
					if (traceLastTime2 < threadLastTime) {
						traceLastTime2 = threadLastTime;
						traceLastThread2 = threadId;
					}
				}
			}
		}
		return traverseMethodEntriesInTraceBackwardSub(visitor, threadRoots, threadLastPoints, traceLastThread, traceLastThread2, traceLastTime2);		
	}
	
	private MethodExecution traverseMethodEntriesInTraceBackwardSub(
			final IMethodExecutionVisitor visitor, 
			HashMap<String, ArrayList<MethodExecution>> threadRoots, HashMap<String, TracePoint> threadLastPoints, 
			String traceLastThread, String traceLastThread2, long traceLastTime2) {
		// 全スレッドの同期をとりながら逆向きにメソッド実行を探索する
		for (;;) {
			// 探索対象のスレッド内の逆向き探索
			TracePoint threadLastTp = threadLastPoints.get(traceLastThread);
			MethodExecution threadLastExecution = threadLastTp.getMethodExecution();
			do {
				threadLastTp.stepBackOver();
				// そのスレッドの次のメソッド実行開始時点まで探索する
				threadLastTp = getLastMethodEntryInThread(threadRoots.get(traceLastThread), threadLastTp);
				if (threadLastTp == null) break;
				if (visitor.postVisitMethodExecution(threadLastExecution, threadLastExecution.getChildren())) {
					// 該当するメソッド実行を見つけた
					return threadLastExecution;
				}
				threadLastExecution = threadLastTp.getMethodExecution();
			} while (threadLastExecution.getEntryTime() > traceLastTime2);
			threadLastPoints.put(traceLastThread, threadLastTp);
			traceLastThread = traceLastThread2;
			// 次の次に探索すべきスレッド(未探索の領域が一番最後まで残っているスレッド)を決定する
			traceLastTime2 = 0;
			traceLastThread2 = null;
			boolean continueTraverse = false;
			for (String threadId: threadLastPoints.keySet()) {
				if (!threadId.equals(traceLastThread)) {
					TracePoint lastTp = threadLastPoints.get(threadId);
					if (lastTp != null) {
						continueTraverse = true;
						long threadLastTime = lastTp.getMethodExecution().getEntryTime();
						if (traceLastTime2 < threadLastTime) {
							traceLastTime2 = threadLastTime;
							traceLastThread2 = threadId;
						}
					}
				}
			}
			if (!continueTraverse && threadLastPoints.get(traceLastThread) == null) break;
		}
		return null;
	}
	
	/**
	 * 呼び出し時の時刻が markStart から markEnd の間にある全スレッド中の全メソッド実行を探索する
	 * @param visitor　ビジター
	 * @param markStart 開始時刻
	 * @param markEnd 終了時刻
	 */
	public void traverseMarkedMethodExecutions(IMethodExecutionVisitor visitor, long markStart, long markEnd) {
		for (String threadId: threads.keySet()) {
			ThreadInstance thread = threads.get(threadId);
			thread.traverseMarkedMethodExecutions(visitor, markStart, markEnd);
		}		
	}
	
	/**
	 * トレース内の全スレッドを同期させならが全実行文を順方向に探索する
	 * 
	 * @param visitor 実行文のビジター
	 * @return 中断したトレースポイント
	 */
	public TracePoint traverseStatementsInTrace(IStatementVisitor visitor) {
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadCurPoints = new HashMap<String, TracePoint>();
		// 各スレッドにおいて一番最初に開始したメソッド実行を探す
		long traceCurTime = -1;
		String traceCurThread = null;
		long traceCurTime2 = -1;
		String traceCurThread2 = null;
		for (String threadId: threads.keySet()) {
			ThreadInstance thread = threads.get(threadId);
			ArrayList<MethodExecution> roots = (ArrayList<MethodExecution>)thread.getRoot().clone();
			threadRoots.put(threadId, roots);
			TracePoint threadCurTp;
			do {
				MethodExecution threadCurExecution = roots.remove(0);
				threadCurTp = threadCurExecution.getEntryPoint();
			} while (!threadCurTp.isValid() && roots.size() > 0);
			if (threadCurTp.isValid()) {
				threadCurPoints.put(threadId, threadCurTp);
				long methodEntry = threadCurTp.getMethodExecution().getEntryTime();
				if (traceCurTime == -1 || traceCurTime > methodEntry) {
					traceCurTime2 = traceCurTime;
					traceCurThread2 = traceCurThread;
					traceCurTime = methodEntry;
					traceCurThread = threadId;
				} else if (traceCurTime2 == -1 || traceCurTime2 > methodEntry) {
					traceCurTime2 = methodEntry;
					traceCurThread2 = threadId;
				}
			} else {
				threadCurPoints.put(threadId, null);				
			}
		}
		return traverseStatementsInTraceSub(visitor, threadRoots, threadCurPoints, traceCurThread, traceCurThread2, traceCurTime2);
	}
	
	private TracePoint traverseStatementsInTraceSub(IStatementVisitor visitor,
			HashMap<String, ArrayList<MethodExecution>> threadRoots,
			HashMap<String, TracePoint> threadCurPoints, 
			String curThreadId, String nextThreadId, long nextThreadTime) {
		// 全スレッドの同期をとりながら順方向に実行文を探索する
		for (;;) {
			// 探索対象のスレッド内の順方向探索
			TracePoint curTp = threadCurPoints.get(curThreadId);
			while (curTp != null 
					&& (curTp.getStatement().getTimeStamp() <= nextThreadTime || nextThreadTime == -1)) {
				Statement statement = curTp.getStatement();
				if (!(visitor instanceof AbstractTracePointVisitor)) {
					if (visitor.preVisitStatement(statement)) return curTp;
					if (!(statement instanceof MethodInvocation)) {
						if (visitor.postVisitStatement(statement)) return curTp;
					}
				} else {
					// トレースポイントの情報も必要な場合
					if (((AbstractTracePointVisitor) visitor).preVisitStatement(statement, curTp)) return curTp;
					if (!(statement instanceof MethodInvocation)) {
						if (((AbstractTracePointVisitor) visitor).postVisitStatement(statement, curTp)) return curTp;
					}
				}
				curTp.stepNoReturn();		// 復帰せずに呼び出し木を潜っていく
				if (!curTp.isValid()) {
					// 復帰しないとこれ以上探索できない
					while (!curTp.stepOver()) {		// 今度は復帰はするが潜らずに探索
						if (curTp.isValid()) {
							// 呼び出し先探索前に一度訪問済みのメソッド呼び出し行を、探索後もう一度訪問する
							if (!(visitor instanceof AbstractTracePointVisitor)) {
								if (visitor.postVisitStatement(curTp.getStatement())) return curTp;
							} else {
								// トレースポイントの情報も必要な場合
								if (((AbstractTracePointVisitor) visitor).postVisitStatement(curTp.getStatement(), curTp)) return curTp;
							}
						} else {
							// 呼び出し木の開始時点まで探索し終えた場合
							ArrayList<MethodExecution> roots = threadRoots.get(curThreadId);
							while (!curTp.isValid() && roots.size() > 0) {
								// 次の呼び出し木があればそれを最初から探索
								MethodExecution firstExecution = roots.remove(0);
								curTp = firstExecution.getEntryPoint();							
							}
							if (curTp.isValid()) {
								// 次の呼び出し木があればそれを最初から探索
								threadCurPoints.put(curThreadId, curTp);
							} else {
								// そのスレッドの探索がすべて終了した場合
								threadCurPoints.put(curThreadId, null);
								curTp = null;
							}						
							break;
						}
					}
				}
			}
			curThreadId = nextThreadId;
			if (curThreadId == null) break;
			// 次の次に探索すべきスレッド(未探索の領域が一番最初に始まるスレッド)を決定する
			nextThreadTime = -1;
			nextThreadId = null;
			boolean continueTraverse = false;
			for (String threadId: threadCurPoints.keySet()) {
				if (!threadId.equals(curThreadId)) {
					TracePoint threadTp = threadCurPoints.get(threadId);
					if (threadTp != null) {
						continueTraverse = true;
						long threadTime = threadTp.getStatement().getTimeStamp();
						if (threadTime > 0 && (nextThreadTime == -1 || nextThreadTime > threadTime)) {
							nextThreadTime = threadTime;
							nextThreadId = threadId;
						}
					}
				}
			}
			if (!continueTraverse && threadCurPoints.get(curThreadId) == null) break;
		}
		return null;
	}
	
	/**
	 * トレース内の全スレッドを同期させならが全実行文を逆方向に探索する
	 * 
	 * @param visitor 実行文のビジター
	 * @return 中断したトレースポイント
	 */
	public TracePoint traverseStatementsInTraceBackward(IStatementVisitor visitor) {
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadLastPoints = new HashMap<String, TracePoint>();
		// 各スレッドにおいて一番最後に開始したメソッド実行を探す
		long traceLastTime = 0;
		String traceLastThread = null;
		long traceLastTime2 = 0;
		String traceLastThread2 = null;
		for (String threadId: threads.keySet()) {
			ThreadInstance thread = threads.get(threadId);
			ArrayList<MethodExecution> root = (ArrayList<MethodExecution>)thread.getRoot().clone();
			threadRoots.put(threadId, root);
			TracePoint threadLastTp;
			do {
				MethodExecution threadLastExecution = root.remove(root.size() - 1);
				threadLastTp = threadLastExecution.getExitPoint();
			} while (!threadLastTp.isValid() && root.size() > 0);
			if (threadLastTp.isValid()) {
				threadLastPoints.put(threadId, threadLastTp);
				if (traverseStatamentsInCallTreeBackwardNoReturn(visitor, threadLastTp)) return threadLastTp;
				long methodEntry = threadLastTp.getMethodExecution().getEntryTime();
				if (traceLastTime < methodEntry) {
					traceLastTime2 = traceLastTime;
					traceLastThread2 = traceLastThread;
					traceLastTime = methodEntry;
					traceLastThread = threadId;
				} else if (traceLastTime2 < methodEntry) {
					traceLastTime2 = methodEntry;
					traceLastThread2 = threadId;
				}
			} else {
				threadLastPoints.put(threadId, null);				
			}
		}
		return traverseStatementsInTraceBackwardSub(visitor, threadRoots, threadLastPoints, traceLastThread, traceLastThread2, traceLastTime2);
	}
	
	/**
	 * 
	 * @param visitor
	 * @param before
	 * @return
	 */
	public TracePoint traverseStatementsInTraceBackward(IStatementVisitor visitor, TracePoint before) {
		if (before == null) {
			return traverseStatementsInTraceBackward(visitor);
		}
		if (traverseStatamentsInCallTreeBackwardNoReturn(visitor, before)) return before;
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadLastPoints = new HashMap<String, TracePoint>();
		String traceLastThread = null;
		long traceLastTime2 = 0;
		String traceLastThread2 = null;
		Statement st = before.getStatement();
		if (st == null) {
			st = before.getMethodExecution().getCallerTracePoint().getStatement();
		}
		ThreadInstance thread = threads.get(st.getThreadNo());
		for (String threadId: threads.keySet()) {
			ThreadInstance t = threads.get(threadId);
			ArrayList<MethodExecution> rootExecutions = (ArrayList<MethodExecution>)t.getRoot().clone();
			threadRoots.put(threadId, rootExecutions);
			if (t == thread) {
				traceLastThread = threadId;
				threadLastPoints.put(threadId, before);
				for (int n = rootExecutions.size() - 1; n >= 0; n--) {
					MethodExecution root = rootExecutions.get(n);
					if (root.getEntryTime() > before.getMethodExecution().getEntryTime()) {
						rootExecutions.remove(n);
					} else {
						break;
					}
				}
				if (rootExecutions.size() > 0) {
					rootExecutions.remove(rootExecutions.size() - 1);
				}
			} else {
				MethodExecution threadLastExecution = rootExecutions.remove(rootExecutions.size() - 1);
				TracePoint threadBeforeTp = getLastMethodEntryInThread(rootExecutions, threadLastExecution.getExitOutPoint(), before.getMethodExecution().getEntryTime());
				threadLastPoints.put(threadId, threadBeforeTp);
				if (threadBeforeTp != null) {
					long threadLastTime = threadBeforeTp.getMethodExecution().getEntryTime();
					if (traceLastTime2 < threadLastTime) {
						traceLastTime2 = threadLastTime;
						traceLastThread2 = threadId;
					}
				}
			}
		}
		return traverseStatementsInTraceBackwardSub(visitor, threadRoots, threadLastPoints, traceLastThread, traceLastThread2, traceLastTime2);
	}

	private TracePoint traverseStatementsInTraceBackwardSub(IStatementVisitor visitor,
			HashMap<String, ArrayList<MethodExecution>> threadRoots,
			HashMap<String, TracePoint> threadLastPoints, 
			String traceLastThread, String traceLastThread2, long traceLastTime2) {
		// 全スレッドの同期をとりながら逆向きに実行文を探索する
		for (;;) {
			// 探索対象のスレッド内の逆向き探索
			TracePoint lastTp = threadLastPoints.get(traceLastThread);
			do {
				if (lastTp.stepBackOver()) {
					// そのスレッドの次のメソッド実行開始時点まで探索する
					if (traverseStatamentsInCallTreeBackwardNoReturn(visitor, lastTp)) return lastTp;					
				} else {
					// 呼び出し元に戻った場合
					if (lastTp.isValid()) {
						if (visitor.postVisitStatement(lastTp.getStatement())) return lastTp;
					} else {
						// 呼び出し木の開始時点まで探索し終えた場合
						ArrayList<MethodExecution> root = threadRoots.get(traceLastThread);
						while (!lastTp.isValid() && root.size() > 0) {
							// 次の呼び出し木があればそれを最後から探索
							MethodExecution lastExecution = root.remove(root.size() - 1);
							lastTp = lastExecution.getExitPoint();							
						}
						if (lastTp.isValid()) {
							// 次の呼び出し木があればそれを最後から探索
							threadLastPoints.put(traceLastThread, lastTp);
							if (traverseStatamentsInCallTreeBackwardNoReturn(visitor, lastTp)) return lastTp;
						} else {
							// そのスレッドの探索がすべて終了した場合
							threadLastPoints.put(traceLastThread, null);
							break;
						}
					}
				}
			} while (lastTp.getMethodExecution().getEntryTime() >= traceLastTime2);
			traceLastThread = traceLastThread2;
			// 次の次に探索すべきスレッド(未探索の領域が一番最後まで残っているスレッド)を決定する
			traceLastTime2 = 0;
			traceLastThread2 = null;
			boolean continueTraverse = false;
			for (String threadId: threadLastPoints.keySet()) {
				if (!threadId.equals(traceLastThread)) {
					TracePoint threadLastTp = threadLastPoints.get(threadId);
					if (threadLastTp != null) {
						continueTraverse = true;
						long threadLastTime = threadLastTp.getMethodExecution().getEntryTime();
						if (traceLastTime2 < threadLastTime) {
							traceLastTime2 = threadLastTime;
							traceLastThread2 = threadId;
						}
					}
				}
			}
			if (!continueTraverse && threadLastPoints.get(traceLastThread) == null) break;
		}
		return null;
	}
		
	/**
	 * before で指定したトレースポイント以前の同一呼び出し木内の全実行文を逆向きに探索する(ただし、visitor が true を返すまで)
	 * @param visitor ビジター
	 * @param before 探索の開始点(探索対象スレッドも指定している)
	 * @return true -- 探索を中断した, false -- 最後まで探索した
	 */
	public boolean traverseStatementsInCallTreeBackward(IStatementVisitor visitor, TracePoint before) {
		for (;;) {
			if (traverseStatamentsInCallTreeBackwardNoReturn(visitor, before)) return true;
			while (!before.stepBackOver()) {
				if (!before.isValid()) break;
				if (visitor.postVisitStatement(before.getStatement())) return true;
			}
			if (!before.isValid()) break;
		}
		return false;
	}

	/**
	 * before以前の呼び出し木を呼び出し先からの復帰をせずに行けるところまで逆向きに探索する
	 * 
	 * @param visitor 実行文のビジター
	 * @param before 探索開始実行時点
	 * @return true -- 探索を中断した, false -- 復帰をしない限りこれ以上進めない 
	 */
	private boolean traverseStatamentsInCallTreeBackwardNoReturn(IStatementVisitor visitor, TracePoint before) {
		for (;;) {
			Statement statement = before.getStatement();
			if (statement instanceof MethodInvocation) {
				// メソッド呼び出し文の場合は、呼び出しの前後で preVisit と postVisit を別々に実行する
				if (visitor.preVisitStatement(statement)) return true;
				before.stepBackNoReturn();
				if (!before.isValid()) {
					// 呼び出し先のメソッドで実行文が記録されていない場合
					before.stepBackOver();
					if (visitor.postVisitStatement(statement)) return true;
					if (before.isMethodEntry()) return false;
					before.stepBackOver();
				}
			} else {
				if (visitor.preVisitStatement(statement)) return true;
				if (visitor.postVisitStatement(statement)) return true;
				if (before.isMethodEntry()) return false;
				before.stepBackNoReturn();
			}
		}
	}
	
	/**
	 * before で指定したトレースポイント以前の同一スレッド内の全メソッド実行を呼び出し木の中で逆向きに探索する(ただし、visitor が true を返すまで)
	 * @param visitor ビジター
	 * @param before 探索の開始点(探索対象スレッドも指定している)
	 * @return true -- 探索を中断した, false -- 最後まで探索した
	 */
	public boolean traverseMethodExecutionsInCallTreeBackward(IMethodExecutionVisitor visitor, TracePoint before) {
		ArrayList<MethodExecution> prevMethodExecutions = before.getPreviouslyCalledMethods();
		for (int i = prevMethodExecutions.size() - 1; i >= 0; i--) {
			MethodExecution child = prevMethodExecutions.get(i);
			if (child.traverseMethodExecutionsBackward(visitor)) return true;
		}
		MethodExecution methodExecution = before.getMethodExecution();
		if (visitor.postVisitMethodExecution(methodExecution, null)) return true;
		TracePoint caller = methodExecution.getCallerTracePoint();
		if (caller != null) {
			if (traverseMethodExecutionsInCallTreeBackward(visitor, caller)) return true;
		}
		return false;
	}

	public static String getDeclaringType(String methodSignature, boolean isConstructor) {
		if (methodSignature == null) return null;
		if (isConstructor) {
			String[] fragments = methodSignature.split("\\(");
			return fragments[0].substring(fragments[0].lastIndexOf(' ') + 1);			
		}
		String[] fragments = methodSignature.split("\\(");
		return fragments[0].substring(fragments[0].lastIndexOf(' ') + 1, fragments[0].lastIndexOf('.'));		
	}		
	
	public static String getMethodName(String methodSignature) {
		String[] fragments = methodSignature.split("\\(");
		String[] fragments2 = fragments[0].split("\\.");
		return fragments2[fragments2.length - 1];
	}
	
	public static String getReturnType(String methodSignature) {
		String[] fragments = methodSignature.split(" ");
		for (int i = 0; i < fragments.length; i++) {
			if (!fragments[i].equals("public") && !fragments[i].equals("private") && !fragments[i].equals("protected")
					&& !fragments[i].equals("abstract") && !fragments[i].equals("final") && !fragments[i].equals("static")
					&& !fragments[i].equals("synchronized") && !fragments[i].equals("native")) {
				return fragments[i];
			}
		}
		return "";
	}
	
	public static boolean isNull(String objectId) {
		return objectId.equals("0");
	}
	
	public static String getNull() {
		return "0";
	}

	public static boolean isPrimitive(String typeName) {
		if (typeName.equals("int") 
				|| typeName.equals("boolean") 
				|| typeName.equals("long") 
				|| typeName.equals("double") 
				|| typeName.equals("float") 
				|| typeName.equals("char") 
				|| typeName.equals("byte") 
				|| typeName.equals("java.lang.Integer") 
				|| typeName.equals("java.lang.Boolean") 
				|| typeName.equals("java.lang.Long") 
				|| typeName.equals("java.lang.Double") 
				|| typeName.equals("java.lang.Float") 
				|| typeName.equals("java.lang.Character") 
				|| typeName.equals("java.lang.Byte")) return true;
		return false;
	}
}
