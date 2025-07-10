package edu.rit.se.design.dodo.codeanalyzer.taint.ifds;

import com.ibm.wala.dataflow.IFDS.PathEdge;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.MutableMapping;

/**
 * Domain of facts for tabulation.
 * Facts are a {@link Pair<Integer,BasicBlockInContext<IExplodedBasicBlock>>}.
 * It tracks tainted value numbers for each node
 */
public class TaintDomain extends MutableMapping<Pair<Integer,BasicBlockInContext<IExplodedBasicBlock>>>
        implements TabulationDomain<Pair<Integer,BasicBlockInContext<IExplodedBasicBlock>>, BasicBlockInContext<IExplodedBasicBlock>> {

    private static final long serialVersionUID = -1897766113586243833L;

    @Override
    public boolean hasPriorityOver(PathEdge<BasicBlockInContext<IExplodedBasicBlock>> p1,
                                   PathEdge<BasicBlockInContext<IExplodedBasicBlock>> p2) {
        // don't worry about worklist priorities
        return false;
    }
}
