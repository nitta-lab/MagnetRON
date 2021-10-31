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
	protected static final boolean EAGER_DETECTION_OF_ARRAY_SET = false;		// �z��v�f�ւ̑���̌��o�𑽂����ς��邩?(�������ς����False Positive�ɂȂ�\��������)
	protected static Trace theTrace = null;
	protected HashMap<String, ThreadInstance> threads = new HashMap<String, ThreadInstance>();

	protected Trace() {
	}
	
	/**
	 * �w�肵��PlainText�̃g���[�X�t�@�C������ǂ��� Trace �I�u�W�F�N�g�𐶐�����
	 * @param file �g���[�X�t�@�C��
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
	 * �w�肵��PlainText�̃g���[�X�t�@�C������ǂ��� Trace �I�u�W�F�N�g�𐶐�����
	 * @param traceFile �g���[�X�t�@�C���̃p�X
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
		// �g���[�X�t�@�C���ǂݍ���
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
			// �g���[�X�t�@�C���̉��
			if (line.startsWith("Method")) {
				// ���\�b�h�Ăяo���i�R���X�g���N�^�Ăяo�����܂ށj
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
				// ���\�b�h�Ăяo���̈���
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
				// ���\�b�h����̕��A
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
				// �t�B�[���h�A�N�Z�X
				accessData = line.split(":");
				if (accessData.length >= 9) {
					threadNo = getThreadNo(line);
//					threadNo = accessData[8].split(" ")[1];
					if (threadNo != null) {
						thread = threads.get(threadNo);
						timeStamp++;				// ���̃^�C���X�^���v(���s����ێ����邽��)
						if (thread != null) thread.fieldAccess(accessData[5], accessData[6], accessData[3], accessData[4], accessData[1], accessData[2], 0, timeStamp);
					}
				}
			} else if (line.startsWith("set")) {
				// �t�B�[���h�X�V
				updateData = line.split(":");
				if (updateData.length >= 7) {
					threadNo = getThreadNo(line);
//					threadNo = updateData[6].split(" ")[1];
					if (threadNo != null) {
						thread = threads.get(threadNo);
						timeStamp++;				// ���̃^�C���X�^���v(���s����ێ����邽��)
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
	 * �I�����C����͗p�V���O���g���̎擾
	 * @return �I�����C����͗p�g���[�X
	 */
	public static Trace getInstance() {
		if (theTrace == null) {
			theTrace = new Trace();
		}
		return theTrace;
	}
	
	/**
	 * �X���b�hID���w�肵�ăX���b�h�C���X�^���X���擾����(�I�����C����͗p)
	 * @param threadId
	 * @return �X���b�h�C���X�^���X
	 */
	public static ThreadInstance getThreadInstance(String threadId) {
		return getInstance().threads.get(threadId);
	}
	
	/**
	 * �w�肵���X���b�h��Ō��ݎ��s���̃��\�b�h���s���擾����(�I�����C����͗p)
	 * @param thread �ΏۃX���b�h
	 * @return thread ��Ō��ݎ��s���̃��\�b�h���s
	 */
	public static MethodExecution getCurrentMethodExecution(Thread thread) {
		ThreadInstance t = getInstance().threads.get(String.valueOf(thread.getId()));
		return t.getCurrentMethodExecution();
	}
	
	/**
	 * �w�肵���X���b�h��Ō��ݎ��s���̃g���[�X�|�C���g���擾����(�I�����C����͗p)
	 * @param thread �ΏۃX���b�h
	 * @return thread ��Ō��ݎ��s���̎��s���̃g���[�X�|�C���g
	 */
	public static TracePoint getCurrentTracePoint(Thread thread) {
		ThreadInstance t = getInstance().threads.get(String.valueOf(thread.getId()));
		return t.getCurrentTracePoint();
	}
	
	/**
	 * �S�X���b�h���擾����
	 * @return �X���b�hID����X���b�h�C���X�^���X�ւ̃}�b�v
	 */
	public HashMap<String, ThreadInstance> getAllThreads() {
		return threads;
	}

	/**
	 * ���\�b�h���ɑS���\�b�h���s��S�ẴX���b�h������o��
	 * @return ���\�b�h�V�O�j�`�����烁�\�b�h���s�̃��X�g�ւ�HashMap
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
	 * �S���\�b�h�̃V�O�j�`�����擾����
	 * @return �S���\�b�h�V�O�j�`��
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
	 * ���\�b�h���� methodSignature�@�ɑO����v���郁�\�b�h���s��S�ẴX���b�h������o��
	 * @param methodSignature ����������
	 * @return ��v�����S���\�b�h���s
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
	 * methodSignature �ɑO����v���郁�\�b�h���������\�b�h�̍Ō�̎��s
	 * @param methodSignature ���\�b�h��(�O����v�Ō�������)
	 * @return �Y������Ō�̃��\�b�h���s
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
	 * methodSignature �ɑO����v���郁�\�b�h���������\�b�h�� before �ȑO�̍Ō�̎��s
	 * @param methodSignature ���\�b�h��(�O����v�Ō�������)
	 * @param before�@�T���J�n�g���[�X�|�C���g(������ȑO��T��)
	 * @return�@�Y������Ō�̃��\�b�h���s
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
	 * �}�[�N���Ŏ��s���J�n���ꂽ�S���\�b�h�̃V�O�j�`�����擾����
	 * @param markStart �}�[�N�̊J�n����
	 * @param markEnd �}�[�N�̏I������
	 * @return �Y�����郁�\�b�h�V�O�j�`��
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
	 * �}�[�N���Ŏ��s���J�n���ꂽ�S���\�b�h���s�����\�b�h���Ɏ擾����
	 * @param markStart �}�[�N�̊J�n����
	 * @param markEnd �}�[�N�̏I������
	 * @return ���\�b�h�V�O�j�`������Y�����郁�\�b�h���s�̃��X�g�ւ�HashMap
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
	 * �}�[�N�O�Ŏ��s���J�n���ꂽ�S���\�b�h�̃V�O�j�`�����擾����
	 * @param markStart �}�[�N�̊J�n����
	 * @param markEnd �}�[�N�̏I������
	 * @return �Y�����郁�\�b�h�V�O�j�`��
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
	 * �}�[�N�O�Ŏ��s���J�n���ꂽ�S���\�b�h���s���擾����
	 * @param markStart �}�[�N�̊J�n����
	 * @param markEnd �}�[�N�̏I������
	 * @return ���\�b�h�V�O�j�`������Y�����郁�\�b�h���s�̃��X�g�ւ�HashMap
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
						// �I�u�W�F�N�gID���݂��Ɉ�v�����ꍇ
						return true;
					} else if ((srcObjId == null || isNull(srcObjId)) && fu.getContainerClassName().equals(srcType)) {
						if ((dstObjId == null || isNull(dstObjId)) && fu.getValueClassName().equals(dstType)) {
							// ref �ɃI�u�W�F�N�gID���w�肵�Ă��Ȃ������ꍇ
							ref.setSrcObjectId(fu.getContainerObjId());
							ref.setDstObjectId(fu.getValueObjId());
							return true;
						} else if (fu.getValueObjId().equals(dstObjId)) {
							// �N���X�ϐ��ւ̑���̏ꍇ
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
			// srcId �͔z��
			// ���\�b�h���s�J�n���� fieldAccessPoint �܂ł̊Ԃ̃��\�b�h���s���� dstId ���o��������?
			TracePoint p = fieldAccessPoint.duplicate();
			while (p.stepBackOver()) {
				Statement statement = p.getStatement();
				if (statement instanceof MethodInvocation) {
					MethodExecution calledMethod = ((MethodInvocation)statement).getCalledMethodExecution();
					if (calledMethod.getReturnValue().getId().equals(dstObjId)) {
						// dstId �͖߂�l�Ƃ��ďo��
						ref.setSrcClassName(fieldAccess.getValueClassName());
						ref.setDstClassName(calledMethod.getReturnValue().getActualType());
						return true;
					} else if (dstObjId == null || isNull(dstObjId) && calledMethod.getReturnValue().getActualType().equals(ref.getDstClassName())) {
						// dstClassName �͖߂�l�̌^�Ƃ��ďo��
						ref.setSrcObjectId(fieldAccess.getValueObjId());
						ref.setDstObjectId(calledMethod.getReturnValue().getId());
						return true;								
					}				
				}
				if (EAGER_DETECTION_OF_ARRAY_SET) {
					if (statement instanceof FieldAccess) {
						if (((FieldAccess)statement).getContainerObjId().equals(dstObjId)) {
							// dstId �̓t�B�[���h�ɏo��
							ref.setSrcClassName(fieldAccess.getValueClassName());
							ref.setDstClassName(((FieldAccess)statement).getContainerClassName());
							return true;
						} else if (dstObjId == null || isNull(dstObjId) && ((FieldAccess)statement).getContainerClassName().equals(ref.getDstClassName())) {
							// dstClassName �̓t�B�[���h�̌^�Ƃ��ďo��					
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
				// dstId �͈����ɏo��
				ref.setSrcClassName(fieldAccess.getValueClassName());
				ref.setDstClassName(args.get(argindex).getActualType());
				return true;
			} else if (dstObjId == null || isNull(dstObjId)) {
				for (int j = 0; j < args.size(); j++) {
					if (args.get(j).getActualType().equals(ref.getDstClassName())) {
						// dstClassName �͈����̌^�ɏo��							
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
	 * �g���[�X���̑S���\�b�h���s�̊J�n�n�_���t�����ɒT������
	 * @param visitor ���\�b�h���s�̃r�W�^�[(postVisitMethodExecution()�����ĂѕԂ��Ȃ��̂Œ���)
	 * @return ���f�������\�b�h���s
	 */
	public MethodExecution traverseMethodEntriesInTraceBackward(IMethodExecutionVisitor visitor) {
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadLastPoints = new HashMap<String, TracePoint>();
		// �e�X���b�h�ɂ����Ĉ�ԍŌ�ɊJ�n�������\�b�h���s��T��
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
	 * �w�肵�����s���_�ȑO�Ɏ��s���J�n���ꂽ���\�b�h���s�̊J�n���_���t�����ɒT������
	 * @param visitor ���\�b�h���s�̃r�W�^�[(postVisitMethodExecution()�����ĂѕԂ��Ȃ��̂Œ���)
	 * @param before �T���J�n���_
	 * @return ���f�������\�b�h���s
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
		// �S�X���b�h�̓������Ƃ�Ȃ���t�����Ƀ��\�b�h���s��T������
		for (;;) {
			// �T���Ώۂ̃X���b�h���̋t�����T��
			TracePoint threadLastTp = threadLastPoints.get(traceLastThread);
			MethodExecution threadLastExecution = threadLastTp.getMethodExecution();
			do {
				threadLastTp.stepBackOver();
				// ���̃X���b�h�̎��̃��\�b�h���s�J�n���_�܂ŒT������
				threadLastTp = getLastMethodEntryInThread(threadRoots.get(traceLastThread), threadLastTp);
				if (threadLastTp == null) break;
				if (visitor.postVisitMethodExecution(threadLastExecution, threadLastExecution.getChildren())) {
					// �Y�����郁�\�b�h���s��������
					return threadLastExecution;
				}
				threadLastExecution = threadLastTp.getMethodExecution();
			} while (threadLastExecution.getEntryTime() > traceLastTime2);
			threadLastPoints.put(traceLastThread, threadLastTp);
			traceLastThread = traceLastThread2;
			// ���̎��ɒT�����ׂ��X���b�h(���T���̗̈悪��ԍŌ�܂Ŏc���Ă���X���b�h)�����肷��
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
	 * �Ăяo�����̎����� markStart ���� markEnd �̊Ԃɂ���S�X���b�h���̑S���\�b�h���s��T������
	 * @param visitor�@�r�W�^�[
	 * @param markStart �J�n����
	 * @param markEnd �I������
	 */
	public void traverseMarkedMethodExecutions(IMethodExecutionVisitor visitor, long markStart, long markEnd) {
		for (String threadId: threads.keySet()) {
			ThreadInstance thread = threads.get(threadId);
			thread.traverseMarkedMethodExecutions(visitor, markStart, markEnd);
		}		
	}
	
	/**
	 * �g���[�X���̑S�X���b�h�𓯊������Ȃ炪�S���s�����������ɒT������
	 * 
	 * @param visitor ���s���̃r�W�^�[
	 * @return ���f�����g���[�X�|�C���g
	 */
	public TracePoint traverseStatementsInTrace(IStatementVisitor visitor) {
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadCurPoints = new HashMap<String, TracePoint>();
		// �e�X���b�h�ɂ����Ĉ�ԍŏ��ɊJ�n�������\�b�h���s��T��
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
		// �S�X���b�h�̓������Ƃ�Ȃ��珇�����Ɏ��s����T������
		for (;;) {
			// �T���Ώۂ̃X���b�h���̏������T��
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
					// �g���[�X�|�C���g�̏����K�v�ȏꍇ
					if (((AbstractTracePointVisitor) visitor).preVisitStatement(statement, curTp)) return curTp;
					if (!(statement instanceof MethodInvocation)) {
						if (((AbstractTracePointVisitor) visitor).postVisitStatement(statement, curTp)) return curTp;
					}
				}
				curTp.stepNoReturn();		// ���A�����ɌĂяo���؂�����Ă���
				if (!curTp.isValid()) {
					// ���A���Ȃ��Ƃ���ȏ�T���ł��Ȃ�
					while (!curTp.stepOver()) {		// ���x�͕��A�͂��邪���炸�ɒT��
						if (curTp.isValid()) {
							// �Ăяo����T���O�Ɉ�x�K��ς݂̃��\�b�h�Ăяo���s���A�T���������x�K�₷��
							if (!(visitor instanceof AbstractTracePointVisitor)) {
								if (visitor.postVisitStatement(curTp.getStatement())) return curTp;
							} else {
								// �g���[�X�|�C���g�̏����K�v�ȏꍇ
								if (((AbstractTracePointVisitor) visitor).postVisitStatement(curTp.getStatement(), curTp)) return curTp;
							}
						} else {
							// �Ăяo���؂̊J�n���_�܂ŒT�����I�����ꍇ
							ArrayList<MethodExecution> roots = threadRoots.get(curThreadId);
							while (!curTp.isValid() && roots.size() > 0) {
								// ���̌Ăяo���؂�����΂�����ŏ�����T��
								MethodExecution firstExecution = roots.remove(0);
								curTp = firstExecution.getEntryPoint();							
							}
							if (curTp.isValid()) {
								// ���̌Ăяo���؂�����΂�����ŏ�����T��
								threadCurPoints.put(curThreadId, curTp);
							} else {
								// ���̃X���b�h�̒T�������ׂďI�������ꍇ
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
			// ���̎��ɒT�����ׂ��X���b�h(���T���̗̈悪��ԍŏ��Ɏn�܂�X���b�h)�����肷��
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
	 * �g���[�X���̑S�X���b�h�𓯊������Ȃ炪�S���s�����t�����ɒT������
	 * 
	 * @param visitor ���s���̃r�W�^�[
	 * @return ���f�����g���[�X�|�C���g
	 */
	public TracePoint traverseStatementsInTraceBackward(IStatementVisitor visitor) {
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadLastPoints = new HashMap<String, TracePoint>();
		// �e�X���b�h�ɂ����Ĉ�ԍŌ�ɊJ�n�������\�b�h���s��T��
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
		// �S�X���b�h�̓������Ƃ�Ȃ���t�����Ɏ��s����T������
		for (;;) {
			// �T���Ώۂ̃X���b�h���̋t�����T��
			TracePoint lastTp = threadLastPoints.get(traceLastThread);
			do {
				if (lastTp.stepBackOver()) {
					// ���̃X���b�h�̎��̃��\�b�h���s�J�n���_�܂ŒT������
					if (traverseStatamentsInCallTreeBackwardNoReturn(visitor, lastTp)) return lastTp;					
				} else {
					// �Ăяo�����ɖ߂����ꍇ
					if (lastTp.isValid()) {
						if (visitor.postVisitStatement(lastTp.getStatement())) return lastTp;
					} else {
						// �Ăяo���؂̊J�n���_�܂ŒT�����I�����ꍇ
						ArrayList<MethodExecution> root = threadRoots.get(traceLastThread);
						while (!lastTp.isValid() && root.size() > 0) {
							// ���̌Ăяo���؂�����΂�����Ōォ��T��
							MethodExecution lastExecution = root.remove(root.size() - 1);
							lastTp = lastExecution.getExitPoint();							
						}
						if (lastTp.isValid()) {
							// ���̌Ăяo���؂�����΂�����Ōォ��T��
							threadLastPoints.put(traceLastThread, lastTp);
							if (traverseStatamentsInCallTreeBackwardNoReturn(visitor, lastTp)) return lastTp;
						} else {
							// ���̃X���b�h�̒T�������ׂďI�������ꍇ
							threadLastPoints.put(traceLastThread, null);
							break;
						}
					}
				}
			} while (lastTp.getMethodExecution().getEntryTime() >= traceLastTime2);
			traceLastThread = traceLastThread2;
			// ���̎��ɒT�����ׂ��X���b�h(���T���̗̈悪��ԍŌ�܂Ŏc���Ă���X���b�h)�����肷��
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
	 * before �Ŏw�肵���g���[�X�|�C���g�ȑO�̓���Ăяo���ؓ��̑S���s�����t�����ɒT������(�������Avisitor �� true ��Ԃ��܂�)
	 * @param visitor �r�W�^�[
	 * @param before �T���̊J�n�_(�T���ΏۃX���b�h���w�肵�Ă���)
	 * @return true -- �T���𒆒f����, false -- �Ō�܂ŒT������
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
	 * before�ȑO�̌Ăяo���؂��Ăяo���悩��̕��A�������ɍs����Ƃ���܂ŋt�����ɒT������
	 * 
	 * @param visitor ���s���̃r�W�^�[
	 * @param before �T���J�n���s���_
	 * @return true -- �T���𒆒f����, false -- ���A�����Ȃ����肱��ȏ�i�߂Ȃ� 
	 */
	private boolean traverseStatamentsInCallTreeBackwardNoReturn(IStatementVisitor visitor, TracePoint before) {
		for (;;) {
			Statement statement = before.getStatement();
			if (statement instanceof MethodInvocation) {
				// ���\�b�h�Ăяo�����̏ꍇ�́A�Ăяo���̑O��� preVisit �� postVisit ��ʁX�Ɏ��s����
				if (visitor.preVisitStatement(statement)) return true;
				before.stepBackNoReturn();
				if (!before.isValid()) {
					// �Ăяo����̃��\�b�h�Ŏ��s�����L�^����Ă��Ȃ��ꍇ
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
	 * before �Ŏw�肵���g���[�X�|�C���g�ȑO�̓���X���b�h���̑S���\�b�h���s���Ăяo���؂̒��ŋt�����ɒT������(�������Avisitor �� true ��Ԃ��܂�)
	 * @param visitor �r�W�^�[
	 * @param before �T���̊J�n�_(�T���ΏۃX���b�h���w�肵�Ă���)
	 * @return true -- �T���𒆒f����, false -- �Ō�܂ŒT������
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
