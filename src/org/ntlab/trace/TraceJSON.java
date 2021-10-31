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
	 * �w�肵��JSON�̃g���[�X�t�@�C������ǂ��� Trace �I�u�W�F�N�g�𐶐�����
	 * @param file �g���[�X�t�@�C��
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
	 * �w�肵��JSON�̃g���[�X�t�@�C������ǂ��� Trace �I�u�W�F�N�g�𐶐�����
	 * @param traceFile �g���[�X�t�@�C���̃p�X
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
		// �g���[�X�t�@�C���ǂݍ���
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
			// �g���[�X�t�@�C���̉��
			if (line.startsWith("{\"type\":\"classDef\"")) {
				// �N���X��`
				type = line.split(",\"name\":\"");
				classNameData = type[1].split("\",\"path\":\"");
				className = classNameData[0];
				pathData = classNameData[1].split("\",\"loaderPath\":\"");
				classPath = pathData[0].substring(1);								// �擪�� / ����菜��
				loaderPath = pathData[1].substring(1, pathData[1].length() - 3);	// �擪�� / �ƁA������ "}, ����菜��
				initializeClass(className, classPath, loaderPath);
			} else if (line.startsWith("{\"type\":\"methodCall\"")) {
				// ���\�b�h�Ăяo���̌Ăяo����
				type = line.split(",\"callerSideSignature\":\"");
				signature = type[1].split("\",\"threadId\":");
				threadId = signature[1].split(",\"lineNum\":");
				lineNum = Integer.parseInt(threadId[1].substring(0, threadId[1].length() - 2));	// ������ }, ����菜��
				thread = threads.get(threadId[0]);
				thread.preCallMethod(signature[0], lineNum);
			} else if (line.startsWith("{\"type\":\"methodEntry\"")) {
				// ���\�b�h�Ăяo��
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
				time = threadId[1].substring(0, threadId[1].length() - 2);			// ������ }, ����菜��
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
				// ���\�b�h�Ăяo���̐ݒ�
				thread.callMethod(signature[0], null, thisClassName, thisObjectId, isConstractor, isStatic, timeStamp);
				// �����̐ݒ�
				thread.setArgments(parseArguments(arguments));
			} else if (line.startsWith("{\"type\":\"constructorEntry\"")) {
				// �R���X�g���N�^�Ăяo��
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
				time = threadId[1].substring(0, threadId[1].length() - 2);			// ������ }, ����菜��
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
				// ���\�b�h�Ăяo���̐ݒ�
				thread.callMethod(signature[0], null, thisClassName, thisObjectId, isConstractor, isStatic, timeStamp);
				// �����̐ݒ�
				thread.setArgments(parseArguments(arguments));
			} else if (line.startsWith("{\"type\":\"methodExit\"")) {
				// ���\�b�h����̕��A
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
				time = threadId[1].substring(0, threadId[1].length() - 2);		// ������ }, ����菜��
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
					// ���\�b�h����̕��A�̐ݒ�
					thread.returnMethod(returnVal, thisObjectId, isCollectionType, timeStamp);
				}
			} else if (line.startsWith("{\"type\":\"constructorExit\"")) {
				// �R���X�g���N�^����̕��A
				type = line.split(",\"shortSignature\":\"");
				signature = type[1].split("\",\"returnValue\":");
				returnValue = signature[1].split(",\"threadId\":");
				threadId = returnValue[1].split(",\"time\":");
				returnData = parseClassNameAndObjectId(returnValue[0]);
				thisClassName = returnClassName = returnData[0];
				thisObjectId = returnObjectId = returnData[1];
				time = threadId[1].substring(0, threadId[1].length() - 2);		// ������ }, ����菜��
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
					// ���\�b�h����̕��A�̐ݒ�
					thread.returnMethod(returnVal, thisObjectId, isCollectionType, timeStamp);
				}
			} else if (line.startsWith("{\"type\":\"fieldGet\"")) {
				// �t�B�[���h�A�N�Z�X
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
				time = lineData[1].substring(0, lineData[1].length() - 2);		// ������ }, ����菜��
				timeStamp = Long.parseLong(time);
				// �t�B�[���h�A�N�Z�X�̐ݒ�
				if (thread != null) thread.fieldAccess(fieldData[0], valueClassName, valueObjectId, containerClassName, containerObjectId, thisClassName, thisObjectId, lineNum, timeStamp);
			} else if (line.startsWith("{\"type\":\"fieldSet\"")) {
				// �t�B�[���h�X�V
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
				time = lineData[1].substring(0, lineData[1].length() - 2);		// ������ }, ����菜��
				timeStamp = Long.parseLong(time);
				// �t�B�[���h�X�V�̐ݒ�
				if (thread != null) thread.fieldUpdate(fieldData[0], valueClassName, valueObjectId, containerClassName, containerObjectId, lineNum, timeStamp);
			} else if (line.startsWith("{\"type\":\"arrayCreate\"")) {
				// �z�񐶐�
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
				time = lineData[1].substring(0, lineData[1].length() - 2);		// ������ }, ����菜��
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.arrayCreate(arrayClassName, arrayObjectId, dimension, lineNum, timeStamp);
			} else if (line.startsWith("{\"type\":\"arraySet\"")) {
				// �z��v�f�ւ̑��
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
				time = threadId[1].substring(0, threadId[1].length() - 2);		// ������ }, ����菜��					
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.arraySet(arrayClassName, arrayObjectId, index, valueClassName, valueObjectId, 0, timeStamp);
			} else if (line.startsWith("{\"type\":\"arrayGet\"")) {
				// �z��v�f�̎Q��
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
				time = threadId[1].substring(0, threadId[1].length() - 2);		// ������ }, ����菜��					
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.arrayGet(arrayClassName, arrayObjectId, index, valueClassName, valueObjectId, 0, timeStamp);
			} else if (line.startsWith("{\"type\":\"blockEntry\"")) {
				// �u���b�N�̊J�n
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
				time = lineData[1].substring(0, lineData[1].length() - 2);		// ������ }, ����菜��
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.blockEnter(blockId, incomings, lineNum, timeStamp);
			}
		}
	}

	/**
	 * �N���X���ƃI�u�W�F�N�gID��\��JSON�I�u�W�F�N�g����ǂ���
	 * @param classNameAndObjectIdJSON �g���[�X�t�@�C������JSON�I�u�W�F�N�g
	 * @return
	 */
	protected String[] parseClassNameAndObjectId(String classNameAndObjectIdJSON) {
		// �擪�� {"class":" ��10�����Ɩ����� } ����菜���ĕ���
		return classNameAndObjectIdJSON.substring(10, classNameAndObjectIdJSON.length() - 1).split("\",\"id\":");
	}
	
	/**
	 * ������\��JSON�z�����ǂ���
	 * @param arguments
	 * @return
	 */
	protected ArrayList<ObjectReference> parseArguments(String[] arguments) {
		String[] argData;
		argData = arguments[0].substring(1, arguments[0].length() - 1).split(",");		// �擪�� [ �Ɩ����� ] ����菜��
		ArrayList<ObjectReference> argumentsData = new ArrayList<ObjectReference>();
		for (int k = 0; k < argData.length - 1; k += 2) {
			argumentsData.add(new ObjectReference(argData[k+1].substring(5, argData[k+1].length() - 1), argData[k].substring(10, argData[k].length() - 1)));
		}
		return argumentsData;
	}
	
	/**
	 * �I�����C����͗p�V���O���g���̎擾
	 * @return �I�����C����͗p�g���[�X
	 */
	public static TraceJSON getInstance() {
		if (theTrace == null) {
			theTrace = new TraceJSON();
		}
		return (TraceJSON)theTrace;
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
	 * �w�肵���X���b�h��Ō��ݎ��s���̃��\�b�h���s���擾����
	 * @param thread �ΏۃX���b�h
	 * @return thread ��Ō��ݎ��s���̃��\�b�h���s
	 */
	public static MethodExecution getCurrentMethodExecution(Thread thread) {
		ThreadInstance t = getInstance().threads.get(String.valueOf(thread.getId()));
		return t.getCurrentMethodExecution();
	}
	
	/**
	 * �w�肵���X���b�h��Ō��ݎ��s���̃g���[�X�|�C���g���擾����
	 * @param thread �ΏۃX���b�h
	 * @return thread ��Ō��ݎ��s���̎��s���̃g���[�X�|�C���g
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
	 * ���s���ꂽ�S�u���b�N���擾����
	 * @return �S�u���b�N(���\�b�h��:�u���b�NID)
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
	 * �}�[�N���Ŏ��s���J�n���ꂽ�u���b�N���擾����
	 * @param markStart �}�[�N�̊J�n����
	 * @param markEnd �}�[�N�̏I������
	 * @return �Y������u���b�N(���\�b�h��:�u���b�NID)
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
					if (!methodExecution.isTerminated() && methodExecution.getExitTime() < markStart) return true;		// �T���I��
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
	 * ���s���ꂽ�S�t���[���擾����
	 * @return �S�t���[(���\�b�h��:�t���[���u���b�NID:�t���[��u���b�NID)
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
	 * �}�[�N���Ŏ��s���ꂽ�t���[���擾����
	 * @param markStart �}�[�N�̊J�n����
	 * @param markEnd �}�[�N�̏I������
	 * @return �Y������t���[(���\�b�h��:�t���[���u���b�NID:�t���[��u���b�NID)
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
					if (!methodExecution.isTerminated() && methodExecution.getExitTime() < markStart) return true;		// �T���I��
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
	 * �g���[�X���̑S�X���b�h�𓯊������Ȃ���S���s�����t�����ɒT������
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
	 * �g���[�X���̑S�X���b�h�𓯊������Ȃ���w�肵���g���[�X�|�C���g����t�����ɒT������
	 * 
	 * @param visitor�@���s���̃r�W�^�[
	 * @param before �T���J�n�g���[�X�|�C���g
	 * @return�@���f�����g���[�X�|�C���g
	 */
	public TracePoint traverseStatementsInTraceBackward(IStatementVisitor visitor, TracePoint before) {
		if (before == null) {
			return traverseStatementsInTraceBackward(visitor);
		}
		// �S�ẴX���b�h�̃g���[�X�|�C���g��  before �̑O�܂Ŋ����߂�
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
							threadLastTime = ((MethodInvocation) tp[0].getStatement()).getCalledMethodExecution().getExitTime();	// ��Exception �����������ꍇ�͍l���Ȃ��Ă悢?
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
	 * �g���[�X���̑S�X���b�h�𓯊������Ȃ���w�肵���g���[�X�|�C���g����t�����ɒT������
	 * 
	 * @param visitor�@���s���̃r�W�^�[
	 * @param before �T���J�n����
	 * @return�@���f�����g���[�X�|�C���g
	 */
	public TracePoint traverseStatementsInTraceBackward(IStatementVisitor visitor, long before) {
		if (before <= 0L) {
			return traverseStatementsInTraceBackward(visitor);
		}
		// �S�ẴX���b�h�̃g���[�X�|�C���g��  before �̑O�܂Ŋ����߂�
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
						threadLastTime = ((MethodInvocation) tp[0].getStatement()).getCalledMethodExecution().getExitTime();	// ��Exception �����������ꍇ�͍l���Ȃ��Ă悢?
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
		// �S�X���b�h�̓������Ƃ�Ȃ���t�����Ɏ��s����T������
		for (;;) {
			// �T���Ώۂ̃X���b�h���̋t�����T��
			TracePoint lastTp = threadLastPoints.get(traceLastThread);
			while (lastTp != null) {
				Statement statement = lastTp.getStatement();
				if (visitor.preVisitStatement(statement)) return lastTp;
				if (statement instanceof MethodInvocation) {
					// �Ăяo���悪����ꍇ�A�Ăяo����ɐ���
					lastTp.stepBackNoReturn();
					if (lastTp.isValid()) {
						// ���ʂɌĂяo����Ɉڂ����ꍇ
						MethodExecution methodExecution = ((MethodInvocation) statement).getCalledMethodExecution();
						if (!methodExecution.isTerminated() && methodExecution.getExitTime() < traceLastTime2) {
							break;
						}
						continue;
					}
					// ��̃��\�b�h���s�̏ꍇ
				} else {
					if (visitor.postVisitStatement(statement)) return lastTp;					
				}
				if (lastTp.isValid() && lastTp.getStatement().getTimeStamp() < traceLastTime2) break;				
				// 1�X�e�b�v�����߂�
				while (!lastTp.stepBackOver() && lastTp.isValid()) {
					// �Ăяo�����ɖ߂����ꍇ(���\�b�h�Ăяo�����ɕ��A)
					statement = lastTp.getStatement();
					if (visitor.postVisitStatement(statement)) return lastTp;
				}
				if (!lastTp.isValid()) {
					// �Ăяo���؂̊J�n���_�܂ŒT�����I�����ꍇ
					ArrayList<MethodExecution> roots = threadRoots.get(traceLastThread);
					while (!lastTp.isValid() && roots.size() > 0) {
						// ���̌Ăяo���؂�T��
						MethodExecution lastExecution = roots.remove(roots.size() - 1);
						lastTp = lastExecution.getExitPoint();							
					}
					if (lastTp.isValid()) {
						// ���̌Ăяo���؂�����΂�����Ōォ��T��
						threadLastPoints.put(traceLastThread, lastTp);							
						if (lastTp.getStatement().getTimeStamp() < traceLastTime2) break;				
					} else {
						// ���̃X���b�h�̒T�������ׂďI�������ꍇ
						threadLastPoints.put(traceLastThread, null);
						break;
					}
				}
			}
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