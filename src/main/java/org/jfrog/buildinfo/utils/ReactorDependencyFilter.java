package org.jfrog.buildinfo.utils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *  A filter to exclude dependency nodes by artifacts.
 *
 */
public class ReactorDependencyFilter implements DependencyFilter {

    private Set<String> keys = new HashSet<>();

    public ReactorDependencyFilter(Collection<Artifact> artifacts) {
        for (Artifact artifact : artifacts) {
            String key = ArtifactUtils.key(artifact);
            keys.add(key);
        }
    }

    /**
     * Indicates whether the specified dependency node shall be included or excluded.
     *
     * @param node The dependency node to filter, must not be {@code null}.
     * @param parents The (read-only) chain of parent nodes that leads to the node to be filtered, must not be
     *            {@code null}. Iterating this (possibly empty) list walks up the dependency graph towards the root
     *            node, i.e. the immediate parent node (if any) is the first node in the list. The size of the list also
     *            denotes the zero-based depth of the filtered node.
     * @return {@code true} to include the dependency node, {@code false} to exclude it.
     */
    @Override
    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
        Dependency dependency = node.getDependency();
        if (dependency != null) {
            org.eclipse.aether.artifact.Artifact artifact = dependency.getArtifact();
            String key = ArtifactUtils.key(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
            return !keys.contains(key);
        }
        return false;
    }

}
