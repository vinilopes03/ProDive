package edu.rit.se.design.dodo.codeanalyzer.config;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.WalaRuntimeException;
import edu.rit.se.design.dodo.codeanalyzer.CodeAnalyzerTestUtils;
import edu.rit.se.design.dodo.utils.wala.WalaUtils;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static edu.rit.se.design.dodo.codeanalyzer.config.AnalysisScopeFactory.ArtifactType.JAR_FILE;
import static org.junit.Assert.*;

/**
 * Test {@link AnalysisScopeFactory}.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class AnalysisScopeFactoryTest extends CodeAnalyzerTestUtils {


    private final String TEST_ASSETS_FOLDER = ROOT_TEST_ASSETS + "config/";
    private static final String[] JLEX_CLASSES = new String[]{"LJLex/CLexGen", "LJLex/CSpec", "LJLex/CEmit", "LJLex/CBunch", "LJLex/CMakeNfa", "LJLex/CSimplifyNfa", "LJLex/CMinimize", "LJLex/CNfa2Dfa", "LJLex/CAlloc", "LJLex/Main", "LJLex/CDTrans", "LJLex/CDfa", "LJLex/CAccept", "LJLex/CAcceptAnchor", "LJLex/CNfaPair", "LJLex/CInput", "LJLex/CUtility", "LJLex/CError", "LJLex/CSet", "LJLex/CNfa", "LJLex/CLexGen", "LJLex/SparseBitSet", "LJLex/SparseBitSet$1", "LJLex/SparseBitSet$2", "LJLex/SparseBitSet$3", "LJLex/SparseBitSet$4", "LJLex/SparseBitSet$BinOp"};


    private static void verifyClasses(IClassHierarchy cha) {
        Set<String> expResult = new HashSet<>(Arrays.asList(JLEX_CLASSES));
        Set<String> actualResult = new HashSet<>();

        cha.forEach(iClass -> {
            if (WalaUtils.isApplicationScope(iClass)) {
                actualResult.add(iClass.getName().toString());
            }
        });

        assertTrue(expResult.containsAll(actualResult) && actualResult.containsAll(expResult));
    }


    @Test
    public void testGetAnalysisScopeForJLexJRE15Correct() throws Exception {
        Config config = Config.loadFromFile(TEST_ASSETS_FOLDER + "JLex-1.5-correct.properties");
        AnalysisScope scope = new AnalysisScopeFactory(config, JAR_FILE).getAnalysisScope();
        IClassHierarchy cha = ClassHierarchyFactory.make(scope);
        assertNotNull(scope);
        assertEquals(scope.getJavaLibraryVersion(), "1.5");
        verifyClasses(cha);
    }

    @Test
    public void testGetAnalysisScopeForJLexJRE18Correct() throws Exception {
        Config config = Config.loadFromFile(TEST_ASSETS_FOLDER + "JLex-1.8-correct.properties");
        File artifact = new File(TEST_ASSETS_FOLDER + "sample-jars/JLex-1.8.jar");

        AnalysisScope scope = new AnalysisScopeFactory(config, JAR_FILE).getAnalysisScope();
        IClassHierarchy cha = ClassHierarchyFactory.make(scope);
        assertNotNull(scope);
        assertEquals(scope.getJavaLibraryVersion(), "1.8");
        verifyClasses(cha);
    }

    @Test
    public void testGetAnalysisScopeForJLexJRE18Wrong() {

        try {
            Config config = Config.loadFromFile(TEST_ASSETS_FOLDER + "JLex-1.8-wrong.properties");
            AnalysisScope scope = new AnalysisScopeFactory(config, JAR_FILE).getAnalysisScope();
            assertNotNull(scope);
            ClassHierarchyFactory.make(scope);
        } catch (Exception e) {
            assertTrue(e instanceof WalaRuntimeException);
            assertTrue(e.getMessage().contains("bad method bytecode"));
        }

    }
}
