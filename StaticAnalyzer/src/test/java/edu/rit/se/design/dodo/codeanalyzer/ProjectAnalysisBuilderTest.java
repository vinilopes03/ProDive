package edu.rit.se.design.dodo.codeanalyzer;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import edu.rit.se.design.dodo.codeanalyzer.config.Config;
import edu.rit.se.design.dodo.codeanalyzer.scanner.listeners.FileEntrypointsDetector;
import org.junit.Test;

import java.util.Set;

import static com.ibm.wala.types.ClassLoaderReference.Application;
import static com.ibm.wala.types.TypeReference.findOrCreate;
import static org.junit.Assert.*;

public class ProjectAnalysisBuilderTest extends CodeAnalyzerTestUtils {


    @Test
    public void buildClassHierarchyThreeLayers5() throws Exception {

        Config config = Config.loadFromFile(CALLGRAPH_TEST_ASSETS + "ThreeLayers5-JRE1.7.properties");
        ProjectAnalysis projectAnalysis = new ProjectAnalysisBuilder(config)
                .buildClassHierarchy()
                .getProjectAnalysis();

        assertNotNull(projectAnalysis);
        IClassHierarchy cha = projectAnalysis.getClassHierarchy();

        assertNotNull(cha);
        assertNotNull(cha.lookupClass(findOrCreate(Application, "Lvulnerable/CacheManager")));
        assertNotNull(cha.lookupClass(findOrCreate(Application, "Lvulnerable/CommandTask")));
        assertNotNull(cha.lookupClass(findOrCreate(Application, "Lvulnerable/ThreeLayers5")));
        assertNotNull(cha.lookupClass(findOrCreate(Application, "Lvulnerable/TaskExecutor$CommandStatus")));
        assertNotNull(cha.lookupClass(findOrCreate(Application, "Lvulnerable/TaskExecutor")));
    }


    @Test
    public void buildSerializableClassesThreeLayers5() throws Exception {
        Config config = Config.loadFromFile(CALLGRAPH_TEST_ASSETS + "ThreeLayers5-JRE1.7.properties");
        ProjectAnalysis projectAnalysis = new ProjectAnalysisBuilder(config)
                .buildClassHierarchy()
                .getProjectAnalysis();

        assertNotNull(projectAnalysis);

        IClassHierarchy cha = projectAnalysis.getClassHierarchy();
    }


    @Test
    public void buildEntrypointsThreeLayers5() throws Exception {
        Config config = Config.loadFromFile(CALLGRAPH_TEST_ASSETS + "ThreeLayers5-JRE1.7.properties");
        ProjectAnalysisBuilder builder = new ProjectAnalysisBuilder(config);
        builder.buildClassHierarchy()
                .buildCriticalPoints(
                        new FileEntrypointsDetector(
                                config.getEntrypointsFile(), builder.getProjectAnalysis().getClassHierarchy()
                        )
                );

        ProjectAnalysis projectAnalysis = builder.getProjectAnalysis();
        assertNotNull(projectAnalysis.getEntrypoints());
        assertTrue(projectAnalysis.getEntrypoints().size() > 0);
        assertTrue(projectAnalysis.getEntrypoints().stream().anyMatch(ep -> ep.toString().contains("readObject")));
    }


}