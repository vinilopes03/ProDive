package edu.rit.se.design.dodo.codeanalyzer.scanner.listeners;


import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;
import edu.rit.se.design.dodo.codeanalyzer.scanner.IEntrypointsDetector;
import edu.rit.se.design.dodo.utils.wala.EntrypointDescriptor;
import edu.rit.se.design.dodo.utils.wala.MethodDescriptorsLoader;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.rit.se.design.dodo.utils.wala.MethodDescriptorsLoader.ClassType.CLASS;
import static edu.rit.se.design.dodo.utils.wala.MethodDescriptorsLoader.ClassType.INTERFACE;
import static edu.rit.se.design.dodo.utils.wala.MethodDescriptorsLoader.loadEntrypointsFromCsv;

/**
 * It detects entrypoints in a program that are specified in a CSV file.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class FileEntrypointsDetector implements IEntrypointsDetector {

    private final IClassHierarchy cha;
    private List<EntrypointDescriptor> descriptors;
    private Set<Entrypoint> entrypoints;

    /**
     * @param entrypointsFilePath path to the CSV file with entrypoints
     * @param cha                 class hierarchy.
     * @throws IOException error while loading entrypoints from CSV file.
     */
    public FileEntrypointsDetector(String entrypointsFilePath, IClassHierarchy cha) throws IOException {
        this.descriptors = loadEntrypointsFromCsv(new File(entrypointsFilePath), true);
        this.entrypoints = new HashSet<>();
        this.cha = cha;
    }

    @Override
    public Set<IMethod> getEntrypointMethods() {
        return entrypoints.stream().map(e -> e.getMethod()).collect(Collectors.toSet());
    }

    @Override
    public Set<Entrypoint> getEntrypoints() {
        return entrypoints;
    }

    @Override
    public void parseClass(IClass klass) {
        descriptors.forEach((EntrypointDescriptor methodSignature) -> {
            IMethod customMethod = getMethod(klass, cha, ClassLoaderReference.Application, methodSignature);
            if (customMethod != null)
                entrypoints.add(new DefaultEntrypoint(customMethod, cha));
        });
    }


    private IMethod getMethod(IClass c, IClassHierarchy cha, ClassLoaderReference clr, EntrypointDescriptor methodSignature) {
        // if class member is null, means the method will match based on the selector
        if (methodSignature.getMemberOf().isEmpty()) {
            MethodReference mainRef = MethodReference.findOrCreate(c.getReference(), methodSignature.getName(), methodSignature.getDescriptor().toString());
            return c.getMethod(mainRef.getSelector());
        }

        MethodReference epMethodRef = MethodReference.findOrCreate(c.getReference(),
                Atom.findOrCreateAsciiAtom(methodSignature.getName()),
                methodSignature.getDescriptor());

        String epMemberOf = methodSignature.getMemberOf();
        IClass epDeclaringClass = cha.lookupClass(TypeReference.findOrCreate(clr, epMemberOf));


        MethodDescriptorsLoader.ClassType classType = methodSignature.getClassType();

        IMethod foundMethod = c.getMethod(epMethodRef.getSelector());
        if (classType.equals(CLASS) && cha.isAssignableFrom(epDeclaringClass, c)) {
            return foundMethod != null
                    && foundMethod.getDeclaringClass().getClassLoader().getReference().equals(clr)
                    && foundMethod.getDeclaringClass().equals(c) ? foundMethod : null;
        }
        if (classType.equals(INTERFACE) && cha.implementsInterface(c, epDeclaringClass)) {
            return foundMethod != null && foundMethod.getDeclaringClass().getClassLoader().getReference().equals(clr) ? foundMethod : null;
        }
        return null;
    }
}
