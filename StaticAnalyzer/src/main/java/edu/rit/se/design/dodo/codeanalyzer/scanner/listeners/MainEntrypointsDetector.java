package edu.rit.se.design.dodo.codeanalyzer.scanner.listeners;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.strings.Atom;
import edu.rit.se.design.dodo.codeanalyzer.scanner.IEntrypointsDetector;
import edu.rit.se.design.dodo.utils.wala.WalaUtils;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detects application-level main methods as entrypoints.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class MainEntrypointsDetector implements IEntrypointsDetector {

    private final IClassHierarchy cha;
    private Set<Entrypoint> entrypoints;

    /**
     * @param cha class hierarchy.
     * @throws IOException error while loading entrypoints from CSV file.
     */
    public MainEntrypointsDetector(IClassHierarchy cha) throws IOException {
        this.entrypoints = HashSetFactory.make();
        this.cha = cha;
    }

    @Override
    public void parseClass(IClass klass) {
        if (WalaUtils.isApplicationScope(klass)) {
            final Atom mainMethod = Atom.findOrCreateAsciiAtom("main");
            MethodReference mainRef =
                    MethodReference.findOrCreate(
                            klass.getReference(),
                            mainMethod,
                            Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V"));
            IMethod m = klass.getMethod(mainRef.getSelector());
            if (m != null) {
                this.entrypoints.add(new DefaultEntrypoint(m, cha));
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
}
