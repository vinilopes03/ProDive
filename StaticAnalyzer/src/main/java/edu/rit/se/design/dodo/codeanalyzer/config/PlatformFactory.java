package edu.rit.se.design.dodo.codeanalyzer.config;

import static edu.rit.se.design.dodo.codeanalyzer.config.ConfigDefaultValues.J2EE_ROOT_PATH;
import static edu.rit.se.design.dodo.codeanalyzer.config.ConfigDefaultValues.JRE_ROOT_PATH;

/**
 * Used for setting up the JAVA's JDK APIs.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class PlatformFactory {

    /**
     * Returns the location for the J2EE' JAR version requested.
     *
     * @param defaultConfigFolder folder that has default configuration files (like entrypoints, sinks, etc)
     * @param j2eeVersion         the J2EE version folder that has default configuration files (like entrypoints, sinks, etc)
     * @return path to the JAR file with the API.
     */
    public static String getJ2EEJar(String defaultConfigFolder, J2EEVersion j2eeVersion) {
        switch (j2eeVersion) {
            case J2EE8:
                return String.format(J2EE_ROOT_PATH, defaultConfigFolder, "8.0");
            case J2EE7:
                return String.format(J2EE_ROOT_PATH, defaultConfigFolder, "7.0");
            case J2EE6:
                return String.format(J2EE_ROOT_PATH, defaultConfigFolder, "6.0");
            case NONE:
                return null;
        }
        throw new IllegalArgumentException("The artifact type is unknown.");
    }

    /**
     * It returns the path to the JAR file for the corresponding JRE version.
     *
     * @param defaultConfigFolder folder that has default configuration files (like entrypoints, sinks, etc)
     * @param jreVersion          the JRE version of desire.
     * @return path to the JAR file for the corresponding JRE version.
     */
    public static String getJREDir(String defaultConfigFolder, JREVersion jreVersion) {
        switch (jreVersion) {
            case JRE1_3:
                return String.format(JRE_ROOT_PATH, defaultConfigFolder, "1.3.1_20");
            case JRE1_4:
                return String.format(JRE_ROOT_PATH, defaultConfigFolder, "1.4.2_18");
            case JRE1_5:
                return String.format(JRE_ROOT_PATH, defaultConfigFolder, "1.5.0_16");
            case JRE1_6:
                return String.format(JRE_ROOT_PATH, defaultConfigFolder, "1.6.0_30");
            case JRE1_7:
                return String.format(JRE_ROOT_PATH, defaultConfigFolder, "1.7.0_95_debug");
            case JRE1_8:
                return String.format(JRE_ROOT_PATH, defaultConfigFolder, "1.8.0_121_debug");

        }
        throw new IllegalArgumentException("The artifact type is unknown.");
    }

    public enum J2EEVersion {
        J2EE8, J2EE7, J2EE6, NONE
    }

    public enum JREVersion {
        JRE1_8, JRE1_7, JRE1_6, JRE1_5, JRE1_4, JRE1_3
    }
}
