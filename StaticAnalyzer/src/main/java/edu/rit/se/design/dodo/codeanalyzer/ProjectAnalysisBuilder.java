package edu.rit.se.design.dodo.codeanalyzer;


import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.java.client.impl.ZeroCFABuilderFactory;
import com.ibm.wala.cast.java.client.impl.ZeroOneCFABuilderFactory;
import com.ibm.wala.cast.java.client.impl.ZeroOneContainerCFABuilderFactory;
import com.ibm.wala.cast.java.translator.jdt.ecj.ECJClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.callgraph.propagation.cfa.*;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ssa.*;
import edu.rit.se.design.dodo.codeanalyzer.callgraph.CallGraphBuilderType;
import edu.rit.se.design.dodo.codeanalyzer.callgraph.cfa.AstJavaNCFABuilder;
import edu.rit.se.design.dodo.codeanalyzer.callgraph.cfa.NCFABuilderFactory;
import edu.rit.se.design.dodo.codeanalyzer.callgraph.cha.CHACallGraphBuilder;
import edu.rit.se.design.dodo.codeanalyzer.callgraph.cha.LambdaChaCallGraphBuilder;
import edu.rit.se.design.dodo.codeanalyzer.config.AnalysisScopeFactory;
import edu.rit.se.design.dodo.codeanalyzer.config.AnalysisScopeFactory.ArtifactType;
import edu.rit.se.design.dodo.codeanalyzer.config.Config;
import edu.rit.se.design.dodo.codeanalyzer.scanner.*;
import edu.rit.se.design.dodo.codeanalyzer.taint.ITaintAnalyzer;
import edu.rit.se.design.dodo.codeanalyzer.taint.ifds.IFDSTaintAnalyzer;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions.FULL;
import static com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions.NONE;
import static edu.rit.se.design.dodo.codeanalyzer.config.AnalysisScopeFactory.ArtifactType.*;


/**
 * This class is used to set up a {@link ProjectAnalysis} instance.
 * This instance contains multiple analysis data structures on top of a project artifact (WAR or JAR file).
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */

public class ProjectAnalysisBuilder {
    private static nCFAContextSelector contextSelector;
    /**
     * the product of this builder
     */
    private ProjectAnalysis projectAnalysis;
    /**
     * global configuration for the static analysis
     */
    private Config config;

    /**
     * Create IRs for methods
     */
    private IRFactory irFactory;
    /**
     * Options for customizing the SSA generation
     */
    private SSAOptions ssaOptions;
    /**
     * caching data structures for speeding up analysis
     */
    private AnalysisCache analysisCache;
    /**
     * Static analysis options (reflection, entrypoints, scope, etc.)
     */
    private AnalysisOptions options;

    /**
     * Create class loaders for the analysis
     */
    private ClassLoaderFactory classLoaderFactory;

    /**
     * Reads a configuration file and sets up the analysis (Reflection=FULL).
     *
     * @param config analysis configuration (@see Config).
     */
    public ProjectAnalysisBuilder(Config config) {
        this.projectAnalysis = new ProjectAnalysis(new File(config.getProjectPath()));
        this.config = config;
        this.projectAnalysis.setArtifactType(getArtifactType(projectAnalysis.getProjectPath()));
        this.irFactory = this.projectAnalysis.getArtifactType() == SOURCE_FILE ? AstIRFactory.makeDefaultFactory() : new DefaultIRFactory();
        this.ssaOptions = new SSAOptions();
        this.analysisCache = new AnalysisCacheImpl(irFactory, ssaOptions);
        this.options = new AnalysisOptions();
        this.options.setReflectionOptions(FULL);
    }


    /**
     * It will make the analysis uses the option {@see NONE} for the construction algorithm.
     * You need to call this PRIOR constructing the call graph.
     */
    public ProjectAnalysisBuilder disableReflection() {
        this.options.setReflectionOptions(NONE);
        return this;
    }

    /**
     * It will make the analysis uses the option {@see FULL} for the construction algorithm.
     */
    public ProjectAnalysisBuilder enableReflection() {
        this.options.setReflectionOptions(FULL);
        return this;
    }

    /**
     * It infers the artifact type for the provided file. It currently supports only JAR and WAR files.
     *
     * @param artifact a file of the project artifact being analyzed.
     * @return the {@link ArtifactType} for the file
     */
    private static ArtifactType getArtifactType(File artifact) {
        if (artifact.getName().endsWith(".jar"))
            return ArtifactType.JAR_FILE;
        if (artifact.getName().endsWith(".war"))
            return ArtifactType.WAR_FILE;
        if (artifact.getName().endsWith(".java"))
            return SOURCE_FILE;
        throw new IllegalArgumentException("Invalid artifact type provided. Only JAR and WAR files are supported.");
    }

    /**
     * Returns the results so far for what has been analyzed
     *
     * @return a {@link ProjectAnalysis} object with everything cached on it.
     */
    public ProjectAnalysis getProjectAnalysis() {
        return projectAnalysis;
    }

    /**
     * Extracts the class hierarchy for a project, given its scope.
     *
     * @throws ClassHierarchyException
     */
    public ProjectAnalysisBuilder buildClassHierarchy() throws IOException, ClassHierarchyException {
        ArtifactType artifactType = projectAnalysis.getArtifactType();
        // gets an analysis scope on top of the source code
        AnalysisScopeFactory scopeFactory = new AnalysisScopeFactory(config, artifactType);
        AnalysisScope scope = scopeFactory.getAnalysisScope();

        this.classLoaderFactory = artifactType == SOURCE_FILE ?
                new ECJClassLoaderFactory(scope.getExclusions()) :
                new ClassLoaderFactoryImpl(scope.getExclusions())
        ;
        // build the class hierarchy
        ClassHierarchy cha = ClassHierarchyFactory.make(scope, this.classLoaderFactory);

        // caches the results
        this.projectAnalysis.setClassHierarchy(cha);


        // update the analysis option to reflect the analysis scope
        this.options.setAnalysisScope(scope);

        return this;
    }


    /**
     * @param entrypointsDetector listener that detects entrypoints (cannot be null).
     * @param sinksDetector       listener that detects sink instructions.
     * @param sourcesDetector     listener that detects source instructions.
     * @return
     */
    public ProjectAnalysisBuilder buildCriticalPoints(IEntrypointsDetector entrypointsDetector, ISinksDetector sinksDetector, ISourcesDetector sourcesDetector) {
        if (entrypointsDetector == null)
            throw new IllegalArgumentException("The " + IEntrypointsDetector.class.getSimpleName() + " parameter can't be null");

        InstructionScannerListener[] instructionListeners = new InstructionScannerListener[]{sinksDetector, sourcesDetector};
        BytecodeInstructionScanner scanner = new BytecodeInstructionScanner(
                new IClassScannerListener[]{entrypointsDetector},
                null,
                Arrays.stream(instructionListeners).filter(Objects::nonNull).toArray(InstructionScannerListener[]::new)
        );

        scanner.scan(this.projectAnalysis.getClassHierarchy(), this.irFactory, this.ssaOptions);
        // cache results in the build instance
        this.projectAnalysis.setEntrypoints(entrypointsDetector.getEntrypoints());
        if (sinksDetector != null) this.projectAnalysis.setSinks(sinksDetector.getSinks());
        if (sourcesDetector != null) this.projectAnalysis.setSources(sourcesDetector.getSources());

        // updates the analysis options with the discovered entrypoints
        this.options.setEntrypoints(this.projectAnalysis.getEntrypoints());

        return this;
    }

    /**
     * Detects entrypoints in the bytecode by scanning its methods.
     *
     * @param entrypointsDetector the listener that will detect the entrypoints.
     * @return
     */
    public ProjectAnalysisBuilder buildCriticalPoints(IEntrypointsDetector entrypointsDetector) {
        return buildCriticalPoints(entrypointsDetector, null, null);
    }

    /**
     * @param cgBuilderType
     * @return
     * @throws CallGraphBuilderCancelException
     */
    public ProjectAnalysisBuilder buildCallGraph(CallGraphBuilderType cgBuilderType) throws CallGraphBuilderCancelException {
        IClassHierarchy cha = this.projectAnalysis.getClassHierarchy();
        Set<Entrypoint> entrypoints = projectAnalysis.getEntrypoints();

        if (cha == null)
            throw new IllegalStateException("Method has to be invoked after class hierarchy has been initialized");
        if (entrypoints == null)
            throw new IllegalStateException("Method has to be invoked after entrypoints were discovered");

        CallGraphBuilder builder = newCallGraphBuilder(options, cgBuilderType, cha, cha.getScope());
        this.buildCallGraph(builder);
        return this;
    }


    /**
     * @param builder the call graph builder to be used
     * @return
     * @throws CallGraphBuilderCancelException
     */
    public ProjectAnalysisBuilder buildCallGraph(CallGraphBuilder builder) throws CallGraphBuilderCancelException {

        // builds call graph
        CallGraph cg = builder.makeCallGraph(options, null);

        // cache results
        this.projectAnalysis.setCallGraph(cg);
        this.projectAnalysis.setCallGraphBuilder(builder);

        return this;
    }

    /**
     * Builds the System Dependence Graph.
     *
     * @param dataOptions
     * @param controlOptions
     * @return
     */
    public ProjectAnalysisBuilder buildSDG(Slicer.DataDependenceOptions dataOptions, Slicer.ControlDependenceOptions controlOptions) {
        SDG sdg = new SDG(this.projectAnalysis.getCallGraph(), this.projectAnalysis.getCallGraphBuilder().getPointerAnalysis(), dataOptions, controlOptions);
        projectAnalysis.setSdg(sdg);
        return this;
    }

    /**
     * Constructs an instance of a {@link CallGraphBuilder}.
     * If you use any of our custom call graph implementations,
     *
     * @param type  the type of call graph
     * @param cha   class hierarchy
     * @param scope scope
     * @return a {@link CallGraphBuilder} instance
     */
    private CallGraphBuilder newCallGraphBuilder(AnalysisOptions options, CallGraphBuilderType type, IClassHierarchy cha, AnalysisScope scope/*, Iterable<Entrypoint> entrypoints*/) {
        Language language = Language.JAVA;
        AbstractRootMethod fakeRootMethod = language.getFakeRootMethod(cha, options, analysisCache);
        if (projectAnalysis.getArtifactType().equals(JAR_FILE) || projectAnalysis.getArtifactType().equals(WAR_FILE)) {
            switch (type) {
                // standard off-the-shelf algorithms provided by WALA
                case CHA:
                    return new CHACallGraphBuilder();
                case LAMBDA_CHA: // custom added support
                    return new LambdaChaCallGraphBuilder(cha);
                case RTA:
                    return Util.makeRTABuilder(options, analysisCache, cha, scope);
                case ZERO_CFA:
                    return Util.makeZeroCFABuilder(language, options, analysisCache, cha, scope);
                case ZERO_CONTAINER_CFA:
                    return Util.makeZeroContainerCFABuilder(options, analysisCache, cha, scope);
                case VANILLA_ZERO_ONE_CFA:
                    return Util.makeVanillaZeroOneCFABuilder(language, options, analysisCache, cha, scope);
                case ZERO_ONE_CFA:
                    return Util.makeZeroOneCFABuilder(language, options, analysisCache, cha, scope);
                case ZERO_ONE_CONTAINER_CFA:
                    return Util.makeZeroOneContainerCFABuilder(options, analysisCache, cha, scope);
                case N1_CFA:
                    return Util.makeNCFABuilder(1, options, analysisCache, cha, scope);
                case N2_CFA:
                    return Util.makeNCFABuilder(2, options, analysisCache, cha, scope);
            }
        } else {
            switch (type){
                case ZERO_CFA:
                    return new ZeroCFABuilderFactory().make(options, analysisCache, cha, scope);
                case ZERO_ONE_CFA:
                    return new ZeroOneCFABuilderFactory().make(options, analysisCache, cha, scope);
                case ZERO_ONE_CONTAINER_CFA:
                    return new ZeroOneContainerCFABuilderFactory().make(options, analysisCache, cha, scope);
                case N1_CFA:
                    return new NCFABuilderFactory().make(options, analysisCache, cha, scope,1);
                case N2_CFA:
                    return new NCFABuilderFactory().make(options, analysisCache, cha, scope,2);
            }

        }
        throw new IllegalArgumentException("Unsupported algorithm " + type + "for artifact type " + projectAnalysis.getArtifactType());
    }


    public ProjectAnalysisBuilder buildTaintedPointerSet() {

        ITaintAnalyzer taintAnalyzer = new IFDSTaintAnalyzer();

        Set<PointerKey> pointerKeys = taintAnalyzer.analyze(projectAnalysis);
        projectAnalysis.setTaintedPointers(pointerKeys);

        return this;
    }
}
