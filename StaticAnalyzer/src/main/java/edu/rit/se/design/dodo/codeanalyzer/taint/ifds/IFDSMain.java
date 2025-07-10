package edu.rit.se.design.dodo.codeanalyzer.taint.ifds;

import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import edu.rit.se.design.dodo.codeanalyzer.ProjectAnalysis;
import edu.rit.se.design.dodo.codeanalyzer.callgraph.CallGraphBuilderType;
import edu.rit.se.design.dodo.codeanalyzer.cli.CLI;
import edu.rit.se.design.dodo.codeanalyzer.config.Config;
import edu.rit.se.design.dodo.utils.viz.ProjectAnalysisViewer;
import edu.rit.se.design.dodo.utils.wala.WalaUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class IFDSMain {
//    public static void main(String... args) throws IllegalArgumentException, CancelException, WalaException, IOException {
//
//        Config config = Config.loadFromFile("StaticAnalyzer/resources/ex2/example2.properties");
//        ProjectAnalysis projectAnalysis = CLI.runAnalysis(config, CallGraphBuilderType.N1_CFA);
//
//
//        CallGraph CG = projectAnalysis.getCallGraph();
//
//        Function<BasicBlockInContext<IExplodedBasicBlock>, Boolean> sources = (ebb) -> {
//            SSAInstruction inst = ebb.getDelegate().getInstruction();
//            if (inst instanceof SSAArrayLoadInstruction && WalaUtils.isApplicationScope(ebb.getNode())) {
//                if (((SSAArrayLoadInstruction) inst).getArrayRef() == 1 && ebb.getNode().getMethod().getName().toString().equals("main")) {
//                    return true;
//                }
//            }
//
//            return false;
//        };
//
//        Function<BasicBlockInContext<IExplodedBasicBlock>, Boolean> sinks = (bb) -> {
//            SSAInstruction inst = bb.getDelegate().getInstruction();
//            if (inst instanceof SSAAbstractInvokeInstruction && WalaUtils.isApplicationScope(bb.getNode())) {
//                for (CGNode target : CG.getPossibleTargets(bb.getNode(), ((SSAAbstractInvokeInstruction) inst).getCallSite())) {
//                    if (target.getMethod().getName().toString().equals("println") && target.getMethod().getDeclaringClass().getName().toString().equals("Ljava/io/PrintStream")) {
//                        return true;
//                    }
//                }
//            }
//
//            return false;
//        };
//
//        Set<PointerKey> taintedPointers = analyzeTaint(CG, sources, sinks);
//
////        new WalaViewer(CG,projectAnalysis.getCallGraphBuilder().getPointerAnalysis());
//        new ProjectAnalysisViewer(CG,taintedPointers,false);
//    }
//
//
//    public static Set</*List<CAstSourcePositionMap.Position>*/PointerKey> analyzeTaint(CallGraph CG, Function<BasicBlockInContext<IExplodedBasicBlock>, Boolean> sources, Function<BasicBlockInContext<IExplodedBasicBlock>, Boolean> sinks) {
//        IFDSTaintAnalyzer taintAnalyzer = new IFDSTaintAnalyzer(CG, sources);
//        TaintDomain domain = taintAnalyzer.getDomain();
//        TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>>> tabulationResult = taintAnalyzer.analyze();
//
////        Set<List<CAstSourcePositionMap.Position>> result = HashSetFactory.make();
//        Set<PointerKey> taintedPointers = HashSetFactory.make();
//
//        tabulationResult.getSupergraphNodesReached().stream()/*.filter(sbb -> WalaUtils.isApplicationScope(sbb.getNode()))*/.forEach((sbb) -> {
//
//            BasicBlockInContext<IExplodedBasicBlock> bb = sbb;
//            List<CAstSourcePositionMap.Position> witness = new LinkedList<>();
//            steps:
//            while (bb != null) {
//                IntSet r = tabulationResult.getResult(bb);
//                SSAInstruction inst = bb.getDelegate().getInstruction();
//                if (inst != null) {
//                    IntIterator vals = r.intIterator();
//                    while (vals.hasNext()) {
//                        int i = vals.next();
//                        Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>> vn = domain.getMappedObject(i);
////                    System.out.println("INTEGER " + vn.fst);
////                    System.out.println("INSTRUCTION " + in);
//                        for (int j = 0; j < inst.getNumberOfUses(); j++) {
//                            if (inst.getUse(j) == vn.fst) {
//                                bb = vn.snd;
////                            System.out.println("step " + bb.getDelegate().getInstruction());
//                                System.out.println("SUSPICIOUS? " + bb.getDelegate().getInstruction());
//                                System.out.println("Var Number = " + vn.fst);
//                                taintedPointers.add(new LocalPointerKey(bb.getNode(), vn.fst));
//                                continue steps;
//                            }
//                        }
//                    }
//
//
//                }else{
////                    System.out.println("WTF IS NULL? " + bb);
//                }
//                bb = null;
//            }
//
//            /*if (sinks.apply(sbb)) {
//                System.out.println("sink " + sbb.getDelegate().getInstruction());
//                BasicBlockInContext<IExplodedBasicBlock> bb = sbb;
//                List<CAstSourcePositionMap.Position> witness = new LinkedList<>();
//                steps:
//                while (bb != null) {
//                    IntSet r = tabulationResult.getResult(bb);
//                    SSAInstruction inst = bb.getDelegate().getInstruction();
//                    if (bb.getMethod() instanceof AstMethod) {
//                        CAstSourcePositionMap.Position pos = ((AstMethod) bb.getMethod()).debugInfo().getInstructionPosition(inst.iIndex());
//                        witness.add(0, pos);
//                    }
//                    IntIterator vals = r.intIterator();
//                    while (vals.hasNext()) {
//                        int i = vals.next();
//                        Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>> vn = domain.getMappedObject(i);
//                        for (int j = 0; j < inst.getNumberOfUses(); j++) {
//                            if (inst.getUse(j) == vn.fst) {
//                                bb = vn.snd;
//                                System.out.println("step " + bb.getDelegate().getInstruction());
//                                continue steps;
//                            }
//                        }
//                    }
//                    bb = null;
//                }
//                if (witness.size() > 0) {
//                    result.add(witness);
//                }
//            }*/
//        });
////        return result;
//        return taintedPointers;
//    }

}
