package edu.rit.se.design.dodo.codeanalyzer.instructions;



import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import edu.rit.se.design.dodo.utils.wala.SinkDescriptor;
import edu.rit.se.design.dodo.utils.wala.SourceDescriptor;

import java.util.Objects;

/**
 * It represents source statement (it is a method call to a sensitive operation).
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class SourceInstruction {
    // class where the invocation source is located
    private final IClass wrapperIClass;
    // method that actually invokes the source
    private final IMethod wrapperIMethod;
    // actual invocation to the source
    private final SSAAbstractInvokeInstruction instruction;
    // describes the source's characteristics
    private final SourceDescriptor sourceDescriptor;


    public SourceInstruction(IClass wrapperIClass, IMethod wrapperIMethod, SSAAbstractInvokeInstruction instruction, SourceDescriptor sourceDescriptor) {
        this.wrapperIClass = wrapperIClass;
        this.wrapperIMethod = wrapperIMethod;
        this.sourceDescriptor = sourceDescriptor;
        this.instruction = instruction;
    }

    public IClass getWrapperIClass() {
        return wrapperIClass;
    }

    public IMethod getWrapperIMethod() {
        return wrapperIMethod;
    }

    public SSAAbstractInvokeInstruction getInstruction() {
        return instruction;
    }

    public SourceDescriptor getSourceDescriptor() {
        return sourceDescriptor;
    }

    @Override
    public String toString() {
        return "SourceInstruction{" +
                "wrapperIClass=" + wrapperIClass +
                ", wrapperIMethod=" + wrapperIMethod +
                ", instruction=" + instruction +
                ", sourceDescriptor=" + sourceDescriptor +
                '}';
    }
}
