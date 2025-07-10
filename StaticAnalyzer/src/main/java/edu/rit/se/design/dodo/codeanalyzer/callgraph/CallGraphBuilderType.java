package edu.rit.se.design.dodo.codeanalyzer.callgraph;

/**
 * Type of call graph to be used.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public enum CallGraphBuilderType {
    CHA,
    LAMBDA_CHA, // custom created
    RTA,
    ZERO_CFA,
    ZERO_CONTAINER_CFA,
    VANILLA_ZERO_ONE_CFA,
    ZERO_ONE_CFA,
    ZERO_ONE_CONTAINER_CFA,
    N1_CFA,
    N2_CFA,
}
