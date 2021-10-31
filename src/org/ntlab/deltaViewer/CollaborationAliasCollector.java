package org.ntlab.deltaViewer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.ntlab.deltaExtractor.Alias;
import org.ntlab.deltaExtractor.IAliasCollector;
import org.ntlab.featureExtractor.Extract;
import org.ntlab.trace.MethodExecution;

/**
 * CollaborationAliasCollector is IAliasCollector implementation class to merge aliasList in time stamp order.
 * 
 * @author Nitta Lab.
 */
public class CollaborationAliasCollector implements IAliasCollector {
	// Execution order.
	private List<Alias> aliasList = new ArrayList<>();

	public CollaborationAliasCollector(IAliasCollector dac) {
		aliasList.addAll(dac.getAliasList());
		aliasList = sortAliasListByTimeStamp(aliasList);
	}
	
	/*
	 * Don't write anything here.
	 */
	@Override
	public void addAlias(Alias alias) {
	}

	@Override
	public List<Alias> getAliasList() {
		return aliasList;
	}

	/**
	 * Merge other into aliasList(this) in time stamp order.
	 * @param otherÅ@IAliasCollector to be merged into the aliasList.
	 * @param extract 
	 */
	public void merge(IAliasCollector other, Extract extract) {
		List<Alias> otherAliasList = other.getAliasList();
		otherAliasList = sortAliasListByTimeStamp(otherAliasList);
		int otherIdx = 0; // Index in otherAliasList
		int thisIdx = 0; // Index in thisAliasList
		int thisOrgIdx = 0; // Index in the original thisAliasList
		while(otherIdx < otherAliasList.size()) {
			Alias otherAlias = otherAliasList.get(otherIdx);
			if (thisIdx >= aliasList.size()) {
				if (extract != null && extract.isToConnect() && otherIdx == 0) {
					Alias thisPrevAlias = aliasList.get(aliasList.size() - 1);
					if (!otherAlias.getMethodExecution().isStatic() && otherAlias.getMethodExecution().getCallerMethodExecution() != thisPrevAlias.getMethodExecution()) {
						// Add a dummy alias to connect disjunct call hierarchies. (thisPrevAlias -> otherAlias)
						MethodExecution caller = thisPrevAlias.getMethodExecution();
//						MethodExecution callee = new DummyMethodExecution(otherAlias.getMethodExecution());		// Currently does not work because this dummy and the original one will be mixed.
						MethodExecution callee = otherAlias.getMethodExecution();
						callee.setCaller(caller, caller.getStatements().indexOf(thisPrevAlias.getOccurrencePoint().getStatement()));
						DummyMethodInvocation dummyInv = new DummyMethodInvocation(callee, caller.getThisClassName(), caller.getThisObjId(), 0, thisPrevAlias.getOccurrencePoint().getStatement().getThreadNo());
						dummyInv.setTimeStamp(callee.getEntryTime());
						DummyTracePoint dummyTp = new DummyTracePoint(caller, dummyInv);
						aliasList.add(new Alias(Alias.AliasType.RECEIVER, 0, callee.getThisObjId(), dummyTp));
						thisIdx++;
					}
				}
				aliasList.add(otherAlias);
				otherIdx++; thisIdx++;
				continue;
			}
			
			Alias thisAlias = aliasList.get(thisIdx);
			if (otherAlias.equals(thisAlias)) {
				otherIdx++; thisIdx++; thisOrgIdx++;
			} else {
				long otherAliasTs = otherAlias.getTimeStamp();
				long thisAliasTs = thisAlias.getTimeStamp();
				
				if (otherAliasTs < thisAliasTs) {
					if (extract != null && extract.isToConnect() && otherIdx == 0 && thisIdx > 0) {
						Alias thisPrevAlias = aliasList.get(thisIdx - 1);
						if (!otherAlias.getMethodExecution().isStatic() && otherAlias.getMethodExecution().getCallerMethodExecution() != thisPrevAlias.getMethodExecution()) {
							// Add a dummy alias to connect disjunct call hierarchies. (thisPrevAlias -> otherAlias)
							MethodExecution caller = thisPrevAlias.getMethodExecution();
//							MethodExecution callee = new DummyMethodExecution(otherAlias.getMethodExecution());		// Currently does not work because this dummy and the original one will be mixed.
							MethodExecution callee = otherAlias.getMethodExecution();
							callee.setCaller(caller, caller.getStatements().indexOf(thisPrevAlias.getOccurrencePoint().getStatement()));
							DummyMethodInvocation dummyInv = new DummyMethodInvocation(callee, caller.getThisClassName(), caller.getThisObjId(), 0, thisPrevAlias.getOccurrencePoint().getStatement().getThreadNo());
							dummyInv.setTimeStamp(callee.getEntryTime());
							DummyTracePoint dummyTp = new DummyTracePoint(caller, dummyInv);
							aliasList.add(new Alias(Alias.AliasType.RECEIVER, 0, callee.getThisObjId(), dummyTp));
							thisIdx++;
						}
					}
					aliasList.add(thisIdx, otherAlias);
					otherIdx++; thisIdx++;
				} else if (otherAliasTs > thisAliasTs) {
					if (extract != null && extract.isToConnect() && thisOrgIdx == 0 && otherIdx > 0) {
						Alias otherPrevAlias = otherAliasList.get(otherIdx - 1);
						if (!thisAlias.getMethodExecution().isStatic() && thisAlias.getMethodExecution().getCallerMethodExecution() != otherPrevAlias.getMethodExecution()) {
							// Add a dummy alias to connect disjunct call hierarchies. (otherPrevAlias -> thisAlias)
							MethodExecution caller = otherPrevAlias.getMethodExecution();
//							MethodExecution callee = new DummyMethodExecution(thisAlias.getMethodExecution());		// Currently does not work because this dummy and the original one will be mixed.
							MethodExecution callee = thisAlias.getMethodExecution();
							callee.setCaller(caller, caller.getStatements().indexOf(otherPrevAlias.getOccurrencePoint().getStatement()));
							DummyMethodInvocation dummyInv = new DummyMethodInvocation(callee, caller.getThisClassName(), caller.getThisObjId(), 0, otherPrevAlias.getOccurrencePoint().getStatement().getThreadNo());
							dummyInv.setTimeStamp(callee.getEntryTime());
							DummyTracePoint dummyTp = new DummyTracePoint(caller, dummyInv);
							aliasList.add(new Alias(Alias.AliasType.RECEIVER, 0, callee.getThisObjId(), dummyTp));
							thisIdx++;
						}
					}
					thisIdx++; thisOrgIdx++;
				} else {
					if (aliasList.contains(otherAlias)) {
						otherIdx++;
					} else {
						// BUGÇ™Ç†ÇÈÇ©Ç‡ÇµÇÍÇ‹ÇπÇÒ(ACTUAL_ARGUMENTÇ∆RECEIVERÇÃèoåªèá)
						aliasList.add(thisIdx, otherAlias);
						otherIdx++; thisIdx++;
					}
				}
			}
		}
		if (extract != null && extract.isToConnect() && thisOrgIdx == 0 && otherIdx > 0 && thisIdx < aliasList.size()) {
			Alias thisAlias = aliasList.get(thisIdx);
			Alias otherPrevAlias = otherAliasList.get(otherIdx - 1);
			if (!thisAlias.getMethodExecution().isStatic() && thisAlias.getMethodExecution().getCallerMethodExecution() != otherPrevAlias.getMethodExecution()) {
				// Add a dummy alias to connect disjunct call hierarchies. (otherPrevAlias -> thisAlias)
				MethodExecution caller = otherPrevAlias.getMethodExecution();
//				MethodExecution callee = new DummyMethodExecution(thisAlias.getMethodExecution());		// Currently does not work because this dummy and the original one will be mixed.
				MethodExecution callee = thisAlias.getMethodExecution();
				callee.setCaller(caller, caller.getStatements().indexOf(otherPrevAlias.getOccurrencePoint().getStatement()));
				DummyMethodInvocation dummyInv = new DummyMethodInvocation(callee, caller.getThisClassName(), caller.getThisObjId(), 0, otherPrevAlias.getOccurrencePoint().getStatement().getThreadNo());
				dummyInv.setTimeStamp(callee.getEntryTime());
				DummyTracePoint dummyTp = new DummyTracePoint(caller, dummyInv);
				aliasList.add(new Alias(Alias.AliasType.RECEIVER, 0, callee.getThisObjId(), dummyTp));
			}
		}
	}
	
	/**
	 * Sort aliasList in time stamp order.
	 * @param aliasList AliasList to sort.
	 * @return Sorted AliasList.
	 */
	private List<Alias> sortAliasListByTimeStamp(List<Alias> aliasList) {
		List<Alias> cloneAliasList = new ArrayList<>(aliasList);
		List<Alias> sortedAliasList = cloneAliasList.stream().sorted(new Comparator<Alias>() {
	                @Override
	                public int compare(Alias alias1, Alias alias2) {
	                	if (alias1.getTimeStamp() > alias2.getTimeStamp()) return 1;
	                	else if (alias1.getTimeStamp() < alias2.getTimeStamp()) return -1;
	                	return 0;
	                }
	            }
	        ).collect(Collectors.toList());
		return sortedAliasList;
	}
}
