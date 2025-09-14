package org.jfrog.buildinfo.deployment;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.graph.DependencyNode;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.builder.ArtifactBuilder;
import org.jfrog.build.extractor.builder.BuildInfoMavenBuilder;
import org.jfrog.build.extractor.builder.DependencyBuilder;
import org.jfrog.build.extractor.builder.ModuleBuilder;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.Dependency;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.packageManager.PackageManagerUtils;
import org.jfrog.buildinfo.resolution.RepositoryListener;
import org.jfrog.buildinfo.types.ModuleArtifacts;
import org.jfrog.buildinfo.utils.DependencyResolutionUtil;
import org.jfrog.buildinfo.utils.ReactorDependencyFilter;
import org.jfrog.buildinfo.utils.Utils;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getModuleIdString;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getTypeString;
import static org.jfrog.build.extractor.packageManager.PackageManagerUtils.filterBuildInfoProperties;
import static org.jfrog.buildinfo.utils.Utils.getArtifactName;
import static org.jfrog.buildinfo.utils.Utils.getDeploymentPath;
import static org.jfrog.buildinfo.utils.Utils.getFileExtension;
import static org.jfrog.buildinfo.utils.Utils.isFile;
import static org.jfrog.buildinfo.utils.Utils.setChecksums;

/**
 * @author yahavi
 */
public class BuildInfoRecorder implements BuildInfoExtractor<ExecutionEvent>, ExecutionListener {

    private final Map<String, DeployDetails> deployableArtifacts = Maps.newConcurrentMap();
    private final Set<Artifact> buildTimeDependencies = Collections.synchronizedSet(new HashSet<>());
    private final ModuleArtifacts currentModuleDependencies = new ModuleArtifacts();
    private final ModuleArtifacts currentModuleArtifacts = new ModuleArtifacts();
    private final ThreadLocal<ModuleBuilder> currentModule = new ThreadLocal<>();
    private final BuildInfoMavenBuilder buildInfoBuilder;
    private final ArtifactoryClientConfiguration conf;
    private final ExecutionListener wrappedListener;
    private final BuildDeployer buildDeployer;
    private final Log logger;
    private Map<String, String[][]> dependencyParentsMap;
    private ProjectDependenciesResolver dependenciesResolver;

    public BuildInfoRecorder(MavenSession session, Log logger, ArtifactoryClientConfiguration conf) {
        this.wrappedListener = ObjectUtils.defaultIfNull(session.getRequest().getExecutionListener(), new AbstractExecutionListener());
        this.buildInfoBuilder = new BuildInfoModelPropertyResolver(logger, session, conf);
        this.buildDeployer = new BuildDeployer(logger);
        this.logger = logger;
        this.conf = conf;
    }

    public BuildInfoRecorder(MavenSession session, Log logger, ArtifactoryClientConfiguration conf, ProjectDependenciesResolver dependenciesResolver) {
        this.wrappedListener = ObjectUtils.defaultIfNull(session.getRequest().getExecutionListener(), new AbstractExecutionListener());
        this.buildInfoBuilder = new BuildInfoModelPropertyResolver(logger, session, conf);
        this.buildDeployer = new BuildDeployer(logger);
        this.logger = logger;
        this.conf = conf;
        this.dependenciesResolver = dependenciesResolver;
    }

    /**
     * Collect artifacts and dependencies from the project.
     *
     * @param event - The Maven execution event
     */
    @Override
    public void projectSucceeded(ExecutionEvent event) {
        MavenProject project = event.getProject();
        ModuleBuilder moduleBuilder = new ModuleBuilder()
                .id(getModuleIdString(project.getGroupId(), project.getArtifactId(), project.getVersion()))
                .properties(project.getProperties());

        // Replace variables
        conf.getAllProperties().replaceAll((key, value) -> Utils.parseInput(value));

        // Fill currentModuleArtifacts
        if (BooleanUtils.toBoolean(project.getProperties().getProperty("maven.deploy.skip"))) {
            logger.info("The artifacts of module '" + project.getArtifactId() +
                    "' will not be deployed because 'maven.deploy.skip' property is true.");
        } else {
            addArtifacts(project);
            addArtifactsToCurrentModule(project, moduleBuilder);
        }

        // Construct map of dependency to parents
        if (dependenciesResolver != null) {
            DependencyNode dependencyNode = getDependencyNode(project, event.getSession());
            dependencyParentsMap = DependencyResolutionUtil.createDependencyParentsMap(dependencyNode);
        }
        // Fill currentModuleDependencies
        addDependencies(project);

        // Build module
        addDependenciesToCurrentModule(moduleBuilder);
        buildInfoBuilder.addModule(moduleBuilder.build());

        // Clean up
        currentModule.remove();
        currentModuleArtifacts.remove();
        currentModuleDependencies.remove();
        buildTimeDependencies.clear();

        wrappedListener.projectSucceeded(event);
    }

    /**
     *  Get current project DependencyNode
     * @param project The Maven project
     * @param session The Maven session
     * @return The DependencyNode Object
     */
    private DependencyNode getDependencyNode(MavenProject project, MavenSession session) {
        Collection<Artifact> projectArtifacts = new HashSet<>();
        projectArtifacts.add(project.getArtifact());
        DefaultDependencyResolutionRequest request =
                new DefaultDependencyResolutionRequest(project, session.getRepositorySession());
        // Filter current project artifact
        request.setResolutionFilter(new ReactorDependencyFilter(projectArtifacts));
        try {
            DependencyResolutionResult result = dependenciesResolver.resolve(request);
            return result.getDependencyGraph();
        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    /**
     * Add dependencies to the current running project.
     *
     * @param event - The Maven execution event
     */
    @Override
    public void mojoSucceeded(ExecutionEvent event) {
        addDependencies(event.getProject());

        wrappedListener.mojoSucceeded(event);
    }

    /**
     * Add dependencies to the current running project.
     *
     * @param event - The Maven execution event
     */
    @Override
    public void mojoFailed(ExecutionEvent event) {
        addDependencies(event.getProject());

        wrappedListener.mojoFailed(event);
    }

    /**
     * Build and publish build info.
     *
     * @param event - The Maven execution event
     */
    @Override
    public void sessionEnded(ExecutionEvent event) {
        BuildInfo build = extract(event);
        if (build != null) {
            buildDeployer.deploy(build, conf, deployableArtifacts);
        }
        deployableArtifacts.clear();

        wrappedListener.sessionEnded(event);
    }

    /**
     * Create a build info from 'buildInfoBuilder'.
     *
     * @param event - The Maven execution event
     * @return - The build info object
     */
    @Override
    public BuildInfo extract(ExecutionEvent event) {
        MavenSession session = event.getSession();
        if (session.getResult().hasExceptions()) {
            return null;
        }

        long startedMillis = session.getRequest().getStartTime().getTime();
        long time = new Date().getTime() - startedMillis;
        BuildInfo buildInfo = buildInfoBuilder.durationMillis(time).startedMillis(startedMillis).build();
        PackageManagerUtils.collectEnvAndFilterProperties(conf, buildInfo);
        return buildInfo;
    }

    public Set<Artifact> getCurrentModuleDependencies() {
        return currentModuleDependencies.get();
    }

    public BuildInfoMavenBuilder getBuildInfoBuilder() {
        return buildInfoBuilder;
    }

    /**
     * Called by {@link RepositoryListener} during resolution of implicit project and build-time dependencies.
     * Enabled if 'recordAllDependencies' is set to true.
     *
     * @param artifact - The resolved artifact
     */
    public void artifactResolved(Artifact artifact) {
        if (artifact != null) {
            buildTimeDependencies.add(artifact);
        }
    }

    /**
     * Add project artifacts to the current module artifacts set.
     *
     * @param project - The Maven project
     */
    private void addArtifacts(MavenProject project) {
        currentModuleArtifacts.add(project.getArtifact());
        currentModuleArtifacts.addAll(project.getAttachedArtifacts());
    }

    /**
     * Add project dependencies to the current module dependencies set.
     * In case an artifact is included in both MavenProject dependencies and currentModuleDependencies,
     * we'd like to keep the one that was taken from the MavenProject, because of the scope it has.
     *
     * @param project - The Maven project
     */
    private void addDependencies(MavenProject project) {
        // Create a set with new project dependencies. These dependencies are 1st priority in the set.
        Set<Artifact> dependencies = Sets.newHashSet();
        for (Artifact artifact : project.getArtifacts()) {
            String classifier = StringUtils.defaultString(artifact.getClassifier());
            String scope = StringUtils.defaultIfBlank(artifact.getScope(), Artifact.SCOPE_COMPILE);
            Artifact art = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                    artifact.getVersion(), scope, artifact.getType(), classifier, artifact.getArtifactHandler());
            art.setFile(artifact.getFile());
            dependencies.add(art);
        }

        // Add current project dependencies. These dependencies are 2nd priority in the set.
        dependencies.addAll(currentModuleDependencies.getOrCreate());

        // if recordAllDependencies=true, add build time dependencies
        if (conf.publisher.isRecordAllDependencies()) {
            synchronized(buildTimeDependencies) {
                dependencies.addAll(buildTimeDependencies);
            }
        }

        currentModuleDependencies.set(dependencies);
    }

    /**
     * Add artifacts from currentModuleArtifacts to the current module.
     *
     * @param project       - The current Maven project
     * @param moduleBuilder - The current Maven module
     */
    private void addArtifactsToCurrentModule(MavenProject project, ModuleBuilder moduleBuilder) {
        Set<Artifact> artifacts = currentModuleArtifacts.getOrCreate();

        IncludeExcludePatterns patterns = new IncludeExcludePatterns(conf.publisher.getIncludePatterns(), conf.publisher.getExcludePatterns());
        boolean excludeArtifactsFromBuild = conf.publisher.isFilterExcludedArtifactsFromBuild();

        boolean pomFileAdded = false;

        for (Artifact moduleArtifact : artifacts) {
            String artifactId = moduleArtifact.getArtifactId();
            String artifactVersion = moduleArtifact.getVersion();
            String artifactClassifier = moduleArtifact.getClassifier();
            String artifactExtension = moduleArtifact.getArtifactHandler().getExtension();
            String type = getTypeString(moduleArtifact.getType(), artifactClassifier, artifactExtension);
            String artifactName = getArtifactName(artifactId, artifactVersion, artifactClassifier, artifactExtension);
            File artifactFile = moduleArtifact.getFile();

            if ("pom".equals(type)) {
                pomFileAdded = true;
                // For pom projects take the file from the project if the artifact file is null.
                if (moduleArtifact.equals(project.getArtifact())) {
                    // project.getFile() returns the project pom file
                    artifactFile = project.getFile();
                }
            }

            if (!isFile(artifactFile)) {
                continue;
            }
            org.jfrog.build.extractor.ci.Artifact artifact = new ArtifactBuilder(artifactName).type(type).build();
            String deploymentPath = getDeploymentPath(moduleArtifact.getGroupId(), artifactId, artifactVersion, artifactClassifier, artifactExtension);
            boolean pathConflicts = PatternMatcher.pathConflicts(deploymentPath, patterns);
            addArtifactToBuildInfo(moduleBuilder, artifact, pathConflicts, excludeArtifactsFromBuild);
            addDeployableArtifact(moduleBuilder, artifact, artifactFile, pathConflicts, moduleArtifact.getGroupId(),
                    artifactId, artifactVersion, artifactClassifier, artifactExtension);
        }
        /*
         * In case of non packaging Pom project module, we need to create the pom file from the project's Artifact
         */
        if (!pomFileAdded) {
            addPomArtifact(project, moduleBuilder, patterns, excludeArtifactsFromBuild);
        }
    }

    /**
     * Add artifacts from currentModuleDependencies to the current module.
     *
     * @param moduleBuilder - The current Maven module
     */
    private void addDependenciesToCurrentModule(ModuleBuilder moduleBuilder) {
        for (Artifact moduleDependency : currentModuleDependencies.getOrCreate()) {
            File depFile = moduleDependency.getFile();
            String gav = getModuleIdString(moduleDependency.getGroupId(), moduleDependency.getArtifactId(), moduleDependency.getVersion());
            DependencyBuilder dependencyBuilder = new DependencyBuilder()
                    .id(gav).type(getTypeString(moduleDependency.getType(), moduleDependency.getClassifier(), getFileExtension(depFile)));
            // add RequestBy to Dependency
            if (dependenciesResolver != null) {
                dependencyBuilder.requestedBy(dependencyParentsMap.get(gav));
            }
            String scopes = moduleDependency.getScope();
            if (StringUtils.isNotBlank(scopes)) {
                dependencyBuilder.scopes(Sets.newHashSet(scopes));
            }
            Dependency dependency = dependencyBuilder.build();
            setChecksums(depFile, dependency, logger);
            moduleBuilder.addDependency(dependency);
        }
    }

    /**
     * Add pom artifact from currentModuleDependencies to the current module.
     *
     * @param moduleBuilder - The current Maven module
     */
    private void addPomArtifact(MavenProject project, ModuleBuilder moduleBuilder, IncludeExcludePatterns patterns, boolean excludeArtifactsFromBuild) {
        File pomFile = project.getFile();
        Artifact projectArtifact = project.getArtifact();
        String artifactName = getArtifactName(projectArtifact.getArtifactId(), projectArtifact.getBaseVersion(), projectArtifact.getClassifier(), "pom");
        org.jfrog.build.extractor.ci.Artifact pomArtifact = new ArtifactBuilder(artifactName).type("pom").build();

        String deploymentPath = getDeploymentPath(projectArtifact.getGroupId(), projectArtifact.getArtifactId(), projectArtifact.getVersion(), projectArtifact.getClassifier(), "pom");
        boolean pathConflicts = PatternMatcher.pathConflicts(deploymentPath, patterns);
        addArtifactToBuildInfo(moduleBuilder, pomArtifact, pathConflicts, excludeArtifactsFromBuild);
        addDeployableArtifact(moduleBuilder, pomArtifact, pomFile, pathConflicts, projectArtifact.getGroupId(),
                projectArtifact.getArtifactId(), projectArtifact.getVersion(), projectArtifact.getClassifier(), "pom");
    }

    /**
     * Add an artifact to the build.
     * If excludeArtifactsFromBuild and the PatternMatcher found conflicts, add the excluded artifact to the excluded artifacts list in the build info.
     * Otherwise, add the artifact to the regular artifacts list.
     *
     * @param module                             - The current Maven module
     * @param artifact                           - The artifact to add
     * @param pathConflicts                      - If true, consider adding the artifact to the excluded artifacts list
     * @param isFilterExcludedArtifactsFromBuild - If true and the artifacts should be excluded, add the artifact to the excluded artifacts list
     */
    private void addArtifactToBuildInfo(ModuleBuilder module, org.jfrog.build.extractor.ci.Artifact artifact, boolean pathConflicts, boolean isFilterExcludedArtifactsFromBuild) {
        if (isFilterExcludedArtifactsFromBuild && pathConflicts) {
            module.addExcludedArtifact(artifact);
            return;
        }
        module.addArtifact(artifact);
    }

    /**
     * Add an artifact to the deployable artifacts map.
     *
     * @param module        - The current Maven module
     * @param artifact      - The artifact to add
     * @param artifactFile  - The file of the artifact
     * @param pathConflicts - If the path conflicts, don't deploy the artifact
     * @param groupId       - The artifact's group ID
     * @param artifactId    - The artifact's ID
     * @param version       - The artifact's version
     * @param classifier    - The artifact's classifier
     * @param fileExtension - The file extension, for example - 'jar' or 'pom'
     */
    private void addDeployableArtifact(ModuleBuilder module, org.jfrog.build.extractor.ci.Artifact artifact, File artifactFile, boolean pathConflicts,
                                       String groupId, String artifactId, String version, String classifier, String fileExtension) {
        if (pathConflicts) {
            logger.info("'" + artifact.getName() + "' will not be deployed due to the defined include-exclude patterns.");
            return;
        }
        String deploymentPath = getDeploymentPath(groupId, artifactId, version, classifier, fileExtension);
        // deploy to snapshots or releases repository based on the deploy version
        String targetRepository = getTargetRepository(deploymentPath);

        DeployDetails deployable = new DeployDetails.Builder()
                .artifactPath(deploymentPath)
                .file(artifactFile)
                .targetRepository(targetRepository)
                .addProperties(conf.publisher.getMatrixParams())
                .packageType(DeployDetails.PackageType.MAVEN).build();
        String myArtifactId = BuildInfoExtractorUtils.getArtifactId(module.build().getId(), artifact.getName());

        deployableArtifacts.put(myArtifactId, deployable);
    }

    /**
     * Return the target deployment repository.
     * Either the releases repository (default) or snapshots if defined and the deployed file is a snapshot.
     *
     * @param deployPath the full path string to extract the repo from
     * @return Return the target deployment repository.
     */
    public String getTargetRepository(String deployPath) {
        String snapshotsRepository = conf.publisher.getSnapshotRepoKey();
        if (snapshotsRepository != null && deployPath.contains("-SNAPSHOT")) {
            return snapshotsRepository;
        }
        return conf.publisher.getRepoKey();
    }

    // Forward any all events to the wrapped listener if set
    @Override
    public void projectDiscoveryStarted(ExecutionEvent event) {
        wrappedListener.projectDiscoveryStarted(event);
    }

    @Override
    public void sessionStarted(ExecutionEvent event) {
        wrappedListener.sessionStarted(event);
    }

    @Override
    public void projectSkipped(ExecutionEvent event) {
        wrappedListener.projectSkipped(event);
    }

    @Override
    public void projectStarted(ExecutionEvent event) {
        wrappedListener.projectStarted(event);
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        wrappedListener.projectFailed(event);
    }

    @Override
    public void forkStarted(ExecutionEvent event) {
        wrappedListener.forkStarted(event);
    }

    @Override
    public void forkSucceeded(ExecutionEvent event) {
        wrappedListener.forkSucceeded(event);
    }

    @Override
    public void forkFailed(ExecutionEvent event) {
        wrappedListener.forkFailed(event);
    }

    @Override
    public void mojoSkipped(ExecutionEvent event) {
        wrappedListener.mojoSkipped(event);
    }

    @Override
    public void mojoStarted(ExecutionEvent event) {
        wrappedListener.mojoStarted(event);
    }

    @Override
    public void forkedProjectStarted(ExecutionEvent event) {
        wrappedListener.forkedProjectStarted(event);
    }

    @Override
    public void forkedProjectSucceeded(ExecutionEvent event) {
        wrappedListener.forkedProjectSucceeded(event);
    }

    @Override
    public void forkedProjectFailed(ExecutionEvent event) {
        wrappedListener.forkedProjectFailed(event);
    }
}
