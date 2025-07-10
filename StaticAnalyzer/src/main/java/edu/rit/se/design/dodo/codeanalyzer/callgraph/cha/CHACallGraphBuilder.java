package edu.rit.se.design.dodo.codeanalyzer.callgraph.cha;


import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyClassTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyMethodTargetSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.rta.TypeBasedPointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.LambdaMethodTargetSelector;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.collections.HashSetFactory;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * It is a builder for the CHA Callgraph
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class CHACallGraphBuilder implements CallGraphBuilder<InstanceKey> {

    private IClassHierarchy cha;
    private CHACallGraph cg;
    private AnalysisCache cache;
    private AnalysisOptions options;

    @Override
    public CallGraph makeCallGraph(AnalysisOptions options, MonitorUtil.IProgressMonitor monitor) throws IllegalArgumentException, CallGraphBuilderCancelException {
        AnalysisScope scope = options.getAnalysisScope();
        Iterable<? extends Entrypoint> entrypoints = options.getEntrypoints();

        try {
            this.cha = ClassHierarchyFactory.make(scope);
            this.cache = new AnalysisCacheImpl();
            this.options = options;
            this.options.setSelector(new ClassHierarchyClassTargetSelector(this.cha));
            this.options.setSelector(new ClassHierarchyMethodTargetSelector(this.cha));
            this.cg = new CHACallGraph(cha);
            this.cg.init((Iterable<Entrypoint>) entrypoints);
        } catch (CancelException | ClassHierarchyException ex) {
            throw CallGraphBuilderCancelException.createCallGraphBuilderCancelException(ex, cg, getPointerAnalysis());
        }

        return cg;
    }

    @Override
    public PointerAnalysis<InstanceKey> getPointerAnalysis() {
        //FIXME this may not be a good implementation actually
        HashSet<IClass> allClasses = HashSetFactory.make();
        cha.forEach(allClasses::add);
        return TypeBasedPointerAnalysis.make(this.options, allClasses.stream().filter(c->!c.isInterface() && !c.isAbstract() ).collect(Collectors.toSet()), this.cg);
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
