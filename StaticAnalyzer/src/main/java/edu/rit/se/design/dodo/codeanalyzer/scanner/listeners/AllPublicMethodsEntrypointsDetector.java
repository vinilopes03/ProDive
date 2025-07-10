package edu.rit.se.design.dodo.codeanalyzer.scanner.listeners;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.ArgumentTypeEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.collections.HashSetFactory;
import edu.rit.se.design.dodo.codeanalyzer.scanner.IEntrypointsDetector;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class AllPublicMethodsEntrypointsDetector implements IEntrypointsDetector {
    private final IClassHierarchy cha;
    private Set<Entrypoint> entrypoints;

    /**
     * @param cha class hierarchy.
     * @throws IOException error while loading entrypoints from CSV file.
     */
    public AllPublicMethodsEntrypointsDetector(IClassHierarchy cha) throws IOException {
        this.entrypoints = HashSetFactory.make();
        this.cha = cha;
    }

    @Override
    public void parseClass(IClass klass) {
        if (!klass.isInterface()) {
            if (isApplicationClass(cha.getScope(), klass)) {
                for (IMethod method : klass.getDeclaredMethods()) {
                    if (!method.isAbstract()) {
                        this.entrypoints.add(new ArgumentTypeEntrypoint(method, cha));
                    }
                }
            }
        }
    }

    /**
     * @return
     */
    @Override
    public Set<IMethod> getEntrypointMethods() {
        return entrypoints.stream().map(e -> e.getMethod()).collect(Collectors.toSet());
    }

    /**
     * @return
     */
    @Override
    public Set<Entrypoint> getEntrypoints() {
        return entrypoints;
    }

    /**
     * @return true iff klass is loaded by the application loader.
     */
    private static boolean isApplicationClass(AnalysisScope scope, IClass klass) {
        return scope.getApplicationLoader().equals(klass.getClassLoader().getReference());
    }
}
