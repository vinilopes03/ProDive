package edu.rit.se.design.dodo.utils.wala;

import com.ibm.wala.types.MethodReference;

import java.util.Arrays;

/**
 * It is a method signature of a method that provides external data to the application.
 *
 * @author Joanna C. S. Santos <jds5109@rit.edu>
 */
public class SourceDescriptor {
    // it is used to indicate what are the variable(s) that are dangerous
    // -1 = return type
    // 0 = if instance method, the receiver object is tainted - i.e., obj.sink() --> obj is tainted
    // 0 = if static method, the first parameter
    // 1..n = the n-th parameter (starting from 0 or 1, depending on whether the method is static or not)
    private final int[] taintedIndexes;

    // the method reference of this object
    private final MethodReference methodRef;

    public SourceDescriptor(MethodReference methodRef, int[] taintedIndexes) {
        this.methodRef = methodRef;
        this.taintedIndexes = taintedIndexes;
    }

    /**
     * It is used to indicate what are the variable(s) that are dangerous
     * -1 = return type
     * 0 = if instance method, the receiver object is tainted - i.e., obj.sink() --> obj is tainted
     * 0 = if static method, the first parameter
     * 1..n = the n-th parameter (starting from 0 or 1, depending on whether the method is static or not)
     *
     * @return an array of tainted indexes
     */
    public int[] getTaintedIndexes() {
        return taintedIndexes;
    }

    public MethodReference getMethodRef() {
        return methodRef;
    }


    @Override
    public String toString() {
        return "SourceDescriptor{" +
                "taintedIndexes=" + Arrays.toString(taintedIndexes) +
                ", methodRef=" + methodRef +
                '}';
    }
}
