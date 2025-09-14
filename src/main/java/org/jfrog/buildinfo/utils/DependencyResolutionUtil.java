package org.jfrog.buildinfo.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import java.util.*;

public class DependencyResolutionUtil {

    /**
     * Create a map of dependency to parents.
     * Key - dependency ID - group:artifact:version.
     * Value - parents path-to-module. For example:
     * [["parentIdA", "a1", "a2",... "moduleId"]
     * ["parentIdB", "b1", "b2",... "moduleId"]]
     *
     * @param dependencyNode - The root dependency node
     * @return map of dependency to parents.
     * @see org.jfrog.build.api.Dependency#setRequestedBy(String[][])
     */
    public static Map<String, String[][]> createDependencyParentsMap(DependencyNode dependencyNode) {
        if (dependencyNode == null) {
            return Collections.EMPTY_MAP;
        }
        Map<String, String[][]> dependencyParentsMap = new HashMap<>();

        for (DependencyNode child : dependencyNode.getChildren()) {
            String childGav = getGavString(child);
            // Populate the direct children with the module's GAV
            List<String> parents = Collections.singletonList(getGavString(dependencyNode));
            addParent(dependencyParentsMap, childGav, parents);

            // Create dependency parent map for children
            createDependencyParentsMap(dependencyParentsMap, child, parents);
        }
        return dependencyParentsMap;
    }

    /**
     * Recursively create a requirements map for transitive dependencies.
     *
     * @param dependencyParentsMap - Output - The map to populate
     * @param dependencyNode       - The current dependency node
     * @param parent               - The parent path-to-module list
     */
    private static void createDependencyParentsMap(Map<String, String[][]> dependencyParentsMap, DependencyNode dependencyNode, List<String> parent) {
        List<DependencyNode> children = dependencyNode.getChildren();
        if (children == null || children.isEmpty()) {
            return;
        }

        // Create the parent path-to-module for the children
        List<String> childParents = new ArrayList<>(parent);
        childParents.add(0, getGavString(dependencyNode));

        for (DependencyNode child : dependencyNode.getChildren()) {
            String childGav = getGavString(child);
            addParent(dependencyParentsMap, childGav, childParents);
            createDependencyParentsMap(dependencyParentsMap, child, childParents);
        }
    }

    /**
     * Add parent to the dependency.
     *
     * @param dependencyParentsMap - The dependency parents map
     * @param childGav             - The child dependency GAV
     * @param parent               - The parent path-to-module list to add to the map
     */
    private static void addParent(Map<String, String[][]> dependencyParentsMap, String childGav, List<String> parent) {
        // Get current parents
        String[][] currentParents = dependencyParentsMap.getOrDefault(childGav, new String[][]{});

        // Add the input parent to the current parents
        currentParents = (String[][]) ArrayUtils.add(currentParents, parent.toArray(ArrayUtils.EMPTY_STRING_ARRAY));

        // Set the updated parents
        dependencyParentsMap.put(childGav, currentParents);
    }

    private  static String getGavString(DependencyNode dependencyNode) {
        Artifact artifact = dependencyNode.getArtifact();
        return BuildInfoExtractorUtils.getModuleIdString(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
    }

}
