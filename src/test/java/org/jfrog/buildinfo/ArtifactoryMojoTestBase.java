package org.jfrog.buildinfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.execution.*;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.jfrog.buildinfo.resolution.RepositoryListener;
import org.jfrog.buildinfo.types.MavenLogger;
import org.jfrog.buildinfo.types.PlexusLogger;
import org.junit.Before;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * Test bases of unit test classes.
 * Initialize the {@link ArtifactoryMojo} with the pom in the following path: "src/test/resources/unit-tests-pom/pom.xml"
 *
 * @author yahavi
 */
public abstract class ArtifactoryMojoTestBase extends AbstractMojoTestCase {

    private final File testPom = new File(getBasedir(), "src/test/resources/unit-tests-pom/pom.xml");
    ArtifactoryMojo mojo;

    static Date TEST_DATE = createTestDate();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        createArtifactoryMojo();
        assertNotNull(mojo);
        mojo.execute();
    }

    @Override
    protected String getPluginDescriptorLocation() {
        return "META-INF/maven/org.jfrog.buildinfo/artifactory-maven-plugin/plugin-help.xml";
    }

    @Override
    protected MavenSession newMavenSession(MavenProject project) {
        try {
            MavenSession session = createMavenSession();
            session.setCurrentProject(project);
            session.setProjects(Collections.singletonList(project));
            session.getGoals().add("deploy");
            return session;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Initialize TEST_DATE static variable.
     *
     * @return a new Date initialized by 01/01/2020
     */
    private static Date createTestDate() {
        try {
            return new SimpleDateFormat("dd/MM/yyyy").parse("01/01/2020");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create and initialize the ArtifactoryMojo object.
     *
     * @throws Exception in case of any error
     */
    private void createArtifactoryMojo() throws Exception {
        ProjectBuildingRequest buildingRequest = createMavenSession().getProjectBuildingRequest();
        ProjectBuilder projectBuilder = lookup(ProjectBuilder.class);
        MavenProject project = projectBuilder.build(testPom, buildingRequest).getProject();
        PluginExecution execution = project.getPlugin("org.apache.maven.plugins:artifactory-maven-plugin").getExecutions().get(0);
        Xpp3Dom dom = (Xpp3Dom) execution.getConfiguration();
        mojo = (ArtifactoryMojo) lookupConfiguredMojo(project, "publish");
        fillMojoFromConfiguration(dom);
    }

    /**
     * Create a new MavenSession.
     *
     * @return MavenSession
     * @throws MavenExecutionRequestPopulationException in case of error during creating MavenExecutionRequest
     * @throws ComponentLookupException                 in case os error during creating MavenExecutionRequest or RepositorySystemSession
     * @throws NoLocalRepositoryManagerException        in case of error during creating DefaultRepositorySystemSession
     */
    private MavenSession createMavenSession() throws MavenExecutionRequestPopulationException, ComponentLookupException, NoLocalRepositoryManagerException {
        // Create the MavenExecutionRequest
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setSystemProperties(System.getProperties());
        request.setStartTime(TEST_DATE);
        getContainer().lookup(MavenExecutionRequestPopulator.class).populateDefaults(request);

        // Create the RepositorySystemSession
        DefaultMaven maven = (DefaultMaven) getContainer().lookup(Maven.class);
        DefaultRepositorySystemSession repoSession = (DefaultRepositorySystemSession) maven.newRepositorySession(request);
        repoSession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory().newInstance(repoSession, new LocalRepository(request.getLocalRepository().getBasedir())));

        //noinspection deprecation
        return new MavenSession(getContainer(), repoSession, request, new DefaultMavenExecutionResult());
    }

    /**
     * Fill the ArtifactoryMojo with data deserialized from the pom.
     *
     * @param configuration - The Artifactory plugin configuration in the pom.
     * @throws JsonProcessingException in case of deserialization error
     */
    private void fillMojoFromConfiguration(Xpp3Dom configuration) throws JsonProcessingException {
        ObjectMapper objectMapper = new XmlMapper().registerModule(new GuavaModule());
        mojo.deployProperties = objectMapper.readValue(configuration.getChild("deployProperties").toString(), new TypeReference<Map<String, String>>() {
        });
        mojo.artifactory = objectMapper.readValue(configuration.getChild("artifactory").toString(), Config.Artifactory.class);
        mojo.buildInfo = objectMapper.readValue(configuration.getChild("buildInfo").toString(), Config.BuildInfo.class);
        mojo.publisher = objectMapper.readValue(configuration.getChild("publisher").toString(), Config.Publisher.class);
        mojo.resolver = objectMapper.readValue(configuration.getChild("resolver").toString(), Config.Resolver.class);
        Log log = new MavenLogger();
        mojo.setLog(log);
        mojo.repositoryListener = new RepositoryListener(new PlexusLogger(log));
    }
}