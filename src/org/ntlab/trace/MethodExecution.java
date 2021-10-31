package org.ntlab.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MethodExecution {
	private String signature;
	private String callerSideSignature;
	private String thisClassName;
	private String thisObjId;
	private ArrayList<ObjectReference> arguments;
	private ObjectReference returnValue = null;
	private boolean isConstructor;
	private boolean isStatic;
	private boolean isCollectionType;
	private ArrayList<Statement> statements = new ArrayList<Statement>();
	private ArrayList<MethodExecution> children = new ArrayList<MethodExecution>();
	private MethodExecution callerMethodExecution = null;
	private int callerStatementExecution = -1;
	private boolean isTerminated = false;
	private AugmentationInfo augmentation = null;
	private long entryTime = 0L;
	private long exitTime = 0L;
	
	public MethodExecution(String signature, String callerSideSignature,
			String thisClassName, String thisObjId, boolean isConstructor,
			boolean isStatic, long enterTime) {
		this.signature = signature;
		this.callerSideSignature = callerSideSignature;
		this.thisClassName = thisClassName;
		this.thisObjId = thisObjId;
		this.isConstructor = isConstructor;
		this.isStatic = isStatic;
		this.isCollectionType = false;
		this.isTerminated = false;
		this.entryTime  = enterTime;
	}

	public void setArguments(ArrayList<ObjectReference> arguments) {
		this.arguments = arguments;
	}
	
	public void setThisObjeId(String thisObjId) {
		this.thisObjId = thisObjId;
	}

	public void setReturnValue(ObjectReference returnValue) {
		this.returnValue = returnValue;
	}
	
	public void setCollectionType(boolean isCollectionType) {
		this.isCollectionType = isCollectionType;
	}

	public void setTerminated(boolean isTerminated) {
		this.isTerminated = isTerminated;
	}
	
	public String getDeclaringClassName() {
		return Trace.getDeclaringType(signature, isConstructor);
	}

	public String getSignature() {
		return signature;
	}

	public String getCallerSideSignature() {
		return callerSideSignature;
	}

	public String getThisClassName() {
		return thisClassName;
	}

	public String getThisObjId() {
		if (isStatic) return Trace.getNull();
		return thisObjId;
	}

	public ArrayList<ObjectReference> getArguments() {
		if (arguments == null) arguments = new ArrayList<ObjectReference>();
		return arguments;
	}

	public ObjectReference getReturnValue() {
		return returnValue;
	}

	public boolean isConstructor() {
		return isConstructor;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public boolean isCollectionType() {
		return isCollectionType;
	}
	
	public boolean isTerminated() {
		return isTerminated;
	}

	public long getEntryTime() {
		return entryTime;
	}

	public long getExitTime() {
		if (isTerminated || exitTime == 0L) {
			TracePoint exitPoint = getExitPoint();
			if (!exitPoint.isValid()) return entryTime;
			Statement lastStatament = exitPoint.getStatement();
			if (lastStatament instanceof MethodInvocation) {
				return ((MethodInvocation) lastStatament).getCalledMethodExecution().getExitTime();
			} else {
				return lastStatament.getTimeStamp();
			}
		}
		return exitTime;
	}

	public void setExitTime(long exitTime) {
		this.exitTime = exitTime;
	}

	public void addStatement(Statement statement) {
		statements.add(statement);
		if (statement instanceof MethodInvocation) {
			children.add(((MethodInvocation)statement).getCalledMethodExecution());
		}
	}

	public ArrayList<Statement> getStatements() {
		return statements;
	}

	public ArrayList<MethodExecution> getChildren() {
		return children;
	}

	public void setCaller(MethodExecution callerMethodExecution, int callerStatementExecution) {
		this.callerMethodExecution  = callerMethodExecution;
		this.callerStatementExecution = callerStatementExecution;
	}

	public MethodExecution getParent() {
		return callerMethodExecution;
	}
	
	public TracePoint getEntryPoint() {
		return new TracePoint(this, 0);
	}
	
	public TracePoint getExitPoint() {
		return new TracePoint(this, statements.size() - 1);
	}
	
	public TracePoint getExitOutPoint() {
		return new TracePoint(this, statements.size());
	}

	public MethodExecution getCallerMethodExecution() {
		return callerMethodExecution;
	}

	public int getCallerStatementExecution() {
		return callerStatementExecution;
	}

	public TracePoint getCallerTracePoint() {
		if (callerMethodExecution == null) return null; 
		return new TracePoint(callerMethodExecution, callerStatementExecution);
	}
	
	/**
	 * ���̃��\�b�h���ŎQ�Ƃ��ꂽ�I�u�W�F�N�g�Ƃ��̃I�u�W�F�N�g�����\�b�h���ōŏ��ɎQ�Ƃ������s���_�̃��X�g���A�I�u�W�F�N�g�̌^���w�肵�Ď擾����
	 * @param actualTypeName �I�u�W�F�N�g�̌^
	 * @return ���̃��\�b�h���ŎQ�Ƃ��ꂽ actualTypeName �̃C���X�^���X�Ƃ��̃C���X�^���X���ŏ��ɎQ�Ƃ������s���_�̃��X�g
	 */
	public Map<ObjectReference, TracePoint> getObjectReferences(String actualTypeName) {
		Map<ObjectReference, TracePoint> objectRefMap = new HashMap<>();
		TracePoint tp = getExitPoint();
		if (tp != null) {
			do {
				Statement s = tp.getStatement();
				if (s instanceof FieldAccess) {
					FieldAccess f = (FieldAccess) s;
					if (f.getValueClassName().equals(actualTypeName)) {
						objectRefMap.put(new ObjectReference(f.getValueObjId(), f.getValueClassName()), tp.duplicate());
					}
				} else if (s instanceof ArrayAccess) {
					ArrayAccess a = (ArrayAccess) s;
					if (a.getValueClassName().equals(actualTypeName)) {
						objectRefMap.put(new ObjectReference(a.getValueObjectId(), a.getValueClassName()), tp.duplicate());
					}
				} else if (s instanceof ArrayCreate) {
					ArrayCreate a = (ArrayCreate) s;
					if (a.getArrayClassName().equals(actualTypeName)) {
						objectRefMap.put(new ObjectReference(a.getArrayObjectId(), a.getArrayClassName()), tp.duplicate());						
					}
				} else if (s instanceof MethodInvocation) {
					MethodInvocation m = (MethodInvocation) s;
					ObjectReference ret = m.getCalledMethodExecution().getReturnValue();
					if (ret != null && ret.getActualType().equals(actualTypeName)) {
						objectRefMap.put(ret, tp.duplicate());												
					}
				}
			} while (tp.stepBackOver());
		}
		for (ObjectReference arg: getArguments()) {
			if (arg.getActualType().equals(actualTypeName)) {
				objectRefMap.put(arg, getEntryPoint().duplicate());
			}
		}
		return objectRefMap;
	}
	
	/**
	 * ���̃��\�b�h���s����т��̑S�Ăяo������Ăяo���؂̒��ŋt�����ɒT������(�������Avisitor �� true ��Ԃ��܂�)
	 * @param visitor �r�W�^�[
	 * @return�@true -- �T���𒆒f����, false -- �Ō�܂ŒT������
	 */
	public boolean traverseMethodExecutionsBackward(IMethodExecutionVisitor visitor) {
		if (visitor.preVisitMethodExecution(this)) return true;
		ArrayList<MethodExecution> calledMethodExecutions = getChildren();
		for (int i = calledMethodExecutions.size() - 1; i >= 0; i--) {
			MethodExecution child = calledMethodExecutions.get(i);
			if (child.traverseMethodExecutionsBackward(visitor)) return true;
		}		
		if (visitor.postVisitMethodExecution(this, null)) return true;
		return false;
	}

	public void traverseMarkedMethodExecutions(IMethodExecutionVisitor visitor, long markStart, long markEnd) {
		if (entryTime <= markEnd) {
			if (entryTime >= markStart) {
				ArrayList<MethodExecution> markedChildren = new ArrayList<MethodExecution>();
				visitor.preVisitMethodExecution(this);
				for (int i = 0; i < children.size(); i++) {
					MethodExecution child = children.get(i);
					if (child.getEntryTime() <= markEnd) {
						child.traverseMarkedMethodExecutions(visitor, markStart, markEnd);
						markedChildren.add(child);
					} else {
						break;
					}
				}
				visitor.postVisitMethodExecution(this, markedChildren);
			} else {
				for (int i = 0; i < children.size(); i++) {
					MethodExecution child = children.get(i);
					if (child.getEntryTime() <= markEnd) {
						child.traverseMarkedMethodExecutions(visitor, markStart, markEnd);
					}
				}				
			}
		}
	}

	public void getMarkedMethodSignatures(HashSet<String> signatures, long markStart, long markEnd) {
		if (entryTime <= markEnd) {
			if (entryTime >= markStart) {
				signatures.add(getSignature());
			}
			for (int i = 0; i < children.size(); i++) {
				MethodExecution child = children.get(i);
				child.getMarkedMethodSignatures(signatures, markStart, markEnd);
			}
		}
	}

	public void getUnmarkedMethodSignatures(HashSet<String> signatures, long markStart, long markEnd) {
		if (entryTime < markStart || entryTime > markEnd) {
			signatures.add(getSignature());
			for (int i = 0; i < children.size(); i++) {
				MethodExecution child = children.get(i);
				child.getUnmarkedMethodSignatures(signatures, markStart, markEnd);
			}
		} else {
			for (int i = 0; i < children.size(); i++) {
				MethodExecution child = children.get(i);
				child.getUnmarkedMethodSignatures(signatures, markStart, markEnd);
			}
		}
	}

	public void getUnmarkedMethodExecutions(HashMap<String, ArrayList<MethodExecution>> allExecutions, long markStart, long markEnd) {
		if (entryTime < markStart || entryTime > markEnd) {
			ArrayList<MethodExecution> executions = allExecutions.get(getSignature());
			if (executions == null) {
				executions = new ArrayList<>();
				allExecutions.put(getSignature(), executions);
			}
			executions.add(this);
			for (int i = 0; i < children.size(); i++) {
				MethodExecution child = children.get(i);
				child.getUnmarkedMethodExecutions(allExecutions, markStart, markEnd);
			}
		} else {
			for (int i = 0; i < children.size(); i++) {
				MethodExecution child = children.get(i);
				child.getUnmarkedMethodExecutions(allExecutions, markStart, markEnd);
			}
		}
	}
	
	public AugmentationInfo getAugmentation() {
		return augmentation;
	}

	public void setAugmentation(AugmentationInfo augmentation) {
		this.augmentation = augmentation;
	}
	
	/**
	 *�@�����œn���ꂽmethodExecution���Ăяo�������\�b�h�Ăяo����T���ĕԂ�
	 * @param child ����methodExecution����Ăяo���ꂽ���Ƃ̂���ʂ�methodExecution
	 * @return �����œn���ꂽmethodExecution���Ăяo�������Ƃ��L�^���Ă��郁�\�b�h�Ăяo��
	 */
	public MethodInvocation getMethodInvocation(MethodExecution child) {
		int callerStatementExecution = child.getCallerStatementExecution();
		if (callerStatementExecution != -1) {
			return (MethodInvocation)statements.get(callerStatementExecution);			
		}
		return null;
	}

	/**
	 * order���w�肵�đΉ�����TracePoint��Ԃ�
	 * @param order TracePoint��order
	 * @return
	 */
	public TracePoint getTracePoint(int order) {
		if (order < this.getStatements().size()) {
			return new TracePoint(this, order);
		}
		return null;
	}
}
