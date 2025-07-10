package edu.rit.se.design.dodo.codeanalyzer.instructions;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import edu.rit.se.design.dodo.utils.wala.SinkDescriptor;

import java.util.Objects;

/**
 * It represents sink statement (it is a method call to a sensitive operation).
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class SinkInstruction {
    // class where the invocation sink is located
    private final IClass wrapperIClass;
    // method that actually invokes the sinks
    private final IMethod wrapperIMethod;
    // actual invocation to the sink
    private final SSAAbstractInvokeInstruction instruction;
    // describes the sink's characteristics
    private final SinkDescriptor sinkDescriptor;


    public SinkInstruction(IClass wrapperIClass, IMethod wrapperIMethod, SSAAbstractInvokeInstruction instruction, SinkDescriptor sinkDescriptor) {
        this.wrapperIClass = wrapperIClass;
        this.wrapperIMethod = wrapperIMethod;
        this.sinkDescriptor = sinkDescriptor;
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

    public SinkDescriptor getSinkDescriptor() {
        return sinkDescriptor;
    }

    @Override
    public String toString() {
        return "SinkInstruction@" + Integer.toHexString(hashCode())+"{" +
                "\n\twrapperIClass=" + Objects.toString(wrapperIClass.getName()) +
                ",\n\twrapperIMethod=" + Objects.toString(wrapperIMethod.getSignature()) +
                ",\n\tinstruction=" + Objects.toString(instruction) +
                ",\n\tsinkDescriptor=" + Objects.toString(sinkDescriptor) +
                "\n}";
    }
}
