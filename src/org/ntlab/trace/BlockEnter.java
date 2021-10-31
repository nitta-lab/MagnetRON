package org.ntlab.trace;

public class BlockEnter extends Statement {
	private int blockId;
	private int incomings;

	public BlockEnter(int blockId, int incomings, int lineNo, String threadNo, long timeStamp) {
		super(lineNo, threadNo, timeStamp);
		this.blockId = blockId;
		this.incomings = incomings;
	}

	public int getBlockId() {
		return blockId;
	}

	public int getIncomings() {
		return incomings;
	}
}
