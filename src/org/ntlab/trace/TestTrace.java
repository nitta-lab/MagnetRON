package org.ntlab.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class TestTrace {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		Trace trace = new Trace("traces\\worstCase.trace");
		Trace trace = new TraceJSON("traces\\_worstCase.trace");
//		HashSet<String> marked = trace.getMarkedMethodSignatures(1255991806833871L, 1255991808597322L);
		HashSet<String> marked = trace.getMarkedMethodSignatures(1699553004208835L, 1699553004739523L);
		System.out.println("===== Marked Methods =====");
		for (String method: marked) {
			System.out.println(method);
		}
//		HashSet<String> unmarked = trace.getUnmarkedMethodSignatures(1255991806833871L, 1255991808597322L);
		HashSet<String> unmarked = trace.getUnmarkedMethodSignatures(1699553004208835L, 1699553004739523L);
		System.out.println("===== Unmarked Methods =====");
		for (String method: unmarked) {
			System.out.println(method);
		}
		
/*
 * ê≥ÇµÇ¢åãâ 
 * 
===== Marked Methods =====
void worstCase.O.passL(worstCase.L)
worstCase.L worstCase.I.getL()
worstCase.L worstCase.K.getL()
worstCase.M worstCase.L.getM()
worstCase.K worstCase.J.getK()
void worstCase.N.passI(worstCase.I)
void worstCase.P.setM(worstCase.M)
===== Unmarked Methods =====
worstCase.F worstCase.C.getF()
worstCase.E worstCase.D.getE()
worstCase.A()
public worstCase.M()
worstCase.F()
void worstCase.A.m()
public static void worstCase.main.main(java.lang.String[])
worstCase.G()
worstCase.H()
worstCase.I()
worstCase.B()
worstCase.C()
worstCase.D()
worstCase.E()
worstCase.N()
worstCase.O()
worstCase.P()
worstCase.F worstCase.E.getF()
worstCase.J()
worstCase.K()
worstCase.L()
worstCase.I worstCase.F.getI()
worstCase.H worstCase.G.getH()
worstCase.I worstCase.H.getI()
worstCase.I worstCase.B.getI()
 */
		HashSet<String> all = trace.getAllMethodSignatures();
		System.out.println("===== All Methods =====");
		for (String method: all) {
			System.out.println(method);
		}
/*
 * ê≥ÇµÇ¢åãâ 
 * 
===== All Methods =====
worstCase.F worstCase.C.getF()
worstCase.E worstCase.D.getE()
worstCase.A()
void worstCase.P.setM(worstCase.M)
public worstCase.M()
worstCase.M worstCase.L.getM()
worstCase.L worstCase.I.getL()
worstCase.L worstCase.K.getL()
void worstCase.N.passI(worstCase.I)
void worstCase.A.m()
worstCase.F()
public static void worstCase.main.main(java.lang.String[])
worstCase.G()
void worstCase.O.passL(worstCase.L)
worstCase.H()
worstCase.I()
worstCase.B()
worstCase.C()
worstCase.D()
worstCase.E()
worstCase.N()
worstCase.O()
worstCase.K worstCase.J.getK()
worstCase.F worstCase.E.getF()
worstCase.P()
worstCase.J()
worstCase.K()
worstCase.I worstCase.F.getI()
worstCase.I worstCase.H.getI()
worstCase.H worstCase.G.getH()
worstCase.L()
worstCase.I worstCase.B.getI()
 */
		
		ArrayList<MethodExecution> specified = trace.getMethodExecutions("void");
		System.out.println("===== Specified Methods =====");
		for (MethodExecution method: specified) {
			System.out.println(method.getSignature());
		}		
/*
 * ê≥ÇµÇ¢åãâ 
 * 
===== Specified Methods =====
void worstCase.A.m()
void worstCase.N.passI(worstCase.I)
void worstCase.O.passL(worstCase.L)
void worstCase.P.setM(worstCase.M) * 
 */	
		HashMap<String, ArrayList<MethodExecution>> allExecutions = trace.getAllMethodExecutions();
		System.out.println("===== All Methods and Executions =====");
		for (String method: allExecutions.keySet()) {
			System.out.println(method + ":" + allExecutions.get(method).size());
		}
/*
 * ê≥ÇµÇ¢åãâ 
 * 
===== All Methods and Executions =====
worstCase.F worstCase.C.getF():1
worstCase.E worstCase.D.getE():1
worstCase.A():1
void worstCase.P.setM(worstCase.M):1
public worstCase.M():1
worstCase.M worstCase.L.getM():1
worstCase.L worstCase.I.getL():1
worstCase.L worstCase.K.getL():1
void worstCase.N.passI(worstCase.I):1
void worstCase.A.m():1
worstCase.F():1
public static void worstCase.main.main(java.lang.String[]):1
worstCase.G():1
void worstCase.O.passL(worstCase.L):1
worstCase.H():1
worstCase.I():1
worstCase.B():1
worstCase.C():1
worstCase.D():1
worstCase.E():1
worstCase.N():1
worstCase.O():1
worstCase.K worstCase.J.getK():1
worstCase.F worstCase.E.getF():1
worstCase.P():1
worstCase.J():1
worstCase.K():1
worstCase.I worstCase.F.getI():1
worstCase.I worstCase.H.getI():1
worstCase.H worstCase.G.getH():1
worstCase.L():1
worstCase.I worstCase.B.getI():1
 */	
		System.out.println("===== All Statements Forward =====");
		trace.traverseStatementsInTrace(new IStatementVisitor() {
			@Override
			public boolean preVisitStatement(Statement statement) {
				System.out.println("pre:" + statement.getClass().getName() + ":" + statement.getTimeStamp());
				return false;
			}
			@Override
			public boolean postVisitStatement(Statement statement) {
				System.out.println("post:" + statement.getClass().getName() + ":" + statement.getTimeStamp());
				return false;
			}
		});
/*
 * ê≥ÇµÇ¢åãâ 
 * 
===== All Statements Forward =====
pre:org.ntlab.trace.BlockEnter:1699552992988213
post:org.ntlab.trace.BlockEnter:1699552992988213
pre:org.ntlab.trace.MethodInvocation:1699552993730471
pre:org.ntlab.trace.MethodInvocation:1699552994339441
pre:org.ntlab.trace.MethodInvocation:1699552994979793
pre:org.ntlab.trace.MethodInvocation:1699552995575363
pre:org.ntlab.trace.MethodInvocation:1699552996163881
pre:org.ntlab.trace.MethodInvocation:1699552996774613
pre:org.ntlab.trace.MethodInvocation:1699552997363836
pre:org.ntlab.trace.MethodInvocation:1699552997949532
pre:org.ntlab.trace.MethodInvocation:1699552998548628
pre:org.ntlab.trace.MethodInvocation:1699552999050402
pre:org.ntlab.trace.MethodInvocation:1699552999466490
pre:org.ntlab.trace.MethodInvocation:1699552999875526
pre:org.ntlab.trace.MethodInvocation:1699553000173135
post:org.ntlab.trace.MethodInvocation:1699553000173135
pre:org.ntlab.trace.FieldUpdate:1699553000225322
post:org.ntlab.trace.FieldUpdate:1699553000225322
post:org.ntlab.trace.MethodInvocation:1699552999875526
pre:org.ntlab.trace.FieldUpdate:1699553000259878
post:org.ntlab.trace.FieldUpdate:1699553000259878
post:org.ntlab.trace.MethodInvocation:1699552999466490
pre:org.ntlab.trace.FieldUpdate:1699553000290908
post:org.ntlab.trace.FieldUpdate:1699553000290908
post:org.ntlab.trace.MethodInvocation:1699552999050402
pre:org.ntlab.trace.FieldUpdate:1699553000331107
post:org.ntlab.trace.FieldUpdate:1699553000331107
post:org.ntlab.trace.MethodInvocation:1699552998548628
pre:org.ntlab.trace.FieldUpdate:1699553000376947
post:org.ntlab.trace.FieldUpdate:1699553000376947
post:org.ntlab.trace.MethodInvocation:1699552997949532
pre:org.ntlab.trace.FieldUpdate:1699553000422435
post:org.ntlab.trace.FieldUpdate:1699553000422435
post:org.ntlab.trace.MethodInvocation:1699552997363836
pre:org.ntlab.trace.FieldUpdate:1699553000463691
post:org.ntlab.trace.FieldUpdate:1699553000463691
post:org.ntlab.trace.MethodInvocation:1699552996774613
pre:org.ntlab.trace.FieldUpdate:1699553000508121
post:org.ntlab.trace.FieldUpdate:1699553000508121
post:org.ntlab.trace.MethodInvocation:1699552996163881
pre:org.ntlab.trace.FieldUpdate:1699553000551845
post:org.ntlab.trace.FieldUpdate:1699553000551845
post:org.ntlab.trace.MethodInvocation:1699552995575363
pre:org.ntlab.trace.FieldUpdate:1699553000596627
post:org.ntlab.trace.FieldUpdate:1699553000596627
post:org.ntlab.trace.MethodInvocation:1699552994979793
pre:org.ntlab.trace.FieldUpdate:1699553000640352
post:org.ntlab.trace.FieldUpdate:1699553000640352
post:org.ntlab.trace.MethodInvocation:1699552994339441
pre:org.ntlab.trace.FieldUpdate:1699553000682666
post:org.ntlab.trace.FieldUpdate:1699553000682666
pre:org.ntlab.trace.MethodInvocation:1699553001472175
pre:org.ntlab.trace.MethodInvocation:1699553002201034
pre:org.ntlab.trace.MethodInvocation:1699553003026510
post:org.ntlab.trace.MethodInvocation:1699553003026510
pre:org.ntlab.trace.FieldUpdate:1699553003101618
post:org.ntlab.trace.FieldUpdate:1699553003101618
post:org.ntlab.trace.MethodInvocation:1699553002201034
pre:org.ntlab.trace.FieldUpdate:1699553003149926
post:org.ntlab.trace.FieldUpdate:1699553003149926
post:org.ntlab.trace.MethodInvocation:1699553001472175
pre:org.ntlab.trace.FieldUpdate:1699553003190477
post:org.ntlab.trace.FieldUpdate:1699553003190477
post:org.ntlab.trace.MethodInvocation:1699552993730471
pre:org.ntlab.trace.MethodInvocation:1699553003253243
pre:org.ntlab.trace.BlockEnter:1699553003273695
post:org.ntlab.trace.BlockEnter:1699553003273695
pre:org.ntlab.trace.FieldAccess:1699553003299083
post:org.ntlab.trace.FieldAccess:1699553003299083
pre:org.ntlab.trace.FieldAccess:1699553003355502
post:org.ntlab.trace.FieldAccess:1699553003355502
pre:org.ntlab.trace.MethodInvocation:1699553003386885
pre:org.ntlab.trace.BlockEnter:1699553003400637
post:org.ntlab.trace.BlockEnter:1699553003400637
pre:org.ntlab.trace.FieldAccess:1699553003436956
post:org.ntlab.trace.FieldAccess:1699553003436956
pre:org.ntlab.trace.MethodInvocation:1699553003482444
pre:org.ntlab.trace.BlockEnter:1699553003500427
post:org.ntlab.trace.BlockEnter:1699553003500427
pre:org.ntlab.trace.FieldAccess:1699553003526169
post:org.ntlab.trace.FieldAccess:1699553003526169
pre:org.ntlab.trace.MethodInvocation:1699553003556141
pre:org.ntlab.trace.BlockEnter:1699553003570951
post:org.ntlab.trace.BlockEnter:1699553003570951
pre:org.ntlab.trace.FieldAccess:1699553003599513
post:org.ntlab.trace.FieldAccess:1699553003599513
post:org.ntlab.trace.MethodInvocation:1699553003556141
pre:org.ntlab.trace.MethodInvocation:1699553003668273
pre:org.ntlab.trace.BlockEnter:1699553003688020
post:org.ntlab.trace.BlockEnter:1699553003688020
pre:org.ntlab.trace.FieldAccess:1699553003715876
post:org.ntlab.trace.FieldAccess:1699553003715876
post:org.ntlab.trace.MethodInvocation:1699553003668273
post:org.ntlab.trace.MethodInvocation:1699553003482444
pre:org.ntlab.trace.MethodInvocation:1699553003805088
pre:org.ntlab.trace.BlockEnter:1699553003818135
post:org.ntlab.trace.BlockEnter:1699553003818135
pre:org.ntlab.trace.FieldAccess:1699553003846345
post:org.ntlab.trace.FieldAccess:1699553003846345
pre:org.ntlab.trace.MethodInvocation:1699553003896769
pre:org.ntlab.trace.BlockEnter:1699553003912989
post:org.ntlab.trace.BlockEnter:1699553003912989
pre:org.ntlab.trace.FieldAccess:1699553003944020
post:org.ntlab.trace.FieldAccess:1699553003944020
post:org.ntlab.trace.MethodInvocation:1699553003896769
pre:org.ntlab.trace.MethodInvocation:1699553004012075
pre:org.ntlab.trace.BlockEnter:1699553004029706
post:org.ntlab.trace.BlockEnter:1699553004029706
pre:org.ntlab.trace.FieldAccess:1699553004082951
post:org.ntlab.trace.FieldAccess:1699553004082951
post:org.ntlab.trace.MethodInvocation:1699553004012075
post:org.ntlab.trace.MethodInvocation:1699553003805088
post:org.ntlab.trace.MethodInvocation:1699553003386885
pre:org.ntlab.trace.MethodInvocation:1699553004208835
pre:org.ntlab.trace.BlockEnter:1699553004228229
post:org.ntlab.trace.BlockEnter:1699553004228229
pre:org.ntlab.trace.FieldAccess:1699553004257849
post:org.ntlab.trace.FieldAccess:1699553004257849
pre:org.ntlab.trace.MethodInvocation:1699553004302631
pre:org.ntlab.trace.BlockEnter:1699553004326962
post:org.ntlab.trace.BlockEnter:1699553004326962
pre:org.ntlab.trace.FieldAccess:1699553004373507
post:org.ntlab.trace.FieldAccess:1699553004373507
pre:org.ntlab.trace.MethodInvocation:1699553004418995
pre:org.ntlab.trace.BlockEnter:1699553004434510
post:org.ntlab.trace.BlockEnter:1699553004434510
pre:org.ntlab.trace.FieldAccess:1699553004461661
post:org.ntlab.trace.FieldAccess:1699553004461661
post:org.ntlab.trace.MethodInvocation:1699553004418995
pre:org.ntlab.trace.MethodInvocation:1699553004515964
pre:org.ntlab.trace.BlockEnter:1699553004526543
post:org.ntlab.trace.BlockEnter:1699553004526543
pre:org.ntlab.trace.FieldAccess:1699553004546994
post:org.ntlab.trace.FieldAccess:1699553004546994
post:org.ntlab.trace.MethodInvocation:1699553004515964
post:org.ntlab.trace.MethodInvocation:1699553004302631
pre:org.ntlab.trace.MethodInvocation:1699553004606587
pre:org.ntlab.trace.BlockEnter:1699553004615402
post:org.ntlab.trace.BlockEnter:1699553004615402
pre:org.ntlab.trace.FieldAccess:1699553004629507
post:org.ntlab.trace.FieldAccess:1699553004629507
pre:org.ntlab.trace.MethodInvocation:1699553004648195
pre:org.ntlab.trace.BlockEnter:1699553004655953
post:org.ntlab.trace.BlockEnter:1699553004655953
pre:org.ntlab.trace.FieldAccess:1699553004670763
post:org.ntlab.trace.FieldAccess:1699553004670763
post:org.ntlab.trace.MethodInvocation:1699553004648195
pre:org.ntlab.trace.MethodInvocation:1699553004703556
pre:org.ntlab.trace.BlockEnter:1699553004712019
post:org.ntlab.trace.BlockEnter:1699553004712019
pre:org.ntlab.trace.FieldUpdate:1699553004728240
post:org.ntlab.trace.FieldUpdate:1699553004728240
post:org.ntlab.trace.MethodInvocation:1699553004703556
post:org.ntlab.trace.MethodInvocation:1699553004606587
post:org.ntlab.trace.MethodInvocation:1699553004208835
post:org.ntlab.trace.MethodInvocation:1699553003253243
 */	
		System.out.println("===== All Statements Backward =====");
		trace.traverseStatementsInTraceBackward(new IStatementVisitor() {
			@Override
			public boolean preVisitStatement(Statement statement) {
				System.out.println("post:" + statement.getClass().getName() + ":" + statement.getTimeStamp());
				return false;
			}
			@Override
			public boolean postVisitStatement(Statement statement) {
				System.out.println("pre:" + statement.getClass().getName() + ":" + statement.getTimeStamp());
				return false;
			}
		});
	}
/*
 * ê≥ÇµÇ¢åãâ 
 * 
===== All Statements Backward =====
post:org.ntlab.trace.MethodInvocation:1699553003253243
post:org.ntlab.trace.MethodInvocation:1699553004208835
post:org.ntlab.trace.MethodInvocation:1699553004606587
post:org.ntlab.trace.MethodInvocation:1699553004703556
post:org.ntlab.trace.FieldUpdate:1699553004728240
pre:org.ntlab.trace.FieldUpdate:1699553004728240
post:org.ntlab.trace.BlockEnter:1699553004712019
pre:org.ntlab.trace.BlockEnter:1699553004712019
pre:org.ntlab.trace.MethodInvocation:1699553004703556
post:org.ntlab.trace.MethodInvocation:1699553004648195
post:org.ntlab.trace.FieldAccess:1699553004670763
pre:org.ntlab.trace.FieldAccess:1699553004670763
post:org.ntlab.trace.BlockEnter:1699553004655953
pre:org.ntlab.trace.BlockEnter:1699553004655953
pre:org.ntlab.trace.MethodInvocation:1699553004648195
post:org.ntlab.trace.FieldAccess:1699553004629507
pre:org.ntlab.trace.FieldAccess:1699553004629507
post:org.ntlab.trace.BlockEnter:1699553004615402
pre:org.ntlab.trace.BlockEnter:1699553004615402
pre:org.ntlab.trace.MethodInvocation:1699553004606587
post:org.ntlab.trace.MethodInvocation:1699553004302631
post:org.ntlab.trace.MethodInvocation:1699553004515964
post:org.ntlab.trace.FieldAccess:1699553004546994
pre:org.ntlab.trace.FieldAccess:1699553004546994
post:org.ntlab.trace.BlockEnter:1699553004526543
pre:org.ntlab.trace.BlockEnter:1699553004526543
pre:org.ntlab.trace.MethodInvocation:1699553004515964
post:org.ntlab.trace.MethodInvocation:1699553004418995
post:org.ntlab.trace.FieldAccess:1699553004461661
pre:org.ntlab.trace.FieldAccess:1699553004461661
post:org.ntlab.trace.BlockEnter:1699553004434510
pre:org.ntlab.trace.BlockEnter:1699553004434510
pre:org.ntlab.trace.MethodInvocation:1699553004418995
post:org.ntlab.trace.FieldAccess:1699553004373507
pre:org.ntlab.trace.FieldAccess:1699553004373507
post:org.ntlab.trace.BlockEnter:1699553004326962
pre:org.ntlab.trace.BlockEnter:1699553004326962
pre:org.ntlab.trace.MethodInvocation:1699553004302631
post:org.ntlab.trace.FieldAccess:1699553004257849
pre:org.ntlab.trace.FieldAccess:1699553004257849
post:org.ntlab.trace.BlockEnter:1699553004228229
pre:org.ntlab.trace.BlockEnter:1699553004228229
pre:org.ntlab.trace.MethodInvocation:1699553004208835
post:org.ntlab.trace.MethodInvocation:1699553003386885
post:org.ntlab.trace.MethodInvocation:1699553003805088
post:org.ntlab.trace.MethodInvocation:1699553004012075
post:org.ntlab.trace.FieldAccess:1699553004082951
pre:org.ntlab.trace.FieldAccess:1699553004082951
post:org.ntlab.trace.BlockEnter:1699553004029706
pre:org.ntlab.trace.BlockEnter:1699553004029706
pre:org.ntlab.trace.MethodInvocation:1699553004012075
post:org.ntlab.trace.MethodInvocation:1699553003896769
post:org.ntlab.trace.FieldAccess:1699553003944020
pre:org.ntlab.trace.FieldAccess:1699553003944020
post:org.ntlab.trace.BlockEnter:1699553003912989
pre:org.ntlab.trace.BlockEnter:1699553003912989
pre:org.ntlab.trace.MethodInvocation:1699553003896769
post:org.ntlab.trace.FieldAccess:1699553003846345
pre:org.ntlab.trace.FieldAccess:1699553003846345
post:org.ntlab.trace.BlockEnter:1699553003818135
pre:org.ntlab.trace.BlockEnter:1699553003818135
pre:org.ntlab.trace.MethodInvocation:1699553003805088
post:org.ntlab.trace.MethodInvocation:1699553003482444
post:org.ntlab.trace.MethodInvocation:1699553003668273
post:org.ntlab.trace.FieldAccess:1699553003715876
pre:org.ntlab.trace.FieldAccess:1699553003715876
post:org.ntlab.trace.BlockEnter:1699553003688020
pre:org.ntlab.trace.BlockEnter:1699553003688020
pre:org.ntlab.trace.MethodInvocation:1699553003668273
post:org.ntlab.trace.MethodInvocation:1699553003556141
post:org.ntlab.trace.FieldAccess:1699553003599513
pre:org.ntlab.trace.FieldAccess:1699553003599513
post:org.ntlab.trace.BlockEnter:1699553003570951
pre:org.ntlab.trace.BlockEnter:1699553003570951
pre:org.ntlab.trace.MethodInvocation:1699553003556141
post:org.ntlab.trace.FieldAccess:1699553003526169
pre:org.ntlab.trace.FieldAccess:1699553003526169
post:org.ntlab.trace.BlockEnter:1699553003500427
pre:org.ntlab.trace.BlockEnter:1699553003500427
pre:org.ntlab.trace.MethodInvocation:1699553003482444
post:org.ntlab.trace.FieldAccess:1699553003436956
pre:org.ntlab.trace.FieldAccess:1699553003436956
post:org.ntlab.trace.BlockEnter:1699553003400637
pre:org.ntlab.trace.BlockEnter:1699553003400637
pre:org.ntlab.trace.MethodInvocation:1699553003386885
post:org.ntlab.trace.FieldAccess:1699553003355502
pre:org.ntlab.trace.FieldAccess:1699553003355502
post:org.ntlab.trace.FieldAccess:1699553003299083
pre:org.ntlab.trace.FieldAccess:1699553003299083
post:org.ntlab.trace.BlockEnter:1699553003273695
pre:org.ntlab.trace.BlockEnter:1699553003273695
pre:org.ntlab.trace.MethodInvocation:1699553003253243
post:org.ntlab.trace.MethodInvocation:1699552993730471
post:org.ntlab.trace.FieldUpdate:1699553003190477
pre:org.ntlab.trace.FieldUpdate:1699553003190477
post:org.ntlab.trace.MethodInvocation:1699553001472175
post:org.ntlab.trace.FieldUpdate:1699553003149926
pre:org.ntlab.trace.FieldUpdate:1699553003149926
post:org.ntlab.trace.MethodInvocation:1699553002201034
post:org.ntlab.trace.FieldUpdate:1699553003101618
pre:org.ntlab.trace.FieldUpdate:1699553003101618
post:org.ntlab.trace.MethodInvocation:1699553003026510
pre:org.ntlab.trace.MethodInvocation:1699553003026510
pre:org.ntlab.trace.MethodInvocation:1699553002201034
pre:org.ntlab.trace.MethodInvocation:1699553001472175
post:org.ntlab.trace.FieldUpdate:1699553000682666
pre:org.ntlab.trace.FieldUpdate:1699553000682666
post:org.ntlab.trace.MethodInvocation:1699552994339441
post:org.ntlab.trace.FieldUpdate:1699553000640352
pre:org.ntlab.trace.FieldUpdate:1699553000640352
post:org.ntlab.trace.MethodInvocation:1699552994979793
post:org.ntlab.trace.FieldUpdate:1699553000596627
pre:org.ntlab.trace.FieldUpdate:1699553000596627
post:org.ntlab.trace.MethodInvocation:1699552995575363
post:org.ntlab.trace.FieldUpdate:1699553000551845
pre:org.ntlab.trace.FieldUpdate:1699553000551845
post:org.ntlab.trace.MethodInvocation:1699552996163881
post:org.ntlab.trace.FieldUpdate:1699553000508121
pre:org.ntlab.trace.FieldUpdate:1699553000508121
post:org.ntlab.trace.MethodInvocation:1699552996774613
post:org.ntlab.trace.FieldUpdate:1699553000463691
pre:org.ntlab.trace.FieldUpdate:1699553000463691
post:org.ntlab.trace.MethodInvocation:1699552997363836
post:org.ntlab.trace.FieldUpdate:1699553000422435
pre:org.ntlab.trace.FieldUpdate:1699553000422435
post:org.ntlab.trace.MethodInvocation:1699552997949532
post:org.ntlab.trace.FieldUpdate:1699553000376947
pre:org.ntlab.trace.FieldUpdate:1699553000376947
post:org.ntlab.trace.MethodInvocation:1699552998548628
post:org.ntlab.trace.FieldUpdate:1699553000331107
pre:org.ntlab.trace.FieldUpdate:1699553000331107
post:org.ntlab.trace.MethodInvocation:1699552999050402
post:org.ntlab.trace.FieldUpdate:1699553000290908
pre:org.ntlab.trace.FieldUpdate:1699553000290908
post:org.ntlab.trace.MethodInvocation:1699552999466490
post:org.ntlab.trace.FieldUpdate:1699553000259878
pre:org.ntlab.trace.FieldUpdate:1699553000259878
post:org.ntlab.trace.MethodInvocation:1699552999875526
post:org.ntlab.trace.FieldUpdate:1699553000225322
pre:org.ntlab.trace.FieldUpdate:1699553000225322
post:org.ntlab.trace.MethodInvocation:1699553000173135
pre:org.ntlab.trace.MethodInvocation:1699553000173135
pre:org.ntlab.trace.MethodInvocation:1699552999875526
pre:org.ntlab.trace.MethodInvocation:1699552999466490
pre:org.ntlab.trace.MethodInvocation:1699552999050402
pre:org.ntlab.trace.MethodInvocation:1699552998548628
pre:org.ntlab.trace.MethodInvocation:1699552997949532
pre:org.ntlab.trace.MethodInvocation:1699552997363836
pre:org.ntlab.trace.MethodInvocation:1699552996774613
pre:org.ntlab.trace.MethodInvocation:1699552996163881
pre:org.ntlab.trace.MethodInvocation:1699552995575363
pre:org.ntlab.trace.MethodInvocation:1699552994979793
pre:org.ntlab.trace.MethodInvocation:1699552994339441
pre:org.ntlab.trace.MethodInvocation:1699552993730471
post:org.ntlab.trace.BlockEnter:1699552992988213
pre:org.ntlab.trace.BlockEnter:1699552992988213
 */
}
