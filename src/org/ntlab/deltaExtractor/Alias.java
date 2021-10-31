package org.ntlab.deltaExtractor;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.Statement;
import org.ntlab.trace.TracePoint;
/**
 * �I�u�W�F�N�g�̎Q�Ə��(�G�C���A�X)��\���N���X
 * @author Isitani
 *
 */
public class Alias {
	private String objectId;
	private TracePoint occurrencePoint; // ���Y�I�u�W�F�N�g�̎Q�Ƃ��s���Ă�����s�ӏ��ɑΉ�����TracePoint
	private AliasType aliasType;
	private int index;
	
	public enum AliasType {
		// ���\�b�h�ւ̓���
		FORMAL_PARAMETER, 
		THIS, 
		METHOD_INVOCATION, 
		CONSTRACTOR_INVOCATION, 
		
		// �ǐՃI�u�W�F�N�g�̐؂�ւ�
		FIELD, 
		CONTAINER, 
		ARRAY_ELEMENT, 
		ARRAY, 
		ARRAY_CREATE, 

		// ���\�b�h����̏o��
		ACTUAL_ARGUMENT, 
		RECEIVER, 
		RETURN_VALUE
	}
	
	public Alias(AliasType aliasType, int index, String objectId, TracePoint occurrencePoint) {
		this.aliasType = aliasType;
		this.index = index;
		this.objectId = objectId;
		this.occurrencePoint = occurrencePoint;
	}
	
	public AliasType getAliasType() {
		return aliasType;
	}
	
	public int getIndex() {
		return index;
	}
	
	public String getObjectId() {
		return objectId;
	}
	
	public TracePoint getOccurrencePoint() {
		return occurrencePoint;
	}
	
	public MethodExecution getMethodExecution() {
		return occurrencePoint.getMethodExecution();
	}
	
	public String getMethodSignature() {
		return occurrencePoint.getMethodExecution().getSignature();
	}

	/**
	 * Get time stamp of statement.
	 * @return
	 */
	public long getTimeStamp() {
		if (!occurrencePoint.isValid()) {
			return occurrencePoint.getMethodExecution().getEntryTime();
		}
		long stTimeStamp = occurrencePoint.getStatement().getTimeStamp();
		Statement st = occurrencePoint.getStatement();
		if (aliasType == AliasType.RETURN_VALUE) {
			stTimeStamp = occurrencePoint.getMethodExecution().getExitTime();
		} else if (aliasType == AliasType.METHOD_INVOCATION && st instanceof MethodInvocation) {
			stTimeStamp = ((MethodInvocation) st).getCalledMethodExecution().getExitTime();
		} else if (aliasType == AliasType.FORMAL_PARAMETER) {
			stTimeStamp = occurrencePoint.getMethodExecution().getEntryTime();			
		}
		return stTimeStamp;
	}

	public int getLineNo() {
		try {
			Statement statement = occurrencePoint.getStatement();
			return statement.getLineNo();
		} catch (Exception e) {
			return -1;
		}
	}
	
	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aliasType == null) ? 0 : aliasType.hashCode());
		result = prime * result + index;
		result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result + ((occurrencePoint == null) ? 0 : occurrencePoint.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Alias))
			return false;
		Alias other = (Alias) obj;
		if (aliasType != other.aliasType)
			return false;
		if (index != other.index)
			return false;
		if (objectId == null) {
			if (other.objectId != null)
				return false;
		} else if (!objectId.equals(other.objectId))
			return false;
		if (occurrencePoint == null) {
			if (other.occurrencePoint != null)
				return false;
		} else if (!occurrencePoint.equals(other.occurrencePoint))
			return false;
		return true;
	}
	
}