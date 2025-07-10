package edu.rit.se.design.dodo.codeanalyzer.callgraph.cfa;

import com.ibm.wala.cast.java.ipa.callgraph.AstJavaCFABuilder;
import com.ibm.wala.cast.java.ipa.callgraph.JavaScopeMappingInstanceKeys;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * k-CFA Call graph builder
 *
 * @author Joanna C. S. Santos
 */
public class AstJavaNCFABuilder extends AstJavaCFABuilder {
    /**
     * @param cha                   class hierarchy
     * @param options               analysis options
     * @param cache                 analysis cache
     * @param appContextSelector    application-specific context selector
     * @param appContextInterpreter application-specific context interpretation
     * @param n                     value for n (n-CFA)
     */
    public AstJavaNCFABuilder(
            IClassHierarchy cha,
            AnalysisOptions options,
            IAnalysisCacheView cache,
            ContextSelector appContextSelector,
            SSAContextInterpreter appContextInterpreter,
            int n) {
        super(cha, options, cache);

        // context selection
        ContextSelector def = new DefaultContextSelector(options, cha);
        ContextSelector contextSelector =
                appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);
        contextSelector = new nCFAContextSelector(n, contextSelector);
        setContextSelector(contextSelector);

        // context interpretation
        SSAContextInterpreter contextInterpreter =
                makeDefaultContextInterpreters(appContextInterpreter, options, cha);
        setContextInterpreter(contextInterpreter);

        // uses allocation site-based heap abstraction by default
        ZeroXInstanceKeys zik = new ZeroXInstanceKeys(
                options,
                cha,
                contextInterpreter,
                ZeroXInstanceKeys.ALLOCATIONS
                        | ZeroXInstanceKeys.SMUSH_MANY
                        | ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS
                        | ZeroXInstanceKeys.SMUSH_STRINGS
                        | ZeroXInstanceKeys.SMUSH_THROWABLES);

        setInstanceKeys(new JavaScopeMappingInstanceKeys(this, zik));

    }

}
