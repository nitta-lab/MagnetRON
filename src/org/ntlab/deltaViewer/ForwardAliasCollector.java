package org.ntlab.deltaViewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.ntlab.deltaExtractor.Alias;
import org.ntlab.deltaExtractor.Alias.AliasType;
import org.ntlab.deltaExtractor.IAliasCollector;
import org.ntlab.trace.ArrayAccess;
import org.ntlab.trace.ArrayCreate;
import org.ntlab.trace.ArrayUpdate;
import org.ntlab.trace.FieldAccess;
import org.ntlab.trace.FieldUpdate;
import org.ntlab.trace.IStatementVisitor;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.AbstractTracePointVisitor;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.ObjectReference;
import org.ntlab.trace.Reference;
import org.ntlab.trace.Statement;
import org.ntlab.trace.TracePoint;

public class ForwardAliasCollector extends AbstractTracePointVisitor implements IAliasCollector {
	private List<Alias> aliasList = new ArrayList<>();
	private FullObjectCallGraph objectCallGraph;
	private TracePoint firstTracePoint = null;
	
	public ForwardAliasCollector() {
		objectCallGraph = new FullObjectCallGraph();
	}

	@Override
	public boolean preVisitStatement(Statement statement, TracePoint tp) {
		if (firstTracePoint == null) {
			objectCallGraph.addStartPoint(tp.getMethodExecution());
			firstTracePoint = tp.duplicate();
			// for MagnetRON
			Alias thisAlias = new Alias(AliasType.THIS, 0, tp.getMethodExecution().getThisObjId(), tp.duplicate());
			addAlias(thisAlias);			
		}
		if (statement instanceof MethodInvocation) {
			MethodInvocation mi = (MethodInvocation) statement;
			String methodSignature = mi.getCalledMethodExecution().getSignature();
			if (!methodSignature.contains("<clinit>")) {
				// Not a class initializer.
				if (!mi.getCalledMethodExecution().isConstructor()) {
					String receiver = mi.getCalledMethodExecution().getThisObjId();
					Alias recerverAlias = new Alias(AliasType.RECEIVER, 0, receiver, tp.duplicate());
					addAlias(recerverAlias);
				}
				int index = 0;
				for (ObjectReference arg: mi.getCalledMethodExecution().getArguments()) {
					Alias argAlias = new Alias(AliasType.ACTUAL_ARGUMENT, index, arg.getId(), tp.duplicate());
					addAlias(argAlias);
					index++;
				}
				if (mi.getCalledMethodExecution().isCollectionType()
						&& (methodSignature.contains("add(") 
								|| methodSignature.contains("set(") 
								|| methodSignature.contains("put(") 
								|| methodSignature.contains("push(") 
								|| methodSignature.contains("addElement("))) {
					objectCallGraph.addRelatedPoint(tp.duplicate());
				}
				index = 0;
				for (ObjectReference arg: mi.getCalledMethodExecution().getArguments()) {
					Alias argAlias = new Alias(AliasType.FORMAL_PARAMETER, index, arg.getId(), mi.getCalledMethodExecution().getEntryPoint());
					addAlias(argAlias);
					index++;
				}
			}
		} else if (statement instanceof FieldAccess) {
			FieldAccess fa = (FieldAccess) statement;
			if (fa.getContainerObjId().equals(fa.getThisObjId())) {
				Alias thisAlias = new Alias(AliasType.THIS, 0, fa.getThisObjId(), tp.duplicate());
				addAlias(thisAlias);
			} else {
				Alias containerAlias = new Alias(AliasType.CONTAINER, 0, fa.getContainerObjId(), tp.duplicate());
				addAlias(containerAlias);
			}
			Alias fieldAlias = new Alias(AliasType.FIELD, 0, fa.getValueObjId(), tp.duplicate());
			addAlias(fieldAlias);
			objectCallGraph.addReference(fa.getReference());
		} else if (statement instanceof FieldUpdate) {
			FieldUpdate fu = (FieldUpdate) statement;
			if (!fu.getValueClassName().equals("---")) {
				// Updated by a non-null value.
//				if (fu.getContainerObjId().equals(tp.getMethodExecution().getThisObjId())) {
//					Alias thisAlias = new Alias(AliasType.THIS, 0, tp.getMethodExecution().getThisObjId(), tp.duplicate());
//					addAlias(thisAlias);
//				} else {
//					Alias containerAlias = new Alias(AliasType.CONTAINER, 0, fu.getContainerObjId(), tp.duplicate());
//					addAlias(containerAlias);
//				}
				objectCallGraph.addRelatedPoint(tp.duplicate());
			}
		} else if (statement instanceof ArrayCreate) {
			ArrayCreate ac = (ArrayCreate) statement;
			Alias thisAlias = new Alias(AliasType.THIS, 0, tp.getMethodExecution().getThisObjId(), tp.duplicate());
			addAlias(thisAlias);
			Alias arrayCreateAlias = new Alias(AliasType.ARRAY_CREATE, 0, ac.getArrayObjectId(), tp.duplicate());
			addAlias(arrayCreateAlias);
			Reference ref = new Reference(tp.getMethodExecution().getThisObjId(), ac.getArrayObjectId(), tp.getMethodExecution().getThisClassName(), ac.getArrayClassName());
			ref.setCreation(true);
			objectCallGraph.addReference(ref);
		} else if (statement instanceof ArrayAccess) {
			ArrayAccess aa = (ArrayAccess) statement;
			Alias arrayAlias = new Alias(AliasType.ARRAY, 0, aa.getArrayObjectId(), tp.duplicate());
			addAlias(arrayAlias);
			Alias arrayElementAlias = new Alias(AliasType.ARRAY_ELEMENT, aa.getIndex(), aa.getValueObjectId(), tp.duplicate());
			addAlias(arrayElementAlias);
			Reference ref = new Reference(aa.getArrayObjectId(), aa.getValueObjectId(), aa.getArrayClassName(), aa.getValueClassName());
			ref.setArray(true);
			objectCallGraph.addReference(ref);
		} else if (statement instanceof ArrayUpdate) {
			ArrayUpdate au = (ArrayUpdate) statement;			
			Alias arrayAlias = new Alias(AliasType.ARRAY, 0, au.getArrayObjectId(), tp.duplicate());
			addAlias(arrayAlias);
			objectCallGraph.addRelatedPoint(tp.duplicate());
		}
		return false;
	}

	@Override
	public boolean postVisitStatement(Statement statement, TracePoint tp) {
		if (statement instanceof MethodInvocation) {
			MethodInvocation mi = (MethodInvocation) statement;
			String returnValue = mi.getCalledMethodExecution().getReturnValue().getId();
			String methodSignature = mi.getCalledMethodExecution().getSignature();
			if (!methodSignature.contains("<clinit>")) {
				// Not a class initializer.
				if (!mi.getCalledMethodExecution().isConstructor()) {
					if (!mi.getCalledMethodExecution().getReturnValue().getActualType().equals("void")) {
						Alias returnAlias = new Alias(AliasType.RETURN_VALUE, 0, returnValue, mi.getCalledMethodExecution().getExitPoint());
						addAlias(returnAlias);
						Alias methodInvAlias = new Alias(AliasType.METHOD_INVOCATION, 0, returnValue, tp.duplicate());
						addAlias(methodInvAlias);
					}
					if (methodSignature.contains("List.get(") ||
							methodSignature.contains("Map.get(") ||
							methodSignature.contains(".next()") ||
							methodSignature.contains(".iterator()") ||
							methodSignature.contains(".listIterator()")) {
						String returnClass = mi.getCalledMethodExecution().getReturnValue().getActualType();
						Reference ref = new Reference(mi.getCalledMethodExecution().getThisObjId(), returnValue, mi.getCalledMethodExecution().getThisClassName(), returnClass);
						ref.setCollection(true);
						objectCallGraph.addReference(ref);
					}
				} else {
					Alias methodInvAlias = new Alias(AliasType.CONSTRACTOR_INVOCATION, 0, returnValue, tp.duplicate());
					addAlias(methodInvAlias);
					String returnClass = mi.getCalledMethodExecution().getReturnValue().getActualType();
					Reference ref = new Reference(tp.getMethodExecution().getThisObjId(), returnValue, tp.getMethodExecution().getThisClassName(), returnClass);
					ref.setCreation(true);
					objectCallGraph.addReference(ref);
				}
			}
		} else if (statement instanceof FieldAccess) {
			
		} else if (statement instanceof FieldUpdate) {
			
		} else if (statement instanceof ArrayCreate) {
			
		} else if (statement instanceof ArrayAccess) {
			
		} else if (statement instanceof ArrayUpdate) {
			
		}
		return false;
	}

	@Override
	public void addAlias(Alias alias) {
		aliasList.add(alias);
	}

	@Override
	public List<Alias> getAliasList() {
		return aliasList;
	}
	
	public IObjectCallGraph getObjectCallGraph() {
		return objectCallGraph;
	}
	
	private class FullObjectCallGraph implements IObjectCallGraph {
		private List<Reference> references = new ArrayList<>();
		private List<MethodExecution> startPoints = new ArrayList<>();
		private List<TracePoint> relatedPoints = new ArrayList<>();
		private SortedSet<Long> timeStamps = new TreeSet<>();		// List of time stamps of related points
		
		public void addReference(Reference r) {
			if (!references.contains(r)) references.add(r);
		}
		
		public void addStartPoint(MethodExecution me) {
			startPoints.add(me);
		}
		
		public void addRelatedPoint(TracePoint tp) {
			relatedPoints.add(tp);
			timeStamps.add(tp.getStatement().getTimeStamp());		// Will not work when a formal parameter is passed and related to this object.
		}

		@Override
		public List<Reference> getReferences() {
			return references;
		}

		@Override
		public List<MethodExecution> getStartPoints() {
			return startPoints;
		}

		@Override
		public List<TracePoint> getRelatedPoints() {
			return relatedPoints;
		}

		@Override
		public Map<MethodExecution, List<MethodExecution>> getCallTree() {
			return null;
		}

		@Override
		public SortedSet<Long> getTimeStamps() {
			return null;
		}
		
	}
}
