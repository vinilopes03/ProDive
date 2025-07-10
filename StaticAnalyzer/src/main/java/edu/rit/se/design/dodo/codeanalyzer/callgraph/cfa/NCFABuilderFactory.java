package edu.rit.se.design.dodo.codeanalyzer.callgraph.cfa;

import com.ibm.wala.cast.java.ipa.callgraph.AstJavaZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * @author Joanna C. S. Santos
 * A factory to create call graph builders using k-CFA.
 */
public class NCFABuilderFactory {
    public AstJavaNCFABuilder make(
            AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha, AnalysisScope scope, int n) {
        Util.addDefaultSelectors(options, cha);
        Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);
        return new AstJavaNCFABuilder(cha, options, cache, null, null, n);
    }
}
