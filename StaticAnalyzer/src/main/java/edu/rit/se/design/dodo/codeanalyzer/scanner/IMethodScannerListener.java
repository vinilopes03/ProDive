package edu.rit.se.design.dodo.codeanalyzer.scanner;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;

/**
 * Interface for listeners interested only on methods {@link IMethod}.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public interface IMethodScannerListener {
    void parseMethod(IClass c, IMethod m);
}
