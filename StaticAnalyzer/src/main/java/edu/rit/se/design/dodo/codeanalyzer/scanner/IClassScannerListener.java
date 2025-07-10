package edu.rit.se.design.dodo.codeanalyzer.scanner;

import com.ibm.wala.classLoader.IClass;

/**
 * Interface for listeners interested only on classes {@link IClass}.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public interface IClassScannerListener {
    void parseClass(IClass c);
}
