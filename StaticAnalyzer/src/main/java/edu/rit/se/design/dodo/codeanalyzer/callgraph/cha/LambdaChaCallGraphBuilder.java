package edu.rit.se.design.dodo.codeanalyzer.callgraph.cha;


import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.rta.TypeBasedPointerAnalysis;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.collections.HashSetFactory;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * It is a builder for the CHA Callgraph but that supports lambdas.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class LambdaChaCallGraphBuilder implements CallGraphBuilder<InstanceKey> {

    private IClassHierarchy cha;
    private ChaLambdaCallGraph cg;
    private AnalysisCache cache;
    private AnalysisOptions options;

    public LambdaChaCallGraphBuilder(IClassHierarchy cha) {
        this.cha =  cha;
    }

    @Override
    public CallGraph makeCallGraph(AnalysisOptions options, MonitorUtil.IProgressMonitor monitor) throws IllegalArgumentException, CallGraphBuilderCancelException {

        Iterable<? extends Entrypoint> entrypoints = options.getEntrypoints();

        try {
            this.cache = new AnalysisCacheImpl();
            this.options = options;
            this.cg = new ChaLambdaCallGraph(cha);
            this.cg.init((Iterable<Entrypoint>) entrypoints);
        } catch (CancelException ex) {
            throw CallGraphBuilderCancelException.createCallGraphBuilderCancelException(ex, cg, getPointerAnalysis());
        }

        return cg;
    }

    @Override
    public PointerAnalysis<InstanceKey> getPointerAnalysis() {
        HashSet<IClass> allClasses = HashSetFactory.make();
        cha.forEach(allClasses::add);
        return TypeBasedPointerAnalysis.make(this.options, allClasses.stream().filter(c -> !c.isInterface() && !c.isAbstract()).collect(Collectors.toSet()), this.cg);
    }

    @Override
    public IAnalysisCacheView getAnalysisCache() {
        return cache;
    }

    @Override
    public IClassHierarchy getClassHierarchy() {
        return cha;
    }

}
