package org.jfrog.buildinfo.deployment;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.jfrog.build.extractor.ci.Agent;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.BuildAgent;
import org.jfrog.build.extractor.ci.MatrixParameter;
import org.jfrog.build.extractor.builder.BuildInfoMavenBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.util.GitUtils;
import org.jfrog.buildinfo.utils.ArtifactoryMavenLogger;

import java.io.IOException;

import static org.jfrog.build.api.BuildInfoFields.*;
import static org.jfrog.buildinfo.utils.Utils.getMavenVersion;
import static org.jfrog.buildinfo.utils.Utils.getPluginVersion;

/**
 * Maven Build info builder that resolves the configuration from {@link ArtifactoryClientConfiguration}
 *
 * @author yahavi
 */
public class BuildInfoModelPropertyResolver extends BuildInfoMavenBuilder {

    private final Log logger;

    public BuildInfoModelPropertyResolver(Log logger, MavenSession session, ArtifactoryClientConfiguration clientConf) {
        super(StringUtils.firstNonBlank(clientConf.info.getBuildName(), session.getTopLevelProject().getName()));
        this.logger = logger;
        resolveCoreProperties(session, clientConf);
        resolveProperties(clientConf);
        resolveBuildAgent(clientConf);
        addRunParameters(clientConf);
        extractVcs(session);
    }

    /**
     * Resolve mandatory properties - build-name, build-number, project and build-started.
     *
     * @param session    - The current maven session to extract the start time
     * @param clientConf - Artifactory client configuration
     */
    private void resolveCoreProperties(MavenSession session, ArtifactoryClientConfiguration clientConf) {
        String buildNumber = StringUtils.defaultIfBlank(clientConf.info.getBuildNumber(), Long.toString(System.currentTimeMillis()));
        number(buildNumber);
        setProject(clientConf.info.getProject());
        long buildStartTime = session.getRequest().getStartTime().getTime();
        String buildStarted = StringUtils.defaultIfBlank(clientConf.info.getBuildStarted(), BuildInfo.formatBuildStarted(buildStartTime));
        started(buildStarted);

        logResolvedProperty(BUILD_NAME, super.name);
        logResolvedProperty(BUILD_NUMBER, buildNumber);
        if (StringUtils.isNotBlank(super.project)) {
            logResolvedProperty(BUILD_PROJECT, super.project);
        }
        logResolvedProperty(BUILD_STARTED, buildStarted);
    }

    /**
     * Resolve general optional properties.
     *
     * @param clientConf - Artifactory client configuration
     */
    private void resolveProperties(ArtifactoryClientConfiguration clientConf) {
        artifactoryPluginVersion(clientConf.info.getArtifactoryPluginVersion());
        artifactoryPrincipal(clientConf.publisher.getName());
        parentNumber(clientConf.info.getParentBuildNumber());
        parentName(clientConf.info.getParentBuildName());
        principal(clientConf.info.getPrincipal());
        url(clientConf.info.getBuildUrl());
    }

    /**
     * Resolve the build agent
     *
     * @param clientConf - Artifactory client configuration
     */
    private void resolveBuildAgent(ArtifactoryClientConfiguration clientConf) {
        BuildAgent buildAgent = new BuildAgent("Maven", getMavenVersion(getClass()));
        buildAgent(buildAgent);
        String agentName = clientConf.info.getAgentName();
        String agentVersion = clientConf.info.getAgentVersion();
        if (StringUtils.isBlank(agentName)) {
            agentName = "artifactory-maven-plugin";
            agentVersion = getPluginVersion();
        }
        agent(new Agent(agentName, agentVersion));
    }

    /**
     * Add run parameters
     *
     * @param clientConf - Artifactory client configuration
     */
    private void addRunParameters(ArtifactoryClientConfiguration clientConf) {
        clientConf.info.getRunParameters().entrySet().stream()
                .map(param -> new MatrixParameter(param.getKey(), param.getValue()))
                .forEach(this::addRunParameters);
    }

    /**
     * Extract VCS information from the nearest .git directory.
     *
     * @param session - The Maven session
     */
    private void extractVcs(MavenSession session) {
        try {
            vcs(Lists.newArrayList(GitUtils.extractVcs(session.getCurrentProject().getBasedir(), new ArtifactoryMavenLogger(logger))));
        } catch (IOException e) {
            throw new RuntimeException("Couldn't extract VCS information: " + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private void logResolvedProperty(String key, String value) {
        logger.debug("Artifactory Build Info Model Property Resolver: " + key + " = " + value);
    }
}