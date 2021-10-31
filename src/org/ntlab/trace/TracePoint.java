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
	 * �������̑S�T��(�������A���s�����L�^����Ă��Ȃ����\�b�h���s�ɂ͐���Ȃ�)
	 * @return false: ����ȏ�H��Ȃ��ꍇ, true: ����ȊO
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
	 * �t�����̑S�T��(�������A���s�����L�^����Ă��Ȃ����\�b�h���s�ɂ͐���Ȃ��B�܂��A�Ăяo����̃��\�b�h���s����T��������ɁA�Ăяo�����̃��\�b�h�Ăяo������K�₷��̂Œ���!!)
	 * @return false: ����ȏ�H��Ȃ��ꍇ, true: ����ȊO
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
	 * �������ɒT������B�Ăяo�����ɖ߂邪�Ăяo����ɂ͐���Ȃ��B
	 * @return false: �Ăяo�����ɖ߂����ꍇ�܂��͂���ȏ�H��Ȃ��ꍇ, true: ����ȊO 
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
	 * �t�����ɒT������B�Ăяo�����ɖ߂邪�Ăяo����ɂ͐���Ȃ��B
	 * @return false: �Ăяo�����ɖ߂����ꍇ�܂��͂���ȏ�H��Ȃ��ꍇ, true: ����ȊO 
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
	 * �������ɒT������B�Ăяo�����H�邪�Ăяo�����ɂ͖߂�Ȃ��B
	 * @return false: �Ăяo����Ɉڂ����ꍇ�܂��͂���ȏ�H��Ȃ��ꍇ, true: ����ȊO
	 */
	public boolean stepNoReturn() {
		if (getStatement() instanceof MethodInvocation) {
			methodExecution = ((MethodInvocation)getStatement()).getCalledMethodExecution();
			if (methodExecution.getStatements().size() > 0) {
				order = 0;
			} else {
				order = -1;				// �Ăяo����Ŏ��s�����L�^����Ă��Ȃ��ꍇ
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
	 * �t�����ɒT������B�Ăяo�����H�邪�Ăяo�����ɂ͖߂�Ȃ��B(��ɌĂяo�����̃��\�b�h�Ăяo������K�₵�Ă���Ăяo����̃��\�b�h���s��K�₷��̂Œ���!!)
	 * @return�@false: �Ăяo����Ɉڂ����ꍇ�܂��͂���ȏ�H��Ȃ��ꍇ, true: ����ȊO
	 */
	public boolean stepBackNoReturn() {
		if (getStatement() instanceof MethodInvocation) {
			methodExecution = ((MethodInvocation)getStatement()).getCalledMethodExecution();
			order = methodExecution.getStatements().size() - 1;			// -1 �ɂȂ�ꍇ������(�Ăяo����Ŏ��s�����L�^����Ă��Ȃ��ꍇ)
			return false;
		}
		order--;
		if (order >= 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * �������\�b�h���ŏ�������1�i�߂�B�Ăяo����ɂ��Ăяo�����ɂ��s���Ȃ��B
	 * @return false:���\�b�h�𔲂��o���ꍇ, true: ����ȊO
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
