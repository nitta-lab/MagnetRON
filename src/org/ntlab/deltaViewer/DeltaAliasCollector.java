package org.ntlab.deltaViewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ntlab.deltaExtractor.Alias;
import org.ntlab.deltaExtractor.Alias.AliasType;
import org.ntlab.deltaExtractor.IAliasTracker;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.Statement;
import org.ntlab.trace.Trace;
import org.ntlab.trace.TracePoint;

/**
 * Collect delta alias for MagnetRON.(Derived from DeltaAliasTracker.)
 * 
 * @author Nitta Lab.
 */
public class DeltaAliasCollector implements IAliasTracker {
	// Reverse execution order.
	private List<Alias> aliasList = new ArrayList<>();

	public DeltaAliasCollector() {
	}

	@Override
	public void addAlias(Alias alias) {
		switch(alias.getAliasType()) {
		case FORMAL_PARAMETER:
			aliasList.add(0, alias);
			break;
		case THIS:
			aliasList.add(0, alias);
			break;
		case METHOD_INVOCATION:
			aliasList.add(0, alias);
			break;
		case CONSTRACTOR_INVOCATION:
			aliasList.add(0, alias);
			break;
		case FIELD:
			aliasList.add(0, alias);
			break;
		case ARRAY_ELEMENT:
			aliasList.add(0, alias);
			break;
		case ARRAY:
			aliasList.add(0, alias);
			break;
		case ACTUAL_ARGUMENT:
			aliasList.add(0, alias);
			break;
		case RECEIVER:
			aliasList.add(0, alias);
			if (alias.getOccurrencePoint().getStatement() instanceof MethodInvocation) {
				MethodExecution me = ((MethodInvocation) alias.getOccurrencePoint().getStatement()).getCalledMethodExecution();
			}
			break;
		case RETURN_VALUE:
			aliasList.add(0, alias);
			break;
		default:
			break;
		}
		System.out.println(alias.getObjectId() + ", " + alias.getMethodSignature() + " l." + alias.getLineNo() + " : " + alias.getAliasType().toString());
	}

	@Override
	public List<Alias> getAliasList() {
		return this.aliasList;
	}

	/*
	 * Don't write anything here.
	 */
	@Override
	public void changeTrackingObject(String from, String to, boolean isSrcSide) {
		
	}

	public Map<MethodExecution, Set<MethodExecution>> shrink() {
		List<Alias> oldAliasList = new ArrayList<>(aliasList);
		List<Alias> standardMethodInvocations = collectStandardMethodInvocations(aliasList);
		List<List<Alias>> invocationChains = collectInvocationChains(standardMethodInvocations);
		aliasList = replaceInvocationChains(aliasList, invocationChains);
		Map<MethodExecution, Set<MethodExecution>> newToOldMethodExecMap = collectNewToOldMethodExecutionMap(oldAliasList, aliasList);
		aliasList = removeEnclosingInstanceAccessor(aliasList, newToOldMethodExecMap);
		aliasList = replaceStaticObjectIds(aliasList);

		// for debug.
		System.out.println("standardMethodInvocations: ");
		for (Alias alias: standardMethodInvocations) {
			System.out.println(alias.getAliasType() + ":: " + alias.getObjectId() + ": " + alias.getMethodSignature());
		}
		System.out.println("invocationChains: ");
		for (int i = 0; i < invocationChains.size(); i++) {
			System.out.println("i = " + i);
			for (Alias alias: invocationChains.get(i)) {
				System.out.println("\t" + alias.getAliasType() + ":: " + alias.getObjectId() + ": " + alias.getMethodSignature());
			}
		}
		System.out.println("replaceInvocationChains: ");
		for (Alias alias: aliasList) {
			System.out.println(alias.getObjectId() + ": " + alias.getMethodSignature() + " l." + alias.getLineNo() + " :: " + alias.getAliasType().toString());
		}
		return newToOldMethodExecMap;
	}

	private List<Alias> collectStandardMethodInvocations(List<Alias> aliasList) {
		List<Alias> standardMethodInvocations = new ArrayList<>();
		List<Integer> standardMethodInvsIdx = new ArrayList<>();
		// Collect 1 set of RECEIVER, THIS RETURN_VALUE, METHOD_INVOCATION and it is CollectionType.
		for (int i = 0; i < aliasList.size(); i++) {
			Alias alias = aliasList.get(i);
			if (alias.getAliasType() == AliasType.RECEIVER) {
				Statement st = alias.getOccurrencePoint().getStatement();
				MethodInvocation methodInvocation = (MethodInvocation)st;
				if (methodInvocation.getCalledMethodExecution().isCollectionType()) {
					if (standardMethodInvsIdx.size() != 0) standardMethodInvsIdx.clear();
					standardMethodInvsIdx.add(i);
				}
			} else if (alias.getAliasType() == AliasType.THIS) {
				if (alias.getMethodExecution().isCollectionType() && standardMethodInvsIdx.size() == 1) {	
					standardMethodInvsIdx.add(i);
				}
			} else if (alias.getAliasType() == AliasType.RETURN_VALUE) {
				if (alias.getMethodExecution().isCollectionType() && standardMethodInvsIdx.size() == 2) {
					standardMethodInvsIdx.add(i);
				}
			} else if (alias.getAliasType() == AliasType.METHOD_INVOCATION) {
				Statement st = alias.getOccurrencePoint().getStatement();
				MethodInvocation methodInvocation = (MethodInvocation)st;
				if (methodInvocation.getCalledMethodExecution().isCollectionType() && standardMethodInvsIdx.size() == 3) {
					standardMethodInvsIdx.add(i);
					for (int index: standardMethodInvsIdx) {
						standardMethodInvocations.add(aliasList.get(index));
					}
					standardMethodInvsIdx.clear();
				}
			}
		}
		return standardMethodInvocations;
	}
	
	private List<List<Alias>> collectInvocationChains(List<Alias> standardMethodInvocations) {
		List<List<Alias>> invocationChains = new ArrayList<>();
		if (standardMethodInvocations.isEmpty()) return invocationChains;
		List<Integer> invChainsIdx = new ArrayList<>();
		// Compare whether same callerMethodExecution.
		MethodExecution compareMethodExec = null;
		for (int i = 0; i < standardMethodInvocations.size(); i++) {
			Alias standardMethodInv = standardMethodInvocations.get(i);
			MethodExecution methodExec = null;
			if (standardMethodInv.getAliasType() == AliasType.RECEIVER && invChainsIdx.size() == 0) {
				methodExec = standardMethodInv.getMethodExecution();
			} else if (standardMethodInv.getAliasType() == AliasType.THIS && invChainsIdx.size() == 1) {
				methodExec = standardMethodInv.getMethodExecution().getCallerMethodExecution();				
			} else if (standardMethodInv.getAliasType() == AliasType.RETURN_VALUE && invChainsIdx.size() == 2) {
				methodExec = standardMethodInv.getMethodExecution().getCallerMethodExecution();								
			} else if (standardMethodInv.getAliasType() == AliasType.METHOD_INVOCATION && invChainsIdx.size() == 3) {
				methodExec = standardMethodInv.getMethodExecution();				
			} else {
				invChainsIdx.clear();
				continue;
			}

			if (compareMethodExec == null) {
				compareMethodExec = methodExec;
				invocationChains.add(new ArrayList<>());
			} else {
				if (compareMethodExec != methodExec) {
					compareMethodExec = methodExec;
					invocationChains.add(new ArrayList<>());
				}
			}
			invChainsIdx.add(i);
			if (invChainsIdx.size() == 4) {
				for (int index: invChainsIdx) {
					invocationChains.get(invocationChains.size() - 1).add(standardMethodInvocations.get(index));
				}
				invChainsIdx.clear();
			}
		}

		// Compare whether same objectId from RETURN_VALUE to THIS.
		int i = 0;
		while (i < invocationChains.size()) {
			List<Alias> invChain = invocationChains.get(i);
			if (invChain.size() > 4) {
				int j = 2;
				String compareObjId = null;
				while (j < invChain.size() - (1 + 2)) {
					Alias pRetauVal = invChain.get(j);
					Alias pMethodInv = invChain.get(j + 1);
					Alias nReceiver = invChain.get(j + 2);
					Alias nThis = invChain.get(j + 3);
					compareObjId = pRetauVal.getObjectId();
					if (compareObjId.equals(pMethodInv.getObjectId()) 
							&& compareObjId.equals(nReceiver.getObjectId()) 
							&& compareObjId.equals(nThis.getObjectId())) {
						j += 4;
					} else {
						// Remove 1 set of from RECEIVER to METHOD_INVOCATION.
						for (int k = i - 2; k < i + 2; k++) {
							invChain.remove(k);
						}
						if (invChain.size() <= 4) {
							invocationChains.remove(i);
							i--;
						}
					}
				}
				i++;
			} else {
				invocationChains.remove(i);
			}
		}
		return invocationChains;	
	}
	
	private List<Alias> replaceInvocationChains(List<Alias> aliasList, List<List<Alias>> invocationChains) {
		List<Alias> replacedAliasList = new ArrayList<>(aliasList);
		if (invocationChains.isEmpty()) return replacedAliasList;
		for (List<Alias> invChain: invocationChains) {
			int firstIdx = replacedAliasList.indexOf(invChain.get(0)); // RECEIVER
			int secondIdx = replacedAliasList.indexOf(invChain.get(1)); // THIS
			int thirdIdx = replacedAliasList.indexOf(invChain.get(invChain.size() - 2)); // RETURN_VALUE
			int lastIdx = replacedAliasList.indexOf(invChain.get(invChain.size() - 1)); // METHOD_INVOCATION
			if(firstIdx != -1 && secondIdx != -1 && thirdIdx != -1 && lastIdx != -1) {
				Alias receiverAlias = replacedAliasList.get(firstIdx);
				Alias oldThisAlias = replacedAliasList.get(secondIdx);
				Alias oldReturnValAlias = replacedAliasList.get(thirdIdx);
	
				// Collect signature chains.
				StringBuilder sb = new StringBuilder();
				for (int i = 1; i < invChain.size(); i+=4) {
					if (i == 1) {
						sb.append(invChain.get(i).getMethodSignature());
					} else {
						String[] splitMethodSig = invChain.get(i).getMethodSignature().split("\\.");
						sb.append(".");
						sb.append(splitMethodSig[splitMethodSig.length - 1]);					
					}
				}
				String signatureChains = sb.toString();
				String callerSideSignature = signatureChains;
				String thisClassName = oldThisAlias.getMethodExecution().getThisClassName();
				long enterTime = oldThisAlias.getOccurrencePoint().getMethodExecution().getEntryTime();
				long exitTime = oldReturnValAlias.getOccurrencePoint().getMethodExecution().getExitTime();
	
				// Create new alias for THIS.
				String thisObjId = oldThisAlias.getObjectId();
				MethodExecution newCalledMethodExec = new MethodExecution(signatureChains, callerSideSignature, thisClassName, thisObjId, false, false, enterTime);
				newCalledMethodExec.setCollectionType(true);
				newCalledMethodExec.setExitTime(exitTime);
				newCalledMethodExec.setReturnValue(oldReturnValAlias.getOccurrencePoint().getMethodExecution().getReturnValue());
				TracePoint newThisTp = new TracePoint(newCalledMethodExec, -1);
				Alias newThisAlias = new Alias(AliasType.THIS, 0, thisObjId, newThisTp.duplicate());
	
				// Change called method execution of RECEIVER alias.
				// TODO Caution: The trace will be changed by the following code!
				TracePoint receiverTp = receiverAlias.getOccurrencePoint();
				Statement st = receiverTp.getStatement();
				MethodInvocation methodInvocation = (MethodInvocation)st;
				methodInvocation.setCalledMethodExecution(newCalledMethodExec);
				newCalledMethodExec.setCaller(receiverTp.getMethodExecution(), receiverTp.getMethodExecution().getStatements().indexOf(st));
	
				// Create new alias for RETURN_VALUE.
				String returnValObjId = oldReturnValAlias.getObjectId();
				TracePoint newReturnValTp = new TracePoint(newCalledMethodExec, -1);
				Alias newReturnValAlias = new Alias(AliasType.RETURN_VALUE, 0, returnValObjId, newReturnValTp.duplicate());
	
				// Create new alias for METHOD_INVOCATION.
				Alias newMethodInvAlias = new Alias(AliasType.METHOD_INVOCATION, 0, returnValObjId, receiverTp.duplicate());
				
				/* Replace InvocationChains */
				// Remove invocationChains from aliasList.
				for (int i = 1; i < invChain.size(); i++) { // Except first alias of THIS.
					Alias invAlias = invChain.get(i);
					int invAliasIdx = replacedAliasList.indexOf(invAlias); // Get index of invAlias in aliasList.
					if (invAliasIdx != - 1) replacedAliasList.remove(invAliasIdx);
					else System.out.println("Failed to remove invAlias in aliasList...");
				}
				// Add new Alias for THIS and RETURN_VALUE, METHOD_INVOCATION.
				replacedAliasList.add(secondIdx, newMethodInvAlias);
				replacedAliasList.add(secondIdx, newReturnValAlias);
				replacedAliasList.add(secondIdx, newThisAlias);
			} else {
				System.out.println("Failed to shrink aliasList...");
			}
		}
		return replacedAliasList;
	}
	
	/**
	 * Replace objectId of {@code Alias} of static object in aliasList.
	 * 
	 * @param aliasList
	 * @return
	 */
	private List<Alias> replaceStaticObjectIds(List<Alias> aliasList) {
		List<Alias> replacedAliasList = new ArrayList<>(aliasList);
		for (Alias alias: replacedAliasList) {
			if (alias.getObjectId().matches("0")) {
				alias.setObjectId(alias.getObjectId() + ":" + alias.getMethodExecution().getThisClassName());
			}
		}
		return replacedAliasList;
	}
	
	private List<Alias> removeEnclosingInstanceAccessor(List<Alias> aliasList, Map<MethodExecution, Set<MethodExecution>> newToOldMethodExecMap) {
		List<Alias> removedAliasList = new ArrayList<>(aliasList);
		MethodExecution caller = null;
		int callerStatement = 0;
		MethodExecution accessor = null;
		for (int i = 0; i < removedAliasList.size(); i++) {
			Alias a = removedAliasList.get(i);
			if (a.getAliasType() == Alias.AliasType.ACTUAL_ARGUMENT) {
				Statement st = a.getOccurrencePoint().getStatement();
				if (st instanceof MethodInvocation) {
					if (Trace.getMethodName(((MethodInvocation) st).getCalledMethodExecution().getSignature()).startsWith("access$")) {
						caller =  a.getMethodExecution();
						MethodInvocation mi = ((MethodInvocation) st);
						callerStatement = mi.getCalledMethodExecution().getCallerStatementExecution();
						accessor = mi.getCalledMethodExecution();						
						removedAliasList.remove(i);
						i--;
						continue;
					}
				}
			} else if (a.getAliasType() == Alias.AliasType.FORMAL_PARAMETER) {
				if (Trace.getMethodName(a.getMethodSignature()).startsWith("access$")) {
					removedAliasList.remove(i);
					i--;
					continue;
				}
			}
			if (a.getMethodExecution() == accessor) {
				Statement st = a.getOccurrencePoint().getStatement();
				if (a.getAliasType() == Alias.AliasType.ACTUAL_ARGUMENT) {
//					MethodExecution callee = new DummyMethodExecution(((MethodInvocation) st).getCalledMethodExecution());		// Currently does not work because this dummy and the original one will be mixed.
					MethodExecution callee = ((MethodInvocation) st).getCalledMethodExecution();
					callee.setCaller(caller, callerStatement);
					DummyMethodInvocation dummyInv = new DummyMethodInvocation(callee, caller.getThisClassName(), caller.getThisObjId(), 0, st.getThreadNo());
					dummyInv.setTimeStamp(callee.getEntryTime());
					DummyTracePoint dummyTp = new DummyTracePoint(caller, dummyInv);
					Alias newAlias = new Alias(Alias.AliasType.ACTUAL_ARGUMENT, a.getIndex(), a.getObjectId(), dummyTp);
					removedAliasList.set(i, newAlias);
				} else if (a.getAliasType() == Alias.AliasType.RECEIVER) {
//					MethodExecution callee = new DummyMethodExecution(((MethodInvocation) st).getCalledMethodExecution());		// Currently does not work because this dummy and the original one will be mixed.
					MethodExecution callee = ((MethodInvocation) st).getCalledMethodExecution();
					callee.setCaller(caller, callerStatement);
					DummyMethodInvocation dummyInv = new DummyMethodInvocation(callee, caller.getThisClassName(), caller.getThisObjId(), 0, st.getThreadNo());
					dummyInv.setTimeStamp(callee.getEntryTime());
					DummyTracePoint dummyTp = new DummyTracePoint(caller, dummyInv);
					Alias newAlias = new Alias(Alias.AliasType.RECEIVER, a.getIndex(), a.getObjectId(), dummyTp);
					removedAliasList.set(i, newAlias);
				}
			}
		}
		return removedAliasList;
	}
	
	/**
	 * Collect methodExecutions for Alias that are not equal in oldAliasList and newAliasList.
	 * @param oldAliasList
	 * @param newAliasList
	 * @return One-to-many, Key is new methodExecution in newAliasList, Values are set of old methodExecutions in oldAlias replaced by newAlias.
	 */
	private Map<MethodExecution, Set<MethodExecution>> collectNewToOldMethodExecutionMap(List<Alias> oldAliasList, List<Alias> newAliasList) {
		Map<MethodExecution, Set<MethodExecution>> newToOldMethodExecMap = new HashMap<>();
		int oldIdx = 0, newIdx = 0;
		Alias lastMatchedNextAlias = newAliasList.get(newIdx);
		for (oldIdx = 0; oldIdx < oldAliasList.size(); oldIdx++) {
			Alias oldAlias = oldAliasList.get(oldIdx);
			Alias newAlias = newAliasList.get(newIdx);
			if (oldAlias.equals(newAlias)) {
				if (newIdx + 1 < newAliasList.size()) newIdx++;
				lastMatchedNextAlias = newAliasList.get(newIdx);
			} else {
				MethodExecution oldMethodExec = oldAlias.getMethodExecution();
				MethodExecution newMethodExec = lastMatchedNextAlias.getMethodExecution();
				if (!newToOldMethodExecMap.containsKey(newMethodExec)) {
					newToOldMethodExecMap.put(newMethodExec, new HashSet<>());
					newToOldMethodExecMap.get(newMethodExec).add(oldMethodExec);
				} else {
					newToOldMethodExecMap.get(newMethodExec).add(oldMethodExec);
				}
				if (!oldAliasList.contains(newAlias) && newIdx + 1 < newAliasList.size()) newIdx++;
			}
		}
		return newToOldMethodExecMap;
	}

}
