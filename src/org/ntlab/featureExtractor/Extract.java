package org.ntlab.featureExtractor;

public class Extract {
	// Delta Extract Type
	public static final String CONTAINER_COMPONENT = "Container-Component";
	public static final String CONTAINER_COMPONENT_COLLECTION = "Container-Component(Collection)";
	public static final String THIS_ANOTHER = "This-Another";

	private String srcId = null;
	private String srcClass = null;
	
	private String dstId = null;
	private String dstClass = null;
	
	private String type = null;
	
	private int order = 0;
	
	private boolean toConnect = false;
	
	public Extract(String srcId, String srcClass, String dstId, String dstClass, String type, int order, boolean toConnect) {
		this.srcId = srcId;
		this.srcClass = srcClass;
		this.dstId = dstId;
		this.dstClass = dstClass;
		this.type = type;
		this.order = order;
		this.toConnect = toConnect;
	}

	public String getSrcId() {
		return srcId;
	}

	public String getSrcClass() {
		return srcClass;
	}

	public String getDstId() {
		return dstId;
	}

	public String getDstClass() {
		return dstClass;
	}

	public String getType() {
		return type;
	}

	public int getOrder() {
		return order;
	}

	public boolean isToConnect() {
		return toConnect;
	}
}
