package edu.rit.se.design.dodo.codeanalyzer.scanner;

import edu.rit.se.design.dodo.codeanalyzer.instructions.SourceInstruction;

import java.util.Set;

/**
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public interface ISourcesDetector extends InstructionScannerListener {
    Set<SourceInstruction> getSources();
}
