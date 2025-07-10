package edu.rit.se.design.dodo.codeanalyzer.config;

import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import edu.rit.se.design.dodo.codeanalyzer.callgraph.CallGraphBuilderType;

/**
 * This class holds default configurations for multiple aspects.
 *
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class ConfigDefaultValues {
    /**
     * Char that separates dependencies' paths
     */
    public static final String SEPARATOR_CHAR = ",";
    public static final String J2EE_ROOT_PATH = "%s/J2EEs/javaee-api-%s.jar";
    public static final String JRE_ROOT_PATH = "%s/JREs/jre%s/";

    // default call graph construction configurations
    public static final CallGraphBuilderType DEFAULT_BUILDER_TYPE = CallGraphBuilderType.ZERO_ONE_CONTAINER_CFA;
    public static final DataDependenceOptions DEFAULT_DATA_DEP_OPTIONS = DataDependenceOptions.NO_BASE_NO_HEAP;
    public static final ControlDependenceOptions DEFAULT_CONTROL_DEP_OPTIONS = ControlDependenceOptions.FULL;

    /**
     * the location for the JRE/J2EE snapshots
     */
     public static String DEFAULT_CONFIG_FOLDER = "./config/";

}
