package edu.rit.se.design.dodo.codeanalyzer.config;

import edu.rit.se.design.dodo.codeanalyzer.config.PlatformFactory.J2EEVersion;
import edu.rit.se.design.dodo.codeanalyzer.config.PlatformFactory.JREVersion;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import static edu.rit.se.design.dodo.codeanalyzer.config.Config.ConfigKey.*;
import static edu.rit.se.design.dodo.codeanalyzer.config.ConfigDefaultValues.DEFAULT_CONFIG_FOLDER;
import static edu.rit.se.design.dodo.codeanalyzer.config.ConfigDefaultValues.SEPARATOR_CHAR;

/**
 * It is used to configure the generation of the project's data structures (Java's HOME location, exclusions file, etc). See the {@link ConfigKey} enumeration for the list of property keys that have to be
 * declared in the configuration file.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class Config {

    // property keys that  can be provided in the configuration file
    public enum ConfigKey {CONFIG_FOLDER, PROJECT_PATH, JRE_VERSION, J2EE_VERSION, ENTRYPOINTS_FILE, SINKS_FILE, SOURCES_FILE, EXCLUSIONS_FILE, DEPENDENCIES}

    // required/optional values
    private static String[] REQUIRED_KEYS = new String[]{PROJECT_PATH.name(), JRE_VERSION.name(), ENTRYPOINTS_FILE.name(), SOURCES_FILE.name(), SINKS_FILE.name(), EXCLUSIONS_FILE.name()};
    private static String[] OPTIONAL_KEYS = new String[]{CONFIG_FOLDER.name(), DEPENDENCIES.name(), J2EE_VERSION.name() };

    // specified configuration parameters
    private String projectPath; // binary of the project under analysis
    private String jreDir; // directory where the J2SE API is located (standard Java)
    private String j2eeJar; // the JAR file that has the J2EE API (Java Web API as a bunch of interfaces/abstract classes)
    private String exclusionsFile; //a TXT file that  lists the classes/packages to be excluded from analysis
    private String entrypointsFile; // CSV file that has a list of entrypoint signatures
    private String sinksFile; // a file that has a list of sink methods signatures
    private String sourcesFile;// a file that has a list of source methods signatures
    private JREVersion jreVersion; // the Java Runtime Version used for the JAR analysis
    private J2EEVersion j2eeVersion; // the J2EE version used for the JAR analysis
    private String[] dependenciesList; // a list of JARs/folders to be included in the classpath.
    private String configFolder; // the root of the folder that has snapshots of the JRE/J2EE platforms


    private Config() {
    }

    /**
     * @param propertiesFile a path to a *.properties file
     * @return a {@link Config} object instance populated from the attributes in the property file
     * @throws FileNotFoundException if property file is not found in the machine
     * @throws IOException           an error while reading the configuration file
     */
    public static Config loadFromFile(String propertiesFile) throws FileNotFoundException, IOException {
        Properties p = new Properties();
        File configFile = new File(propertiesFile);
        p.load(new FileInputStream(configFile));
        if (!isValid(p)) {
            throw new IllegalArgumentException("Properties file malformed.\n" +
                    "Required properties:\n\t" + Arrays.toString(REQUIRED_KEYS) +
                    "\nOptional properties:\n\t" + Arrays.toString(OPTIONAL_KEYS)
            );
        }


        Config c = new Config();

        c.configFolder = p.getProperty(CONFIG_FOLDER.toString(), DEFAULT_CONFIG_FOLDER);
        c.jreVersion = JREVersion.valueOf(p.getProperty(JRE_VERSION.toString()));


        if (p.stringPropertyNames().contains(J2EE_VERSION.toString())) {
            c.j2eeVersion = J2EEVersion.valueOf(p.getProperty(J2EE_VERSION.toString()));
            c.j2eeJar = PlatformFactory.getJ2EEJar(c.configFolder, c.j2eeVersion);
        }
        c.jreDir = PlatformFactory.getJREDir(c.configFolder, c.jreVersion);
        // paths to files that need to be handled based whether they're absolute or relative
        c.projectPath = getFilePathProperty(configFile, p, PROJECT_PATH.toString());//p.getProperty(PROJECT_PATH.toString());
        c.entrypointsFile = getFilePathProperty(configFile, p, ENTRYPOINTS_FILE.toString());//p.getProperty(ENTRYPOINTS_FILE.toString());
        c.sinksFile = getFilePathProperty(configFile, p, SINKS_FILE.toString());//p.getProperty(SINKS_FILE.toString());
        c.sourcesFile = getFilePathProperty(configFile, p, SOURCES_FILE.toString());//p.getProperty(SINKS_FILE.toString());
        c.exclusionsFile = getFilePathProperty(configFile, p, EXCLUSIONS_FILE.toString());//p.getProperty(EXCLUSIONS_FILE.toString());

        String dependenciesAsString = p.getProperty(DEPENDENCIES.toString(), null);
        // if provided, parses dependencies and convert to array of strings
        if (dependenciesAsString != null) {
            c.dependenciesList = dependenciesAsString.split(SEPARATOR_CHAR);
            for (int i = 0; i < c.dependenciesList.length; i++) {
                c.dependenciesList[i] = getAbsoluteFilePath(c.dependenciesList[i].trim(), configFile);
                System.out.println(c.dependenciesList[i]);
            }
        } else {
            c.dependenciesList = null;
        }

        return c;
    }

    /**
     * Verifies  that all required values were provided
     *
     * @param p properties object
     * @return true if all required values were specified
     */
    private static boolean isValid(Properties p) {
        for (String key : REQUIRED_KEYS) {
            if (!p.containsKey(key))
                return false;
        }
        return true;
    }

    /**
     * @param path A relative path in relation to the given file.
     * @param file
     * @return The absolute path of the given relative path.
     * @throws IOException
     */
    private static String getAbsoluteFilePath(String path, File file) throws IOException {
        if (new File(path).isAbsolute()) return path;
        return new File(file.getParentFile(), path).getCanonicalPath();
    }

    private static String getFilePathProperty(File configFile, Properties p, String propertyName) throws IOException {
        String filepath = p.getProperty(propertyName);

        // it is a relative path, let's fix it to relative the config file location
        if (filepath != null && !new File(filepath).isAbsolute()) {
            return new File(configFile.getParentFile(), filepath).getCanonicalPath();
        }
        return filepath;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public String getJreDir() {
        return jreDir;
    }

    public String getJ2eeJar() {
        return j2eeJar;
    }

    public String getExclusionsFile() {
        return exclusionsFile;
    }

    public String getEntrypointsFile() {
        return entrypointsFile;
    }

    public String getSinksFile() {
        return sinksFile;
    }

    public String getSourcesFile() {
        return sourcesFile;
    }

    public JREVersion getJreVersion() {
        return jreVersion;
    }

    public J2EEVersion getJ2eeVersion() {
        return j2eeVersion;
    }

    public String[] getDependenciesList() {
        return dependenciesList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Config config = (Config) o;

        return new EqualsBuilder()
                .append(projectPath, config.projectPath)
                .append(jreDir, config.jreDir)
                .append(j2eeJar, config.j2eeJar)
                .append(exclusionsFile, config.exclusionsFile)
                .append(entrypointsFile, config.entrypointsFile)
                .append(sinksFile, config.sinksFile)
                .append(sourcesFile, config.sourcesFile)
                .append(jreVersion, config.jreVersion)
                .append(j2eeVersion, config.j2eeVersion)
                .append(dependenciesList, config.dependenciesList)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(projectPath)
                .append(jreDir)
                .append(j2eeJar)
                .append(exclusionsFile)
                .append(entrypointsFile)
                .append(sinksFile)
                .append(sourcesFile)
                .append(jreVersion)
                .append(j2eeVersion)
                .append(dependenciesList)
                .toHashCode();
    }

    @Override
    public String toString() {

        return String.format(super.toString() + "{\n\tprojectPath=%s, \n\tj2seDir=%s,\n\tj2eeJar=%s,\n\texclusionsFile=%s,\n\tentrypointsFile=%s,\n\tsinksFile=%s,\n\tsourcesFile=%s,\n\tdependenciesList=%s\n}",
                this.projectPath,
                this.jreDir,
                this.j2eeJar,
                this.exclusionsFile,
                this.entrypointsFile,
                this.sinksFile,
                this.sourcesFile,
                String.join(",", dependenciesList == null ? new String[0] : dependenciesList));
    }


}
