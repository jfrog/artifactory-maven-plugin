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

public class ReactorDependencyFilter implements DependencyFilter {

    private Set<String> keys = new HashSet<>();

    public ReactorDependencyFilter(Collection<Artifact> artifacts) {
        for (Artifact artifact : artifacts) {
            String key = ArtifactUtils.key(artifact);
            keys.add(key);
        }
    }

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
