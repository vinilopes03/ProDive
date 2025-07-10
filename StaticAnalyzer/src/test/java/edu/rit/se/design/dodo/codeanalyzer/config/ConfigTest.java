package edu.rit.se.design.dodo.codeanalyzer.config;

import edu.rit.se.design.dodo.codeanalyzer.CodeAnalyzerTestUtils;
import org.junit.Test;

import java.io.File;

import static edu.rit.se.design.dodo.codeanalyzer.config.PlatformFactory.J2EEVersion.J2EE7;
import static edu.rit.se.design.dodo.codeanalyzer.config.PlatformFactory.JREVersion.JRE1_5;
import static org.junit.Assert.*;


/**
 * Test {@link Config}.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class ConfigTest extends CodeAnalyzerTestUtils{

    private static final String CONFIG_SAMPLES_FOLDER = ROOT_TEST_ASSETS + "config/";


    @Test
    public void testLoadFromFile() throws Exception {
        System.out.println(new java.io.File(".").getAbsolutePath());
        String propertiesFile = CONFIG_SAMPLES_FOLDER + "sample.properties";
        Config result = Config.loadFromFile(propertiesFile);

        assertTrue(new File(result.getProjectPath()).exists());

        assertEquals(new File(new File(propertiesFile).getParentFile(),"CustomJavaExclusions-DODO.txt").getCanonicalPath(), result.getExclusionsFile());
        assertEquals(new File(new File(propertiesFile).getParentFile(),"sinks.csv").getCanonicalPath(), result.getSinksFile());
        assertEquals(new File(new File(propertiesFile).getParentFile(),"entrypoints.csv").getCanonicalPath(), result.getEntrypointsFile());
        assertEquals(JRE1_5, result.getJreVersion());
        assertEquals(J2EE7, result.getJ2eeVersion());
        assertArrayEquals(new String[]{"dep1.jar", "dep2.jar", "depFolder"}, result.getDependenciesList());
    }

}
