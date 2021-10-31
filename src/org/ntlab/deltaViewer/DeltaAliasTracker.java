package org.ntlab.deltaViewer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ntlab.deltaExtractor.Alias;
import org.ntlab.deltaExtractor.IAliasTracker;
import org.ntlab.deltaExtractor.AliasPair;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;

/**
 * Collect delta aliases for MagnetRON. 
 * 
 * @author Nitta Lab.
 */
public class DeltaAliasTracker implements IAliasTracker {
	// Reverse execution order.
	private String srcObjId;
	private String dstObjId;
	private List<Alias> aliasList = new ArrayList<>();
	private List<AliasPair> aliasPairList = new ArrayList<>();

	private Alias curAlias = null;
	private AliasPair curAliasPair = null;

	public DeltaAliasTracker() {
	}

	/**
	 * @param srcObjId
	 * @param dstObjId
	 */
	public DeltaAliasTracker(String srcObjId, String dstObjId) {
		this.srcObjId = srcObjId;
		this.dstObjId = dstObjId;
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
		if (curAliasPair != null) {
			curAliasPair.setAliasPair(alias, curAlias);
			aliasPairList.add(curAliasPair);
			curAliasPair = null;
		}
		curAlias = alias;
		System.out.println(alias.getObjectId() + ", " + alias.getMethodSignature() + " l." + alias.getLineNo() + " : " + alias.getAliasType().toString());
	}

	@Override
	// from‚Ì•û‚ª‰ß‹Ž
	public void changeTrackingObject(String fromObjId, String toObjId, boolean isSrcSideChanged) {
		curAliasPair = new AliasPair(isSrcSideChanged);
		System.out.println(fromObjId + " -> " + toObjId + " " + isSrcSideChanged);
	}

	@Override
	public List<Alias> getAliasList() {
		return this.aliasList;
	}

	public List<AliasPair> getAliasPairList() {
		return aliasPairList;
	}

	public List<AliasPair> getAliasPairListByAlias(Alias toAlias) {
		List<AliasPair> aliasPairListByAlias = new ArrayList<>();
		for(AliasPair ap: aliasPairList) {
			if(ap.getAliasPair().getValue().equals(toAlias)) {
				aliasPairListByAlias.add(ap);
			}
		}
		return aliasPairListByAlias;
	}

	public List<AliasPair> getAliasPairListByAliasPair(Alias fromAlias, Alias toAlias) {
		List<AliasPair> aliasPairListByAlias = new ArrayList<>();
		for(AliasPair ap: aliasPairList) {
			if(ap.getAliasPair().getKey().equals(fromAlias) && ap.getAliasPair().getValue().equals(toAlias)) {
				aliasPairListByAlias.add(ap);
			}
		}
		return aliasPairListByAlias;
	}

}
