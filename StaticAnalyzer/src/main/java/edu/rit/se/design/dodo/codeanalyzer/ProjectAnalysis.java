package edu.rit.se.design.dodo.codeanalyzer;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.HashSetFactory;
import edu.rit.se.design.dodo.codeanalyzer.config.AnalysisScopeFactory.ArtifactType;
import edu.rit.se.design.dodo.codeanalyzer.instructions.SinkInstruction;
import edu.rit.se.design.dodo.codeanalyzer.instructions.SourceInstruction;
import edu.rit.se.design.dodo.utils.wala.SourceDescriptor;

import java.io.File;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.StreamSupport;

import static edu.rit.se.design.dodo.utils.wala.WalaUtils.isApplicationScope;


/**
 * This class holds all the project's analysis results, such as class hierarchy, call graph, SDG etc. The objects from this class is meant
 * to be passed to our modules such that the project does not have to be analyzed multiple times.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class ProjectAnalysis {

    // flags for the sake of debugging (enables/disable) printing of extra information
    private static boolean DETAILED_CG_STATS = true;
    private static boolean DETAILED_CHA_STATS = true;

    // project's metadata
    private final File projectPath;
    private ArtifactType artifactType;

    // project's core data structures
    private Set<Entrypoint> entrypoints;
    private IClassHierarchy cha;
    private CallGraph cg;
    private SDG<InstanceKey> sdg;

    // project's taint information
    private CallGraphBuilder cgBuilder;
    private Set<SinkInstruction> sinks;
    private Set<SourceInstruction> sources;
    private Set<PointerKey> taintedPointers;

    /**
     * Package protected such that only the ProjectAnalysisBuilder can create an instance of this class.
     *
     * @param projectPath classpath of the project (ex: a JAR file)
     */
    ProjectAnalysis(File projectPath) {
        this.projectPath = projectPath;
    }


    public File getProjectPath() {
        return projectPath;
    }

    public ArtifactType getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(ArtifactType artifactType) {
        this.artifactType = artifactType;
    }

    public Set<Entrypoint> getEntrypoints() {
        return entrypoints;
    }

    public void setEntrypoints(Set<Entrypoint> entrypoints) {
        this.entrypoints = entrypoints;
    }

    public IClassHierarchy getClassHierarchy() {
        return cha;
    }

    public void setClassHierarchy(IClassHierarchy cha) {
        this.cha = cha;
    }

    public CallGraphBuilder getCallGraphBuilder() {
        return cgBuilder;
    }

    public void setCallGraphBuilder(CallGraphBuilder cgBuilder) {
        this.cgBuilder = cgBuilder;
    }

    public CallGraph getCallGraph() {
        return cg;
    }

    public void setCallGraph(CallGraph cg) {
        this.cg = cg;
    }

    public SDG<InstanceKey> getSdg() {
        return sdg;
    }

    public void setSdg(SDG<InstanceKey> sdg) {
        this.sdg = sdg;
    }


    public Set<PointerKey> getTaintedPointers() {
        return taintedPointers;
    }

    public void setTaintedPointers(Set<PointerKey> taintedPointers) {
        this.taintedPointers = taintedPointers;
    }

    public void setSinks(Set<SinkInstruction> sinks) {
        this.sinks = sinks;
    }

    public void setSources(Set<SourceInstruction> sources) {
        this.sources = sources;
    }

    public Set<SinkInstruction> getSinks() {
        return sinks;
    }

    public Set<SourceInstruction> getSources() {
        return sources;
    }

    public Set<Statement> getSinkStatements() {
        if (this.cg == null || this.sinks == null)
            throw new IllegalArgumentException("Call graph and/or sinks not initialized");


        Set<Statement> sinkStatements = new LinkedHashSet<>();

        for (SinkInstruction sink : this.sinks) {
            Set<CGNode> nodes = this.cg.getNodes(sink.getWrapperIMethod().getReference());
            int iIndex = sink.getInstruction().iIndex();
            for (CGNode node : nodes) {
                sinkStatements.add(new NormalStatement(node, iIndex));
            }
        }


        return sinkStatements;
    }


    public Set<Statement> getSourceStatements() {
        Set<Statement> sourceStatements = new LinkedHashSet<>();


        // get all method invocations that matches the sources' signatures
        for (SourceInstruction source : this.sources) {
            Set<CGNode> nodes = this.cg.getNodes(source.getWrapperIMethod().getReference());
            int iIndex = source.getInstruction().iIndex();
            for (CGNode node : nodes) {
                sourceStatements.add(new NormalStatement(node, iIndex));
            }
        }

        return sourceStatements;
    }


    @Override
    public String toString() {
        String stats = "";
        if (cg != null) {
            NumberFormat numberFormatter = NumberFormat.getInstance();
            CallGraphStats.CGStats cgStats = CallGraphStats.getCGStats(cg);
            long searchSpace = ((long) cgStats.getNEdges()) * ((long) cgStats.getNNodes());
            stats = "{\n" +
                    "\t\tNodes: " + numberFormatter.format(cgStats.getNNodes()) + "\n" +
                    "\t\tEdges: " + numberFormatter.format(cgStats.getNEdges()) + "\n" +
                    "\t\tMethods: " + numberFormatter.format(cgStats.getNMethods()) + " \n" +
                    "\t\tSearch Space: " + numberFormatter.format(searchSpace) + " \n";

            if (DETAILED_CG_STATS) {
                long nPointers = StreamSupport.stream(cgBuilder.getPointerAnalysis().getPointerKeys().spliterator(), true).count();
                stats = stats + "\t\tPointers: " + numberFormatter.format(nPointers);
            }

            stats += "\n\t}";
        }
        StringBuilder chaStats = new StringBuilder();
        chaStats.append(cha.getNumberOfClasses());
        if (DETAILED_CHA_STATS) {
            chaStats.append(" (");
            Map<String, Integer> classesPerLoader = new HashMap<>();
            cha.forEach(c -> {
                int total = classesPerLoader.getOrDefault(c.getClassLoader().toString(), 0);
                classesPerLoader.put(c.getClassLoader().toString(), ++total);
            });
            classesPerLoader.forEach((loader, total) -> {
                chaStats.append(loader).append("=").append(total).append(",");
            });
            chaStats.deleteCharAt(chaStats.length() - 1);
            chaStats.append(")");
        }


        return "ProjectAnalysis{\n" +
                "\tprojectPath=" + projectPath + ",\n" +
                "\tartifactType=" + artifactType + ",\n" +
                "\tcha=" + chaStats + ",\n" +
                "\tentrypoints=" + this.entrypoints.size() + ",\n" +
                "\tsinks=" + (this.sinks != null ? this.sinks.size() : "N/A") + ",\n" +
                "\tsources=" + (this.sources != null ? this.sources.size() : "N/A") + ",\n" +
                "\tcgBuilder=" +  (this.cgBuilder != null ? cgBuilder.getClass().getSimpleName(): "N/A") + ",\n" +
                "\tcg=" + stats + "\n" +
                "}";
    }


}
