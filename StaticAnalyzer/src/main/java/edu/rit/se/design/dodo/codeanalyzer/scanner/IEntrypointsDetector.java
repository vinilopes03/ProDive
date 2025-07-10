package edu.rit.se.design.dodo.codeanalyzer.scanner;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;

import java.util.Set;

/**
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public interface IEntrypointsDetector extends IClassScannerListener {
    /**
     * @return
     */
    Set<IMethod> getEntrypointMethods();

    /**
     * @return
     */
    Set<Entrypoint> getEntrypoints();
}
