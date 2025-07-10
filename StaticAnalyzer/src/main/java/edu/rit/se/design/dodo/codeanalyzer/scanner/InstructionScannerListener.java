package edu.rit.se.design.dodo.codeanalyzer.scanner;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;

/**
 * Interface for listeners interested only on invocation instructions {@link SSAAbstractInvokeInstruction}.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public interface InstructionScannerListener {
    void parseInstruction(IClass c, IMethod m, SSAAbstractInvokeInstruction call);
}
