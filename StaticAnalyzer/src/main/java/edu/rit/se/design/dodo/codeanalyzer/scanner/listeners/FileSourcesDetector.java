package edu.rit.se.design.dodo.codeanalyzer.scanner.listeners;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import edu.rit.se.design.dodo.codeanalyzer.instructions.SourceInstruction;
import edu.rit.se.design.dodo.codeanalyzer.scanner.ISourcesDetector;
import edu.rit.se.design.dodo.utils.wala.SourceDescriptor;
import edu.rit.se.design.dodo.utils.wala.WalaUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.rit.se.design.dodo.utils.wala.MethodDescriptorsLoader.loadSourcesFromCsv;

/**
 * It detects the statements in a bytecode that are invoking sources.
 * The methods under interest are specified within a CSV file.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class FileSourcesDetector implements ISourcesDetector {
    private final Set<SourceInstruction> sources;
    private final Map<String, SourceDescriptor> sourceSignatures;
    private final boolean excludePrimordialReflectionSources;


    /**
     * @param sourcesFilePath path to the CSV file with the sources' signatures
     * @throws IOException
     */
    public FileSourcesDetector(String sourcesFilePath, boolean excludePrimordialReflectionSources) throws IOException {
        this.sources = new HashSet<>();
        this.sourceSignatures = loadSourcesFromCsv(new File(sourcesFilePath), true)
                .stream()
                .collect(
                        Collectors.toMap(
                                (SourceDescriptor d) -> d.getMethodRef().getSignature(),
                                (SourceDescriptor d) -> d
                        )
                );
        this.excludePrimordialReflectionSources = excludePrimordialReflectionSources;
    }

    /**
     * @param sourcesFilePath path to the CSV file with the source' signatures
     * @throws IOException
     */
    public FileSourcesDetector(String sourcesFilePath) throws IOException {
        this(sourcesFilePath, false);
    }

    /**
     * Returns list of source statements found in all classes (application-level and library-level).
     */
    @Override
    public Set<SourceInstruction> getSources() {
        return this.sources;
    }

    @Override
    public void parseInstruction(IClass c, IMethod m, SSAAbstractInvokeInstruction call) {
        MethodReference declaredTarget = call.getDeclaredTarget();
        String signature = declaredTarget.getSignature();

        if (sourceSignatures.containsKey(signature)) {
            if (!excludePrimordialReflectionSources || !WalaUtils.isPrimordialScope(m.getDeclaringClass()))
                this.sources.add(new SourceInstruction(c, m, call, sourceSignatures.get(signature)));
        }
    }
}
