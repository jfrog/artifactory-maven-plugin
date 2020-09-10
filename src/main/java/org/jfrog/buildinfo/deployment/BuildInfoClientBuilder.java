package org.jfrog.buildinfo.deployment;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.buildinfo.utils.ArtifactoryMavenLogger;

import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_CONNECTION_RETRIES;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_TIMEOUT;

/**
 * Build {@link ArtifactoryBuildInfoClient} for deployment.
 *
 * @author yahavi
 */
public class BuildInfoClientBuilder {

    private ArtifactoryClientConfiguration clientConf;
    private final Log logger;

    public BuildInfoClientBuilder(Log logger) {
        this.logger = logger;
    }

    public BuildInfoClientBuilder clientConf(ArtifactoryClientConfiguration clientConf) {
        this.clientConf = clientConf;
        return this;
    }

    /**
     * Build {@link ArtifactoryBuildInfoClient}
     */
    public ArtifactoryBuildInfoClient build() {
        ArtifactoryBuildInfoClient client = createClient();
        setTimeout(client);
        setRetriesParams(client);
        setInsecureTls(client);
        return client;
    }

    /**
     * Create the build info client with URL and credentials.
     *
     * @return ArtifactoryBuildInfoClient
     */
    private ArtifactoryBuildInfoClient createClient() {
        // Resolve URL
        String contextUrl = clientConf.publisher.getContextUrl();
        if (StringUtils.isBlank(contextUrl)) {
            throw new IllegalArgumentException("Unable to resolve Artifactory Build Info Client properties: no context URL was found.");
        }
        logResolvedProperty(clientConf.publisher.getPrefix() + "." + ClientConfigurationFields.CONTEXT_URL, contextUrl);

        // Resolve username and password
        String username = clientConf.publisher.getUsername();
        String password = clientConf.publisher.getPassword();
        if (StringUtils.isNotBlank(username)) {
            logResolvedProperty(ClientConfigurationFields.USERNAME, username);
            return new ArtifactoryBuildInfoClient(contextUrl, username, password, new ArtifactoryMavenLogger(logger));
        }
        return new ArtifactoryBuildInfoClient(contextUrl, new ArtifactoryMavenLogger(logger));
    }

    private void setTimeout(ArtifactoryBuildInfoClient client) {
        if (clientConf.getTimeout() == null) {
            return;
        }
        int timeout = clientConf.getTimeout();
        logResolvedProperty(PROP_TIMEOUT, String.valueOf(timeout));
        client.setConnectionTimeout(timeout);
    }

    private void setRetriesParams(ArtifactoryBuildInfoClient client) {
        if (clientConf.getConnectionRetries() == null) {
            return;
        }
        int configMaxRetries = clientConf.getConnectionRetries();
        logResolvedProperty(PROP_CONNECTION_RETRIES, String.valueOf(configMaxRetries));
        client.setConnectionRetries(configMaxRetries);
    }

    private void setInsecureTls(ArtifactoryBuildInfoClient client) {
        boolean insecureTls = clientConf.getInsecureTls();
        logResolvedProperty(PROP_CONNECTION_RETRIES, String.valueOf(insecureTls));
        client.setInsecureTls(insecureTls);
    }

    private void logResolvedProperty(String key, String value) {
        logger.debug("Artifactory Client Property Resolver: " + key + " = " + value);
    }
}
