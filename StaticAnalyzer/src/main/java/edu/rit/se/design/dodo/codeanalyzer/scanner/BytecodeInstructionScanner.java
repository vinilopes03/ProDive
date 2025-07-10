package edu.rit.se.design.dodo.codeanalyzer.scanner;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAOptions;

import java.util.Arrays;

/**
 * Scans the bytecode and invokes listeners as classes, methods and invoke instructions are traversed.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class BytecodeInstructionScanner {


    private IClassScannerListener[] classListeners;
    private IMethodScannerListener[] methodListeners;
    private InstructionScannerListener[] instructionListeners;

    public BytecodeInstructionScanner(IClassScannerListener[] classListeners, IMethodScannerListener[] methodListeners, InstructionScannerListener[] instructionListeners) {
        if (classListeners == null && methodListeners == null && instructionListeners == null)
            throw new IllegalArgumentException("At least one listener has to be provided!");
        this.classListeners = classListeners;
        this.methodListeners = methodListeners;
        this.instructionListeners = instructionListeners;
    }


    /**
     * Iterates over each instruction and invokes listeners.
     *
     * @param cha class hierarchy
     */
    public void scan(IClassHierarchy cha, IRFactory<IMethod> irFactory, SSAOptions ssaOptions) {
        // iterates over each class in the class hierarchy
        for (IClass aClass : cha) {
            if (aClass.isInterface()) continue; // ignore if it is an interface without implementation
            // invoke listeners to process the method declaration
            if (classListeners != null) Arrays.stream(classListeners).forEach(l -> l.parseClass(aClass));

            // in each declared method in the class
            for (IMethod aMethod : aClass.getDeclaredMethods()) {
                if (aMethod.isAbstract() || aMethod.isNative() || aMethod.isSynthetic())
                    continue; // ignore if method has no implementation

                // invoke listeners to process the method declaration
                if (methodListeners != null)
                    Arrays.stream(methodListeners).forEach(l -> l.parseMethod(aClass, aMethod));

                IR ir = irFactory.makeIR(aMethod, Everywhere.EVERYWHERE, ssaOptions);
                ir.iterateCallSites().forEachRemaining(callSiteReference -> {
                    // invoke listeners to process each method call instruction
                    for (SSAAbstractInvokeInstruction call : ir.getCalls(callSiteReference)) {
                        if (instructionListeners != null)
                            Arrays.stream(instructionListeners).forEach(l -> l.parseInstruction(aClass, aMethod, call));
                    }
                });
            }
        }
    }


}
