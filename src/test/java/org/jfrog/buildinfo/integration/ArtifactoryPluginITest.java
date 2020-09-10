package org.jfrog.buildinfo.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.RequestDefinition;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import static org.mockserver.model.HttpRequest.request;

/**
 * === Integration tests ===
 * The tests execute 'mvn clean deploy' automatically on each one of the test projects.
 * In order to use the latest plugin code, run 'maven clean install' before running the tests.
 * <p>
 * To run the integration tests, execute 'mvn verify -DskipITs=false'
 * To remote debug the integration tests, run 'mvn verify -DskipITs=false -DdebugITs=true', add a break point and start
 * remote debugging on port 5005.
 *
 * @author yahavi
 */
public class ArtifactoryPluginITest extends TestCase {

    private static final String PLUGIN_NOT_INSTALLED = "Couldn't find 'artifactory-maven-plugin-*.jar' file. Please make sure to run 'mvn install' before running the integration tests.";

    // Artifactory Maven plugin artifacts
    private static final String[] MULTI_MODULE_ARTIFACTS = {
            "/artifactory/libs-snapshot-local/org/jfrog/test/multi/3.7-SNAPSHOT/multi-3.7-SNAPSHOT.pom"
    };
    private static final String[] MULTI_MODULE_1_ARTIFACTS = {
            "/artifactory/libs-snapshot-local/org/jfrog/test/multi1/3.7-SNAPSHOT/multi1-3.7-SNAPSHOT.pom",
            "/artifactory/libs-snapshot-local/org/jfrog/test/multi1/3.7-SNAPSHOT/multi1-3.7-SNAPSHOT.jar",
            "/artifactory/libs-snapshot-local/org/jfrog/test/multi1/3.7-SNAPSHOT/multi1-3.7-SNAPSHOT-sources.jar"
    };
    private static final String[] MULTI_MODULE_2_ARTIFACTS = {
            "/artifactory/libs-snapshot-local/org/jfrog/test/multi2/3.7-SNAPSHOT/multi2-3.7-SNAPSHOT.pom",
            "/artifactory/libs-snapshot-local/org/jfrog/test/multi2/3.7-SNAPSHOT/multi2-3.7-SNAPSHOT.jar"
    };
    private static final String[] MULTI_MODULE_3_ARTIFACTS = {
            "/artifactory/libs-snapshot-local/org/jfrog/test/multi3/3.7-SNAPSHOT/multi3-3.7-SNAPSHOT.pom",
            "/artifactory/libs-snapshot-local/org/jfrog/test/multi3/3.7-SNAPSHOT/multi3-3.7-SNAPSHOT.war"
    };

    // Maven archetype artifacts
    private static final String[] MAVEN_ARC_ARTIFACTS = {
            "/artifactory/libs-snapshot-local/org/example/maven-archetype-simple/1.0-SNAPSHOT/maven-archetype-simple-1.0-SNAPSHOT.jar",
            "/artifactory/libs-snapshot-local/org/example/maven-archetype-simple/1.0-SNAPSHOT/maven-archetype-simple-1.0-SNAPSHOT.pom"
    };

    public void testMultiModule() throws Exception {
        try (ClientAndServer mockServer = ClientAndServer.startClientAndServer(8081)) {
            initializeMockServer(mockServer);
            runProject("artifactory-maven-plugin-example");

            // Check deployed artifacts
            checkDeployedArtifacts(mockServer, MULTI_MODULE_ARTIFACTS);
            checkDeployedArtifacts(mockServer, MULTI_MODULE_1_ARTIFACTS);
            checkDeployedArtifacts(mockServer, MULTI_MODULE_2_ARTIFACTS);
            checkDeployedArtifacts(mockServer, MULTI_MODULE_3_ARTIFACTS);

            // Extract build from request
            Build build = getAndAssertBuild(mockServer);

            // Check project specific fields
            assertEquals("plugin-demo", build.getName());
            assertTrue(StringUtils.isNotBlank(build.getNumber()));
            assertEquals("http://build-url.org", build.getUrl());

            // Check parent module
            Module parent = build.getModule("org.jfrog.test:multi:3.7-SNAPSHOT");
            assertNotNull(parent);
            assertEquals(MULTI_MODULE_ARTIFACTS.length, CollectionUtils.size(parent.getArtifacts()));
            assertEquals(0, CollectionUtils.size(parent.getDependencies()));
            assertEquals(4, CollectionUtils.size(parent.getProperties()));

            // Check multi1
            Module multi1 = build.getModule("org.jfrog.test:multi1:3.7-SNAPSHOT");
            assertNotNull(multi1);
            assertEquals(MULTI_MODULE_1_ARTIFACTS.length, CollectionUtils.size(multi1.getArtifacts()));
            assertEquals(13, CollectionUtils.size(multi1.getDependencies()));
            assertEquals(4, CollectionUtils.size(multi1.getProperties()));

            // Check multi2
            Module multi2 = build.getModule("org.jfrog.test:multi2:3.7-SNAPSHOT");
            assertNotNull(multi2);
            assertEquals(MULTI_MODULE_2_ARTIFACTS.length, CollectionUtils.size(multi2.getArtifacts()));
            assertEquals(1, CollectionUtils.size(multi2.getDependencies()));
            assertEquals(5, CollectionUtils.size(multi2.getProperties()));

            // Check multi3
            Module multi3 = build.getModule("org.jfrog.test:multi3:3.7-SNAPSHOT");
            assertNotNull(multi1);
            assertEquals(MULTI_MODULE_3_ARTIFACTS.length, CollectionUtils.size(multi3.getArtifacts()));
            assertEquals(15, CollectionUtils.size(multi3.getDependencies()));
            assertEquals(4, CollectionUtils.size(multi3.getProperties()));
        }
    }

    public void testMavenArchetypeExample() throws Exception {
        try (ClientAndServer mockServer = ClientAndServer.startClientAndServer(8081)) {
            initializeMockServer(mockServer);
            runProject("maven-archetype-simple");
            checkDeployedArtifacts(mockServer, MAVEN_ARC_ARTIFACTS);
            Build build = getAndAssertBuild(mockServer);

            // Check project specific fields
            assertEquals("maven-archetype-simple", build.getName());
            assertTrue(StringUtils.isNotBlank(build.getNumber()));

            // Check module
            Module module = build.getModule("org.example:maven-archetype-simple:1.0-SNAPSHOT");
            assertEquals(MAVEN_ARC_ARTIFACTS.length, CollectionUtils.size(module.getArtifacts()));
            assertEquals(209, CollectionUtils.size(module.getDependencies()));
            assertEquals(4, CollectionUtils.size(module.getProperties()));
        }
    }

    private void initializeMockServer(ClientAndServer mockServer) {
        mockServer.when(request("/artifactory/api/system/version")).respond(HttpResponse.response("{\"version\":\"7.0.0\"}"));
        mockServer.when(request()).respond(HttpResponse.response().withStatusCode(200));
    }

    private void runProject(String projectName) throws VerificationException, IOException {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/integration/" + projectName);
        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        if (StringUtils.equalsIgnoreCase(System.getProperty("debugITs"), "true")) {
            verifier.setEnvironmentVariable("MAVEN_OPTS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
        }
        verifier.executeGoals(Lists.newArrayList("clean", "deploy", "-Dartifactory.plugin.version=" + getPluginVersion()));
        verifier.verifyErrorFreeLog();
    }

    /**
     * Get the Maven Artifactory plugin version to configure it in the running projects.
     *
     * @return - Maven Artifactory plugin version
     */
    private String getPluginVersion() {
        FileFilter fileFilter = new WildcardFileFilter("artifactory-maven-plugin*");
        File[] files = new File("target").listFiles(fileFilter);
        assertNotNull(PLUGIN_NOT_INSTALLED, files);
        assertEquals(1, files.length);
        String withoutStart = StringUtils.removeStart(files[0].getName(), "artifactory-maven-plugin-");
        return StringUtils.removeEnd(withoutStart, ".jar");
    }

    /**
     * Extract and assert the deployed artifacts from the request.
     *
     * @param mockServer        - The mock server
     * @param expectedArtifacts - The expected artifacts
     */
    private void checkDeployedArtifacts(ClientAndServer mockServer, String[] expectedArtifacts) {
        RequestDefinition[] requestDefinitions = mockServer.retrieveRecordedRequests(null);
        for (String expectedArtifact : expectedArtifacts) {
            assertTrue(Arrays.stream(requestDefinitions)
                    .map(Objects::toString)
                    .anyMatch(request -> request.contains(expectedArtifact)));
        }
    }

    /**
     * Extract the build info from the request.
     *
     * @param mockServer - The mock server
     * @return the build info
     * @throws JsonProcessingException - In case of parsing error
     */
    private Build getAndAssertBuild(ClientAndServer mockServer) throws JsonProcessingException {
        RequestDefinition[] requestDefinitions = mockServer.retrieveRecordedRequests(request("/artifactory/api/build"));
        assertEquals(1, ArrayUtils.getLength(requestDefinitions));
        RequestDefinition buildInfoRequest = requestDefinitions[0];
        ObjectMapper mapper = new ObjectMapper();
        JsonNode buildInfoRequestNode = mapper.readTree(buildInfoRequest.toString());
        JsonNode body = buildInfoRequestNode.get("body");
        JsonNode json = body.get("json");
        Build build = mapper.readValue(json.toString(), Build.class);
        assertNotNull(build);

        // Check common fields
        assertEquals("1.0.1", build.getVersion());
        assertEquals("artifactory-maven-plugin", build.getAgent().getName());
        assertEquals(getPluginVersion(), build.getAgent().getVersion());
        assertEquals("Maven", build.getBuildAgent().getName());
        assertTrue(StringUtils.isNotBlank(build.getBuildAgent().getVersion()));
        assertTrue(StringUtils.isNotBlank(build.getStarted()));
        assertTrue(build.getDurationMillis() > 0);
        assertFalse(build.getProperties().isEmpty());
        return build;
    }
}
