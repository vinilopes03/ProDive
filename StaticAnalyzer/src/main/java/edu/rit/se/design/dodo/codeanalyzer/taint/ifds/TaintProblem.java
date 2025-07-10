package edu.rit.se.design.dodo.codeanalyzer.taint.ifds;

import com.ibm.wala.dataflow.IFDS.*;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class TaintProblem
        implements PartiallyBalancedTabulationProblem<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>>> {

    private final TaintFlowFunctions flowFunctions;
    private final TaintDomain domain;
    private final ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> supergraph;
    private final Function<BasicBlockInContext<IExplodedBasicBlock>, Boolean> sources;



    // path edges corresponding to taint sources
    private final Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> initialSeeds;


    public TaintProblem(TaintDomain domain, ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> supergraph, Function<BasicBlockInContext<IExplodedBasicBlock>, Boolean> sources, CallGraph cg) {
        this.domain = domain;
        this.supergraph = supergraph;
        this.sources = sources;
        this.flowFunctions = new TaintFlowFunctions(domain, supergraph);
        this.initialSeeds = collectInitialSeeds();
    }

    /**
     * we use the entry block of the CGNode as the fake entry when propagating from
     * callee to caller with unbalanced parens
     */
    @Override
    public BasicBlockInContext<IExplodedBasicBlock> getFakeEntry(BasicBlockInContext<IExplodedBasicBlock> node) {
        final CGNode cgNode = node.getNode();
        return getFakeEntry(cgNode);
    }

    /**
     * We use the entry block of the CGNode as the "fake" entry when propagating
     * from callee to caller with unbalanced parens.
     */
    private BasicBlockInContext<IExplodedBasicBlock> getFakeEntry(final CGNode cgNode) {
        BasicBlockInContext<IExplodedBasicBlock>[] entriesForProcedure = supergraph.getEntriesForProcedure(cgNode);
        assert entriesForProcedure.length == 1;
        return entriesForProcedure[0];
    }

    /**
     * Collect sources of taint to mark as tainted.
     */
    private Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> collectInitialSeeds() {
        Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> result = HashSetFactory.make();
        for (BasicBlockInContext<IExplodedBasicBlock> bb : supergraph) {
            IExplodedBasicBlock ebb = bb.getDelegate();
            SSAInstruction instruction = ebb.getInstruction();
            CGNode node = bb.getNode();
            // taints the return of a source method
            if (sources.apply(bb)) {
                Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>> fact = Pair.make(instruction.getDef(), bb);

                int factNum = domain.add(fact);
                BasicBlockInContext<IExplodedBasicBlock> fakeEntry = getFakeEntry(node);
                // note that the fact number used for the source of this path edge doesn't really matter
                result.add(PathEdge.createPathEdge(fakeEntry, factNum, bb, factNum));
            }
        }
        return result;
    }



    @Override
    public IPartiallyBalancedFlowFunctions<BasicBlockInContext<IExplodedBasicBlock>> getFunctionMap() {
        return flowFunctions;
    }

    @Override
    public TaintDomain getDomain() {
        return domain;
    }

    /**
     * we don't need a merge function; the default unioning of tabulation works fine
     */
    @Override
    public IMergeFunction getMergeFunction() {
        return null;
    }

    @Override
    public ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> getSupergraph() {
        return supergraph;
    }

    @Override
    public Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> initialSeeds() {
        return initialSeeds;
    }
}
