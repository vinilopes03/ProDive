package edu.rit.se.design.dodo.codeanalyzer.cli;


import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import edu.rit.se.design.dodo.codeanalyzer.ProjectAnalysis;
import edu.rit.se.design.dodo.codeanalyzer.ProjectAnalysisBuilder;
import edu.rit.se.design.dodo.codeanalyzer.accessPath.AccessPathManager;
import edu.rit.se.design.dodo.codeanalyzer.accessPath.TrieAccessPath;
import edu.rit.se.design.dodo.codeanalyzer.callgraph.CallGraphBuilderType;
import edu.rit.se.design.dodo.codeanalyzer.config.Config;
import edu.rit.se.design.dodo.codeanalyzer.scanner.listeners.FileSinksDetector;
import edu.rit.se.design.dodo.codeanalyzer.scanner.listeners.FileSourcesDetector;
import edu.rit.se.design.dodo.codeanalyzer.scanner.listeners.MainEntrypointsDetector;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * Command Line Interface for this tool.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class CLI {

    //    private static final String[] PATH_ARGS = new String[]{"p", "path", "Project's JAR filepath"};
    private static final String[] CONFIG_ARGS = new String[]{"c", "config", "Configuration (.properties) filepath"};
    private static final String[] OUTPUT_ARGS = new String[]{"o", "output", "Path to where the analysis will be saved (as a json file)"};

    private static List<TrieAccessPath> trieAccessPaths = new ArrayList<>();

    private static File source;

    private static ProjectAnalysis projectAnalysis;

    /**
     * Set up the Apache's CLI API to parse the command line arguments provided by the user.
     *
     * @param args command line arguments provided by the user
     * @return
     */
    public static CommandLine parseArgs(String[] args) throws ParseException {
        Option config = new Option(CONFIG_ARGS[0], CONFIG_ARGS[1], true, CONFIG_ARGS[2]);
        config.setRequired(true);

        Option output = new Option(OUTPUT_ARGS[0], OUTPUT_ARGS[1], true, OUTPUT_ARGS[2]);
        output.setRequired(false);


        Options options = new Options();
        options.addOption(config);
        options.addOption(output);

        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    /**
     * Run the static analysis on the provided project
     *
     * @param config
     * @return
     */
    public static ProjectAnalysis runAnalysis(Config config, CallGraphBuilderType cgType) throws ClassHierarchyException, CallGraphBuilderCancelException, IOException {
        ProjectAnalysisBuilder builder = new ProjectAnalysisBuilder(config);
        ProjectAnalysis projectAnalysis = builder.getProjectAnalysis();

        builder
                .disableReflection()
                .buildClassHierarchy()
                .buildCriticalPoints(
                        // new FileEntrypointsDetector(config.getEntrypointsFile(), builder.getProjectAnalysis().getClassHierarchy())
                        new MainEntrypointsDetector(projectAnalysis.getClassHierarchy()),
                        new FileSinksDetector(config.getSinksFile(), true),
                        new FileSourcesDetector(config.getSourcesFile(), true)
                )
                .buildCallGraph(cgType)
                .buildTaintedPointerSet();

        return projectAnalysis;
    }

    public static void main(String[] args) throws Exception {

        args = new String[]{"-c", "StaticAnalyzer/resources/ex3/example3.properties"};
        CommandLine cmd = parseArgs(args);

        String configPath = cmd.getOptionValue(CONFIG_ARGS[0]);


        Config config = Config.loadFromFile(configPath);
        setSource(new File(config.getProjectPath()));

        CallGraphBuilderType cgType = CallGraphBuilderType.N1_CFA; // use the ones prefixed with TAINTED_, those also produces tainted sets
        ProjectAnalysis projectAnalysis = runAnalysis(config, cgType);
        setProjectAnalysis(projectAnalysis);


        IClassHierarchy cha = projectAnalysis.getClassHierarchy();
        for (IClass klass : cha) {
            processClass(klass, projectAnalysis.getClassHierarchy().getScope());
        }
        for (TrieAccessPath tap : trieAccessPaths) {
            tap.printTree(tap.getRoot(), tap.getRoot().getData());
        }
    }


    public static void processClass(IClass klass, AnalysisScope scope) throws InvalidClassFileException {
        if (AccessPathManager.isFromApplication(klass, scope)) {

            String className = klass.getName().toString();
            TrieAccessPath trie = new TrieAccessPath(className);
            System.out.println("Processing class: " + className);

            for (IMethod method : klass.getDeclaredMethods()) {
                if (!method.isInit() && !method.isClinit()) {
                    trie = AccessPathManager.generateMethodSummary(method, trie);
                }
            }
            trieAccessPaths.add(trie);
        }
    }


    public static ProjectAnalysis getProjectAnalysis() {
        return CLI.projectAnalysis;
    }

    public static void setProjectAnalysis(ProjectAnalysis projectAnalysis) {
        CLI.projectAnalysis = projectAnalysis;
    }

    public static File getSource() {
        return CLI.source;
    }

    public static void setSource(File source) {
        CLI.source = source;
    }


}
