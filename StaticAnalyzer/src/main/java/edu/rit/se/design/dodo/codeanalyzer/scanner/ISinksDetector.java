package edu.rit.se.design.dodo.codeanalyzer.scanner;

import edu.rit.se.design.dodo.codeanalyzer.instructions.SinkInstruction;
import edu.rit.se.design.dodo.codeanalyzer.scanner.InstructionScannerListener;

import java.util.Set;

/**
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public interface ISinksDetector extends InstructionScannerListener {
    Set<SinkInstruction> getSinks();
}
