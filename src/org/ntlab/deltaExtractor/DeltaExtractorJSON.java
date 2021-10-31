package org.ntlab.deltaExtractor;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.ntlab.trace.ArrayAccess;
import org.ntlab.trace.ArrayCreate;
import org.ntlab.trace.FieldAccess;
import org.ntlab.trace.FieldUpdate;
import org.ntlab.trace.MethodExecution;
import org.ntlab.trace.MethodInvocation;
import org.ntlab.trace.ObjectReference;
import org.ntlab.trace.Reference;
import org.ntlab.trace.Statement;
import org.ntlab.trace.Trace;
import org.ntlab.trace.TraceJSON;
import org.ntlab.trace.TracePoint;

/**
 * デルタ抽出アルゴリズム(配列へのアクセスを検知できるJavassist版JSONトレースに対応し、アルゴリズムを単純化)
 * 
 * @author Nitta
 *
 */
public class DeltaExtractorJSON extends DeltaExtractor {
	public DeltaExtractorJSON(String traceFile) {
		super(new TraceJSON(traceFile));
	}

	public DeltaExtractorJSON(TraceJSON trace) {
		super(trace);
	}
	
	/**
	 * デルタ抽出アルゴリズムの呼び出し元探索部分（calleeSearchと相互再帰になっている）
	 * @param trace　解析対象トレース
	 * @param methodExecution 探索するメソッド実行
	 * @param objList　追跡中のオブジェクト
	 * @param child　直前に探索していた呼び出し先のメソッド実行
	 * @return 見つかったコーディネータ
	 * @throws TraceFileException
	 */
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
		ObjectReference thisObj = new ObjectReference(thisObjectId, methodExecution.getThisClassName(), 
				Trace.getDeclaringType(methodExecution.getSignature(), methodExecution.isConstructor()), Trace.getDeclaringType(methodExecution.getCallerSideSignature(), methodExecution.isConstructor()));
		
		HashMap<String, Alias>  aliasList = new HashMap<>();
				
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
				// オブジェクト内呼び出しのとき※1（※3の逆）
				aliasCollector.addAlias(new Alias(Alias.AliasType.RECEIVER, 0, childMethodExecution.getThisObjId(), tracePoint.duplicate()));
			} else if (!childMethodExecution.isConstructor()) {
				// オブジェクト間呼び出しで呼び出し先がコンストラクタでない場合※2（※3の逆）
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
					aliasCollector.addAlias(new Alias(Alias.AliasType.ACTUAL_ARGUMENT, -1, objId, tracePoint.duplicate())); // argIndexは不明
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
					// 呼び出し先がコンストラクタで、オブジェクト間呼び出しの時※3（※1、※2の逆）
					aliasList.put(childMethodExecution.getThisObjId(), new Alias(Alias.AliasType.RECEIVER, 0, childMethodExecution.getThisObjId(), tracePoint.duplicate()));
//					aliasList.put(childMethodExecution.getThisObjId(), new Alias(Alias.AliasType.CONSTRACTOR_INVOCATION, 0, childMethodExecution.getThisObjId(), tracePoint.duplicate()));
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
				aliasList.put(enclosingObj, new Alias(Alias.AliasType.FIELD, 0, enclosingObj, tracePoint.duplicate()));
			}
		}

		// 戻り値に探索対象が含まれていればcalleeSearchを再帰呼び出し
		while (tracePoint.stepBackOver()) {
			Statement statement = tracePoint.getStatement();
			// 直接参照、フィールド参照および配列アクセスの探索
			if (statement instanceof FieldAccess) {
				FieldAccess fs = (FieldAccess)statement;
				String refObjectId = fs.getValueObjId();
				int index = objList.indexOf(refObjectId);
				if (index != -1) {
					String ownerObjectId = fs.getContainerObjId();
										
					if (ownerObjectId.equals(thisObjectId)) {
						// フィールド参照の場合
						if (!removeList.contains(refObjectId)) {
							// 一番近いフィールド参照を優先する
							removeList.add(refObjectId);
							removeList.add(thisObjectId);		// 後で一旦、thisObject を取り除く
							aliasList.put(refObjectId, new Alias(Alias.AliasType.FIELD, 0, refObjectId, tracePoint.duplicate()));
							existsInFields++;					// setした後のgetを検出している可能性がある
						}
					} else {
						// 直接参照の場合
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
						aliasCollector.changeTrackingObject(refObjectId, ownerObjectId, isSrcSideChanged); //追跡対象オブジェクトの切り替え
						aliasCollector.addAlias(new Alias(Alias.AliasType.CONTAINER, 0, ownerObjectId, tracePoint.duplicate()));
					}
				}
			} else if (statement instanceof ArrayAccess) {
				ArrayAccess aa = (ArrayAccess)statement;
				String elementObjectId = aa.getValueObjectId();
				int index = objList.indexOf(elementObjectId);
				if (index != -1) {
					// 配列アクセスの場合
					boolean isSrcSideChanged = false;
					String arrayObjectId = aa.getArrayObjectId();
					if (elementObjectId.equals(srcObject.getId())) {
						eStructure.addSrcSide(new Reference(arrayObjectId, elementObjectId,
								aa.getArrayClassName(), srcObject.getActualType()));
						srcObject = new ObjectReference(arrayObjectId, aa.getArrayClassName());
						isSrcSideChanged = true;
					} else if(elementObjectId.equals(dstObject.getId())) {
						eStructure.addDstSide(new Reference(arrayObjectId, elementObjectId,
								aa.getArrayClassName(), dstObject.getActualType()));
						dstObject = new ObjectReference(arrayObjectId, aa.getArrayClassName());
						isSrcSideChanged = false;
					}
					objList.set(index, arrayObjectId);
					aliasCollector.addAlias(new Alias(Alias.AliasType.ARRAY_ELEMENT, aa.getIndex(), elementObjectId, tracePoint.duplicate()));
					aliasCollector.changeTrackingObject(elementObjectId, arrayObjectId, isSrcSideChanged); //追跡対象オブジェクトの切り替え
					aliasCollector.addAlias(new Alias(Alias.AliasType.ARRAY, 0, arrayObjectId, tracePoint.duplicate()));
				}
			} else if (statement instanceof ArrayCreate) {
				ArrayCreate ac = (ArrayCreate)statement;
				String arrayObjectId = ac.getArrayObjectId();
				int index = objList.indexOf(arrayObjectId);
				if (index != -1) {
					// 配列生成の場合fieldと同様に処理
					creationList.add(arrayObjectId);
					removeList.add(arrayObjectId);
					existsInFields++;
					removeList.add(thisObjectId);		// 後で一旦、thisObject を取り除く
					aliasList.put(arrayObjectId, new Alias(Alias.AliasType.ARRAY_CREATE, 0, arrayObjectId, tracePoint.duplicate()));
				}
			} else if (statement instanceof MethodInvocation) {
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
								((DeltaAugmentationInfo)prevChildMethodExecution.getAugmentation()).setTraceObjectId(Integer.parseInt(newObjId));		// 追跡対象
								((DeltaAugmentationInfo)prevChildMethodExecution.getAugmentation()).setSetterSide(false);	// getter呼び出しと同様
								aliasList.put(newObjId, new Alias(Alias.AliasType.CONSTRACTOR_INVOCATION, 0, newObjId, tracePoint.duplicate()));
								continue;
							}
							String retObj = objList.get(retIndex);
							aliasCollector.addAlias(new Alias(Alias.AliasType.METHOD_INVOCATION, 0, retObj, tracePoint.duplicate()));
							if (removeList.contains(retObj)) {
								// 一度getで検出してフィールドに依存していると判断したが本当の由来が戻り値だったことが判明したので、フィールドへの依存をキャンセルする
								removeList.remove(retObj);
								existsInFields--;
								if (existsInFields == 0) {
									removeList.remove(thisObjectId);
								}
							}
							((DeltaAugmentationInfo)prevChildMethodExecution.getAugmentation()).setTraceObjectId(Integer.parseInt(retObj));					// 追跡対象
							TracePoint prevChildTracePoint = tracePoint.duplicate();
							prevChildTracePoint.stepBackNoReturn();
							calleeSearch(trace, prevChildTracePoint, objList, prevChildMethodExecution.isStatic(), retIndex, aliasCollector);	// 呼び出し先を探索
							if (objList.get(retIndex) != null && objList.get(retIndex).equals(prevChildMethodExecution.getThisObjId())) {
								if ( thisObjectId.equals(prevChildMethodExecution.getThisObjId())) {
									// 呼び出し先でフィールドに依存していた場合の処理
									removeList.add(thisObjectId);		// 後で一旦、thisObject を取り除く
									isTrackingThis = true;				// 呼び出し元探索前に復活
								}
								aliasCollector.addAlias(new Alias(Alias.AliasType.RECEIVER, 0, objList.get(retIndex), tracePoint.duplicate()));
							} else if (objList.get(retIndex) == null) {
								// static 呼び出しだった場合
								removeList.add(thisObjectId);		// 後で一旦、thisObject を取り除く
								isTrackingThis = true;				// 呼び出し元探索前に復活
								aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId, tracePoint.duplicate()));
							}
							if (isLost) {
								checkList.add(objList.get(retIndex));
								isLost = false;
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
		
		// 引数とフィールドに同じIDのオブジェクトがある場合を想定
		Reference r;
		for (int i = 0; i < removeList.size(); i++) {
			String removeId = removeList.get(i);
			if (arguments.contains(new ObjectReference(removeId))) { 
				removeList.remove(removeId);	// フィールドと引数の両方に追跡対象が存在した場合、引数を優先(レアケース)
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
					} else if (removeId.equals(dstObject.getId())) {
						r = new Reference(thisObj, dstObject);
						r.setCreation(creationList.contains(removeId));		// オブジェクトの生成か?
						eStructure.addDstSide(r);
						dstObject = thisObj;
						isSrcSide = false;
					}
					aliasCollector.addAlias(aliasList.get(removeId));
					aliasCollector.changeTrackingObject(removeId, thisObjectId, isSrcSide);
					aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId, aliasList.get(removeId).getOccurrencePoint()));
				}
			}
		}
		// --- この時点で this が追跡対象であったとしても objList の中からいったん削除されている ---
		
		// 引数探索
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
			if (tracePoint.isValid()) {
				finalCount = 0;
				return callerSearch(trace, tracePoint, objList, methodExecution, aliasCollector);		// 呼び出し元をさらに探索				
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
						MethodExecution c = callerSearch(trace, tracePoint, objList, methodExecution, aliasCollector);		// 呼び出し元をさらに探索	
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
	 * @throws TraceFileException
	 */
	protected void calleeSearch(Trace trace, TracePoint tracePoint, ArrayList<String> objList, Boolean isStatic, int index, IAliasTracker aliasCollector) {
		MethodExecution methodExecution = tracePoint.getMethodExecution();
		Boolean isResolved = false;
		String objectId = objList.get(index);		// calleeSearch() では追跡対象のオブジェクトは一つだけ、※objListはindex番目の要素以外変更してはいけない
		String thisObjectId = methodExecution.getThisObjId();
		ObjectReference thisObj = new ObjectReference(thisObjectId, methodExecution.getThisClassName(), 
				Trace.getDeclaringType(methodExecution.getSignature(), methodExecution.isConstructor()), 
				Trace.getDeclaringType(methodExecution.getCallerSideSignature(), methodExecution.isConstructor()));
		
		((DeltaAugmentationInfo)methodExecution.getAugmentation()).setSetterSide(false);		// 基本的にgetter呼び出しのはずだが、注意
		ArrayList<ObjectReference> arguments = methodExecution.getArguments();
		ObjectReference trackingObj = null;

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
			// 戻り値に探索対象が含まれていればcalleeSearch呼び出し
			do {
				if (!tracePoint.isValid()) break;
				Statement statement = tracePoint.getStatement();
				// 直接参照およびフィールド参照の探索
				if (statement instanceof FieldAccess) {
					FieldAccess fs = (FieldAccess)statement;
					if (objectId != null && objectId.equals(fs.getValueObjId())) {						
						String ownerObjectId = fs.getContainerObjId();
						if (ownerObjectId.equals(thisObjectId)) {							
							// フィールド参照の場合
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
							// 直接参照の場合
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
					}
				} else if (statement instanceof ArrayAccess) {
					ArrayAccess aa = (ArrayAccess)statement;
					if (objectId != null && objectId.equals(aa.getValueObjectId())) {
						// 配列アクセスの場合
						boolean isSrcSideChanged = false;
						String arrayObjectId = aa.getArrayObjectId();
						if (objectId.equals(srcObject.getId())) {
							eStructure.addSrcSide(new Reference(arrayObjectId, objectId,
									aa.getArrayClassName(), srcObject.getActualType()));
							srcObject = new ObjectReference(arrayObjectId, aa.getArrayClassName());
							trackingObj = srcObject;
							isSrcSideChanged = true;
						} else if(objectId.equals(dstObject.getId())) {
							eStructure.addDstSide(new Reference(arrayObjectId, objectId,
									aa.getArrayClassName(), dstObject.getActualType()));
							dstObject = new ObjectReference(arrayObjectId, aa.getArrayClassName());
							trackingObj = dstObject;
							isSrcSideChanged = false;
						}
						aliasCollector.addAlias(new Alias(Alias.AliasType.ARRAY_ELEMENT, aa.getIndex(), objectId, tracePoint.duplicate()));
						aliasCollector.changeTrackingObject(objectId, arrayObjectId, isSrcSideChanged);
						aliasCollector.addAlias(new Alias(Alias.AliasType.ARRAY, 0, arrayObjectId, tracePoint.duplicate()));
						objectId = arrayObjectId;
						objList.set(index, objectId);
						isResolved = true;
					}
				} else if (statement instanceof ArrayCreate) {
					ArrayCreate ac = (ArrayCreate)statement;
					if (objectId != null && objectId.equals(ac.getArrayObjectId())) {
						// 配列生成の場合
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
						aliasCollector.addAlias(new Alias(Alias.AliasType.ARRAY_CREATE, 0, ac.getArrayObjectId(), tracePoint.duplicate()));
						aliasCollector.changeTrackingObject(ac.getArrayObjectId(), thisObjectId, isSrcSideChanged);
						aliasCollector.addAlias(new Alias(Alias.AliasType.THIS, 0, thisObjectId, tracePoint.duplicate()));
						if (Trace.isNull(thisObjectId)) objectId = null;	// static変数の場合
						else objectId = thisObjectId;
						objList.set(index, objectId);
					}
				} else if (statement instanceof MethodInvocation) {
					// 戻り値
					MethodExecution childMethodExecution = ((MethodInvocation)statement).getCalledMethodExecution();
					ObjectReference ret = childMethodExecution.getReturnValue();
					if (ret != null && objectId != null && objectId.equals(ret.getId())) {
						childMethodExecution.setAugmentation(new DeltaAugmentationInfo());
						((DeltaAugmentationInfo)childMethodExecution.getAugmentation()).setTraceObjectId(Integer.parseInt(objectId));
						TracePoint childTracePoint = tracePoint.duplicate();
						childTracePoint.stepBackNoReturn();
						if (!childMethodExecution.isConstructor()) {
							aliasCollector.addAlias(new Alias(Alias.AliasType.METHOD_INVOCATION, 0, ret.getId(), tracePoint.duplicate()));
							calleeSearch(trace, childTracePoint, objList, childMethodExecution.isStatic(), index, aliasCollector);		// 呼び出し先をさらに探索	
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
					}
				}
			} while (tracePoint.stepBackOver());
			
			//引数探索
			if (arguments.contains(new ObjectReference(objectId))) {
				((DeltaAugmentationInfo)methodExecution.getAugmentation()).setSetterSide(true);		// ※多分必要?
				isResolved = true;
				aliasCollector.addAlias(new Alias(Alias.AliasType.FORMAL_PARAMETER, arguments.indexOf(new ObjectReference(objectId)), objectId, methodExecution.getEntryPoint()));
			}
		}
		
		//コレクション型対応
		Reference r;
		if (methodExecution.isCollectionType()) {
			if (objectId != null) {
				if (methodExecution.getSignature().contains("Collections.unmodifiable") 
						|| methodExecution.getSignature().contains("Collections.checked") 
						|| methodExecution.getSignature().contains("Collections.synchronized") 
						|| methodExecution.getSignature().contains("Arrays.asList") 
						|| methodExecution.getSignature().contains("Arrays.copyOf")) {
					// 配列やコレクションの間の変換の場合、変換元の第一引数に依存する
					if (arguments.size() > 0) {
						if (objectId.equals(srcObject.getId())) {
							r = new Reference(arguments.get(0), srcObject);
							r.setCollection(true);
							r.setCreation(true);		// 戻り値オブジェクトを生成したとみなす
							eStructure.addSrcSide(r);
							srcObject = arguments.get(0);
							aliasCollector.changeTrackingObject(objectId, arguments.get(0).getId(), true);
							aliasCollector.addAlias(new Alias(Alias.AliasType.FORMAL_PARAMETER, 0, arguments.get(0).getId(), tracePoint.duplicate()));
						} else if(objectId.equals(dstObject.getId())) {
							r = new Reference(arguments.get(0), dstObject);
							r.setCollection(true);
							r.setCreation(true);		// 戻り値オブジェクトを生成したとみなす
							eStructure.addDstSide(r);
							dstObject =arguments.get(0);
							aliasCollector.changeTrackingObject(objectId, arguments.get(0).getId(), false);
							aliasCollector.addAlias(new Alias(Alias.AliasType.FORMAL_PARAMETER, 0, arguments.get(0).getId(), tracePoint.duplicate()));
						}
					}
					objList.set(index, arguments.get(0).getId());
				} else {
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
					objList.set(index, methodExecution.getThisObjId());
				}
			}
			isResolved = true;		// 必要なのでは?
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
}
