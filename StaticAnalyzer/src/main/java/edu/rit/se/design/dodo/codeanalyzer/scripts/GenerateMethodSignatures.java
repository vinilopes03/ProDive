package edu.rit.se.design.dodo.codeanalyzer.scripts;


import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.shrikeBT.Util;
import com.ibm.wala.types.ClassLoaderReference;
import edu.rit.se.design.dodo.codeanalyzer.config.Config;
import edu.rit.se.design.dodo.codeanalyzer.config.ConfigDefaultValues;
import edu.rit.se.design.dodo.codeanalyzer.config.PlatformFactory;

import static edu.rit.se.design.dodo.codeanalyzer.config.ConfigDefaultValues.DEFAULT_CONFIG_FOLDER;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Scanner;
import java.util.jar.JarFile;

/**
 * It simply computes all the methods from a class and prints it in the same format as the SINK_CSV file.
 *
 * @author Joanna C. S. Santos <jds5109@rit.edu>
 */
public class GenerateMethodSignatures {


    private static void getInSinksFormat(Class c) throws ClassHierarchyException, IOException {
        String jreDir = PlatformFactory.getJREDir(DEFAULT_CONFIG_FOLDER, PlatformFactory.JREVersion.JRE1_8);
        String className = "L" + c.getCanonicalName().replace(".", "/");

        // gets an analysis scope on top of the source code
        AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();

        for (String jarInDirectory : WalaProperties.getJarsInDirectory(jreDir)) {
            scope.addToScope(ClassLoaderReference.Primordial, new JarFile(jarInDirectory));
        }
        // build the class hierarchy
        IClassHierarchy cha = ClassHierarchyFactory.make(scope);

//       MemberOf,MethodName,ParametersList,Return Type,SourceType,TaintedVar
        System.out.println("MemberOf\tMethodName\tParametersList\tReturn Type\tDangerousRange");
        cha.forEach(klazz -> {
            if (klazz.getName().toUnicodeString().equals(className)) {
                klazz.getAllMethods().forEach(iMethod -> {

                    System.out.println(String.format("%s,%s,%s,%s,%s",
                            klazz.getName().toString(),
                            iMethod.getName().toString(),
                            iMethod.getDescriptor().toString().split("\\)")[0].replace("(", ""),
                            iMethod.getReturnType().getName().toString(),
                            iMethod.getDeclaringClass().isInterface() ? "INTERFACE": "CLASS",
                            "1"
                    ));
                });

            }

        });
    }


    private static void getSelectorFormatForClass(Class c) {
        // 0 = methodName; 1= parameters list; 2=return type
        String format = "new ImmutablePair(new Selector(Atom.findOrCreateAsciiAtom(\"%s\"), Descriptor.findOrCreateUTF8(\"%s\")),NUMBER)\n";
        for (Method method : c.getMethods()) {
            System.out.printf(format, method.getName(), Util.computeSignature(method.getParameterTypes(), method.getReturnType()));
        }
    }


    public static void main(String[] args) throws ClassHierarchyException, IOException {
//        getSelectorFormatForClass(java.util.List.class);

        getInSinksFormat(Scanner.class);

    }
}
