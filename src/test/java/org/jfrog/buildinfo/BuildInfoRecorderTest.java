package org.jfrog.buildinfo;

import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.execution.ExecutionEvent;
import org.jfrog.build.extractor.builder.BuildInfoBuilder;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.Dependency;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.buildinfo.deployment.BuildInfoRecorder;
import org.jfrog.buildinfo.types.TestExecutionEvent;
import org.junit.Before;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.jfrog.build.api.BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX;
import static org.jfrog.buildinfo.utils.Utils.getArtifactName;

/**
 * Test {@link BuildInfoRecorder} class functionality.
 *
 * @author yahavi
 */
public class BuildInfoRecorderTest extends ArtifactoryMojoTestBase {

    private static final Artifact TEST_ARTIFACT = new DefaultArtifact("groupId", "artifactId", "1", "compile", "jar", "", null);
    private BuildInfoRecorder buildInfoRecorder;
    private ExecutionEvent executionEvent;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executionEvent = new TestExecutionEvent(mojo.session, mojo.project);
        buildInfoRecorder = new BuildInfoRecorder(mojo.session, mojo.getLog(), mojo.artifactory.delegate);
        mojo.project.setArtifacts(Sets.newHashSet(TEST_ARTIFACT));
    }

    public void testProjectSucceeded() {
        buildInfoRecorder.projectSucceeded(executionEvent);
        BuildInfoBuilder buildInfoBuilder = buildInfoRecorder.getBuildInfoBuilder();
        assertNotNull(buildInfoBuilder);
        BuildInfo build = buildInfoBuilder.build();

        // Check build details
        assertEquals("buildName", build.getName());
        assertEquals("1", build.getNumber());
        assertEquals("micprj", build.getProject());
        assertEquals("1.0.1", build.getVersion());
        assertEquals("agentName", build.getAgent().getName());
        assertEquals("2", build.getAgent().getVersion());
        assertTrue(build.getStarted().startsWith("2020-01-01T00:00:00.000"));
        assertEquals(0, build.getDurationMillis());
        assertEquals("http://1.2.3.4", build.getUrl());
        assertFalse(CollectionUtils.isEmpty(build.getVcs()));

        // Check module
        List<Module> modules = build.getModules();
        assertEquals(1, modules.size());
        Module module = modules.get(0);
        assertEquals("org.jfrog.test:unit-tests-pom:1.0.0", module.getId());
        assertEquals("value", module.getProperties().getProperty("test.property.key"));

        // Check artifact
        List<org.jfrog.build.extractor.ci.Artifact> artifacts = module.getArtifacts();
        assertEquals(1, artifacts.size());
        org.jfrog.build.extractor.ci.Artifact artifact = artifacts.get(0);
        assertEquals("unit-tests-pom-1.0.0.pom", artifact.getName());
        assertEquals("pom", artifact.getType());

        // Check dependency
        List<Dependency> dependencies = module.getDependencies();
        assertEquals(1, dependencies.size());
        Dependency dependency = dependencies.get(0);
        assertEquals(String.join(":", TEST_ARTIFACT.getGroupId(), TEST_ARTIFACT.getArtifactId(), TEST_ARTIFACT.getVersion()), dependency.getId());
        assertEquals(1, dependency.getScopes().size());
        assertTrue(dependency.getScopes().contains("compile"));
        assertEquals(TEST_ARTIFACT.getType(), dependency.getType());
    }

    public void testExtract() {
        Properties properties = new Properties();
        properties.put(BUILD_INFO_ENVIRONMENT_PREFIX + "testProperty", "testPropertyValue");
        mojo.artifactory.delegate.fillFromProperties(properties);
        BuildInfo build = buildInfoRecorder.extract(executionEvent);
        assertNotNull(build);
        assertTrue(build.getDurationMillis() > 0);
        assertTrue(build.getStartedMillis() > 0);
        assertEquals("testPropertyValue", build.getProperties().get(BUILD_INFO_ENVIRONMENT_PREFIX + "testProperty"));
    }

    public void testMojoSucceeded() {
        buildInfoRecorder.mojoSucceeded(executionEvent);
        checkDependencyPopulated(buildInfoRecorder);
    }

    public void testMojoFailed() {
        buildInfoRecorder.mojoFailed(executionEvent);
        checkDependencyPopulated(buildInfoRecorder);
    }

    private void checkDependencyPopulated(BuildInfoRecorder buildInfoRecorder) {
        Set<Artifact> dependencies = buildInfoRecorder.getCurrentModuleDependencies();
        assertNotNull(dependencies);
        assertEquals(1, dependencies.size());

        Artifact dependency = dependencies.iterator().next();
        assertEquals(TEST_ARTIFACT.getGroupId(), dependency.getGroupId());
        assertEquals(TEST_ARTIFACT.getArtifactId(), dependency.getArtifactId());
        assertEquals(TEST_ARTIFACT.getVersion(), dependency.getVersion());
        assertEquals(TEST_ARTIFACT.getScope(), dependency.getScope());
        assertEquals(TEST_ARTIFACT.getType(), dependency.getType());
        assertEquals(TEST_ARTIFACT.getClassifier(), dependency.getClassifier());
    }

    public void testArtifactNameWithoutClassifierForPom() {
        Artifact pomArtifact = new DefaultArtifact("groupId", "artifactId", "1.0.0", "compile", "pom", "", null);
        String artifactName = getArtifactName(pomArtifact.getArtifactId(), pomArtifact.getVersion(), pomArtifact.getClassifier(), "pom");
        assertEquals("artifactId-1.0.0.pom", artifactName);
    }

    public void testArtifactNameWithClassifierForPom() {
        Artifact pomArtifact = new DefaultArtifact("groupId", "artifactId", "1.0.0", "compile", "pom", "my-classifier", null);
        String artifactName = getArtifactName(pomArtifact.getArtifactId(), pomArtifact.getVersion(), pomArtifact.getClassifier(), "pom");
        assertEquals("artifactId-1.0.0.pom", artifactName); // Ensure suffix is NOT added
    }

    public void testArtifactNameWithClassifier() {
        Artifact jarArtifact = new DefaultArtifact("groupId", "artifactId", "1.0.0", "compile", "jar", "my-classifier", null);
        String artifactName = getArtifactName(jarArtifact.getArtifactId(), jarArtifact.getVersion(), jarArtifact.getClassifier(), jarArtifact.getType());
        assertEquals("artifactId-1.0.0-my-classifier.jar", artifactName); // Ensure suffix is added
    }

    public void testGetArtifactNameWithNullFileExtension() {
        String artifactId = "test-artifact";
        String version = "1.0.0";
        String classifier = "my-classifier";
        String fileExtension = null; // Simulate null file extension

        String artifactName = getArtifactName(artifactId, version, classifier, fileExtension);
        String expectedName = "test-artifact-1.0.0-my-classifier.null";
        assertEquals(expectedName, artifactName);
    }

}
