package edu.rit.se.design.dodo.codeanalyzer;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.config.AnalysisScopeReader;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.File;

/**
 * Utility class.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class CodeAnalyzerTestUtils {

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            System.out.println(CodeAnalyzerTestUtils.this.getClass().getSimpleName() + "::" + description.getMethodName());
        }

        @Override
        protected void succeeded(Description description) {
            System.out.println(CodeAnalyzerTestUtils.this.getClass().getSimpleName() + "::" + description.getMethodName() + " PASSED");
        }

        @Override
        protected void failed(Throwable e, Description description) {
            System.err.println(CodeAnalyzerTestUtils.this.getClass().getSimpleName() + "::" + description.getMethodName() + " FAILED");
        }

    };
    protected static final String ROOT_TEST_ASSETS = "../../../DODO-TestData/code-analyzer/";
    protected static final String CALLGRAPH_TEST_ASSETS = ROOT_TEST_ASSETS + "/callgraph/";


    protected IClassHierarchy buildClassHierarchy(String path, String exclusionFilePath) throws Exception {
        // gets an analysis scope on top of the source code
        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(path, new File(exclusionFilePath));
        // build the class hierarchy
        return ClassHierarchyFactory.make(scope);
    }

}
