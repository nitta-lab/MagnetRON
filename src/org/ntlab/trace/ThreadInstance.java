package org.ntlab.trace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class ThreadInstance {
	private ArrayList<MethodExecution> roots = new ArrayList<MethodExecution>();
	private MethodExecution curMethodExecution = null;
	private MethodInvocation curMethodInvocation = null;
	private String id;
	
	public ThreadInstance(String id) {
		this.id = id;
	}
	
	public void addRoot(MethodExecution root) {
		this.roots.add(root);
		curMethodExecution = root;
	}
	
	public ArrayList<MethodExecution> getRoot() {
		return roots;
	}

	public String getId() {
		return id;
	}

	public void preCallMethod(String callerSideSignature, int lineNumOfInvocationStatement) {
		if (curMethodExecution != null) {
			curMethodInvocation = new MethodInvocation(callerSideSignature, curMethodExecution.getThisClassName(), curMethodExecution.getThisObjId(), lineNumOfInvocationStatement, id);
		}
	}
	
	public void callMethod(String signature, String callerSideSignature, String receiverClassName, String receiverObjId, 
			boolean isConstractor, boolean isStatic, long timeStamp) {
		if (callerSideSignature == null && curMethodInvocation != null) {
			callerSideSignature = curMethodInvocation.getCallerSideMethodName();
		}
		MethodExecution newMethodExecution = new MethodExecution(signature, callerSideSignature, receiverClassName, receiverObjId, isConstractor, isStatic, timeStamp);
		if (curMethodExecution != null) {
			if (curMethodInvocation == null) {
				curMethodExecution.addStatement(new MethodInvocation(newMethodExecution, curMethodExecution.getThisClassName(), curMethodExecution.getThisObjId(), 0, id));
			} else {
				curMethodInvocation.setCalledMethodExecution(newMethodExecution);
				curMethodExecution.addStatement(curMethodInvocation);
				curMethodInvocation = null;
			}
			newMethodExecution.setCaller(curMethodExecution, curMethodExecution.getStatements().size() - 1);
			curMethodExecution = newMethodExecution;
		} else {
			addRoot(newMethodExecution);
		}
	}
	
	public void setArgments(ArrayList<ObjectReference> arguments) {
		curMethodExecution.setArguments(arguments);
	}
	
	public void returnMethod(ObjectReference returnValue, String thisObjId, boolean isCollectionType) {
		if (curMethodExecution == null) return; 
		curMethodExecution.setReturnValue(returnValue);
		if (curMethodExecution.getThisObjId().equals("0")) {
			curMethodExecution.setThisObjeId(thisObjId);
		}
		curMethodExecution.setCollectionType(isCollectionType);
		curMethodExecution = curMethodExecution.getParent();
		curMethodInvocation =  null;		// ”O‚Ì‚½‚ß
	}

	public void returnMethod(ObjectReference returnValue, String thisObjId, boolean isCollectionType, long exitTime) {
		if (curMethodExecution != null) curMethodExecution.setExitTime(exitTime);
		returnMethod(returnValue, thisObjId, isCollectionType);
	}
	
	public void terminateMethod() {
		if (curMethodExecution == null) return; 
		curMethodExecution.setTerminated(true);
		curMethodExecution = curMethodExecution.getParent();		
	}

	public void fieldAccess(String valueClassName, String valueObjId, String containerClassName, String containerObjId, String thisClassName, String thisId) {
		FieldAccess fieldAccess = new FieldAccess(valueClassName, valueObjId, containerClassName, containerObjId, thisClassName, thisId, 0, id);
		if (curMethodExecution != null) curMethodExecution.addStatement(fieldAccess);
	}

	public void fieldAccess(String valueClassName, String valueObjId, String containerClassName, String containerObjId, String thisClassName, String thisId, int lineNo, long timeStamp) {
		FieldAccess fieldAccess = new FieldAccess(valueClassName, valueObjId, containerClassName, containerObjId, thisClassName, thisId, lineNo, id, timeStamp);
		if (curMethodExecution != null) curMethodExecution.addStatement(fieldAccess);
	}

	public void fieldAccess(String fieldName, String valueClassName, String valueObjId, String containerClassName, String containerObjId, String thisClassName, String thisId, int lineNo, long timeStamp) {
		FieldAccess fieldAccess = new FieldAccess(fieldName, valueClassName, valueObjId, containerClassName, containerObjId, thisClassName, thisId, lineNo, id, timeStamp);
		if (curMethodExecution != null) curMethodExecution.addStatement(fieldAccess);
	}

	public void fieldUpdate(String valueClassName, String valueObjId, String containerClassName, String containerObjId) {
		FieldUpdate fieldUpdate = new FieldUpdate(valueClassName, valueObjId, containerClassName, containerObjId, 0, id);
		if (curMethodExecution != null) curMethodExecution.addStatement(fieldUpdate);
	}

	public void fieldUpdate(String valueClassName, String valueObjId, String containerClassName, String containerObjId, int lineNo, long timeStamp) {
		FieldUpdate fieldUpdate = new FieldUpdate(valueClassName, valueObjId, containerClassName, containerObjId, lineNo, id, timeStamp);
		if (curMethodExecution != null) curMethodExecution.addStatement(fieldUpdate);
	}

	public void fieldUpdate(String fieldName, String valueClassName, String valueObjId, String containerClassName, String containerObjId, int lineNo, long timeStamp) {
		FieldUpdate fieldUpdate = new FieldUpdate(fieldName, valueClassName, valueObjId, containerClassName, containerObjId, lineNo, id, timeStamp);
		if (curMethodExecution != null) curMethodExecution.addStatement(fieldUpdate);
	}
	
	public void arrayCreate(String arrayClassName, String arrayObjectId, int dimension, int lineNo, long timeStamp) {
		ArrayCreate arrayCreate = new ArrayCreate(arrayClassName, arrayObjectId, dimension, lineNo, id, timeStamp);
		if (curMethodExecution != null) curMethodExecution.addStatement(arrayCreate);
	}

	public void arraySet(String arrayClassName, String arrayObjectId, int index, String valueClassName, String valueObjectId, int lineNo, long timeStamp) {
		ArrayUpdate arraySet = new ArrayUpdate(arrayClassName, arrayObjectId, index, valueClassName, valueObjectId, lineNo, id, timeStamp);
		if (curMethodExecution != null) curMethodExecution.addStatement(arraySet);
	}

	public void arrayGet(String arrayClassName, String arrayObjectId, int index, String valueClassName, String valueObjectId, int lineNo, long timeStamp) {
		ArrayAccess arrayGet = new ArrayAccess(arrayClassName, arrayObjectId, index, valueClassName, valueObjectId, lineNo, id, timeStamp);
		if (curMethodExecution != null) curMethodExecution.addStatement(arrayGet);
	}

	public void blockEnter(int blockId, int incomings, int lineNo, long timeStamp) {
		BlockEnter blockEnter = new BlockEnter(blockId, incomings, lineNo, id, timeStamp);
		if (curMethodExecution != null) curMethodExecution.addStatement(blockEnter);
	}
	
	public void traverseMethodExecutionsBackward(IMethodExecutionVisitor visitor) {
		visitor.preVisitThread(this);
		for (int i = 0; i < roots.size(); i++) {
			MethodExecution root = roots.get(i);
			root.traverseMethodExecutionsBackward(visitor);
		}
		visitor.postVisitThread(this);				
	}

	public void traverseMarkedMethodExecutions(IMethodExecutionVisitor visitor, long markStart, long markEnd) {
		visitor.preVisitThread(this);
		for (int i = 0; i < roots.size(); i++) {
			MethodExecution root = roots.get(i);
			if (root.getEntryTime() <= markEnd) {
				root.traverseMarkedMethodExecutions(visitor, markStart, markEnd);
			} else {
				break;
			}
		}
		visitor.postVisitThread(this);				
	}

	public void getUnmarkedMethodSignatures(HashSet<String> signatures, long markStart, long markEnd) {
		for (int i = 0; i < roots.size(); i++) {
			MethodExecution root = roots.get(i);
			root.getUnmarkedMethodSignatures(signatures, markStart, markEnd);
		}
	}

	public void getUnmarkedMethodExecutions(HashMap<String, ArrayList<MethodExecution>> executions, long markStart, long markEnd) {
		for (int i = 0; i < roots.size(); i++) {
			MethodExecution root = roots.get(i);
			root.getUnmarkedMethodExecutions(executions, markStart, markEnd);
		}
	}
	
	public MethodExecution getCurrentMethodExecution() {
		return curMethodExecution;
	}
	
	public TracePoint getCurrentTracePoint() {
		return new TracePoint(curMethodExecution, curMethodExecution.getStatements().size() - 1);
	}
	
	public TracePoint getLastTracePoint() {
		return roots.get(roots.size() - 1).getExitPoint();		
	}
}
