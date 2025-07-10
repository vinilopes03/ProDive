package edu.rit.se.design.dodo.codeanalyzer.scanner.listeners;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import edu.rit.se.design.dodo.codeanalyzer.instructions.SinkInstruction;
import edu.rit.se.design.dodo.codeanalyzer.scanner.ISinksDetector;
import edu.rit.se.design.dodo.utils.wala.SinkDescriptor;
import edu.rit.se.design.dodo.utils.wala.WalaUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.rit.se.design.dodo.utils.wala.MethodDescriptorsLoader.loadSinksFromCsv;

/**
 * It detects the statements in a bytecode that are invoking the sinks of interest.
 * The methods under interest are specified within a CSV file.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class FileSinksDetector implements ISinksDetector {
    private final Set<SinkInstruction> sinks;
    private final Map<String, SinkDescriptor> sinkSignatures;
    private final boolean excludePrimordialReflectionSinks;

    /**
     * @param sinksFilePath path to the CSV file with the sinks' signatures
     * @throws IOException
     */
    public FileSinksDetector(String sinksFilePath, boolean excludePrimordialReflectionSinks) throws IOException {
        this.sinks = new HashSet<>();
        this.sinkSignatures = loadSinksFromCsv(new File(sinksFilePath), true)
                .stream()
                .collect(
                        Collectors.toMap(
                                (SinkDescriptor d) -> d.getMethodRef().getSignature(),
                                (SinkDescriptor d) -> d
                        )
                );
        this.excludePrimordialReflectionSinks = excludePrimordialReflectionSinks;
    }

    /**
     * @param sinksFilePath path to the CSV file with the sinks' signatures
     * @throws IOException
     */
    public FileSinksDetector(String sinksFilePath) throws IOException {
        this.sinks = new HashSet<>();
        this.excludePrimordialReflectionSinks = false;
        this.sinkSignatures = loadSinksFromCsv(new File(sinksFilePath), true)
                .stream()
                .collect(
                        Collectors.toMap(
                                (SinkDescriptor d) -> d.getMethodRef().getSignature(),
                                (SinkDescriptor d) -> d
                        )
                );
    }

    /**
     * Returns list of sink statements found in all classes (application-level and library-level).
     */
    @Override
    public Set<SinkInstruction> getSinks() {
        return this.sinks;
    }

    @Override
    public void parseInstruction(IClass c, IMethod m, SSAAbstractInvokeInstruction call) {
        MethodReference declaredTarget = call.getDeclaredTarget();
        String signature = declaredTarget.getSignature();

        if (sinkSignatures.containsKey(signature)) {
            if (!excludePrimordialReflectionSinks || !WalaUtils.isPrimordialScope(m.getDeclaringClass()))
                this.sinks.add(new SinkInstruction(c, m, call, sinkSignatures.get(signature)));
        }
    }
}
