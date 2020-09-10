package org.jfrog.buildinfo;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;

import java.util.List;
import java.util.Map;

import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX;

/**
 * Test {@link ArtifactoryMojo} class functionality.
 *
 * @author yahavi
 */
public class ArtifactoryMojoTest extends ArtifactoryMojoTestBase {

    public void testArtifactoryConfiguration() {
        Config.Artifactory configArtifactory = mojo.artifactory;
        assertNotNull(configArtifactory);
        ArtifactoryClientConfiguration artifactory = configArtifactory.delegate;
        assertTrue(artifactory.isIncludeEnvVars());
        assertEquals("*password*,*secret*,*key*,*token*,*passphrase*", artifactory.getEnvVarsExcludePatterns());
        assertEquals("*", artifactory.getEnvVarsIncludePatterns());
        assertEquals(60, artifactory.getTimeout().intValue());
    }

    public void testResolverConfiguration() {
        Config.Resolver configResolver = mojo.resolver;
        assertNotNull(configResolver);
        ArtifactoryClientConfiguration.ResolverHandler resolver = configResolver.delegate;
        assertEquals("http://1.2.3.4/artifactory", resolver.getContextUrl());
        assertEquals("admin", resolver.getUsername());
        assertEquals("password", resolver.getPassword());
        assertEquals("libs-release", resolver.getRepoKey());
        assertEquals("libs-snapshot", resolver.getDownloadSnapshotRepoKey());
    }

    public void testPublisherConfiguration() {
        Config.Publisher configPublisher = mojo.publisher;
        assertNotNull(configPublisher);
        ArtifactoryClientConfiguration.PublisherHandler publisher = configPublisher.delegate;
        assertEquals("http://1.2.3.4/artifactory", publisher.getContextUrl());
        assertEquals("admin", publisher.getUsername());
        assertEquals("password", publisher.getPassword());
        assertEquals("*-tests.jar", publisher.getExcludePatterns());
        assertEquals("libs-release-local", publisher.getRepoKey());
        assertEquals("libs-snapshot-local", publisher.getSnapshotRepoKey());
    }

    public void testBuildInfoConfiguration() {
        Config.BuildInfo configBuildInfo = mojo.buildInfo;
        assertNotNull(configBuildInfo);
        ArtifactoryClientConfiguration.BuildInfoHandler buildInfo = configBuildInfo.delegate;
        assertEquals("http://1.2.3.4", buildInfo.getBuildUrl());
        assertEquals("buildName", buildInfo.getBuildName());
        assertEquals("1", buildInfo.getBuildNumber());
        assertEquals("agentName", buildInfo.getAgentName());
        assertEquals("2", buildInfo.getAgentVersion());
        assertEquals(5, buildInfo.getBuildRetentionDays().intValue());
        assertEquals("5", buildInfo.getBuildRetentionMinimumDate());
        assertTrue(buildInfo.getBuildStarted().startsWith("2020-01-01T00:00:00.000"));
    }

    public void testDeployProperties() {
        // Test input deploy properties
        Map<String, String> deployProperties = mojo.deployProperties;
        assertNotNull(deployProperties);
        assertEquals(1, deployProperties.size());
        assertEquals("propVal", deployProperties.get("propKey"));

        // Test actual deploy properties
        Config.Artifactory configArtifactory = mojo.artifactory;
        assertNotNull(configArtifactory);
        ArtifactoryClientConfiguration artifactory = configArtifactory.delegate;
        deployProperties = artifactory.getAllProperties();
        assertEquals(Long.toString(TEST_DATE.getTime()), deployProperties.get(PROP_DEPLOY_PARAM_PROP_PREFIX + BuildInfoFields.BUILD_TIMESTAMP));
        assertEquals("buildName", deployProperties.get(PROP_DEPLOY_PARAM_PROP_PREFIX + BuildInfoFields.BUILD_NAME));
        assertEquals("1", deployProperties.get(PROP_DEPLOY_PARAM_PROP_PREFIX + BuildInfoFields.BUILD_NUMBER));
        assertEquals("propVal", deployProperties.get(PROP_DEPLOY_PARAM_PROP_PREFIX + "propKey"));
    }

    public void testResolutionRepositories() {
        for (MavenProject project : mojo.session.getProjects()) {
            List<ArtifactRepository> pluginRepositories = project.getPluginArtifactRepositories();
            assertEquals(2, pluginRepositories.size());

            // Test snapshots repository
            ArtifactRepository snapshotsRepo = pluginRepositories.get(0);
            assertEquals("artifactory-snapshot", snapshotsRepo.getId());
            assertEquals("http://1.2.3.4/artifactory/libs-snapshot", snapshotsRepo.getUrl());
            assertTrue(snapshotsRepo.getSnapshots().isEnabled());
            assertFalse(snapshotsRepo.getReleases().isEnabled());

            // Test releases repository
            ArtifactRepository releasesRepo = pluginRepositories.get(1);
            assertEquals("artifactory-release", releasesRepo.getId());
            assertEquals("http://1.2.3.4/artifactory/libs-release", releasesRepo.getUrl());
            assertFalse(releasesRepo.getSnapshots().isEnabled());
            assertTrue(releasesRepo.getReleases().isEnabled());
        }
    }
}
