package org.ntlab.deltaViewer;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.Reference;
import org.ntlab.trace.TracePoint;

public interface IObjectCallGraph {
	
	List<Reference> getReferences();

	/*
	 * 起点となるメソッド実行(今後2つの起点にも拡張予定)
	 */
	List<MethodExecution> getStartPoints();

	List<TracePoint> getRelatedPoints();
	
	SortedSet<Long> getTimeStamps();

	Map<MethodExecution, List<MethodExecution>> getCallTree();
}
