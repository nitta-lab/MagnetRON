package org.ntlab.deltaExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.ntlab.trace.FieldAccess;
import org.ntlab.trace.FieldUpdate;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.ObjectReference;
import org.ntlab.trace.Reference;
import org.ntlab.trace.Statement;
import org.ntlab.trace.Trace;
import org.ntlab.trace.TracePoint;

/**
 * デルタ抽出アルゴリズム(配列へのアクセスを推測する従来のバージョン)
 *    extract(...)メソッド群で抽出する。
 * 
 * @author Nitta
 *
 */
public class DeltaExtractor {
	protected static final int LOST_DECISION_EXTENSION = 0;		// 基本は 0 に設定。final変数の追跡アルゴリズムの不具合修正後は不要のはず。
	protected ArrayList<String> data = new ArrayList<String>();
	protected ArrayList<String> objList = new ArrayList<String>(2);
	protected ArrayList<String> methodList = new ArrayList<String>();
	protected ExtractedStructure eStructure = new ExtractedStructure();
	protected ObjectReference srcObject = null;
	protected ObjectReference dstObject = null;
	protected String returnValue;
	protected String threadNo;
	protected boolean isLost = false;
	protected ArrayList<String> checkList = new ArrayList<String>();
	protected Trace trace = null;
	protected int finalCount = 0;			// final変数を検出できない可能性があるので、由来の解決ができなかった場合でもしばらく追跡しつづける
	
	protected static final boolean DEBUG1 = true;
	protected static final boolean DEBUG2 = true;
	protected final IAliasTracker defaultAliasCollector = new IAliasTracker() {
		@Override
		public void changeTrackingObject(String from, String to, boolean isSrcSide) {
		}
		@Override
		public void addAlias(Alias alias) {
		}
		@Override
		public List<Alias> getAliasList() {
			// TODO Auto-generated method stub
			return null;
		}
	};
	
	public DeltaExtractor(String traceFile) {
		trace = new Trace(traceFile);
	}

	public DeltaExtractor(Trace trace) {
		this.trace = trace;
	}
	
//	public MethodExecution getMethodExecution(Reference createdReference, MethodExecution before) {
//		return trace.getMethodExecution(createdReference, before);
//	}
//	
//	public MethodExecution getMethodExecution(String methodSignature) {
//		return trace.getMethodExecution(methodSignature);
//	}
//	
//	public MethodExecution getMethodExecutionBackwardly(String methodSignature) {
//		return trace.getMethodExecutionBackwardly(methodSignature);
//	}
//	
//	public MethodExecution getCollectionTypeMethodExecution(Reference r, MethodExecution before) {
//		return trace.getCollectionTypeMethodExecution(r, before);
//	}
//	
//	public MethodExecution getArraySetMethodExecution(Reference r, MethodExecution before)  {
//		return trace.getArraySetMethodExecution(r, before);
//	}
//	
//	public CallTree getLastCallTree(ArrayList<Reference> refs, 
//			ArrayList<Reference> colls, 
//			ArrayList<Reference> arrys, 
//			int endLine, 
//			Reference[] lastRef) throws TraceFileException {
//		return trace.getLastCallTree(refs, colls, arrys, endLine, lastRef);
//	}

	/**
	 * デルタ抽出アルゴリズムの呼び出し元探索部分（calleeSearchと相互再帰になっている）
	 * @param trace　解析対象トレース
	 * @param methodExecution 探索するメソッド実行
	 * @param objList　追跡中のオブジェクト
	 * @param child　直前に探索していた呼び出し先のメソッド実行
	 * @return 見つかったコーディネータ
	 * @throws TraceFileException
	 */
	protected MethodExecution callerSearch(Trace trace, TracePoint tracePoint, ArrayList<String> objList, MethodExecution childMethodExecution) {
		return callerSearch(trace, tracePoint, objList, childMethodExecution, defaultAliasCollector);
	}

	/**
	 * デルタ抽出アルゴリズムの呼び出し元探索部分（calleeSearchと相互再帰になっている）
	 * @param trace　解析対象トレース
	 * @param methodExecution 探索を始めるメソッド実行
	 * @param objList　追跡中のオブジェクトのリスト
	 * @param child　直前に探索していた呼び出し先のメソッド実行
	 * @param aliasCollector エイリアスを収集するためのビジター
	 * @return 見つかったコーディネータ
	 */
	@Deprecated
	protected MethodExecution callerSearch(Trace trace, TracePoint tracePoint, ArrayList<String> objList, MethodExecution childMethodExecution, IAliasTracker aliasCollector) {
		MethodExecution methodExecution = tracePoint.getMethodExecution();
		methodExecution.setAugmentation(new DeltaAugmentationInfo());
		eStructure.createParent(methodExecution);
		String thisObjectId = methodExecution.getThisObjId();
		ArrayList<String> removeList = new ArrayList<String>();		// 追跡しているオブジェクト中で削除対象となっているもの
		ArrayList<String> creationList = new ArrayList<String>();	// このメソッド実行中に生成されたオブジェクト
		int existsInFields = 0;			// このメソッド実行内でフィールドに由来しているオブジェクトの数(1以上ならこのメソッド実行内でthisに依存)
		boolean isTrackingThis = false;	// 呼び出し先でthisに依存した
		boolean isSrcSide = true;		// 参照元か参照先のいずれの側のオブジェクトの由来をたどってthisオブジェクトに到達したか?
		ArrayList<ObjectReference> fieldArrays = new ArrayList<ObjectReference>();
		ArrayList<ObjectReference> fieldArrayElements = new ArrayList<ObjectReference>();
		ObjectReference thisObj = new ObjectReference(thisObjectId, methodExecution.getThisClassName(), 
				Trace.getDeclaringType(methodExecution.getSignature(), methodExecution.isConstructor()), Trace.getDeclaringType(methodExecution.getCallerSideSignature(), methodExecution.isConstructor()));
		
		HashMap<String, DeltaAlias>  srcAliasList = new HashMap<>();
		HashMap<String, DeltaAlias>  dstAliasList = new HashMap<>();
		
		if (childMethodExecution == null) {
			// 探索開始時は一旦削除し、呼び出し元の探索を続ける際に復活させる
			removeList.add(thisObjectId);		// 後で一旦、thisObject を取り除く
			isTrackingThis = true;				// 呼び出し元探索前に復活
		}
		
		if (childMethodExecution != null && objList.contains(childMethodExecution.getThisObjId())) {
			// 呼び出し先でthisに依存した
			if (thisObjectId.equals(childMethodExecution.getThisObjId())) {
				// オブジェクト内呼び出しのときのみ一旦削除し、呼び出し元の探索を続ける際に復活させる
				removeList.add(thisObjectId);		// 後で一旦、thisObject を取り除く
				isTrackingThis = true;				// 呼び出し元探索前に復活
				// オブジェクト内呼び出しの場合 [case 1]
				aliasCollector.addAlias(new Alias(Alias.AliasType.RECEIVER, 0, childMethodExecution.getThisObjId(), tracePoint.duplicate()));
			} else if (!childMethodExecution.isConstructor()) {
				// オブジェクト間の非コンストラクタ呼び出しの場合 [case 2]
				aliasCollector.addAlias(new Alias(Alias.AliasType.RECEIVER, 0, childMethodExecution.getThisObjId(), tracePoint.duplicate()));
			}
		}
		
		if (childMethodExecution != null && childMethodExecution.isStatic() && objList.contains(null)) {
			// staticな呼び出し先でthisに依存した
			removeList.add(thisObjectId);		// 後で一旦、thisObject を取り除く
			isTrackingThis = true;				// 呼び出し元探索前に復活
			if (methodExecution.isStatic()) {
				// 呼び出し元もstaticの場合
				aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, methodExecution.getThisObjId(), tracePoint.duplicate()));		// static呼び出しもエイリアスとして記録する
			}
		}
		
		if (childMethodExecution != null) {
			for (String objId : objList) {
				if (!objId.equals(childMethodExecution.getThisObjId())) {
					aliasCollector.addAlias(new Alias(Alias.AliasType.ACTUAL_ARGUMENT, -1, objId, tracePoint.duplicate())); // 引数番号がわからない
				}
			}
		}
		
		if (childMethodExecution != null && childMethodExecution.isConstructor()) {
			// 呼び出し先がコンストラクタだった場合
			int newIndex = objList.indexOf(childMethodExecution.getThisObjId());
			if (newIndex != -1) {
				// 呼び出し先が追跡対象のコンストラクタだったらfieldと同様に処理
				removeList.add(childMethodExecution.getThisObjId());
				existsInFields++;
				removeList.add(thisObjectId);		// 後で一旦、thisObject を取り除く
				if (!thisObjectId.equals(childMethodExecution.getThisObjId())) {
					//  オブジェクト間のコンストラクタ呼び出しの場合l [case 3]
					if (childMethodExecution.getThisObjId().equals(srcObject.getId())) {
						srcAliasList.put(childMethodExecution.getThisObjId(), new DeltaAlias(Alias.AliasType.RECEIVER, 0, childMethodExecution.getThisObjId(), tracePoint.duplicate(), true));
					} else if (childMethodExecution.getThisObjId().equals(dstObject.getId())) {
						dstAliasList.put(childMethodExecution.getThisObjId(), new DeltaAlias(Alias.AliasType.RECEIVER, 0, childMethodExecution.getThisObjId(), tracePoint.duplicate(), false));
					}
				}
			}
		}
		
		if (childMethodExecution != null && Trace.getMethodName(childMethodExecution.getSignature()).startsWith("access$")) {
			// エンクロージングインスタンスに対するメソッド呼び出しだった場合
			String enclosingObj = childMethodExecution.getArguments().get(0).getId();	// エンクロージングインスタンスは第一引数に入っているらしい
			int encIndex = objList.indexOf(enclosingObj);
			if (encIndex != -1) {
				// thisObject に置き換えた後、fieldと同様に処理
				removeList.add(enclosingObj);
				existsInFields++;
				removeList.add(thisObjectId);		// 後で一旦、thisObject を取り除く
				if (enclosingObj.equals(srcObject.getId())) {
					srcAliasList.put(enclosingObj, new DeltaAlias(Alias.AliasType.FIELD, 0, enclosingObj, tracePoint.duplicate(), true));
				} else if (enclosingObj.equals(dstObject.getId())) {
					dstAliasList.put(enclosingObj, new DeltaAlias(Alias.AliasType.FIELD, 0, enclosingObj, tracePoint.duplicate(), false));
				}
			}
		}
 
		// callerSearch のメインループ．現在のメソッドのステートメントの実行が実行と逆向きに探索される．
		// 戻り値に探索対象が含まれていればcalleeSearchを再帰呼び出し
		while (tracePoint.stepBackOver()) {
			Statement statement = tracePoint.getStatement();
			if (statement instanceof FieldAccess) {
				// 実行文がフィールド参照だった場合
				FieldAccess fs = (FieldAccess)statement;
				String refObjectId = fs.getValueObjId();
				int index = objList.indexOf(refObjectId);
				if (index != -1) {
					String ownerObjectId = fs.getContainerObjId();
					if (ownerObjectId.equals(thisObjectId)) {
						// 自分のフィールドの参照の場合
						removeList.add(refObjectId);
						removeList.add(thisObjectId);		// 後で一旦、thisObject を取り除く
						if (refObjectId.equals(srcObject.getId())) {
							srcAliasList.put(refObjectId, new DeltaAlias(Alias.AliasType.FIELD, 0, refObjectId, tracePoint.duplicate(), true));
						} else if (refObjectId.equals(dstObject.getId())) {
							dstAliasList.put(refObjectId, new DeltaAlias(Alias.AliasType.FIELD, 0, refObjectId, tracePoint.duplicate(), false));
						}
						existsInFields++;					// setした後のgetを検出している可能性がある
					} else {
						// 他のオブジェクトのフィールドの参照の場合
						boolean isSrcSideChanged = false;
						if (refObjectId.equals(srcObject.getId())) {
							eStructure.addSrcSide(new Reference(ownerObjectId, refObjectId,
									fs.getContainerClassName(), srcObject.getActualType()));
							srcObject = new ObjectReference(ownerObjectId, fs.getContainerClassName());
							isSrcSideChanged = true;
						} else if(refObjectId.equals(dstObject.getId())) {
							eStructure.addDstSide(new Reference(ownerObjectId, refObjectId,
									fs.getContainerClassName(), dstObject.getActualType()));
							dstObject = new ObjectReference(ownerObjectId, fs.getContainerClassName());
							isSrcSideChanged = false;
						}
						objList.set(index, ownerObjectId);
						aliasCollector.addAlias(new Alias(Alias.AliasType.FIELD, 0, refObjectId, tracePoint.duplicate()));
						aliasCollector.changeTrackingObject(refObjectId, ownerObjectId, isSrcSideChanged); // 追跡対象をフィールドによる参照先オブジェクトから参照元オブジェクト（コンテナ）に置き換え
						aliasCollector.addAlias(new Alias(Alias.AliasType.CONTAINER, 0, ownerObjectId, tracePoint.duplicate()));
					}
				} else {
					// 最終的にオブジェクトの由来が見つからなかった場合に、ここで参照した配列内部の要素に由来している可能性がある
					String refObjType = fs.getValueClassName();
					if (refObjType.startsWith("[L")) {
						// 参照したフィールドが配列の場合
						ObjectReference trackingObj = null;
						if ((srcObject.getActualType() != null && refObjType.endsWith(srcObject.getActualType() + ";"))
								|| (srcObject.getCalleeType() != null && refObjType.endsWith(srcObject.getCalleeType() + ";"))
								|| (srcObject.getCallerType() != null && refObjType.endsWith(srcObject.getCallerType() + ";"))) {
							trackingObj = srcObject;
						} else if ((dstObject.getActualType() != null && refObjType.endsWith(dstObject.getActualType() + ";")) 
								|| (dstObject.getCalleeType() != null && refObjType.endsWith(dstObject.getCalleeType() + ";"))
								|| (dstObject.getCallerType() != null && refObjType.endsWith(dstObject.getCallerType() + ";"))) {
							trackingObj = dstObject;
						}
						if (trackingObj != null) {
							// 追跡中のオブジェクトに、配列要素と同じ型を持つオブジェクトが存在する場合
							String ownerObjectId = fs.getContainerObjId();
							if (ownerObjectId.equals(thisObjectId)) {
								// フィールド参照の場合（他に由来の可能性がないとわかった時点で、この配列の要素に由来しているものと推測する。）
								fieldArrays.add(new ObjectReference(refObjectId, refObjType));
								fieldArrayElements.add(trackingObj);
								if (trackingObj.getId().equals(srcObject.getId())) {
									srcAliasList.put(trackingObj.getId(), new DeltaAlias(Alias.AliasType.ARRAY_ELEMENT, 0, trackingObj.getId(), tracePoint.duplicate(), true));
								} else if (trackingObj.getId().equals(dstObject.getId())) {
									dstAliasList.put(trackingObj.getId(), new DeltaAlias(Alias.AliasType.ARRAY_ELEMENT, 0, trackingObj.getId(), tracePoint.duplicate(), false));
								}
							} else {
								// 直接参照の場合(本当にこの配列の要素から取得されたものならここで追跡対象を置き換えるべきだが、
								// この時点で他の由来の可能性を排除できない。ここで追跡対象を置き換えてしまうと、後で別に由来があることがわかった場合に
								// やり直しが困難。)
							}
						}
					}
				}
			} else if (statement instanceof MethodInvocation) {
				// 実行文がメソッド呼び出しだった場合
				MethodExecution prevChildMethodExecution = ((MethodInvocation)statement).getCalledMethodExecution();
				if (!prevChildMethodExecution.equals(childMethodExecution)) {
					// 戻り値
					ObjectReference ret = prevChildMethodExecution.getReturnValue();
					if (ret != null) {
						int retIndex = -1;
						retIndex = objList.indexOf(ret.getId());
						if (retIndex != -1) {
							// 戻り値が由来だった
							prevChildMethodExecution.setAugmentation(new DeltaAugmentationInfo());
							if (prevChildMethodExecution.isConstructor()) {
								// 追跡対象のconstractorを呼んでいたら(オブジェクトの生成だったら)fieldと同様に処理
								String newObjId = ret.getId();
								creationList.add(newObjId);
								removeList.add(newObjId);
								existsInFields++;
								removeList.add(thisObjectId);		// 後で一旦、thisObject を取り除く
								((DeltaAugmentationInfo)prevChildMethodExecution.getAugmentation()).setTraceObjectId(Integer.parseInt(newObjId));	// 追跡対象
								((DeltaAugmentationInfo)prevChildMethodExecution.getAugmentation()).setSetterSide(false);	// 参照側の呼び出しと同様
								if (newObjId.equals(srcObject.getId())) {
									srcAliasList.put(newObjId, new DeltaAlias(Alias.AliasType.CONSTRACTOR_INVOCATION, 0, newObjId, tracePoint.duplicate(), true));
								} else if (newObjId.equals(dstObject.getId())) {
									dstAliasList.put(newObjId, new DeltaAlias(Alias.AliasType.CONSTRACTOR_INVOCATION, 0, newObjId, tracePoint.duplicate(), false));
								}
								continue;
							}
							String retObj = objList.get(retIndex);
							if (retObj.equals(srcObject.getId())) {
								isSrcSide = true;
							} else if (retObj.equals(dstObject.getId())) {
								isSrcSide = false;
							}
							aliasCollector.addAlias(new Alias(Alias.AliasType.METHOD_INVOCATION, 0, retObj, tracePoint.duplicate()));
							if (removeList.contains(retObj)) {
								// 一度getで検出してフィールドに依存していると判断したが本当の由来が戻り値だったことが判明したので、フィールドへの依存をキャンセルする
								removeList.remove(retObj);
								existsInFields--;
								if (existsInFields == 0) {
									removeList.remove(thisObjectId);
								}
							}
							((DeltaAugmentationInfo)prevChildMethodExecution.getAugmentation()).setTraceObjectId(Integer.parseInt(retObj));		// 追跡対象
							TracePoint prevChildTracePoint = tracePoint.duplicate();
							prevChildTracePoint.stepBackNoReturn();
							calleeSearch(trace, prevChildTracePoint, objList, prevChildMethodExecution.isStatic(), retIndex, aliasCollector);	// 呼び出し先メソッド実行の探索のため再帰呼び出し
							if (objList.get(retIndex) != null && objList.get(retIndex).equals(prevChildMethodExecution.getThisObjId())) { 
								if (thisObjectId.equals(prevChildMethodExecution.getThisObjId())) {
									// 呼び出し先でフィールドに依存していた場合の処理
									removeList.add(thisObjectId);		// 後で一旦、thisObject を取り除く
									isTrackingThis = true;				// 呼び出し元探索前に復活
								}
								if (isSrcSide) {
									aliasCollector.addAlias(new DeltaAlias(Alias.AliasType.RECEIVER, 0, objList.get(retIndex), tracePoint.duplicate(), true));
								} else {
									aliasCollector.addAlias(new DeltaAlias(Alias.AliasType.RECEIVER, 0, objList.get(retIndex), tracePoint.duplicate(), false));
								}
							} else if (objList.get(retIndex) == null) {
								// static 呼び出しだった場合
								removeList.add(thisObjectId);		// 後で一旦、thisObject を取り除く
								isTrackingThis = true;				// 呼び出し元探索前に復活
								if (isSrcSide) {
									aliasCollector.addAlias(new DeltaAlias(Alias.AliasType.THIS, 0, thisObjectId, tracePoint.duplicate(), true));
								} else {
									aliasCollector.addAlias(new DeltaAlias(Alias.AliasType.THIS, 0, thisObjectId, tracePoint.duplicate(), false));
								}
							}
							if (isLost) {
								checkList.add(objList.get(retIndex));
								isLost = false;
							}
						} else {
							// 最終的にオブジェクトの由来が見つからなかった場合に、この戻り値で取得した配列内部の要素に由来している可能性がある
							String retType = ret.getActualType();
							if (retType.startsWith("[L")) {
								// 戻り値が配列の場合
								if ((srcObject.getActualType() != null && retType.endsWith(srcObject.getActualType() + ";")) 
										|| (srcObject.getCalleeType() != null && retType.endsWith(srcObject.getCalleeType() + ";"))
										|| (srcObject.getCallerType() != null && retType.endsWith(srcObject.getCallerType() + ";"))) {
									retType = srcObject.getActualType();
								} else if ((dstObject.getActualType() != null && retType.endsWith(dstObject.getActualType() + ";"))
										|| (dstObject.getCalleeType() != null && retType.endsWith(dstObject.getCalleeType() + ";"))
										|| (dstObject.getCallerType() != null && retType.endsWith(dstObject.getCallerType() + ";"))) {
									retType = dstObject.getActualType();
								} else {
									retType = null;
								}
								if (retType != null) {
									// 本当にこの配列の要素から取得されたものならここで追跡対象を置き換えて、呼び出し先を探索すべきだが、
									// この時点で他の由来の可能性を排除できない。ここで追跡対象を置き換えてしまうと、後で別に由来があることがわかった場合に
									// やり直しが困難。
								}
							}
						}
					}
				}
			}
		}
		// --- この時点で tracePoint は呼び出し元を指している ---
		
		// コレクション型対応
		if (methodExecution.isCollectionType()) {
			objList.add(thisObjectId);
		}
 
		// 引数の取得
		ArrayList<ObjectReference> arguments = methodExecution.getArguments();
		
		// 引数とフィールドに同じIDのオブジェクトがある場合を想定（由来として引数とフィールドの両方が疑われる場合）
		Reference r;
		for (int i = 0; i < removeList.size(); i++) {
			String removeId = removeList.get(i);
			if (arguments.contains(new ObjectReference(removeId))) { 
				removeList.remove(removeId);	// フィールドと引数の両方に追跡対象が存在した場合、引数を優先
			} else if(objList.contains(removeId)) {
				// フィールドにしかなかった場合(ただし、オブジェクトの生成もフィールドと同様に扱う)
				objList.remove(removeId);		// 追跡対象から外す
				if (!removeId.equals(thisObjectId)) {
					// フィールド（this から removeId への参照）がデルタの構成要素になる
					if (removeId.equals(srcObject.getId())) {
						r = new Reference(thisObj, srcObject);
						r.setCreation(creationList.contains(removeId));		// オブジェクトの生成か?
						eStructure.addSrcSide(r);
						srcObject = thisObj;
						isSrcSide = true;
						if (srcAliasList.containsKey(removeId)) {
							aliasCollector.addAlias(srcAliasList.get(removeId));
							aliasCollector.changeTrackingObject(removeId, thisObjectId, isSrcSide);
							aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId, srcAliasList.get(removeId).getOccurrencePoint()));
							srcAliasList.remove(removeId);
						}
					} else if (removeId.equals(dstObject.getId())) {
						r = new Reference(thisObj, dstObject);
						r.setCreation(creationList.contains(removeId));		// オブジェクトの生成か?
						eStructure.addDstSide(r);
						dstObject = thisObj;
						isSrcSide = false;
						if (dstAliasList.containsKey(removeId)) {
							aliasCollector.addAlias(dstAliasList.get(removeId));
							aliasCollector.changeTrackingObject(removeId, thisObjectId, isSrcSide);
							aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId, dstAliasList.get(removeId).getOccurrencePoint()));
							dstAliasList.remove(removeId);
						}
					}					
				}
			}
		}
		// --- この時点で this が追跡対象であったとしても objList の中からいったん削除されている ---
		
		// 由来として引数を探索
		boolean existsInAnArgument = false;
		for (int i = 0; i < objList.size(); i++) {
			String objectId = objList.get(i);
			if (objectId != null) {
				ObjectReference trackingObj = new ObjectReference(objectId);
				if (arguments.contains(trackingObj)) {
					// 引数が由来だった
					existsInAnArgument = true;
					((DeltaAugmentationInfo)methodExecution.getAugmentation()).setTraceObjectId(Integer.parseInt(objectId));
					aliasCollector.addAlias(new Alias(Alias.AliasType.FORMAL_PARAMETER, arguments.indexOf(trackingObj), trackingObj.getId(), methodExecution.getEntryPoint()));					
				} else {
					// 由来がどこにも見つからなかった
					boolean isSrcSide2 = true;
					trackingObj = null;
					if (objectId.equals(srcObject.getId())) {
						isSrcSide2 = true;
						trackingObj = srcObject;
					} else if (objectId.equals(dstObject.getId())) {
						isSrcSide2 = false;
						trackingObj = dstObject;
					}
					if (trackingObj != null) {
						// まず配列引数の要素を由来として疑う(引数を優先)
						for (int j = 0; j < arguments.size(); j++) {
							ObjectReference argArray = arguments.get(j);
							if (argArray.getActualType().startsWith("[L") 
									&& (trackingObj.getActualType() != null && (argArray.getActualType().endsWith(trackingObj.getActualType() + ";"))
											|| (trackingObj.getCalleeType() != null && argArray.getActualType().endsWith(trackingObj.getCalleeType() + ";"))
											|| (trackingObj.getCallerType() != null && argArray.getActualType().endsWith(trackingObj.getCallerType() + ";")))) {
								// 型が一致したら配列引数の要素を由来とみなす
								existsInAnArgument = true;
								objList.remove(objectId);
								objList.add(argArray.getId());	// 追跡対象を配列要素から配列に置き換え
								((DeltaAugmentationInfo)methodExecution.getAugmentation()).setTraceObjectId(Integer.parseInt(argArray.getId()));
								r = new Reference(argArray.getId(), trackingObj.getId(), 
										argArray.getActualType(), trackingObj.getActualType());
								r.setArray(true);
								if (isSrcSide2) {
									eStructure.addSrcSide(r);
									srcObject = new ObjectReference(argArray.getId(), argArray.getActualType());
								} else {
									eStructure.addDstSide(r);
									dstObject = new ObjectReference(argArray.getId(), argArray.getActualType());
								}
								objectId = null;
								aliasCollector.addAlias(new Alias(Alias.AliasType.ARRAY_ELEMENT, 0, trackingObj.getId(), methodExecution.getEntryPoint()));	// 配列要素はメソッドの先頭で取得されたと仮定する
								aliasCollector.changeTrackingObject(trackingObj.getId(), argArray.getId(), isSrcSide2); // 追跡対象を配列要素から配列に置き換え
								aliasCollector.addAlias(new Alias(Alias.AliasType.ARRAY, 0, argArray.getId(), methodExecution.getEntryPoint()));		// 配列はメソッドの先頭でアクセスされたと仮定する
								aliasCollector.addAlias(new Alias(Alias.AliasType.FORMAL_PARAMETER, arguments.indexOf(argArray), trackingObj.getId(), methodExecution.getEntryPoint()));					
								break;
							}
						}
						if (objectId != null) {
							// 次に配列フィールドの要素を由来として疑う(フィールドは引数より後)
							int index = fieldArrayElements.indexOf(trackingObj);
							if (index != -1) {
								// 型が一致してるので配列フィールドの要素を由来とみなす
								ObjectReference fieldArray = fieldArrays.get(index);
								existsInFields++;
								objList.remove(objectId);
								r = new Reference(fieldArray.getId(), trackingObj.getId(),
										fieldArray.getActualType(), trackingObj.getActualType());
								r.setArray(true);
								if (isSrcSide2) {
									eStructure.addSrcSide(r);
									eStructure.addSrcSide(new Reference(thisObjectId, fieldArray.getId(),
											methodExecution.getThisClassName(), fieldArray.getActualType()));
									srcObject = thisObj;
									if (srcAliasList.containsKey(trackingObj.getId())) {
										aliasCollector.addAlias(srcAliasList.get(trackingObj.getId()));
										aliasCollector.changeTrackingObject(trackingObj.getId(), fieldArray.getId(), isSrcSide2); // 追跡対象を配列要素から配列フィールドに置き換え
										aliasCollector.addAlias(new Alias(Alias.AliasType.ARRAY, 0, fieldArray.getId(), srcAliasList.get(trackingObj.getId()).getOccurrencePoint()));
										aliasCollector.changeTrackingObject(fieldArray.getId(), thisObjectId, isSrcSide2); // 追跡対象を配列フィールドからthisオブジェクトに置き換え
										aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId, srcAliasList.get(trackingObj.getId()).getOccurrencePoint()));					
										srcAliasList.remove(trackingObj.getId());
									}
								} else {
									eStructure.addDstSide(r);
									eStructure.addDstSide(new Reference(thisObjectId, fieldArray.getId(),
											methodExecution.getThisClassName(), fieldArray.getActualType()));
									dstObject = thisObj;
									if (dstAliasList.containsKey(trackingObj.getId())) {
										aliasCollector.addAlias(dstAliasList.get(trackingObj.getId()));
										aliasCollector.changeTrackingObject(trackingObj.getId(), fieldArray.getId(), isSrcSide2); // 追跡対象を配列要素から配列フィールドに置き換え
										aliasCollector.addAlias(new Alias(Alias.AliasType.ARRAY, 0, fieldArray.getId(), dstAliasList.get(trackingObj.getId()).getOccurrencePoint()));
										aliasCollector.changeTrackingObject(fieldArray.getId(), thisObjectId, isSrcSide2); // 追跡対象を配列フィールドからthisオブジェクトに置き換え
										aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId, dstAliasList.get(trackingObj.getId()).getOccurrencePoint()));					
										dstAliasList.remove(trackingObj.getId());
									}
								}
							}
						}
						if (trackingObj.getActualType() != null && trackingObj.getActualType().startsWith("[L")) {
							// どこにも見つからなかった場合、探しているのが配列型ならば、このメソッド内で生成されたものと考える
							objList.remove(objectId);
							if (isSrcSide2) {
								eStructure.addSrcSide(new Reference(thisObjectId, trackingObj.getId(),
										methodExecution.getThisClassName(), trackingObj.getActualType()));
								srcObject = thisObj;
							} else {
								eStructure.addDstSide(new Reference(thisObjectId, trackingObj.getId(),
										methodExecution.getThisClassName(), trackingObj.getActualType()));
								dstObject = thisObj;
							}
							aliasCollector.addAlias(new Alias(Alias.AliasType.ARRAY_CREATE, 0, trackingObj.getId(), methodExecution.getEntryPoint()));	// 配列はメソッドの先頭で生成されたものと仮定する
							aliasCollector.changeTrackingObject(trackingObj.getId(), thisObjectId, isSrcSide2); // 追跡対象を配列からthisオブジェクトに置き換え
							aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId, methodExecution.getEntryPoint()));		// 配列はメソッドの先頭で生成されたものと仮定する
						}
					}
				}
			}
		}
		if (existsInAnArgument) {
			// 引数に1つでも追跡対象が存在した場合
			if (existsInFields > 0 || isTrackingThis) {
				// thisオブジェクトを追跡中の場合
				if (!Trace.isNull(thisObjectId)) {
					objList.add(thisObjectId);	// さらに探索する場合、一旦取り除いた thisObject を復活
				} else {
					objList.add(null);			// ただしstatic呼び出しだった場合、それ以上追跡しない
				}				
			}
//			if (existsInFields > 0) {
//				// フィールドを由来に持つオブジェクトが存在した場合
//				if (isSrcSide) {
//					srcObject = thisObj;
//				} else {
//					dstObject = thisObj;
//				}
//			}
			if (tracePoint.isValid()) {
				finalCount = 0;
				return callerSearch(trace, tracePoint, objList, methodExecution, aliasCollector);		// 呼び出し元をさらに探索するため再帰呼び出し
			}
		}
		
		for (int i = 0; i < objList.size(); i++) {
			objList.remove(null);
		}
		if (objList.isEmpty()) {
			((DeltaAugmentationInfo)methodExecution.getAugmentation()).setCoodinator(true);
		} else {
			// 由来を解決できなかった
			if (!methodExecution.isStatic()) {
				finalCount++;
				if (finalCount <= LOST_DECISION_EXTENSION) {
					// final変数を参照している場合由来を解決できない可能性があるので、追跡をすぐ終了せず猶予期間を設ける
					if (tracePoint.isValid()) {
						MethodExecution c = callerSearch(trace, tracePoint, objList, methodExecution, aliasCollector);		// 呼び出し元をさらに探索するため再帰呼び出し	
						if (((DeltaAugmentationInfo)c.getAugmentation()).isCoodinator()) {
							methodExecution = c;		// 追跡を続けた結果コーディネータが見つかった
						}
					}
				} else if (thisObj.getActualType().contains("$")) {
					// 自分が内部または無名クラスの場合、見失ったオブジェクトを外側メソッドの内のfinal変数から取得したとみなし、さらに自分の中のフィールドの一種とみなす
					for (int i = objList.size() - 1; i >= 0; i--) {
						String objectId = objList.get(i);
						if (objectId != null) {
							ObjectReference trackingObj = new ObjectReference(objectId);
							boolean isSrcSide2 = true;
							trackingObj = null;
							if (objectId.equals(srcObject.getId())) {
								isSrcSide2 = true;
								trackingObj = srcObject;
							} else if (objectId.equals(dstObject.getId())) {
								isSrcSide2 = false;
								trackingObj = dstObject;
							}
							if (trackingObj != null) {
								r = new Reference(thisObjectId, trackingObj.getId(),
										methodExecution.getThisClassName(), trackingObj.getActualType());
								r.setFinalLocal(true);
								if (isSrcSide2) {
									eStructure.addSrcSide(r);
									srcObject = thisObj;
									isSrcSide = true;
								} else {
									eStructure.addDstSide(r);
									dstObject = thisObj;
									isSrcSide = false;
								}
								existsInFields++;
								objList.remove(objectId);
								aliasCollector.addAlias(new Alias(Alias.AliasType.FIELD, 0, objectId,  methodExecution.getEntryPoint()));
								aliasCollector.changeTrackingObject(objectId, thisObjectId, isSrcSide2); // 追跡対象を final 変数が参照するオブジェクトから this に置き換え
								aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId,  methodExecution.getEntryPoint()));
							}
						}
					}
				}
			}
			((DeltaAugmentationInfo)methodExecution.getAugmentation()).setCoodinator(false);
		}
		finalCount = 0;
		return methodExecution;
	}
 
	/**
	 * デルタ抽出アルゴリズムの呼び出し先探索部分(再帰呼び出しになっている)
	 * @param trace 解析対象トレース
	 * @param methodExecution 探索するメソッド実行
	 * @param objList 追跡中のオブジェクト
	 * @param isStatic　静的メソッドか否か
	 * @param index　objList中のどのオブジェクトを追跡してこのメソッド実行に入ってきたのか
	 * @param aliasCollector エイリアスを収集するためのビジター
	 */
	@Deprecated
	protected void calleeSearch(Trace trace, TracePoint tracePoint, ArrayList<String> objList, Boolean isStatic, int index, IAliasTracker aliasCollector) {
		MethodExecution methodExecution = tracePoint.getMethodExecution();
		Boolean isResolved = false;
		String objectId = objList.get(index);		// calleeSearch() では追跡対象のオブジェクトは一つだけ、※objListはindex番目の要素以外変更してはいけない
		String thisObjectId = methodExecution.getThisObjId();
		ArrayList<ObjectReference> fieldArrays = new ArrayList<ObjectReference>();
		ArrayList<ObjectReference> fieldArrayElements = new ArrayList<ObjectReference>();
		ObjectReference thisObj = new ObjectReference(thisObjectId, methodExecution.getThisClassName(), 
				Trace.getDeclaringType(methodExecution.getSignature(), methodExecution.isConstructor()), 
				Trace.getDeclaringType(methodExecution.getCallerSideSignature(), methodExecution.isConstructor()));
		
		((DeltaAugmentationInfo)methodExecution.getAugmentation()).setSetterSide(false);		// 基本的に参照先側の呼び出しのはずだが、注意
		ArrayList<ObjectReference> arguments = methodExecution.getArguments();
		ObjectReference trackingObj = null;
		
		HashMap<String, DeltaAlias>  srcAliasList = new HashMap<>();
		HashMap<String, DeltaAlias>  dstAliasList = new HashMap<>();

		aliasCollector.addAlias(new Alias(Alias.AliasType.RETURN_VALUE, 0, objectId, tracePoint.duplicate()));
		//staticを経由するとnullが入っている時がある
		if (objectId != null) {
			String returnType = Trace.getReturnType(methodExecution.getSignature());
			if (objectId.equals(srcObject.getId())) {
				trackingObj = srcObject;
				trackingObj.setCalleeType(returnType);
			} else if(objectId.equals(dstObject.getId())) {
				trackingObj = dstObject;
				trackingObj.setCalleeType(returnType);
			} else {
				trackingObj = new ObjectReference(objectId, null, returnType);
			}
			
			Reference r;
			// calleeSearch のメインループ．現在のメソッドのステートメントの実行が実行と逆向きに探索される．
			// 戻り値に探索対象が含まれていればcalleeSearch呼び出し
			do {
				if (!tracePoint.isValid()) break;
				Statement statement = tracePoint.getStatement();
				if (statement instanceof FieldAccess) {
					// 実行文がフィールド参照だった場合
					FieldAccess fs = (FieldAccess)statement;
					if (objectId != null && objectId.equals(fs.getValueObjId())) {
						String ownerObjectId = fs.getContainerObjId();
						if (ownerObjectId.equals(thisObjectId)) {
							// 自分のフィールドの参照の場合
							boolean isSrcSideChanged = false;
							if (objectId.equals(srcObject.getId())) {
								eStructure.addSrcSide(new Reference(thisObj, srcObject));
								srcObject = thisObj;
								trackingObj = srcObject;
								isSrcSideChanged = true;
							} else if(objectId.equals(dstObject.getId())) {
								eStructure.addDstSide(new Reference(thisObj, dstObject));
								dstObject = thisObj;
								trackingObj = dstObject;
								isSrcSideChanged = false;
							}
							aliasCollector.addAlias(new Alias(Alias.AliasType.FIELD, 0, objectId, tracePoint.duplicate()));
							aliasCollector.changeTrackingObject(objectId, ownerObjectId, isSrcSideChanged);
							aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, ownerObjectId, tracePoint.duplicate()));
							if (Trace.isNull(thisObjectId)) objectId = null;	// static変数の場合
							else objectId = thisObjectId;
							objList.set(index, objectId);
						} else {
							// 他のオブジェクトのフィールドの参照の場合
							boolean isSrcSideChanged = false;
							if (objectId.equals(srcObject.getId())) {
								eStructure.addSrcSide(new Reference(ownerObjectId, objectId,
										fs.getContainerClassName(), srcObject.getActualType()));
								srcObject = new ObjectReference(ownerObjectId, fs.getContainerClassName());
								trackingObj = srcObject;
								isSrcSideChanged = true;
							} else if(objectId.equals(dstObject.getId())) {
								eStructure.addDstSide(new Reference(ownerObjectId, objectId,
										fs.getContainerClassName(), dstObject.getActualType()));
								dstObject = new ObjectReference(ownerObjectId, fs.getContainerClassName());
								trackingObj = dstObject;
								isSrcSideChanged = false;
							}
							aliasCollector.addAlias(new Alias(Alias.AliasType.FIELD, 0, objectId, tracePoint.duplicate()));
							aliasCollector.changeTrackingObject(objectId, ownerObjectId, isSrcSideChanged);
							aliasCollector.addAlias(new Alias(Alias.AliasType.CONTAINER, 0, ownerObjectId, tracePoint.duplicate()));
							if (Trace.isNull(ownerObjectId)) objectId = null;	// static変数の場合
							else objectId = ownerObjectId;
							objList.set(index, objectId);
						}
						isResolved = true;
					} else {
						// オブジェクトの由来が直接見つからなかった場合でも、いずれかの配列の要素に由来している可能性がある
						String refObjType = fs.getValueClassName();
						if (refObjType.startsWith("[L")) {
							// 参照したフィールドが配列の場合
							if ((trackingObj.getActualType() != null && refObjType.endsWith(trackingObj.getActualType() + ";")) 
									|| (trackingObj.getCalleeType() != null && refObjType.endsWith(trackingObj.getCalleeType() + ";"))
									|| (trackingObj.getCallerType() != null && refObjType.endsWith(trackingObj.getCallerType() + ";"))) {
								// 配列の要素の方が追跡中のオブジェクトの型と一致した場合
								String ownerObjectId = fs.getContainerObjId();
								if (ownerObjectId.equals(thisObjectId)) {
									// フィールド参照の場合（他に由来の可能性がないとわかった時点で、この配列の要素に由来しているものと推測する。）
									fieldArrays.add(new ObjectReference(fs.getValueObjId(), refObjType));
									fieldArrayElements.add(trackingObj);
									if (objectId.equals(srcObject.getId())) {
										srcAliasList.put(objectId, new DeltaAlias(Alias.AliasType.ARRAY_ELEMENT, 0, objectId, tracePoint.duplicate(), true));
									} else if(objectId.equals(dstObject.getId())) {
										dstAliasList.put(objectId, new DeltaAlias(Alias.AliasType.ARRAY_ELEMENT, 0, objectId, tracePoint.duplicate(), false));
									}
								} else {
									// 直接参照の場合(本当にこの配列の要素から取得されたものならここで追跡対象を置き換えるべきだが、
									// この時点で他の由来の可能性を排除できない。ここで追跡対象を置き換えてしまうと、後で別に由来があることがわかった場合に
									// やり直しが困難。)
								}
							}
						}
					}
				} else if (statement instanceof MethodInvocation) {
					// 実行文がメソッド呼び出しだった場合
					MethodExecution childMethodExecution = ((MethodInvocation)statement).getCalledMethodExecution();
					ObjectReference ret = childMethodExecution.getReturnValue();
					if (ret != null && objectId != null && objectId.equals(ret.getId())) {
						childMethodExecution.setAugmentation(new DeltaAugmentationInfo());
						((DeltaAugmentationInfo)childMethodExecution.getAugmentation()).setTraceObjectId(Integer.parseInt(objectId));
						TracePoint childTracePoint = tracePoint.duplicate();
						childTracePoint.stepBackNoReturn();
						if (!childMethodExecution.isConstructor()) {
							aliasCollector.addAlias(new Alias(Alias.AliasType.METHOD_INVOCATION, 0, ret.getId(), tracePoint.duplicate()));
							calleeSearch(trace, childTracePoint, objList, childMethodExecution.isStatic(), index, aliasCollector);		// 呼び出し先をさらに探索するため再帰呼び出し	
						} else {
							aliasCollector.addAlias(new Alias(Alias.AliasType.CONSTRACTOR_INVOCATION, 0, ret.getId(), tracePoint.duplicate()));
						}
						if (childMethodExecution.isConstructor()) {
							// コンストラクタ呼び出しだった場合
							if (objectId.equals(srcObject.getId())) {
								r = new Reference(thisObj, srcObject);
								r.setCreation(true);
								eStructure.addSrcSide(r);
								srcObject = thisObj;
								trackingObj = srcObject;
								aliasCollector.changeTrackingObject(objectId, thisObjectId, true);
								aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId, tracePoint.duplicate()));
							} else if (objectId.equals(dstObject.getId())) {
								r = new Reference(thisObj, dstObject);
								r.setCreation(true);
								eStructure.addDstSide(r);
								dstObject = thisObj;
								trackingObj = dstObject;
								aliasCollector.changeTrackingObject(objectId, thisObjectId, false);
								aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId, tracePoint.duplicate()));
							}
							if (Trace.isNull(thisObjectId)) objectId = null;	// 呼び出し元がstaticの場合
							else objectId = thisObjectId;
							objList.set(index, objectId);
							isResolved = true;
							isLost = false;
							continue;
						}
						objectId = objList.get(index);
						if (objectId == null) {
							// static呼び出しの戻り値だった場合
							trackingObj = null;
							isResolved = true;
							aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId, tracePoint.duplicate()));		// static呼び出しもエイリアスとして記録する 
						} else if (objectId.equals(srcObject.getId())) {
							trackingObj = srcObject;
						} else if (objectId.equals(dstObject.getId())) {
							trackingObj = dstObject;
						}
						if (isLost) {
							checkList.add(objList.get(index));
							isLost = false;
						}
						if (objectId != null) {
							if (childMethodExecution.getThisObjId().equals(objectId)) {
								aliasCollector.addAlias(new Alias(Alias.AliasType.RECEIVER, 0, objectId, tracePoint.duplicate()));
							}
						}						
					} else {
						// オブジェクトの由来が直接見つからなかった場合でも、どこかの配列の要素に由来している可能性がある
						String retType = ret.getActualType();
						if (retType.startsWith("[L")) {
							// 戻り値が配列の場合
							if ((trackingObj.getActualType() != null && retType.endsWith(trackingObj.getActualType() + ";"))
											|| (trackingObj.getCalleeType() != null && retType.endsWith(trackingObj.getCalleeType() + ";"))
											|| (trackingObj.getCallerType() != null && retType.endsWith(trackingObj.getCallerType() + ";"))) {
								// 本当にこの配列の要素から取得されたものならここで追跡対象を置き換えて、呼び出し先を探索すべきだが、
								// この時点で他の由来の可能性を排除できない。ここで追跡対象を置き換えてしまうと、後で別に由来があることがわかった場合に
								// やり直しが困難。
							}
						}
					}
				}
			} while (tracePoint.stepBackOver());
			
			// 引数探索
			if (arguments.contains(new ObjectReference(objectId))) {
				((DeltaAugmentationInfo)methodExecution.getAugmentation()).setSetterSide(true);		// ※多分必要?
				isResolved = true;
				aliasCollector.addAlias(new Alias(Alias.AliasType.FORMAL_PARAMETER, arguments.indexOf(new ObjectReference(objectId)), objectId, methodExecution.getEntryPoint()));
			}
		}
		
		// コレクション型対応
		Reference r;
		if (methodExecution.isCollectionType()) {
			if (objectId != null) {
				// コレクション型の場合、内部で個々の要素を直接保持していると仮定する
				if (objectId.equals(srcObject.getId())) {
					r = new Reference(thisObj, srcObject);
					r.setCollection(true);
					if (methodExecution.getSignature().contains(".iterator()")
							|| methodExecution.getSignature().contains(".listIterator()") 
							|| methodExecution.getSignature().contains(".entrySet()")
							|| methodExecution.getSignature().contains(".keySet()")
							|| methodExecution.getSignature().contains(".values()")) r.setCreation(true);		// イテレータオブジェクト等を生成したとみなす
					eStructure.addSrcSide(r);
					srcObject = thisObj;
					aliasCollector.changeTrackingObject(objectId, thisObjectId, true);
					aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId, tracePoint.duplicate()));
				} else if(objectId.equals(dstObject.getId())) {
					r = new Reference(thisObj, dstObject);
					r.setCollection(true);
					if (methodExecution.getSignature().contains(".iterator()")
							|| methodExecution.getSignature().contains(".listIterator()") 
							|| methodExecution.getSignature().contains(".entrySet()")
							|| methodExecution.getSignature().contains(".keySet()")
							|| methodExecution.getSignature().contains(".values()")) r.setCreation(true);		// イテレータオブジェクト等を生成したとみなす
					eStructure.addDstSide(r);
					dstObject =thisObj;
					aliasCollector.changeTrackingObject(objectId, thisObjectId, false);
					aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId, tracePoint.duplicate()));
				}
			}
			objList.set(index, methodExecution.getThisObjId());
			isResolved = true;		// 必要なのでは?
		}
		
		if (!isResolved && objectId != null) {
			// 由来がどこにも見つからなかった
			boolean isSrcSide = true;
			if (objectId.equals(srcObject.getId())) {
				isSrcSide = true;
			} else if (objectId.equals(dstObject.getId())) {
				isSrcSide = false;				
			}
			if (trackingObj != null) {
				// まず配列引数の要素を由来として疑う(引数が優先)
				for (int i = 0; i < arguments.size(); i++) {
					ObjectReference argArray = arguments.get(i);
					if (argArray.getActualType().startsWith("[L") 
							&& ((trackingObj.getActualType() != null && argArray.getActualType().endsWith(trackingObj.getActualType() + ";"))
									|| (trackingObj.getCalleeType() != null && argArray.getActualType().endsWith(trackingObj.getCalleeType() + ";"))
									|| (trackingObj.getCallerType() != null && argArray.getActualType().endsWith(trackingObj.getCallerType() + ";")))) {
						// 型が一致したら配列引数の要素を由来とみなす
						isResolved = true;
						objList.set(index, argArray.getId());	// 追跡対象を配列要素から配列に置き換え
						((DeltaAugmentationInfo)methodExecution.getAugmentation()).setTraceObjectId(Integer.parseInt(argArray.getId()));
						r = new Reference(argArray.getId(), trackingObj.getId(),
								argArray.getActualType(), trackingObj.getActualType());
						r.setArray(true);
						if (isSrcSide) {
							eStructure.addSrcSide(r);
							srcObject = new ObjectReference(argArray.getId(), argArray.getActualType());
						} else {
							eStructure.addDstSide(r);
							dstObject = new ObjectReference(argArray.getId(), argArray.getActualType());
						}
						objectId = null;
						aliasCollector.addAlias(new Alias(Alias.AliasType.ARRAY_ELEMENT, 0, trackingObj.getId(), methodExecution.getEntryPoint()));	// 配列要素はメソッドの先頭で取得されたものと仮定する
						aliasCollector.changeTrackingObject(trackingObj.getId(), argArray.getId(), isSrcSide); // 追跡対象を配列要素から配列に置き換え
						aliasCollector.addAlias(new Alias(Alias.AliasType.ARRAY, 0, argArray.getId(), methodExecution.getEntryPoint()));		// 配列はメソッドの先頭でアクセスされたものと仮定する
						aliasCollector.addAlias(new Alias(Alias.AliasType.FORMAL_PARAMETER, arguments.indexOf(argArray), trackingObj.getId(), methodExecution.getEntryPoint()));					
						break;
					}
				}
				if (objectId != null) {
					// 次に配列フィールドの要素を由来として疑う(フィールドは引数より後)
					int indArg = fieldArrayElements.indexOf(trackingObj);
					if (indArg != -1) {
						// 型が一致してるので配列フィールドの要素を由来とみなす
						isResolved = true;
						ObjectReference fieldArray = fieldArrays.get(indArg);
						objList.set(index, thisObjectId);	// 追跡対象をthisに置き換え
						r = new Reference(fieldArray.getId(), trackingObj.getId(),
								fieldArray.getActualType(), trackingObj.getActualType());
						r.setArray(true);
						if (isSrcSide) {
							eStructure.addSrcSide(r);
							eStructure.addSrcSide(new Reference(thisObjectId, fieldArray.getId(),
									methodExecution.getThisClassName(), fieldArray.getActualType()));
							srcObject = thisObj;
							aliasCollector.addAlias(srcAliasList.get(trackingObj.getId()));
							aliasCollector.changeTrackingObject(trackingObj.getId(), fieldArray.getId(), isSrcSide); // 追跡対象を配列要素から配列フィールドに置き換え
							aliasCollector.addAlias(new Alias(Alias.AliasType.ARRAY, 0, fieldArray.getId(), srcAliasList.get(trackingObj.getId()).getOccurrencePoint()));
							aliasCollector.changeTrackingObject(fieldArray.getId(), thisObjectId, isSrcSide); // 追跡対象を配列フィールドからthisオブジェクトに置き換え
							aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId, srcAliasList.get(trackingObj.getId()).getOccurrencePoint()));					
							srcAliasList.remove(trackingObj.getId());
						} else {
							eStructure.addDstSide(r);
							eStructure.addDstSide(new Reference(thisObjectId, fieldArray.getId(),
									methodExecution.getThisClassName(), fieldArray.getActualType()));
							dstObject = thisObj;
							aliasCollector.addAlias(dstAliasList.get(trackingObj.getId()));
							aliasCollector.changeTrackingObject(trackingObj.getId(), fieldArray.getId(), isSrcSide); // 追跡対象を配列要素から配列フィールドに置き換え
							aliasCollector.addAlias(new Alias(Alias.AliasType.ARRAY, 0, fieldArray.getId(), dstAliasList.get(trackingObj.getId()).getOccurrencePoint()));
							aliasCollector.changeTrackingObject(fieldArray.getId(), thisObjectId, isSrcSide); // 追跡対象を配列フィールドからthisオブジェクトに置き換え
							aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId, dstAliasList.get(trackingObj.getId()).getOccurrencePoint()));					
							dstAliasList.remove(trackingObj.getId());
						}
					}
				}
				if (trackingObj.getActualType() != null && trackingObj.getActualType().startsWith("[L")) {
					// どこにも見つからなかった場合、探しているのが配列型ならば、このメソッド内で生成されたものと考える
					isResolved = true;
					objList.set(index, thisObjectId);	// 追跡対象をthisに置き換え
					if (isSrcSide) {
						eStructure.addSrcSide(new Reference(thisObjectId, trackingObj.getId(),
								methodExecution.getThisClassName(), trackingObj.getActualType()));
						srcObject = thisObj;
					} else {
						eStructure.addDstSide(new Reference(thisObjectId, trackingObj.getId(),
								methodExecution.getThisClassName(), trackingObj.getActualType()));
						dstObject = thisObj;
					}
					aliasCollector.addAlias(new Alias(Alias.AliasType.ARRAY_CREATE, 0, trackingObj.getId(), methodExecution.getEntryPoint()));	// 配列はメソッドの先頭で生成されたものと仮定する
					aliasCollector.changeTrackingObject(trackingObj.getId(), thisObjectId, isSrcSide); // 追跡対象を配列からthisオブジェクトに置き換え
					aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId, methodExecution.getEntryPoint()));		// 配列はメソッドの先頭で生成されたものと仮定する
				}
			}
		}
		
		if (objectId == null && isResolved && !isStatic) {	// staticな呼び出し先からの戻り値を追跡した結果、呼び出し先でthisに依存したが、自分自身はstaticでない場合
			objList.set(index, thisObjectId);	// thisの追跡を復活させる
			if (Trace.isNull(srcObject.getId())) {
				srcObject = thisObj;
			} else if (Trace.isNull(dstObject.getId())) {
				dstObject = thisObj;
			}
		}
		
		if (isStatic && !isResolved) {		// 今は起こりえない?(getポイントカットを取得するようにしたため)
			objList.set(index, null);
		}
		if(!isStatic && !isResolved){
			isLost = true;					// final変数を内部クラスで参照している可能性もあるが、calleeSearch()は必ず呼び出し元に復帰していくので、ここでは何もしない
		}
	}
	
	/**
	 * 参照元オブジェクトと参照先オブジェクトを関連付けたデルタを、参照を指定して抽出する
	 * @param targetRef 対象となる参照
	 * @param before 探索開始トレースポイント(これより以前を探索)
	 * @return 抽出結果
	 */
	public ExtractedStructure extract(Reference targetRef, TracePoint before) {
		return extract(targetRef, before, defaultAliasCollector);
	}
	
	/**
	 * 参照元オブジェクトと参照先オブジェクトを関連付けたデルタを、参照を指定して抽出する
	 * @param targetRef 対象となる参照
	 * @param before 探索開始トレースポイント(これより以前を探索)
	 * @param aliasCollector デルタ抽出時に追跡したオブジェクトの全エイリアスを収集するリスナ
	 * @return 抽出結果
	 */
	public ExtractedStructure extract(Reference targetRef, TracePoint before, IAliasTracker aliasCollector) {
		TracePoint creationTracePoint;
		if (targetRef.isArray()) {
			// srcId の配列に dstId が代入されている可能性があるメソッド実行を取得（配列専用の処理）
			creationTracePoint = trace.getArraySetTracePoint(targetRef, before);					
		} else if (targetRef.isCollection()) {
			// srcId のコレクション型オブジェクトに dstId が渡されているメソッド実行を取得（コレクション型専用の処理）
			creationTracePoint = trace.getCollectionAddTracePoint(targetRef, before);
		} else if (targetRef.isFinalLocal()) {
			// srcId の内部または無名クラスのインスタンスに final local 変数に代入されている dstId の オブジェクトが渡された可能性があるメソッド実行を取得（final localの疑いがある場合の処理）
			creationTracePoint = trace.getCreationTracePoint(targetRef.getSrcObject(), before);
			targetRef = new Reference(creationTracePoint.getMethodExecution().getThisObjId(), targetRef.getDstObjectId(), creationTracePoint.getMethodExecution().getThisClassName(), targetRef.getDstClassName());	
		} else {
			// オブジェクト間参照 r が生成されたメソッド実行を取得（通常）
			creationTracePoint = trace.getFieldUpdateTracePoint(targetRef, before);
		}
		if (creationTracePoint == null) {
			return null;
		}
		return extractSub(creationTracePoint, targetRef, aliasCollector);
	}
	
	/**
	 * 参照元オブジェクトと参照先オブジェクトを関連付けたデルタを、オブジェクト間参照が生成されたトレースポイントを指定して抽出する
	 * @param creationTracePoint オブジェクト間参照生成トレースポイント(フィールドへの代入)
	 * @return 抽出結果
	 */
	public ExtractedStructure extract(TracePoint creationTracePoint) {
		creationTracePoint = creationTracePoint.duplicate();
		Statement statement = creationTracePoint.getStatement();
		if (statement instanceof FieldUpdate) {
			Reference targetRef = ((FieldUpdate)statement).getReference();
			return extractSub(creationTracePoint, targetRef, defaultAliasCollector);
		} else {
			return null;
		}
	}
	
	/**
	 * 参照元オブジェクトと参照先オブジェクトを関連付けたデルタを、オブジェクト間参照が生成されたトレースポイントを指定して抽出する
	 * @param creationTracePoint オブジェクト間参照生成トレースポイント(フィールドへの代入)
	 * @param aliasCollector デルタ抽出時に追跡したオブジェクトの全エイリアスを収集するリスナ
	 * @return 抽出結果
	 */
	public ExtractedStructure extract(TracePoint creationTracePoint, IAliasTracker aliasCollector) {
		creationTracePoint = creationTracePoint.duplicate();
		Statement statement = creationTracePoint.getStatement();
		if (statement instanceof FieldUpdate) {
			Reference targetRef = ((FieldUpdate)statement).getReference();
			return extractSub(creationTracePoint, targetRef, aliasCollector);
		} else {
			return null;
		}
	}

	private ExtractedStructure extractSub(TracePoint creationTracePoint, Reference targetRef, IAliasTracker aliasCollector) {
		eStructure = new ExtractedStructure();
		eStructure.setRelatedTracePoint(creationTracePoint.duplicate());
		ArrayList<String> objList = new ArrayList<String>(); 
		srcObject = targetRef.getSrcObject();
		dstObject = targetRef.getDstObject();
if (DEBUG1) {
		System.out.println("extract delta of:" + targetRef.getSrcObject().getActualType() + "(" + targetRef.getSrcObjectId() + ")" + " -> " + targetRef.getDstObject().getActualType()  + "(" + targetRef.getDstObjectId() + ")");
}
		if (!Trace.isNull(targetRef.getSrcObjectId())) {
			objList.add(targetRef.getSrcObjectId());
		} else {
			objList.add(null);
		}
		if (!Trace.isNull(targetRef.getDstObjectId())) {
			objList.add(targetRef.getDstObjectId());
		} else {
			objList.add(null);
		}
		return extractSub2(creationTracePoint, objList, aliasCollector);
	}
	
	/**
	 * 呼び出し元オブジェクトと呼び出し先オブジェクトを関連付けたデルタを、呼び出し先メソッド実行を指定して抽出する
	 * @param calledMethodExecution 呼び出し先メソッド実行
	 * @return　抽出結果
	 */
	public ExtractedStructure extract(MethodExecution calledMethodExecution) {
		return extract(calledMethodExecution, defaultAliasCollector);
	}	
	
	/**
	 * 呼び出し元オブジェクトと呼び出し先オブジェクトを関連付けたデルタを、呼び出し先メソッド実行を指定して抽出する
	 * @param calledMethodExecution 呼び出し先メソッド実行
	 * @param aliasCollector デルタ抽出時に追跡したオブジェクトの全エイリアスを収集するリスナ
	 * @return　抽出結果
	 */
	public ExtractedStructure extract(MethodExecution calledMethodExecution, IAliasTracker aliasCollector) {
		ObjectReference callee = new ObjectReference(calledMethodExecution.getThisObjId(), calledMethodExecution.getThisClassName());
		return extract(calledMethodExecution.getCallerTracePoint(), callee, aliasCollector);
	}

	/**
	 * 自分（thisオブジェクト）と自分がメソッド内で参照したオブジェクトを関連付けたデルタを抽出する
	 * @param thisTracePoint 参照が発生した時点
	 * @param anotherObj 参照したオブジェクト
	 * @return 抽出結果
	 */
	public ExtractedStructure extract(TracePoint thisTracePoint, ObjectReference anotherObj) {
		return extract(thisTracePoint, anotherObj, defaultAliasCollector);
	}

	/**
	 * 自分（thisオブジェクト）とメソッド内で参照されたオブジェクトを関連付けたデルタを抽出する
	 * @param thisTracePoint 参照が発生した時点
	 * @param anotherObj 参照したオブジェクト
	 * @param aliasCollector デルタ抽出時に追跡したオブジェクトの全エイリアスを収集するリスナ
	 * @return 抽出結果
	 */
	public ExtractedStructure extract(TracePoint thisTracePoint, ObjectReference anotherObj, IAliasTracker aliasCollector) {
		eStructure = new ExtractedStructure();
		eStructure.setRelatedTracePoint(thisTracePoint.duplicate());
		MethodExecution methodExecution = thisTracePoint.getMethodExecution();
		thisTracePoint.stepNext();
		ArrayList<String> objList = new ArrayList<String>();
		String thisObjectId = methodExecution.getThisObjId();
		objList.add(thisObjectId);
		objList.add(anotherObj.getId());
		srcObject = new ObjectReference(thisObjectId, methodExecution.getThisClassName(), 
				Trace.getDeclaringType(methodExecution.getSignature(), methodExecution.isConstructor()), Trace.getDeclaringType(methodExecution.getCallerSideSignature(), methodExecution.isConstructor()));
		dstObject = anotherObj;
if (DEBUG1) {
		System.out.println("extract delta of:" + methodExecution.getSignature() + " -> " + anotherObj.getActualType()  + "(" + anotherObj.getId() + ")");
}
		return extractSub2(thisTracePoint, objList, aliasCollector);
	}
	
	private ExtractedStructure extractSub2(TracePoint tracePoint, ArrayList<String> objList, IAliasTracker aliasCollector) {
		eStructure.setCreationMethodExecution(tracePoint.getMethodExecution());
		MethodExecution coordinator = callerSearch(trace, tracePoint, objList, null, aliasCollector);
		eStructure.setCoordinator(coordinator);
if (DEBUG2) {
		if (((DeltaAugmentationInfo)coordinator.getAugmentation()).isCoodinator()) {
			System.out.println("Coordinator");
		} else {
			System.out.println("Warning");
		}
		System.out.println("coordinator:" + coordinator.getSignature());
		System.out.println("srcSide:");
		for (int i = 0; i < eStructure.getDelta().getSrcSide().size(); i++) {
			Reference ref = eStructure.getDelta().getSrcSide().get(i);
			if (!ref.isCreation() || !ref.getSrcObjectId().equals(ref.getDstObjectId())) {
				System.out.println("\t" + ref.getSrcClassName() + "(" + ref.getSrcObjectId() + ")" + " -> " + ref.getDstClassName() + "(" + ref.getDstObjectId() + ")");
			}
		}
		System.out.println("dstSide:");
		for (int i = 0; i < eStructure.getDelta().getDstSide().size(); i++) {
			Reference ref = eStructure.getDelta().getDstSide().get(i);
			if (!ref.isCreation() || !ref.getSrcObjectId().equals(ref.getDstObjectId())) {
				System.out.println("\t" + ref.getSrcClassName() + "(" + ref.getSrcObjectId() + ")" + " -> " + ref.getDstClassName() + "(" + ref.getDstObjectId() + ")");
			}
		}
		System.out.println("overCoordinator:");
		MethodExecution parent = coordinator.getParent();
		while (parent != null) {
			System.out.println("\t" + parent.getSignature());
			parent = parent.getParent();
		}
}
		return eStructure;
	}
	
	/**
	 * 実際の参照元と参照先のオブジェクトを指定してデルタを抽出する(オンライン解析用)
	 * @param srcObj メモリ上にある参照元オブジェクト
	 * @param dstObj メモリ上にある参照先オブジェクト
	 * @param before 探索開始トレースポイント(これより以前を探索)
	 * @return　抽出結果
	 */
	public ExtractedStructure extract(Object srcObj, Object dstObj, TracePoint before) {
		return extract(srcObj, dstObj, before, defaultAliasCollector);
	}
	
	/**
	 * 実際の参照元と参照先のオブジェクトを指定してデルタを抽出する(オンライン解析用)
	 * @param srcObj メモリ上にある参照元オブジェクト
	 * @param dstObj メモリ上にある参照先オブジェクト
	 * @param before 探索開始トレースポイント(これより以前を探索)
	 * @param aliasCollector デルタ抽出時に追跡したオブジェクトの全エイリアスを収集するリスナ
	 * @return　抽出結果
	 */
	public ExtractedStructure extract(Object srcObj, Object dstObj, TracePoint before, IAliasTracker aliasCollector) {
		Reference targetRef = new Reference(Integer.toString(System.identityHashCode(srcObj)), Integer.toString(System.identityHashCode(dstObj)), null, null);
		return extract(targetRef, before, aliasCollector);
	}
	
	/**
	 * メソッド実行内のトレースポイントと実際の参照先オブジェクトを指定してデルタを抽出する(オンライン解析用)
	 * @param tracePoint メソッド実行内のトレースポイント
	 * @param arg メモリ上にある参照先オブジェクト(ローカル変数や引数による参照先)
	 * @return 抽出結果
	 */
	public ExtractedStructure extract(TracePoint tracePoint, Object arg) {
		return extract(tracePoint, arg, defaultAliasCollector);
	}
	
	/**
	 * メソッド実行内のトレースポイントと実際の参照先オブジェクトを指定してデルタを抽出する(オンライン解析用)
	 * @param tracePoint メソッド実行内のトレースポイント
	 * @param arg メモリ上にある参照先オブジェクト(ローカル変数や引数による参照先)
	 * @return 抽出結果
	 */
	public ExtractedStructure extract(TracePoint tracePoint, Object arg, IAliasTracker aliasCollector) {
		ObjectReference argObj = new ObjectReference(Integer.toString(System.identityHashCode(arg)));
		return extract(tracePoint, argObj, aliasCollector);
	}
	
	/**
	 * 指定した実際のスレッド上で現在実行中のメソッド実行を取得する(オンライン解析用)
	 * @param thread 現在実行中の対象スレッド
	 * @return thread 上で現在実行中のメソッド実行
	 */
	public MethodExecution getCurrentMethodExecution(Thread thread) {
		return trace.getCurrentMethodExecution(thread);
	}

	/**
	 * methodSignature に前方一致するメソッド名を持つメソッドの最後の実行
	 * @param methodSignature メソッド名(前方一致で検索する)
	 * @return 該当する最後のメソッド実行
	 */
	public MethodExecution getLastMethodExecution(String methodSignature) {
		return trace.getLastMethodExecution(methodSignature);
	}

	/**
	 * methodSignature に前方一致するメソッド名を持つメソッドの before 以前の最後の実行
	 * @param methodSignature メソッド名(前方一致で検索する)
	 * @param before　探索開始トレースポイント(これより以前を探索)
	 * @return　該当する最後のメソッド実行
	 */
	public MethodExecution getLastMethodExecution(String methodSignature, TracePoint before) {
		return trace.getLastMethodExecution(methodSignature, before);
	}

	public ArrayList<MethodExecution> getMethodExecutions(String methodSignature) {
		return trace.getMethodExecutions(methodSignature);
	}
		
//	public ExtractedStructure extract(MethodExecution caller, MethodExecution callee) {
//		eStructure = new ExtractedStructure();
//		ArrayList<String> objList = new ArrayList<String>();
//		String thisObjectId = caller.getThisObjId();
//		objList.add(thisObjectId);
//		objList.add(callee.getThisObjId());
//		srcObject = new ObjectReference(thisObjectId, caller.getThisClassName(), 
//				Trace.getDeclaringType(caller.getSignature(), caller.isConstractor()), Trace.getDeclaringType(caller.getCallerSideSignature(), caller.isConstractor()));
//		dstObject = new ObjectReference(callee.getThisObjId(), callee.getThisClassName(), 
//				Trace.getDeclaringType(callee.getSignature(), callee.isConstractor()), Trace.getDeclaringType(callee.getCallerSideSignature(), callee.isConstractor()));
//if (DEBUG1) {
//		System.out.println("extract delta of:" + caller.getSignature() + " -> " + callee.getSignature());
//}
//		
//		caller = new MethodExecution(caller);		// 解析用パラメータを初期化したものを使用する
//		eStructure.setCreationMethodExecution(caller);
//		MethodExecution coordinator = callerSearch(trace, caller, objList, null);
//		eStructure.setCoordinator(coordinator);
//if (DEBUG2) {
//		if (coordinator.isCoodinator()) {
//			System.out.println("Coordinator");
//		} else {
//			System.out.println("Warning");
//		}
//		System.out.println("coordinator:" + coordinator.getSignature());
//		System.out.println("srcSide:");
//		for (int i = 0; i < eStructure.getDelta().getSrcSide().size(); i++) {
//			Reference ref = eStructure.getDelta().getSrcSide().get(i);
//			if (!ref.isCreation() || !ref.getSrcObjectId().equals(ref.getDstObjectId())) {
//				System.out.println("\t" + ref.getSrcClassName() + "(" + ref.getSrcObjectId() + ")" + " -> " + ref.getDstClassName() + "(" + ref.getDstObjectId() + ")");
//			}
//		}
//		System.out.println("dstSide:");
//		for (int i = 0; i < eStructure.getDelta().getDstSide().size(); i++) {
//			Reference ref = eStructure.getDelta().getDstSide().get(i);
//			if (!ref.isCreation() || !ref.getSrcObjectId().equals(ref.getDstObjectId())) {
//				System.out.println("\t" + ref.getSrcClassName() + "(" + ref.getSrcObjectId() + ")" + " -> " + ref.getDstClassName() + "(" + ref.getDstObjectId() + ")");
//			}
//		}
//		System.out.println("overCoordinator:");
//		MethodExecution parent = coordinator.getParent();
//		while (parent != null) {
//			System.out.println("\t" + parent.getSignature());
//			parent = parent.getParent();
//		}
//}
//		return eStructure;	
//	}
//	
//	
//	/**
//	 * メソッドの引数としてオブジェクトを参照した場合のデルタを抽出する
//	 * @param caller 参照元のメソッド
//	 * @param argObj 引数として参照したオブジェクト
//	 * @return　抽出結果
//	 */
//	public ExtractedStructure extract(MethodExecution caller, ObjectReference argObj) {
//		eStructure = new ExtractedStructure();
//		ArrayList<String> objList = new ArrayList<String>();
//		String thisObjectId = caller.getThisObjId();
//		objList.add(thisObjectId);
//		objList.add(argObj.getId());
//		srcObject = new ObjectReference(thisObjectId, caller.getThisClassName(), 
//				Trace.getDeclaringType(caller.getSignature(), caller.isConstractor()), Trace.getDeclaringType(caller.getCallerSideSignature(), caller.isConstractor()));
//		dstObject = argObj;
//if (DEBUG1) {
//		System.out.println("extract delta of:" + caller.getSignature() + " -> " + argObj.getActualType()  + "(" + argObj.getId() + ")");
//}
//		
//		caller = new MethodExecution(caller);		// 解析用パラメータを初期化したものを使用する
//		eStructure.setCreationMethodExecution(caller);
//		MethodExecution coordinator = callerSearch(trace, caller, objList, null);
//		eStructure.setCoordinator(coordinator);
//if (DEBUG2) {
//		if (coordinator.isCoodinator()) {
//			System.out.println("Coordinator");
//		} else {
//			System.out.println("Warning");
//		}
//		System.out.println("coordinator:" + coordinator.getSignature());
//		System.out.println("srcSide:");
//		for (int i = 0; i < eStructure.getDelta().getSrcSide().size(); i++) {
//			Reference ref = eStructure.getDelta().getSrcSide().get(i);
//			if (!ref.isCreation() || !ref.getSrcObjectId().equals(ref.getDstObjectId())) {
//				System.out.println("\t" + ref.getSrcClassName() + "(" + ref.getSrcObjectId() + ")" + " -> " + ref.getDstClassName() + "(" + ref.getDstObjectId() + ")");
//			}
//		}
//		System.out.println("dstSide:");
//		for (int i = 0; i < eStructure.getDelta().getDstSide().size(); i++) {
//			Reference ref = eStructure.getDelta().getDstSide().get(i);
//			if (!ref.isCreation() || !ref.getSrcObjectId().equals(ref.getDstObjectId())) {
//				System.out.println("\t" + ref.getSrcClassName() + "(" + ref.getSrcObjectId() + ")" + " -> " + ref.getDstClassName() + "(" + ref.getDstObjectId() + ")");
//			}
//		}
//		System.out.println("overCoordinator:");
//		MethodExecution parent = coordinator.getParent();
//		while (parent != null) {
//			System.out.println("\t" + parent.getSignature());
//			parent = parent.getParent();
//		}
//}
//		return eStructure;	
//	}
}
