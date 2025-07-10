package edu.rit.se.design.dodo.codeanalyzer.taint;

import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import edu.rit.se.design.dodo.codeanalyzer.ProjectAnalysis;

import java.util.Set;

public interface ITaintAnalyzer {
    /**
     * Computes the set of tainted pointers in a project.
     * The results are stored on a {@link ProjectAnalysis} object passed as parameter to this method.
     *
     * @param projectAnalysis a {@link ProjectAnalysis} with the project's metadata.
     */
    Set<PointerKey> analyze(ProjectAnalysis projectAnalysis);
}
