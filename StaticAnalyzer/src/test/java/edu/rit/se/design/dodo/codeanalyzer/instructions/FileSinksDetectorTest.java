package edu.rit.se.design.dodo.codeanalyzer.instructions;

import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.ssa.SSAOptions;
import edu.rit.se.design.dodo.codeanalyzer.CodeAnalyzerTestUtils;
import edu.rit.se.design.dodo.codeanalyzer.config.Config;
import edu.rit.se.design.dodo.codeanalyzer.scanner.BytecodeInstructionScanner;
import edu.rit.se.design.dodo.codeanalyzer.scanner.InstructionScannerListener;
import edu.rit.se.design.dodo.codeanalyzer.scanner.listeners.FileSinksDetector;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test class for {@link FileSinksDetector}.
 */
public class FileSinksDetectorTest extends CodeAnalyzerTestUtils {
    private final String TEST_ASSETS_FOLDER = ROOT_TEST_ASSETS + "instructions/";

    public FileSinksDetector setUp(String filename) throws Exception {
        String propertyFilePath = TEST_ASSETS_FOLDER + filename;
        Config config = Config.loadFromFile(propertyFilePath);
        IClassHierarchy cha = buildClassHierarchy(config.getProjectPath(), config.getExclusionsFile());
        FileSinksDetector detector = new FileSinksDetector(config.getSinksFile());
        BytecodeInstructionScanner scanner = new BytecodeInstructionScanner(null, null, new InstructionScannerListener[]{detector});
        scanner.scan(cha, new DefaultIRFactory(), new SSAOptions());
        return detector;
    }

    private static void assertCorrectness(List<SinkInstruction> foundStatements, Set<String> expectedSignatures) {
        Set<String> foundSignatures = foundStatements.stream().map(s -> s.getSinkDescriptor().getMethodRef().getSignature()).collect(Collectors.toSet());
        Assert.assertEquals(expectedSignatures, foundSignatures);
    }

    /**
     * Test of getSinkStatements method, of class SinkStatementDetector.
     */
    @Test
    public void testMainVulnerable() throws Exception {
        FileSinksDetector detector = setUp("MainVulnerable.properties");
        Set<SinkInstruction> sinks = detector.getSinks();

        sinks.forEach(s -> System.out.println(s));
//        Set<String> expectedSignatures = new HashSet<>(Arrays.asList(
//                "java.lang.Runtime.exec(Ljava/lang/String;)Ljava/lang/Process;"
//        ));
//        assertCorrectness(setUp(path, config), expectedSignatures);

    }


}