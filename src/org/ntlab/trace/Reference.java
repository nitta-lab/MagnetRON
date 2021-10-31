package org.ntlab.trace;

public class Reference {
	private String id;
	private ObjectReference srcObj;			// �Q�ƌ��I�u�W�F�N�g
	private ObjectReference dstObj;			// �Q�Ɛ�I�u�W�F�N�g
	private boolean bCreation = false;		// �Q�ƌ��I�u�W�F�N�g���Q�Ɛ�I�u�W�F�N�g�𐶐�������?
	private boolean bArray = false;			// �Q�ƌ��I�u�W�F�N�g���z��ŎQ�Ɛ�I�u�W�F�N�g��v�f�Ƃ��ĕێ����Ă��邩?
	private boolean bCollection = false;	// �Q�ƌ��I�u�W�F�N�g���R���N�V�����ŎQ�Ɛ�I�u�W�F�N�g��v�f�Ƃ��ĕێ����Ă��邩?
	private boolean bFinalLocal = false;	// �Q�ƌ��I�u�W�F�N�g�������܂��͓����N���X�̂Ƃ��Q�Ɛ�I�u�W�F�N�g���O���N���X�� final local �ϐ��Ƃ��ĎQ�Ƃ��Ă��邩?

	public Reference(String srcObjectId, String dstObjectId, String srcClassName,
			String dstClassName) {
		srcObj = new ObjectReference(srcObjectId, srcClassName);
		dstObj = new ObjectReference(dstObjectId, dstClassName);
	}
	
	public Reference(ObjectReference srcObj, ObjectReference dstObj) {
		this.srcObj = srcObj;
		this.dstObj = dstObj;
	}
	
	public ObjectReference getSrcObject() {
		return srcObj;
	}
	
	public ObjectReference getDstObject() {
		return dstObj;
	}

	public void setSrcClassName(String srcClassName) {
		this.srcObj.setActualType(srcClassName);
	}

	public void setDstClassName(String dstClassName) {
		this.dstObj.setActualType(dstClassName);
	}

	public void setSrcObjectId(String srcObjectId) {
		this.srcObj.setId(srcObjectId);
	}

	public void setDstObjectId(String dstObjectId) {
		this.dstObj.setId(dstObjectId);
	}

	public String getSrcClassName() {
		return srcObj.getActualType();
	}

	public String getDstClassName() {
		return dstObj.getActualType();
	}

	public String getSrcObjectId() {
		return srcObj.getId();
	}

	public String getDstObjectId() {
		return dstObj.getId();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}	

	public void setCreation(boolean bCreation) {
		this.bCreation  = bCreation;
	}
	
	public boolean isCreation(){
		return bCreation;
	}

	public void setArray(boolean bArray) {
		this.bArray = bArray;
	}
	
	public boolean isArray() {
		return bArray;
	}
	
	public void setCollection(boolean bCollection) {
		this.bCollection = bCollection;
	}
	
	public boolean isCollection() {
		return bCollection;
	}
	
	public void setFinalLocal(boolean bFinalLocal) {
		this.bFinalLocal = bFinalLocal;
	}

	public boolean isFinalLocal() {
		return bFinalLocal;
	}
	
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o instanceof Reference) {
			if (this.hashCode() != o.hashCode()) return false;
			return (this.srcObj.getId().equals(((Reference)o).srcObj.getId()) && this.dstObj.getId().equals(((Reference)o).dstObj.getId()));
		}
		return false;
	}
	
	public int hashCode() {
		if (srcObj.getId().matches("[0-9]{1,}") && dstObj.getId().matches("[0-9]{1,}")) {
			return Integer.parseInt(srcObj.getId()) + Integer.parseInt(dstObj.getId());			
		}
		// If objectId contains string not only number.
		return srcObj.getId().hashCode() + dstObj.getId().hashCode();			
	}
	
	public String toString() {
		return srcObj.getId() + "(" + srcObj.getActualType() + ")" + "->" + dstObj.getId() + "(" + dstObj.getActualType() + ")";
	}
}
