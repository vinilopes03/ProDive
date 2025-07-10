package edu.rit.se.design.dodo.codeanalyzer.config;


import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.translator.jdt.ecj.ECJClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.PlatformUtil;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.io.FileUtil;
import edu.rit.se.design.dodo.utils.WarToJarConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.jar.JarFile;

/**
 * This class is used to create an AnalysisScope {@link AnalysisScope} object depending on the type of artifact being analyzed (source code,
 * WAR or JAR file).
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 * @version 1.0
 */
public class AnalysisScopeFactory {


    private final Config config;
    private final ArtifactType artifactType;


    /**
     * @param config       analysis configuration
     * @param artifactType it indicates whether the artifact under analysis is a source code, war file or jar file
     */
    public AnalysisScopeFactory(Config config, ArtifactType artifactType) {
        this.config = config;
        this.artifactType = artifactType;
    }


    /**
     * Returns an {@link AnalysisScope} based on the type of artifact being analyzed.
     *
     * @return an {@link AnalysisScope}
     * @throws IOException              in case the artifact is inaccessible
     * @throws IllegalArgumentException invalid artifact type
     */
    public AnalysisScope getAnalysisScope() throws IOException, IllegalArgumentException {
        // the path to the folder containing the project's source code or the path to the WAR/JAR file.
        File artifact = new File(this.config.getProjectPath());

        AnalysisScope scope = null;
        switch (this.artifactType) {
            case WAR_FILE:
                // converts to JAR file, then it follows the same logic as a JAR file
                File jar = new File(artifact.getAbsolutePath().replace(".war", ".jar"));
                WarToJarConverter.convert(artifact, jar);
                scope = getJarAnalysisScope(this.config, jar);
                break;
            case JAR_FILE:
                scope = getJarAnalysisScope(this.config, artifact);
                break;
            case SOURCE_FILE:
                scope = getJavaSourceAnalysisScope(this.config, artifact.getAbsolutePath());
                break;
        }

        // adds dependencies (libraries) to the analysis scope
        String[] dependencies = this.config.getDependenciesList();
        if (dependencies != null && dependencies.length > 0) {
            for (String dependency : dependencies) {
                File depFile = new File(dependency);
                if (!depFile.exists()) throw new IllegalArgumentException("Dependency " + depFile + " does not exist");

                if (depFile.isFile() && depFile.getName().endsWith(".jar")) {
                    scope.addToScope(ClassLoaderReference.Extension, new JarFile(depFile));
                } else if (depFile.isDirectory()) {
                    for (String jarFile : getJarsInDirectory(depFile.getAbsolutePath()))
                        scope.addToScope(ClassLoaderReference.Extension, new JarFile(jarFile));
                } else {
                    throw new IllegalArgumentException("Invalid file type " + depFile);
                }

            }
        }

        return scope;
    }

    /**
     * Returns a {@link AnalysisScope} object set up to exclude classes from Java's standard classes as well as to include classes from both
     * J2EE and J2SE.
     *
     * @param jarFile The project's source code location
     * @return a {@link AnalysisScope} delimiting the code analysis scope.
     * @throws IOException in case the jars from J2EE/J2SE cannot be found.
     */
    private static AnalysisScope getJarAnalysisScope(Config config, File jarFile) throws IOException {
        File exclusionsFile = new File(config.getExclusionsFile());
        // creates default JAR scope
        AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();

        // add project to scope
        scope.addToScope(ClassLoaderReference.Application, new JarFile(jarFile));

        // add standard libraries to scope (JavaSE)
        for (String stdlib : getJ2SEJarFiles(config)) {
            scope.addToScope(ClassLoaderReference.Primordial, new JarFile(stdlib));
        }

        // adds JavaEE (J2EE) to the scope (if needed)
        if (config.getJ2eeJar() != null)
            scope.addToScope(ClassLoaderReference.Primordial, new JarFile(config.getJ2eeJar()));

        // set exclusions file
        scope.setExclusions(new FileOfClasses(new FileInputStream(exclusionsFile)));

        System.out.println(scope);
        return scope;
    }

    /**
     * Returns a {@link JavaSourceAnalysisScope} object set up to exclude classes from Java's standard classes as well as to include classes
     * from both J2EE and J2SE.
     *
     * @param projectPath The project's source location (directory or a single Java source code)
     * @return a {@link JavaSourceAnalysisScope} delimiting the code analysis scope.
     * @throws IOException in case the jars from J2EE/J2SE cannot be found.
     */
    private static AnalysisScope getJavaSourceAnalysisScope(Config config, String projectPath) throws IOException {
        JavaSourceAnalysisScope scope = new JavaSourceAnalysisScope();
        // add standard libraries to scope (JavaSE)
        for (String stdlib : getJ2SEJarFiles(config)) {
            scope.addToScope(ClassLoaderReference.Primordial, new JarFile(stdlib));
        }

        // adds JavaEE (J2EE) to the scope
        if (config.getJ2eeJar() != null)
            scope.addToScope(ClassLoaderReference.Primordial, new JarFile(config.getJ2eeJar()));

        // add the source directory
        File artifact = new File(projectPath);
        if (artifact.isDirectory())
            scope.addToScope(JavaSourceAnalysisScope.SOURCE, new SourceDirectoryTreeModule(artifact));
        else
            scope.addToScope(JavaSourceAnalysisScope.SOURCE, new SourceFileModule(artifact, artifact.getName(), null));
        // excludes classes from Java standard
        scope.setExclusions(new FileOfClasses(new FileInputStream(config.getExclusionsFile())));

        return scope;
    }

    /**
     * Determine the classpath for the J2SE standard libraries
     *
     * @return
     * @throws IllegalStateException if jar files cannot be discovered
     * @see PlatformUtil#getBootClassPathJars()
     */
    private static String[] getJ2SEJarFiles(Config config) {
        String j2seDir = config.getJreDir();
        if (j2seDir == null || !(new File(j2seDir)).isDirectory()) {
//            System.err.println("WARNING: java_runtime_dir " + j2seDir + " is invalid.  Using boot class path instead.");
//            return PlatformUtil.getBootClassPathJars();
            throw new IllegalStateException("Invalid java_runtime_dir: " + j2seDir);
        }
        return getJarsInDirectory(j2seDir);
    }

    //NOTE: Code methods below are minor adjustments from the code at:
    //    - com.ibm.wala.core/src/com/ibm/wala/properties/WalaProperties.java

    /**
     * Returns a list of jar files in a given directory
     *
     * @param dir directory to be searched
     * @return
     */
    private static String[] getJarsInDirectory(String dir) {
        File f = new File(dir);
        if (!f.isDirectory()) throw new IllegalArgumentException("Not a directory: " + dir);


        Collection<File> col = FileUtil.listFiles(dir, ".*\\.jar$", true);
        String[] result = new String[col.size()];
        int i = 0;
        for (File jarFile : col) result[i++] = jarFile.getAbsolutePath();

        return result;
    }

    /**
     * Indicates what is the artifact type under analysis: binary (WAR/JAR) or code.
     */
    public enum ArtifactType {
        SOURCE_FILE, WAR_FILE, JAR_FILE
    }
}
