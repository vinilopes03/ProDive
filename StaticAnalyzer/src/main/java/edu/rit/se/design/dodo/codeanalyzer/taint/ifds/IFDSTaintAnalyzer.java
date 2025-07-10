package edu.rit.se.design.dodo.codeanalyzer.taint.ifds;

import com.ibm.wala.dataflow.IFDS.ICFGSupergraph;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.PartiallyBalancedTabulationSolver;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import edu.rit.se.design.dodo.codeanalyzer.ProjectAnalysis;
import edu.rit.se.design.dodo.codeanalyzer.taint.ITaintAnalyzer;
import edu.rit.se.design.dodo.utils.wala.WalaUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Code re-used and modified from:
 * https://github.com/wala/Examples/blob/7485c0de4fb170eeaaec4233bacca43da365a27f/%E7%BB%87%E5%A5%B3/source/com/ibm/wala/examples/SOAP2020/%E7%BB%87%E5%A5%B3/%E7%BB%87%E5%A5%B3TaintAnalysisIFDS.java
 *
 * @author Joanna C. S. Santos (joannacss@nd.edu)
 */
public class IFDSTaintAnalyzer implements ITaintAnalyzer {


    /**
     * Perform the tabulation analysis to compute tainted variables in the code.
     */
    @Override
    public Set<PointerKey> analyze(ProjectAnalysis projectAnalysis) {

        // the supergraph over which tabulation is performed
        ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> supergraph = ICFGSupergraph.make(projectAnalysis.getCallGraph());

        // domain of facts for tabulation.
        TaintDomain domain = new TaintDomain();


        // function used to detect sources in the supergraph (true if it is a source, false if it isn't)
        Function<BasicBlockInContext<IExplodedBasicBlock>, Boolean> sources = getSourceFunction(projectAnalysis);


        TaintProblem taintProblem = new TaintProblem(domain, supergraph, sources, projectAnalysis.getCallGraph());
        PartiallyBalancedTabulationSolver<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>>> solver = PartiallyBalancedTabulationSolver
                .createPartiallyBalancedTabulationSolver(taintProblem, null);
        Set<PointerKey> taintedPointers = HashSetFactory.make();
        try {
            TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>>> result = solver.solve();
            taintedPointers = extractTaintedPointers(result, domain);
        } catch (CancelException e) {
            // this shouldn't happen
            assert false;
        }

        return taintedPointers;
    }

    /**
     * Creates a function that returns true if the given basic block is a source, false otherwise.
     * @param projectAnalysis the metadata of the project
     * @return
     */
    private static Function<BasicBlockInContext<IExplodedBasicBlock>, Boolean> getSourceFunction(ProjectAnalysis projectAnalysis) {
        return new Function<BasicBlockInContext<IExplodedBasicBlock>, Boolean>() {
            private Set<SSAInstruction> sourceInstrs = projectAnalysis.getSources().stream().map(s -> s.getInstruction()).collect(Collectors.toSet());
            private Set<CGNode> entrypoints = new HashSet(projectAnalysis.getCallGraph().getEntrypointNodes());

            @Override
            public Boolean apply(BasicBlockInContext<IExplodedBasicBlock> bb) {
                SSAInstruction instruction = bb.getDelegate().getInstruction();
                CGNode node = bb.getNode();
                // rule #1: instruction is a direct invocation to a source method
                if (sourceInstrs.contains(instruction))
                    return true;
                // rule #2: instruction uses a parameter from an entrypoint
                if (instruction != null && entrypoints.contains(node)) {
                    for (int i = 0; i < instruction.getNumberOfUses(); i++) {
                        if (isParameter(node, instruction.getUse(i)))
                            return true;
                    }
                }
                return false;
            }

            /**
             * @param vn a variable number in a call graph node
             * @return true if the variable is a parameter passed to the node
             */
            private boolean isParameter(CGNode node, int vn) {
                int offset = node.getMethod().isStatic() ? 0 : 1;
                return vn <= node.getMethod().getNumberOfParameters() + offset;
            }
        };
    }


    private Set<PointerKey> extractTaintedPointers(TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>>> tabulationResult, TaintDomain domain) {
        Set<PointerKey> taintedPointers = HashSetFactory.make();

        tabulationResult.getSupergraphNodesReached().stream()/*.filter(sbb -> WalaUtils.isApplicationScope(sbb.getNode()))*/.forEach((sbb) -> {

            BasicBlockInContext<IExplodedBasicBlock> bb = sbb;

            steps:
            while (bb != null) {
                IntSet r = tabulationResult.getResult(bb);
                SSAInstruction inst = bb.getDelegate().getInstruction();
                if (inst != null) {
                    IntIterator vals = r.intIterator();
                    while (vals.hasNext()) {
                        int i = vals.next();
                        Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>> vn = domain.getMappedObject(i);
                        for (int j = 0; j < inst.getNumberOfUses(); j++) {
                            if (inst.getUse(j) == vn.fst) {
                                bb = vn.snd;
                                taintedPointers.add(new LocalPointerKey(bb.getNode(), vn.fst));
                                continue steps;
                            }
                        }
                    }
                }
                bb = null;
            }

            /*if (sinks.apply(sbb)) {
                System.out.println("sink " + sbb.getDelegate().getInstruction());
                BasicBlockInContext<IExplodedBasicBlock> bb = sbb;
                List<CAstSourcePositionMap.Position> witness = new LinkedList<>();
                steps:
                while (bb != null) {
                    IntSet r = tabulationResult.getResult(bb);
                    SSAInstruction inst = bb.getDelegate().getInstruction();
                    if (bb.getMethod() instanceof AstMethod) {
                        CAstSourcePositionMap.Position pos = ((AstMethod) bb.getMethod()).debugInfo().getInstructionPosition(inst.iIndex());
                        witness.add(0, pos);
                    }
                    IntIterator vals = r.intIterator();
                    while (vals.hasNext()) {
                        int i = vals.next();
                        Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>> vn = domain.getMappedObject(i);
                        for (int j = 0; j < inst.getNumberOfUses(); j++) {
                            if (inst.getUse(j) == vn.fst) {
                                bb = vn.snd;
                                System.out.println("step " + bb.getDelegate().getInstruction());
                                continue steps;
                            }
                        }
                    }
                    bb = null;
                }
                if (witness.size() > 0) {
                    result.add(witness);
                }
            }*/
        });
        return taintedPointers;
    }
}
