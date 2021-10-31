package org.ntlab.trace;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

public class TraceJSON extends Trace {
	private HashMap<String, ClassInfo> classes = new HashMap<>();
	
	private TraceJSON() {
		
	}

	/**
	 * 指定したJSONのトレースファイルを解読して Trace オブジェクトを生成する
	 * @param file トレースファイル
	 */
	public TraceJSON(BufferedReader file) {
		try {
			readJSON(file);
			file.close();		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 指定したJSONのトレースファイルを解読して Trace オブジェクトを生成する
	 * @param traceFile トレースファイルのパス
	 */
	public TraceJSON(String traceFile) {
		BufferedReader file;
		try {
			file = new BufferedReader(new FileReader(traceFile));
			readJSON(file);
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readJSON(BufferedReader file) throws IOException {
		// トレースファイル読み込み
		String line = null;
		String[] type;
		String[] classNameData;
		String[] pathData;
		String[] signature;
		String[] receiver;
		String[] arguments;
		String[] lineData;
		String[] threadId;
		String[] thisObj;
		String[] containerObj;
		String[] valueObj;
		String[] returnValue;
		String[] arrayObj;
		String[] thisData;
		String[] containerData;
		String[] valueData;
		String[] returnData;
		String[] fieldData;
		String[] arrayData;
		String[] blockIdData;
		String[] incomingsData;
		String[] dimensionData;
		String[] indexData;
		String className;
		String classPath;
		String loaderPath;
		String time;
		String thisObjectId;
		String thisClassName;
		String containerObjectId;
		String containerClassName;
		String valueObjectId;
		String valueClassName;
		String returnClassName;
		String returnObjectId;
		String arrayObjectId;
		String arrayClassName;
		String shortSignature;
		boolean isConstractor = false;
		boolean isCollectionType = false;
		boolean isStatic = false;
		int dimension;
		int index;
		int blockId;
		int incomings;
		int lineNum;
		long timeStamp = 0L;
		ThreadInstance thread = null;
		HashMap<String, Stack<String>> stacks = new HashMap<String, Stack<String>>();
		while ((line = file.readLine()) != null) {
			// トレースファイルの解析
			if (line.startsWith("{\"type\":\"classDef\"")) {
				// クラス定義
				type = line.split(",\"name\":\"");
				classNameData = type[1].split("\",\"path\":\"");
				className = classNameData[0];
				pathData = classNameData[1].split("\",\"loaderPath\":\"");
				classPath = pathData[0].substring(1);								// 先頭の / を取り除く
				loaderPath = pathData[1].substring(1, pathData[1].length() - 3);	// 先頭の / と、末尾の "}, を取り除く
				initializeClass(className, classPath, loaderPath);
			} else if (line.startsWith("{\"type\":\"methodCall\"")) {
				// メソッド呼び出しの呼び出し側
				type = line.split(",\"callerSideSignature\":\"");
				signature = type[1].split("\",\"threadId\":");
				threadId = signature[1].split(",\"lineNum\":");
				lineNum = Integer.parseInt(threadId[1].substring(0, threadId[1].length() - 2));	// 末尾の }, を取り除く
				thread = threads.get(threadId[0]);
				thread.preCallMethod(signature[0], lineNum);
			} else if (line.startsWith("{\"type\":\"methodEntry\"")) {
				// メソッド呼び出し
				type = line.split("\"signature\":\"");
				signature = type[1].split("\",\"receiver\":");
				receiver = signature[1].split(",\"args\":");
				arguments = receiver[1].split(",\"threadId\":");
				threadId = arguments[1].split(",\"time\":");
				thisData = parseClassNameAndObjectId(receiver[0]);
				thisClassName = thisData[0];
				thisObjectId = thisData[1];
				isConstractor = false;
				isStatic = false;
				if (signature[0].contains("static ")) {
					isStatic = true;
				}
				thread = threads.get(threadId[0]);
				time = threadId[1].substring(0, threadId[1].length() - 2);			// 末尾の }, を取り除く
				timeStamp = Long.parseLong(time);
				Stack<String> stack;
				if (thread == null) {
					thread = new ThreadInstance(threadId[0]);
					threads.put(threadId[0], thread);
					stack = new Stack<String>();
					stacks.put(threadId[0], stack);
				} else {
					stack = stacks.get(threadId[0]);
				}
				stack.push(signature[0]);
				// メソッド呼び出しの設定
				thread.callMethod(signature[0], null, thisClassName, thisObjectId, isConstractor, isStatic, timeStamp);
				// 引数の設定
				thread.setArgments(parseArguments(arguments));
			} else if (line.startsWith("{\"type\":\"constructorEntry\"")) {
				// コンストラクタ呼び出し
				type = line.split("\"signature\":\"");
				signature = type[1].split("\",\"class\":\"");
				receiver = signature[1].split("\",\"args\":");
				arguments = receiver[1].split(",\"threadId\":");
				threadId = arguments[1].split(",\"time\":");
				thisClassName = receiver[0];
				thisObjectId = "0";
				isConstractor = true;
				isStatic = false;
				thread = threads.get(threadId[0]);
				time = threadId[1].substring(0, threadId[1].length() - 2);			// 末尾の }, を取り除く
				timeStamp = Long.parseLong(time);
				Stack<String> stack;
				if (thread == null) {
					thread = new ThreadInstance(threadId[0]);
					threads.put(threadId[0], thread);
					stack = new Stack<String>();
					stacks.put(threadId[0], stack);
				} else {
					stack = stacks.get(threadId[0]);
				}
				stack.push(signature[0]);
				// メソッド呼び出しの設定
				thread.callMethod(signature[0], null, thisClassName, thisObjectId, isConstractor, isStatic, timeStamp);
				// 引数の設定
				thread.setArgments(parseArguments(arguments));
			} else if (line.startsWith("{\"type\":\"methodExit\"")) {
				// メソッドからの復帰
				type = line.split(",\"shortSignature\":\"");
				signature = type[1].split("\",\"receiver\":");
				receiver = signature[1].split(",\"returnValue\":");
				returnValue = receiver[1].split(",\"threadId\":");
				threadId = returnValue[1].split(",\"time\":");
				thisData = parseClassNameAndObjectId(receiver[0]);
				thisClassName = thisData[0];
				thisObjectId = thisData[1];
				returnData = parseClassNameAndObjectId(returnValue[0]);
				returnClassName = returnData[0];
				returnObjectId = returnData[1];
				shortSignature = signature[0];
				time = threadId[1].substring(0, threadId[1].length() - 2);		// 末尾の }, を取り除く
				timeStamp = Long.parseLong(time);
				Stack<String> stack = stacks.get(threadId[0]);
				if (!stack.isEmpty()) {
					String line2 = stack.peek();
					if (line2.endsWith(shortSignature)) {
						stack.pop();
					} else {
						do {
							stack.pop();
							thread.terminateMethod();
							if (!stack.isEmpty()) line2 = stack.peek();
						} while (!stack.isEmpty() && !line2.endsWith(shortSignature));
						if (!stack.isEmpty()) stack.pop();
					}
					thread = threads.get(threadId[0]);
					ObjectReference returnVal = new ObjectReference(returnObjectId, returnClassName);					
					isCollectionType = false;
					if(thisClassName.contains("java.util.List")
							|| thisClassName.contains("java.util.Vector")
							|| thisClassName.contains("java.util.Iterator")
							|| thisClassName.contains("java.util.ListIterator")
							|| thisClassName.contains("java.util.ArrayList")
							|| thisClassName.contains("java.util.Stack")
							|| thisClassName.contains("java.util.Hash")
							|| thisClassName.contains("java.util.Map")
							|| thisClassName.contains("java.util.Set")
							|| thisClassName.contains("java.util.Linked")
							|| thisClassName.contains("java.util.Collection")
							|| thisClassName.contains("java.util.Arrays")
							|| thisClassName.contains("java.lang.Thread")) {
						isCollectionType = true;
					}
					// メソッドからの復帰の設定
					thread.returnMethod(returnVal, thisObjectId, isCollectionType, timeStamp);
				}
			} else if (line.startsWith("{\"type\":\"constructorExit\"")) {
				// コンストラクタからの復帰
				type = line.split(",\"shortSignature\":\"");
				signature = type[1].split("\",\"returnValue\":");
				returnValue = signature[1].split(",\"threadId\":");
				threadId = returnValue[1].split(",\"time\":");
				returnData = parseClassNameAndObjectId(returnValue[0]);
				thisClassName = returnClassName = returnData[0];
				thisObjectId = returnObjectId = returnData[1];
				time = threadId[1].substring(0, threadId[1].length() - 2);		// 末尾の }, を取り除く
				timeStamp = Long.parseLong(time);
				Stack<String> stack = stacks.get(threadId[0]);
				shortSignature = signature[0];
				if (!stack.isEmpty()) {
					String line2 = stack.peek();
					if (line2.endsWith(shortSignature)) {
						stack.pop();
					} else {
						do {
							stack.pop();
							thread.terminateMethod();
							line2 = stack.peek();
						} while (!stack.isEmpty() && !line2.endsWith(shortSignature));
						if (!stack.isEmpty()) stack.pop();
					}
					thread = threads.get(threadId[0]);
					ObjectReference returnVal = new ObjectReference(returnObjectId, returnClassName);					
					isCollectionType = false;
					if(thisClassName.contains("java.util.List")
							|| thisClassName.contains("java.util.Vector")
							|| thisClassName.contains("java.util.Iterator")
							|| thisClassName.contains("java.util.ListIterator")
							|| thisClassName.contains("java.util.ArrayList")
							|| thisClassName.contains("java.util.Stack")
							|| thisClassName.contains("java.util.Hash")
							|| thisClassName.contains("java.util.Map")
							|| thisClassName.contains("java.util.Set")
							|| thisClassName.contains("java.util.Linked")
							|| thisClassName.contains("java.lang.Thread")) {
						isCollectionType = true;
					}
					// メソッドからの復帰の設定
					thread.returnMethod(returnVal, thisObjectId, isCollectionType, timeStamp);
				}
			} else if (line.startsWith("{\"type\":\"fieldGet\"")) {
				// フィールドアクセス
				type = line.split(",\"fieldName\":\"");
				fieldData = type[1].split("\",\"this\":");
				thisObj = fieldData[1].split(",\"container\":");
				containerObj = thisObj[1].split(",\"value\":");
				valueObj = containerObj[1].split(",\"threadId\":");
				threadId = valueObj[1].split(",\"lineNum\":");
				lineData = threadId[1].split(",\"time\":");
				thisData = parseClassNameAndObjectId(thisObj[0]);
				thisClassName = thisData[0];
				thisObjectId = thisData[1];
				containerData = parseClassNameAndObjectId(containerObj[0]);
				containerClassName = containerData[0];
				containerObjectId = containerData[1];
				valueData = parseClassNameAndObjectId(valueObj[0]);
				valueClassName = valueData[0];
				valueObjectId = valueData[1];
				thread = threads.get(threadId[0]);
				lineNum = Integer.parseInt(lineData[0]);
				time = lineData[1].substring(0, lineData[1].length() - 2);		// 末尾の }, を取り除く
				timeStamp = Long.parseLong(time);
				// フィールドアクセスの設定
				if (thread != null) thread.fieldAccess(fieldData[0], valueClassName, valueObjectId, containerClassName, containerObjectId, thisClassName, thisObjectId, lineNum, timeStamp);
			} else if (line.startsWith("{\"type\":\"fieldSet\"")) {
				// フィールド更新
				type = line.split(",\"fieldName\":\"");
				fieldData = type[1].split("\",\"container\":");
				containerObj = fieldData[1].split(",\"value\":");
				valueObj = containerObj[1].split(",\"threadId\":");
				threadId = valueObj[1].split(",\"lineNum\":");
				lineData = threadId[1].split(",\"time\":");
				containerData = parseClassNameAndObjectId(containerObj[0]);
				containerClassName = containerData[0];
				containerObjectId = containerData[1];
				valueData = parseClassNameAndObjectId(valueObj[0]);
				valueClassName = valueData[0];
				valueObjectId = valueData[1];
				thread = threads.get(threadId[0]);
				lineNum = Integer.parseInt(lineData[0]);
				time = lineData[1].substring(0, lineData[1].length() - 2);		// 末尾の }, を取り除く
				timeStamp = Long.parseLong(time);
				// フィールド更新の設定
				if (thread != null) thread.fieldUpdate(fieldData[0], valueClassName, valueObjectId, containerClassName, containerObjectId, lineNum, timeStamp);
			} else if (line.startsWith("{\"type\":\"arrayCreate\"")) {
				// 配列生成
				type = line.split(",\"array\":");
				arrayObj = type[1].split(",\"dimension\":");
				arrayData = parseClassNameAndObjectId(arrayObj[0]);
				arrayClassName = arrayData[0];
				arrayObjectId = arrayData[1];
				dimensionData = arrayObj[1].split(",\"threadId\":");
				dimension = Integer.parseInt(dimensionData[0]);
				threadId = dimensionData[1].split(",\"lineNum\":");
				thread = threads.get(threadId[0]);
				lineData = threadId[1].split(",\"time\":");
				lineNum = Integer.parseInt(lineData[0]);
				time = lineData[1].substring(0, lineData[1].length() - 2);		// 末尾の }, を取り除く
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.arrayCreate(arrayClassName, arrayObjectId, dimension, lineNum, timeStamp);
			} else if (line.startsWith("{\"type\":\"arraySet\"")) {
				// 配列要素への代入
				type = line.split(",\"array\":");
				arrayObj = type[1].split(",\"index\":");
				arrayData = parseClassNameAndObjectId(arrayObj[0]);
				arrayClassName = arrayData[0];
				arrayObjectId = arrayData[1];
				indexData = arrayObj[1].split(",\"value\":");
				index = Integer.parseInt(indexData[0]);
				valueObj = indexData[1].split(",\"threadId\":");
				valueData = parseClassNameAndObjectId(valueObj[0]);
				valueClassName = valueData[0];
				valueObjectId = valueData[1];
				threadId = valueObj[1].split(",\"time\":");
				thread = threads.get(threadId[0]);
				time = threadId[1].substring(0, threadId[1].length() - 2);		// 末尾の }, を取り除く					
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.arraySet(arrayClassName, arrayObjectId, index, valueClassName, valueObjectId, 0, timeStamp);
			} else if (line.startsWith("{\"type\":\"arrayGet\"")) {
				// 配列要素の参照
				type = line.split(",\"array\":");
				arrayObj = type[1].split(",\"index\":");
				arrayData = parseClassNameAndObjectId(arrayObj[0]);
				arrayClassName = arrayData[0];
				arrayObjectId = arrayData[1];
				indexData = arrayObj[1].split(",\"value\":");
				index = Integer.parseInt(indexData[0]);
				valueObj = indexData[1].split(",\"threadId\":");
				valueData = parseClassNameAndObjectId(valueObj[0]);
				valueClassName = valueData[0];
				valueObjectId = valueData[1];
				threadId = valueObj[1].split(",\"time\":");
				thread = threads.get(threadId[0]);
				time = threadId[1].substring(0, threadId[1].length() - 2);		// 末尾の }, を取り除く					
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.arrayGet(arrayClassName, arrayObjectId, index, valueClassName, valueObjectId, 0, timeStamp);
			} else if (line.startsWith("{\"type\":\"blockEntry\"")) {
				// ブロックの開始
				type = line.split(",\"methodSignature\":\"");
				signature = type[1].split("\",\"blockId\":");
				blockIdData = signature[1].split(",\"incomings\":");
				blockId = Integer.parseInt(blockIdData[0]);
				incomingsData = blockIdData[1].split(",\"threadId\":");
				incomings = Integer.parseInt(incomingsData[0]);
				threadId = incomingsData[1].split(",\"lineNum\":");
				thread = threads.get(threadId[0]);
				lineData = threadId[1].split(",\"time\":");
				lineNum = Integer.parseInt(lineData[0]);
				time = lineData[1].substring(0, lineData[1].length() - 2);		// 末尾の }, を取り除く
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.blockEnter(blockId, incomings, lineNum, timeStamp);
			}
		}
	}

	/**
	 * クラス名とオブジェクトIDを表すJSONオブジェクトを解読する
	 * @param classNameAndObjectIdJSON トレースファイル内のJSONオブジェクト
	 * @return
	 */
	protected String[] parseClassNameAndObjectId(String classNameAndObjectIdJSON) {
		// 先頭の {"class":" の10文字と末尾の } を取り除いて分離
		return classNameAndObjectIdJSON.substring(10, classNameAndObjectIdJSON.length() - 1).split("\",\"id\":");
	}
	
	/**
	 * 引数を表すJSON配列を解読する
	 * @param arguments
	 * @return
	 */
	protected ArrayList<ObjectReference> parseArguments(String[] arguments) {
		String[] argData;
		argData = arguments[0].substring(1, arguments[0].length() - 1).split(",");		// 先頭の [ と末尾の ] を取り除く
		ArrayList<ObjectReference> argumentsData = new ArrayList<ObjectReference>();
		for (int k = 0; k < argData.length - 1; k += 2) {
			argumentsData.add(new ObjectReference(argData[k+1].substring(5, argData[k+1].length() - 1), argData[k].substring(10, argData[k].length() - 1)));
		}
		return argumentsData;
	}
	
	/**
	 * オンライン解析用シングルトンの取得
	 * @return オンライン解析用トレース
	 */
	public static TraceJSON getInstance() {
		if (theTrace == null) {
			theTrace = new TraceJSON();
		}
		return (TraceJSON)theTrace;
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
	 * 指定したスレッド上で現在実行中のメソッド実行を取得する
	 * @param thread 対象スレッド
	 * @return thread 上で現在実行中のメソッド実行
	 */
	public static MethodExecution getCurrentMethodExecution(Thread thread) {
		ThreadInstance t = getInstance().threads.get(String.valueOf(thread.getId()));
		return t.getCurrentMethodExecution();
	}
	
	/**
	 * 指定したスレッド上で現在実行中のトレースポイントを取得する
	 * @param thread 対象スレッド
	 * @return thread 上で現在実行中の実行文のトレースポイント
	 */
	public static TracePoint getCurrentTracePoint(Thread thread) {
		ThreadInstance t = getInstance().threads.get(String.valueOf(thread.getId()));
		return t.getCurrentTracePoint();
	}

	public void initializeClass(String name, String path, String loaderPath) {
		classes.put(name, new ClassInfo(name, path, loaderPath));
	}

	public ClassInfo getClassInfo(String className) {
		return classes.get(className);
	}
	
	public TracePoint getArraySetTracePoint(final Reference ref, TracePoint before) {
		final TracePoint start = before.duplicate();
		before = traverseStatementsInTraceBackward(new IStatementVisitor() {
				@Override
				public boolean preVisitStatement(Statement statement) {
					if (statement instanceof ArrayUpdate) {
						ArrayUpdate arraySet = (ArrayUpdate)start.getStatement();
						String srcObjId = ref.getSrcObjectId();
						String dstObjId = ref.getDstObjectId();
						String srcClassName = ref.getSrcClassName();
						String dstClassName = ref.getDstClassName();
						if ((srcObjId != null && srcObjId.equals(arraySet.getArrayObjectId())) 
							|| (srcObjId == null || isNull(srcObjId)) && srcClassName.equals(arraySet.getArrayClassName())) {
							if ((dstObjId != null && dstObjId.equals(arraySet.getValueObjectId()))
								|| ((dstObjId == null || isNull(dstObjId)) && dstClassName.equals(arraySet.getValueClassName()))) {
								if (srcObjId == null) {
									ref.setSrcObjectId(arraySet.getArrayObjectId());
								} else if (srcClassName == null) {
									ref.setSrcClassName(arraySet.getArrayClassName());
								}
								if (dstObjId == null) {
									ref.setDstObjectId(arraySet.getValueObjectId());
								} else if (dstClassName == null) {
									ref.setDstClassName(arraySet.getValueClassName());
								}
								return true;
							}
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
	
	/**
	 * 実行された全ブロックを取得する
	 * @return 全ブロック(メソッド名:ブロックID)
	 */
	public HashSet<String> getAllBlocks() {
		final HashSet<String> blocks = new HashSet<String>();
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
					for (Statement s: methodExecution.getStatements()) {
						if (s instanceof BlockEnter) {
							blocks.add(methodExecution.getSignature() + ":" + ((BlockEnter)s).getBlockId());
						}
					}
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}	
		return blocks;
	}
	
	/**
	 * マーク内で実行が開始されたブロックを取得する
	 * @param markStart マークの開始時刻
	 * @param markEnd マークの終了時刻
	 * @return 該当するブロック(メソッド名:ブロックID)
	 */
	public HashSet<String> getMarkedBlocks(final long markStart, final long markEnd) {
		final HashSet<String> blocks = new HashSet<String>();
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
					if (!methodExecution.isTerminated() && methodExecution.getExitTime() < markStart) return true;		// 探索終了
					if (methodExecution.getEntryTime() > markEnd) return false;
					for (Statement s: methodExecution.getStatements()) {
						if (s instanceof BlockEnter) {
							long entryTime = ((BlockEnter)s).getTimeStamp();
							if (entryTime >= markStart && entryTime <= markEnd) {
								blocks.add(methodExecution.getSignature() + ":" + ((BlockEnter)s).getBlockId());
							}
						}
					}
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}	
		return blocks;
	}
	
	/**
	 * 実行された全フローを取得する
	 * @return 全フロー(メソッド名:フロー元ブロックID:フロー先ブロックID)
	 */
	public HashSet<String> getAllFlows() {
		final HashSet<String> flows = new HashSet<String>();
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
					int prevBlockId = -1;
					for (Statement s: methodExecution.getStatements()) {
						if (s instanceof BlockEnter) {
							int curBlockID = ((BlockEnter)s).getBlockId();
							if (prevBlockId != -1) {
								flows.add(methodExecution.getSignature() + ":" + prevBlockId + ":" + curBlockID);
							} else {
								flows.add(methodExecution.getSignature() + ":" + curBlockID);
							}
							prevBlockId = curBlockID;
						}
					}
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}	
		return flows;
	}
	
	/**
	 * マーク内で実行されたフローを取得する
	 * @param markStart マークの開始時刻
	 * @param markEnd マークの終了時刻
	 * @return 該当するフロー(メソッド名:フロー元ブロックID:フロー先ブロックID)
	 */
	public HashSet<String> getMarkedFlows(final long markStart, final long markEnd) {
		final HashSet<String> flows = new HashSet<String>();
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
					if (!methodExecution.isTerminated() && methodExecution.getExitTime() < markStart) return true;		// 探索終了
					if (methodExecution.getEntryTime() > markEnd) return false;
					int prevBlockId = -1;
					for (Statement s: methodExecution.getStatements()) {
						if (s instanceof BlockEnter) {
							long entryTime = ((BlockEnter)s).getTimeStamp();
							int curBlockID = ((BlockEnter)s).getBlockId();
							if (entryTime >= markStart && entryTime <= markEnd) {
								if (prevBlockId != -1) {
									flows.add(methodExecution.getSignature() + ":" + prevBlockId + ":" + curBlockID);
								} else {
									flows.add(methodExecution.getSignature() + ":" + curBlockID);
								}
							}
							prevBlockId = curBlockID;
						}
					}
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}	
		return flows;
	}
	
	/**
	 * トレース内の全スレッドを同期させながら全実行文を逆方向に探索する
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
			ArrayList<MethodExecution> roots = (ArrayList<MethodExecution>)thread.getRoot().clone();
			threadRoots.put(threadId, roots);
			TracePoint threadLastTp;
			do {
				MethodExecution threadLastExecution = roots.remove(roots.size() - 1);
				threadLastTp = threadLastExecution.getExitPoint();
			} while (!threadLastTp.isValid() && roots.size() > 0);
			if (threadLastTp.isValid()) {
				threadLastPoints.put(threadId, threadLastTp);
				long threadLastTime;
				if (threadLastTp.getStatement() instanceof MethodInvocation) {
					threadLastTime = ((MethodInvocation) threadLastTp.getStatement()).getCalledMethodExecution().getExitTime();
				} else {
					threadLastTime = threadLastTp.getStatement().getTimeStamp();
				}				
				if (traceLastTime < threadLastTime) {
					traceLastTime2 = traceLastTime;
					traceLastThread2 = traceLastThread;
					traceLastTime = threadLastTime;
					traceLastThread = threadId;
				} else if (traceLastTime2 < threadLastTime) {
					traceLastTime2 = threadLastTime;
					traceLastThread2 = threadId;
				}
			} else {
				threadLastPoints.put(threadId, null);	
			}
		}
		return traverseStatementsInTraceBackwardSub(visitor, threadRoots, threadLastPoints, traceLastThread, traceLastThread2, traceLastTime2);
	}
	
	/**
	 * トレース内の全スレッドを同期させながら指定したトレースポイントから逆方向に探索する
	 * 
	 * @param visitor　実行文のビジター
	 * @param before 探索開始トレースポイント
	 * @return　中断したトレースポイント
	 */
	public TracePoint traverseStatementsInTraceBackward(IStatementVisitor visitor, TracePoint before) {
		if (before == null) {
			return traverseStatementsInTraceBackward(visitor);
		}
		// 全てのスレッドのトレースポイントを  before の前まで巻き戻す
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
					if (root.getEntryTime() > before.getStatement().getTimeStamp()) {
						rootExecutions.remove(n);
					} else {
						break;
					}
				}
				if (rootExecutions.size() > 0) {
					rootExecutions.remove(rootExecutions.size() - 1);
				}
			} else {
				TracePoint threadBeforeTp;
				do {
					MethodExecution threadLastExecution = rootExecutions.remove(rootExecutions.size() - 1);
					threadBeforeTp = threadLastExecution.getExitPoint();
				} while (!threadBeforeTp.isValid() && rootExecutions.size() > 0);
				if (threadBeforeTp.isValid()) {
					TracePoint[] tp = new TracePoint[] {threadBeforeTp};
					long threadLastTime;
					if (before.getStatement() instanceof MethodInvocation) {
						threadLastTime = ((MethodInvocation) before.getStatement()).getCalledMethodExecution().getExitTime();
					} else {
						threadLastTime = before.getStatement().getTimeStamp();
					}
					getLastStatementInThread(rootExecutions, tp, threadLastTime, new IStatementVisitor() {
						@Override
						public boolean preVisitStatement(Statement statement) { return false; }
						@Override
						public boolean postVisitStatement(Statement statement) { return false; }
					});
					threadLastPoints.put(threadId, tp[0]);
					if (tp[0] != null) {
						if (tp[0].getStatement() instanceof MethodInvocation) {
							threadLastTime = ((MethodInvocation) tp[0].getStatement()).getCalledMethodExecution().getExitTime();	// ※Exception が発生した場合は考えなくてよい?
						} else {
							threadLastTime = tp[0].getStatement().getTimeStamp();
						}
						if (traceLastTime2 < threadLastTime) {
							traceLastTime2 = threadLastTime;
							traceLastThread2 = threadId;
						}
					}					
				} else {
					threadLastPoints.put(threadId, null);					
				}
			}
		}
		return traverseStatementsInTraceBackwardSub(visitor, threadRoots, threadLastPoints, traceLastThread, traceLastThread2, traceLastTime2);
	}
	
	/**
	 * トレース内の全スレッドを同期させながら指定したトレースポイントから逆方向に探索する
	 * 
	 * @param visitor　実行文のビジター
	 * @param before 探索開始時間
	 * @return　中断したトレースポイント
	 */
	public TracePoint traverseStatementsInTraceBackward(IStatementVisitor visitor, long before) {
		if (before <= 0L) {
			return traverseStatementsInTraceBackward(visitor);
		}
		// 全てのスレッドのトレースポイントを  before の前まで巻き戻す
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadLastPoints = new HashMap<String, TracePoint>();
		long traceLastTime = 0;
		String traceLastThread = null;
		long traceLastTime2 = 0;
		String traceLastThread2 = null;
		for (String threadId: threads.keySet()) {
			ThreadInstance t = threads.get(threadId);
			ArrayList<MethodExecution> rootExecutions = (ArrayList<MethodExecution>)t.getRoot().clone();
			threadRoots.put(threadId, rootExecutions);
			TracePoint threadBeforeTp;
			do {
				MethodExecution threadLastExecution = rootExecutions.remove(rootExecutions.size() - 1);
				threadBeforeTp = threadLastExecution.getExitPoint();
			} while (!threadBeforeTp.isValid() && rootExecutions.size() > 0);
			if (threadBeforeTp.isValid()) {
				TracePoint[] tp = new TracePoint[] {threadBeforeTp};
				getLastStatementInThread(rootExecutions, tp, before, new IStatementVisitor() {
					@Override
					public boolean preVisitStatement(Statement statement) { return false; }
					@Override
					public boolean postVisitStatement(Statement statement) { return false; }
				});
				threadLastPoints.put(threadId, tp[0]);
				if (tp[0] != null) {
					long threadLastTime;
					if (tp[0].getStatement() instanceof MethodInvocation) {
						threadLastTime = ((MethodInvocation) tp[0].getStatement()).getCalledMethodExecution().getExitTime();	// ※Exception が発生した場合は考えなくてよい?
					} else {
						threadLastTime = tp[0].getStatement().getTimeStamp();
					}
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
			} else {
				threadLastPoints.put(threadId, null);					
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
			while (lastTp != null) {
				Statement statement = lastTp.getStatement();
				if (visitor.preVisitStatement(statement)) return lastTp;
				if (statement instanceof MethodInvocation) {
					// 呼び出し先がある場合、呼び出し先に潜る
					lastTp.stepBackNoReturn();
					if (lastTp.isValid()) {
						// 普通に呼び出し先に移った場合
						MethodExecution methodExecution = ((MethodInvocation) statement).getCalledMethodExecution();
						if (!methodExecution.isTerminated() && methodExecution.getExitTime() < traceLastTime2) {
							break;
						}
						continue;
					}
					// 空のメソッド実行の場合
				} else {
					if (visitor.postVisitStatement(statement)) return lastTp;					
				}
				if (lastTp.isValid() && lastTp.getStatement().getTimeStamp() < traceLastTime2) break;				
				// 1ステップ巻き戻す
				while (!lastTp.stepBackOver() && lastTp.isValid()) {
					// 呼び出し元に戻った場合(メソッド呼び出し文に復帰)
					statement = lastTp.getStatement();
					if (visitor.postVisitStatement(statement)) return lastTp;
				}
				if (!lastTp.isValid()) {
					// 呼び出し木の開始時点まで探索し終えた場合
					ArrayList<MethodExecution> roots = threadRoots.get(traceLastThread);
					while (!lastTp.isValid() && roots.size() > 0) {
						// 次の呼び出し木を探す
						MethodExecution lastExecution = roots.remove(roots.size() - 1);
						lastTp = lastExecution.getExitPoint();							
					}
					if (lastTp.isValid()) {
						// 次の呼び出し木があればそれを最後から探索
						threadLastPoints.put(traceLastThread, lastTp);							
						if (lastTp.getStatement().getTimeStamp() < traceLastTime2) break;				
					} else {
						// そのスレッドの探索がすべて終了した場合
						threadLastPoints.put(traceLastThread, null);
						break;
					}
				}
			}
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
						long threadLastTime;
						if (threadLastTp.getStatement() instanceof MethodInvocation) {
							threadLastTime = ((MethodInvocation) threadLastTp.getStatement()).getCalledMethodExecution().getExitTime();
						} else {
							threadLastTime = threadLastTp.getStatement().getTimeStamp();
						}				
						if (traceLastTime2 < threadLastTime) {
							traceLastTime2 = threadLastTime;
							traceLastThread2 = threadId;
						}
					}
				}
			}
			if (traceLastThread == null && traceLastThread2 == null) break;
			if (!continueTraverse && threadLastPoints.get(traceLastThread) == null) break;
		}
		return null;
	}
}