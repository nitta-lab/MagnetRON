package org.ntlab.trace;

import java.util.ArrayList;

public class TracePoint {
	private MethodExecution methodExecution;
	private int order = 0;
	
	public TracePoint(MethodExecution methodExecution, int order) {
		this.methodExecution = methodExecution;
		this.order = order;
	}
	
	public TracePoint duplicate() {
		return new TracePoint(methodExecution, order);
	}
	
	public Statement getStatement() {
		if (order < 0 || methodExecution.getStatements().size() <= order) return null;
		return methodExecution.getStatements().get(order);
	}
	
	public MethodExecution getMethodExecution() {
		return methodExecution;
	}
	
	public ArrayList<MethodExecution> getPreviouslyCalledMethods() {
		ArrayList<MethodExecution> children = new ArrayList<MethodExecution>();
		ArrayList<Statement> statements = methodExecution.getStatements();
		for (int i = 0; i < order; i++) {
			Statement statement = statements.get(i);
			if (statement instanceof MethodInvocation) {
				MethodExecution child = ((MethodInvocation)statement).getCalledMethodExecution();
				children.add(child);
			}
		}
		return children;
	}
	
	/**
	 * 順方向の全探索(ただし、実行文が記録されていないメソッド実行には潜らない)
	 * @return false: これ以上辿れない場合, true: それ以外
	 */
	public boolean stepFull() {
		if (getStatement() instanceof MethodInvocation) {
			MethodExecution calledMethodExecution = ((MethodInvocation)getStatement()).getCalledMethodExecution();
			if (calledMethodExecution.getStatements().size() > 0) {
				methodExecution = calledMethodExecution;
				order = 0;
				return true;
			}
		}
		while (order >= methodExecution.getStatements().size() - 1) {
			order = methodExecution.getCallerStatementExecution();
			methodExecution = methodExecution.getCallerMethodExecution();
			if (methodExecution == null) {
				order = -1;
				return false;
			}
		}
		order++;
		return true;
	}
	
	/**
	 * 逆方向の全探索(ただし、実行文が記録されていないメソッド実行には潜らない。また、呼び出し先のメソッド実行内を探索した後に、呼び出し元のメソッド呼び出し文を訪問するので注意!!)
	 * @return false: これ以上辿れない場合, true: それ以外
	 */
	public boolean stepBackFull() {
		if (order <= 0) {
			order = methodExecution.getCallerStatementExecution();
			methodExecution = methodExecution.getCallerMethodExecution();
			if (methodExecution == null) {
				order = -1;
				return false;
			}
			return true;
		}
		order--;
		while (getStatement() instanceof MethodInvocation) {
			MethodExecution calledMethodExecution = ((MethodInvocation)getStatement()).getCalledMethodExecution();
			if (calledMethodExecution.getStatements().size() == 0) break;
			methodExecution = calledMethodExecution;
			order = methodExecution.getStatements().size() - 1;
		}
		return true;
	}
	
	/**
	 * 順方向に探索する。呼び出し元に戻るが呼び出し先には潜らない。
	 * @return false: 呼び出し元に戻った場合またはこれ以上辿れない場合, true: それ以外 
	 */
	public boolean stepOver() {
		if (order < methodExecution.getStatements().size() - 1) {
			order++;
			return true;
		}
		order = methodExecution.getCallerStatementExecution();
		methodExecution = methodExecution.getCallerMethodExecution();
		if (methodExecution == null) {
			order = -1;
			return false;
		}
		return false;
	}
	
	/**
	 * 逆方向に探索する。呼び出し元に戻るが呼び出し先には潜らない。
	 * @return false: 呼び出し元に戻った場合またはこれ以上辿れない場合, true: それ以外 
	 */
	public boolean stepBackOver() {
		if (order > 0) {
			order--;
			return true;
		}
		order = methodExecution.getCallerStatementExecution();
		methodExecution = methodExecution.getCallerMethodExecution();
		if (methodExecution == null) {
			order = -1;
			return false;
		}
		return false;
	}
	
	/**
	 * 順方向に探索する。呼び出し先を辿るが呼び出し元には戻らない。
	 * @return false: 呼び出し先に移った場合またはこれ以上辿れない場合, true: それ以外
	 */
	public boolean stepNoReturn() {
		if (getStatement() instanceof MethodInvocation) {
			methodExecution = ((MethodInvocation)getStatement()).getCalledMethodExecution();
			if (methodExecution.getStatements().size() > 0) {
				order = 0;
			} else {
				order = -1;				// 呼び出し先で実行文が記録されていない場合
			}
			return false;
		}
		order++;
		if (order < methodExecution.getStatements().size()) {
			return true;
		}
		return false;
	}
	
	/**
	 * 逆方向に探索する。呼び出し先を辿るが呼び出し元には戻らない。(先に呼び出し元のメソッド呼び出し文を訪問してから呼び出し先のメソッド実行を訪問するので注意!!)
	 * @return　false: 呼び出し先に移った場合またはこれ以上辿れない場合, true: それ以外
	 */
	public boolean stepBackNoReturn() {
		if (getStatement() instanceof MethodInvocation) {
			methodExecution = ((MethodInvocation)getStatement()).getCalledMethodExecution();
			order = methodExecution.getStatements().size() - 1;			// -1 になる場合もある(呼び出し先で実行文が記録されていない場合)
			return false;
		}
		order--;
		if (order >= 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * 同じメソッド内で順方向に1つ進める。呼び出し先にも呼び出し元にも行かない。
	 * @return false:メソッドを抜け出た場合, true: それ以外
	 */
	public boolean stepNext() {
		order++;
		return (order < methodExecution.getStatements().size());
	}
	
	public boolean isValid() {
		if (methodExecution == null || order == -1 || order >= methodExecution.getStatements().size()) return false;
		return true;
	}
	
	public boolean isMethodEntry() {
		return (order == 0);
	}
	
	public boolean isStepBackOut() {
		if (order < 0) return true;
		return false;
	}
	
	public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof TracePoint)) return false;
		if (methodExecution != ((TracePoint) other).methodExecution) return false;
		if (order != ((TracePoint) other).order) return false;
		return true;
	}
}
