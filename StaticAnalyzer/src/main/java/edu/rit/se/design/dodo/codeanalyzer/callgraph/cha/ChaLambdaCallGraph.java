package edu.rit.se.design.dodo.codeanalyzer.callgraph.cha;

import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.cha.CHAContextInterpreter;
import com.ibm.wala.ipa.callgraph.cha.ContextInsensitiveCHAContextInterpreter;
import com.ibm.wala.ipa.callgraph.impl.BasicCallGraph;
import com.ibm.wala.ipa.callgraph.impl.FakeWorldClinitMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.LambdaSummaryClass;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeCT.BootstrapMethodsReader;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeDynamicInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.*;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.strings.Atom;

import java.lang.ref.SoftReference;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.ibm.wala.ipa.callgraph.impl.Everywhere.EVERYWHERE;

/**
 * This callgraph is built on top of {@link com.ibm.wala.ipa.callgraph.cha.CHACallGraph}.
 * It does copy many of its implementation, but it adds support to handle lambdas.
 *
 */
public class ChaLambdaCallGraph extends BasicCallGraph<CHAContextInterpreter> {
    private final WeakHashMap<BootstrapMethodsReader.BootstrapMethod, SummarizedMethod> summaries = new WeakHashMap<BootstrapMethodsReader.BootstrapMethod, SummarizedMethod>();
    private final IClassHierarchy cha;
    private final AnalysisOptions options;
    private final IAnalysisCacheView cache;
    private final IClass LambdaMetaFactoryClass;

    private final Map<CallSiteReference, Set<IMethod>> targetCache = HashMapFactory.make();
    private final Map<Selector, Set<LambdaSummaryClass>> lambdaSummaryClasses = new HashMap<>();
    private Stack<CGNode> newNodes = new Stack<CGNode>();

    /**
     * if set to true, do not include call graph edges in classes outside
     * the application class loader.  This means callbacks from library
     * to application will be ignored.
     */
    private final boolean applicationOnly;

    private boolean isInitialized = false;

    private class CHANode extends NodeImpl {

        protected CHANode(IMethod method, Context C) {
            super(method, C);
        }

        @Override
        public IR getIR() {
            return cache.getIR(method);
        }

        @Override
        public DefUse getDU() {
            return cache.getDefUse(cache.getIR(method));
        }

        @Override
        public Iterator<NewSiteReference> iterateNewSites() {
            return getInterpreter(this).iterateNewSites(this);
        }

        @Override
        public Iterator<CallSiteReference> iterateCallSites() {
            return getInterpreter(this).iterateCallSites(this);
        }

        @Override
        public boolean equals(Object obj) {
            return obj.getClass() == getClass() && getMethod().equals(((CHANode) obj).getMethod());
        }

        @Override
        public int hashCode() {
            return getMethod().hashCode();
        }

        @Override
        public boolean addTarget(CallSiteReference reference, CGNode target) {
            return false;
        }

    }

    public ChaLambdaCallGraph(IClassHierarchy cha) {
        this(cha, false);
    }

    public ChaLambdaCallGraph(IClassHierarchy cha, boolean applicationOnly) {
        this.cha = cha;
        this.options = new AnalysisOptions();
        this.cache = new AnalysisCacheImpl();
        this.applicationOnly = applicationOnly;
        this.LambdaMetaFactoryClass = cha.lookupClass(TypeReference.LambdaMetaFactory);
        this.setInterpreter(new ContextInsensitiveCHAContextInterpreter());
    }

    @SuppressWarnings("deprecation")
    public void init(Iterable<Entrypoint> entrypoints) throws CancelException {
        super.init();

        CGNode root = getFakeRootNode();
        int programCounter = 0;
        for (Entrypoint e : entrypoints) root.addTarget(e.makeSite(programCounter++), null);

        newNodes.push(root);
        // Step 1: normal CG construction with creation of lambda summaries
        closure();
        // Step 2: enhancing callgraph with lambda summaries previously computed (adding edges to synthetic nodes that act as trampolines)
        lambdasClosure();
        // Step 3: closure of the newly added synthetic methods
        closure();
        isInitialized = true;
    }

    private void lambdasClosure() throws CancelException {

        List<CallSiteReference> toVisit = targetCache.keySet().stream().filter(s -> lambdaSummaryClasses.containsKey(s.getDeclaredTarget().getSelector())).collect(Collectors.toList());

        // Phase 2: uses the cached lambda summaries to add more nodes to existing callgraph
        while (!toVisit.isEmpty()) {

            CallSiteReference site = toVisit.remove(0);
            Selector selector = site.getDeclaredTarget().getSelector();
//            if (lambdaSummaryClasses.containsKey(selector)) {
            Set<LambdaSummaryClass> lambdaSummaryClasses = this.lambdaSummaryClasses.get(selector);

            for (LambdaSummaryClass c : lambdaSummaryClasses) {
                IMethod target = c.getMethod(selector);
                if (isRelevantMethod(target)) {
                    CGNode callee = getNode(target, EVERYWHERE);
                    if (callee == null) {
                        callee = findOrCreateNode(target, EVERYWHERE);
                    }

                    Set<IMethod> newCachedMethods = new HashSet<>();
                    newCachedMethods.add(target);
                    Set<IMethod> oldCache = targetCache.get(site);
                    if (oldCache != null) newCachedMethods.addAll(oldCache);
                    targetCache.put(site, newCachedMethods);
                }


            }

//            }


        }


    }

    @Override
    public IClassHierarchy getClassHierarchy() {
        return cha;
    }


    private boolean isLambdaMetaFactory(CallSiteReference site) {
        return (!site.getDeclaredTarget().getName().equals(MethodReference.clinitName) &&
                LambdaMetaFactoryClass != null &&
                LambdaMetaFactoryClass.equals(cha.lookupClass(site.getDeclaredTarget().getDeclaringClass())));
    }

    private Iterator<IMethod> getPossibleTargets(CallSiteReference site) {
        Set<IMethod> result = targetCache.get(site);
        if (result == null) {
            if (site.isDispatch()) {
                result = cha.getPossibleTargets(site.getDeclaredTarget());
            } else {
                IMethod m = cha.resolveMethod(site.getDeclaredTarget());
                if (m != null) {
                    result = Collections.singleton(m);
                } else {
                    result = Collections.emptySet();
                }
            }
            targetCache.put(site, result);
        }
        return result.iterator();
    }

    @Override
    public Set<CGNode> getPossibleTargets(CGNode node, CallSiteReference site) {
        return Iterator2Collection.toSet(
                new MapIterator<IMethod, CGNode>(
                        new FilterIterator<IMethod>(
                                getPossibleTargets(site),
                                new Predicate<IMethod>() {
                                    @Override
                                    public boolean test(IMethod o) {
                                        return isRelevantMethod(o);
                                    }
                                }
                        ),
                        new Function<IMethod, CGNode>() {
                            @Override
                            public CGNode apply(IMethod object) {
                                try {
                                    return findOrCreateNode(object, EVERYWHERE);
                                } catch (CancelException e) {
                                    assert false : e.toString();
                                    return null;
                                }
                            }
                        }));
    }

    @Override
    public int getNumberOfTargets(CGNode node, CallSiteReference site) {
        return IteratorUtil.count(getPossibleTargets(site));
    }

    @Override
    public Iterator<CallSiteReference> getPossibleSites(final CGNode src, final CGNode target) {
        return
                new FilterIterator<CallSiteReference>(getInterpreter(src).iterateCallSites(src),
                        new Predicate<CallSiteReference>() {
                            @Override
                            public boolean test(CallSiteReference o) {
                                return getPossibleTargets(src, o).contains(target);
                            }
                        });
    }

    private class CHARootNode extends CHANode {
        private final Set<CallSiteReference> calls = HashSetFactory.make();

        protected CHARootNode(IMethod method, Context C) {
            super(method, C);
        }

        @Override
        public Iterator<CallSiteReference> iterateCallSites() {
            return calls.iterator();
        }

        @Override
        public boolean addTarget(CallSiteReference reference, CGNode target) {
            return calls.add(reference);
        }
    }

    @Override
    protected CGNode makeFakeRootNode() throws CancelException {
        return new CHARootNode(Language.JAVA.getFakeRootMethod(cha, options, cache), EVERYWHERE);
    }


    @Override
    protected CGNode makeFakeWorldClinitNode() throws CancelException {
        return new CHARootNode(
                new FakeWorldClinitMethod(
                        Language.JAVA.getFakeRootMethod(cha, options, cache).getDeclaringClass(),
                        options,
                        cache),
                EVERYWHERE);
    }

    private int clinitPC = 0;

    @Override
    @SuppressWarnings("deprecation")
    public CGNode findOrCreateNode(IMethod method, Context C) throws CancelException {
        assert C.equals(EVERYWHERE);
        assert !method.isAbstract();

        CGNode n = getNode(method, C);
        if (n == null) {
            assert !isInitialized;
            n = makeNewNode(method, C);

            IMethod clinit = method.getDeclaringClass().getClassInitializer();
            if (clinit != null && getNode(clinit, EVERYWHERE) == null) {
                CGNode cln = makeNewNode(clinit, EVERYWHERE);
                CGNode clinits = getFakeWorldClinitNode();
                clinits.addTarget(CallSiteReference.make(clinitPC++, clinit.getReference(), IInvokeInstruction.Dispatch.STATIC), cln);
            }
        }
        return n;
    }


    public void computeLambdaTrampoline(CGNode node, CallSiteReference site) throws CancelException {
        if (isLambdaMetaFactory(site)) {
            SSAInvokeDynamicInstruction invoke = (SSAInvokeDynamicInstruction) node.getIR().getCalls(site)[0];

            if (!summaries.containsKey(invoke.getBootstrap())) {
                String cls = node.getMethod().getDeclaringClass().getName().toString().replace("/", "$").substring(1);
                int bootstrapIndex = invoke.getBootstrap().getIndexInClassFile();
                MethodReference ref =
                        MethodReference.findOrCreate(
                                site.getDeclaredTarget().getDeclaringClass(),
                                Atom.findOrCreateUnicodeAtom(site.getDeclaredTarget().getName().toString() + "$" + cls + "$" + bootstrapIndex),
                                site.getDeclaredTarget().getDescriptor());

                MethodSummary summary = new MethodSummary(ref);

                if (site.isStatic()) {
                    summary.setStatic(true);
                }

                int index = 0;
                int v = site.getDeclaredTarget().getNumberOfParameters() + 2;
                IClass lambda = LambdaSummaryClass.create(node, invoke);


                Selector selector = lambda.getDeclaredMethods().iterator().next().getSelector();
                if (lambdaSummaryClasses.containsKey(selector))
                    lambdaSummaryClasses.get(selector).add((LambdaSummaryClass) lambda);
                else {
                    Set<LambdaSummaryClass> hashSet = new HashSet<>();
                    hashSet.add(((LambdaSummaryClass) lambda));
                    lambdaSummaryClasses.put(selector, hashSet);
                }


                SSAInstructionFactory insts = Language.JAVA.instructionFactory();
                summary.addStatement(insts.NewInstruction(index, v, NewSiteReference.make(index, lambda.getReference())));
                index++;
                for (int i = 0; i < site.getDeclaredTarget().getNumberOfParameters(); i++) {
                    summary.addStatement(
                            insts.PutInstruction(index++, v, i + 1, lambda.getField(Atom.findOrCreateUnicodeAtom("c" + i)).getReference()));
                }
                summary.addStatement(insts.ReturnInstruction(index++, v, false));

                summaries.put(invoke.getBootstrap(), new SummarizedMethod(ref, summary, cha.lookupClass(site.getDeclaredTarget().getDeclaringClass())));
            }

            IMethod m = summaries.get(invoke.getBootstrap());
            if (m != null && isRelevantMethod(m)) {
                CGNode callee = getNode(m, EVERYWHERE);
                if (callee == null) {
                    callee = findOrCreateNode(m, EVERYWHERE);
                }
                targetCache.put(site, Collections.singleton(m));
            }
        }
    }

    private void closure() throws CancelException {
        // Phase 1: build the cha callgraph normally, and also computes trampolines/summaries to create a cache
        while (!newNodes.isEmpty()) {
            CGNode n = newNodes.pop();
            for (Iterator<CallSiteReference> sites = n.iterateCallSites(); sites.hasNext(); ) {
                CallSiteReference site = sites.next();
                if (isLambdaMetaFactory(site)) {
                    computeLambdaTrampoline(n, site);
                } else {
                    Iterator<IMethod> methods = getPossibleTargets(site);
                    while (methods.hasNext()) {
                        IMethod target = methods.next();
                        if (isRelevantMethod(target)) {
                            CGNode callee = getNode(target, EVERYWHERE);
                            if (callee == null) {
                                callee = findOrCreateNode(target, EVERYWHERE);
                                if (n == getFakeRootNode()) {
                                    registerEntrypoint(callee);
                                }
                            }
                        }
                    }
                }
            }
        }


    }

    private boolean isRelevantMethod(IMethod target) {
        return !target.isAbstract()
                && (!applicationOnly
                || cha.getScope().isApplicationLoader(target.getDeclaringClass().getClassLoader()));
    }

    private CGNode makeNewNode(IMethod method, Context C) {
        CGNode n;
        Key k = new Key(method, C);
        n = new CHANode(method, C);
        registerNode(k, n);
        newNodes.push(n);
        return n;
    }

    @Override
    protected NumberedEdgeManager<CGNode> getEdgeManager() {
        return new NumberedEdgeManager<CGNode>() {
            private final Map<CGNode, SoftReference<Set<CGNode>>> predecessors = HashMapFactory.make();

            private Set<CGNode> getPreds(CGNode n) {
                if (predecessors.containsKey(n) && predecessors.get(n).get() != null) {
                    return predecessors.get(n).get();
                } else {
                    Set<CGNode> preds = HashSetFactory.make();
                    for (CGNode node : ChaLambdaCallGraph.this) {
                        if (getPossibleSites(node, n).hasNext()) {
                            preds.add(node);
                        }
                    }
                    predecessors.put(n, new SoftReference<Set<CGNode>>(preds));
                    return preds;
                }
            }

            @Override
            public Iterator<CGNode> getPredNodes(CGNode n) {
                return getPreds(n).iterator();
            }

            @Override
            public int getPredNodeCount(CGNode n) {
                return getPreds(n).size();
            }

            @Override
            public Iterator<CGNode> getSuccNodes(final CGNode n) {
                return new FilterIterator<CGNode>(new ComposedIterator<CallSiteReference, CGNode>(n.iterateCallSites()) {
                    @Override
                    public Iterator<? extends CGNode> makeInner(CallSiteReference outer) {
                        return getPossibleTargets(n, outer).iterator();
                    }
                },
                        new Predicate<CGNode>() {
                            private final MutableIntSet nodes = IntSetUtil.make();

                            @Override
                            public boolean test(CGNode o) {
                                if (nodes.contains(o.getGraphNodeId())) {
                                    return false;
                                } else {
                                    nodes.add(o.getGraphNodeId());
                                    return true;
                                }
                            }
                        });
            }

            @Override
            public int getSuccNodeCount(CGNode N) {
                return IteratorUtil.count(getSuccNodes(N));
            }

            @Override
            public void addEdge(CGNode src, CGNode dst) {
                assert false;
            }

            @Override
            public void removeEdge(CGNode src, CGNode dst) throws UnsupportedOperationException {
                assert false;
            }

            @Override
            public void removeAllIncidentEdges(CGNode node) throws UnsupportedOperationException {
                assert false;
            }

            @Override
            public void removeIncomingEdges(CGNode node) throws UnsupportedOperationException {
                assert false;
            }

            @Override
            public void removeOutgoingEdges(CGNode node) throws UnsupportedOperationException {
                assert false;
            }

            @Override
            public boolean hasEdge(CGNode src, CGNode dst) {
                return getPossibleSites(src, dst).hasNext();
            }

            @Override
            public IntSet getSuccNodeNumbers(CGNode node) {
                MutableIntSet result = IntSetUtil.make();
                for (Iterator<CGNode> ss = getSuccNodes(node); ss.hasNext(); ) {
                    result.add(ss.next().getGraphNodeId());
                }
                return result;
            }

            @Override
            public IntSet getPredNodeNumbers(CGNode node) {
                MutableIntSet result = IntSetUtil.make();
                for (Iterator<CGNode> ss = getPredNodes(node); ss.hasNext(); ) {
                    result.add(ss.next().getGraphNodeId());
                }
                return result;
            }

        };
    }

}
