package edu.rit.se.design.dodo.codeanalyzer.taint.graph;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import edu.rit.se.design.dodo.codeanalyzer.ProjectAnalysis;
import edu.rit.se.design.dodo.codeanalyzer.taint.ITaintAnalyzer;

import java.util.*;

public class GraphTaintAnalyzer implements ITaintAnalyzer {
    @Override
    public Set<PointerKey> analyze(ProjectAnalysis projectAnalysis) {
        // builds the data-only SDG first
        if (projectAnalysis.getSdg() == null) {
//            buildSDG(Slicer.DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS, Slicer.ControlDependenceOptions.NONE);
        }

        SDG<InstanceKey> sdg = projectAnalysis.getSdg();

        // taint rules #1: all parameters passed to an entrypoint are tainted
        Set<Statement> taintedStatements = new LinkedHashSet<>();
        for (CGNode ep : sdg.getCallGraph().getEntrypointNodes()) {
            DefUse defUse = ep.getDU();
            IR ir = ep.getIR();
            Map<SSAInstruction, Integer> instructionIndices = PDG.computeInstructionIndices(ir);
            // each variable is identified with a number starting from 1
            // static methods 1 is equals to the first param;
            // instance methods, 1 is equals to the "this" pointer of the class
            int initialValue = ep.getMethod().isStatic() ? 1 : 2;
            for (int i = initialValue; i < ep.getMethod().getNumberOfParameters() + initialValue; i++) {
                Iterator<SSAInstruction> uses = defUse.getUses(i);
                uses.forEachRemaining(ssaInstruction -> {
                    Statement statement = PDG.ssaInstruction2Statement(ep, ssaInstruction, instructionIndices, ir);
                    taintedStatements.add(statement);
                });
            }
            // marks the param calee statements
            for (Statement stmt : sdg.getPDG(ep).getParamCalleeStatements()) {
                taintedStatements.add(stmt);
            }
        }
        taintedStatements.forEach(s -> {
            System.out.println(s);
        });
        // taint rules #2: all statements that invokes sources


//        Set<Statement> sourceStatements = this.projectAnalysis.getSourceStatements();
        // TODO: compute forward slice
        try {
            Collection<Statement> slice = new Slicer().slice(sdg, taintedStatements, false);
//             System.out.println(slice);
        } catch (CancelException e) {
            e.printStackTrace();
        }
        return null;
    }
}
