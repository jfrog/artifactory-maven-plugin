<div align="center">

# Maven Artifactory Plugin

[![Build status](https://github.com/jfrog/artifactory-maven-plugin/workflows/Test/badge.svg)](https://github.com/jfrog/artifactory-maven-plugin/actions?query=workflow%3ATest)

</div>

# Table of Contents

- [Overview](#overview)
- [Using the Maven Artifactory plugin](#using-maven-artifactory-plugin)
    - [Basic Configuration](#basic-configuration)
    - [Full Configuration](#full-configuration)
    - [Configuration Details](#configuration-details)
    - [Reading Environment Variables and System Properties](#reading-environment-variables-and-system-properties)
    - [Example](#example)
- [Contribution](#-contributions)

## Overview

The Maven Artifactory integrates in your build to allow you to do the following:

1. Resolve artifacts from Artifactory.
2. Capture the full build information and publish it to Artifactory.
3. Deploy all build Artifacts to Artifactory.

## Using Maven Artifactory plugin

The Maven Artifactory Plugin coordinates are org.jfrog.buildinfo:artifactory-maven-plugin:x.x.x.
It can be viewed
on [releases.jfrog.io](https://releases.jfrog.io/artifactory/oss-release-local/org/jfrog/buildinfo/artifactory-maven-plugin).

### Basic Configuration

A typical build plugin configuration would be as follows:

```xml

<build>
  <plugins>
    <plugin>
      <groupId>org.jfrog.buildinfo</groupId>
      <artifactId>artifactory-maven-plugin</artifactId>
      <version>3.6.1</version>
      <executions>
        <execution>
          <id>build-info</id>
          <goals>
            <goal>publish</goal>
          </goals>
          <configuration>
            <deployProperties>
              <gradle>awesome</gradle>
              <review.team>qa</review.team>
            </deployProperties>
            <publisher>
              <contextUrl>https://acme.jfrog.io</contextUrl>
              <username>deployer</username>
              <password>dontellanyone</password>
              <repoKey>libs-release-local</repoKey>
              <snapshotRepoKey>libs-snapshot-local</snapshotRepoKey>
            </publisher>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

The plugin's invocation phase is validate by default and we recommend you don't change it so the plugin is called as
early as possible in the lifecycle of your
Maven build.

### Full Configuration

The example above configures the Artifactory publisher, to deploy build artifacts either to the releases or the
snapshots repository of Artifactory when `mvn deploy` is executed.

However, the Maven Artifactory Plugin provides many other configurations:

```xml

<configuration>
  <artifactory>
    <includeEnvVars>true</includeEnvVars>
    <envVarsExcludePatterns>*password*,*secret*,*key*,*token*,*passphrase*</envVarsExcludePatterns>
    <envVarsIncludePatterns>*os*</envVarsIncludePatterns>
    <timeoutSec>60</timeoutSec>
  </artifactory>
  <buildInfo>
    <agentName>..</agentName>
    <agentVersion>..</agentVersion>
    <buildName>plugin-demo</buildName>
    <buildNumber>${buildnumber}</buildNumber>
    <buildUrl>https://build-url.org</buildUrl>
    <!-- If you'd like to associate the published build-info with a JFrog Project, add the project key here -->
    <project>..</project>

    <buildNumbersNotToDelete></buildNumbersNotToDelete>
    <buildRetentionMaxDays>N</buildRetentionMaxDays>
    <buildRetentionCount>N</buildRetentionCount>
    <buildUrl>..</buildUrl>
    <principal>..</principal>
  </buildInfo>
  <deployProperties>
    <maven>awesome</maven>
  </deployProperties>
  <publisher>
    <contextUrl>http://localhost:8081/artifactory</contextUrl>
    <username>${username}</username>
    <password>${password}</password>
    <excludePatterns>*-tests.jar</excludePatterns>
    <repoKey>libs-release-local</repoKey>
    <snapshotRepoKey>libs-snapshot-local</snapshotRepoKey>

    <publishArtifacts>true/false</publishArtifacts>
    <publishBuildInfo>true/false</publishBuildInfo>
    <excludePatterns>..</excludePatterns>
    <includePatterns>..</includePatterns>
    <filterExcludedArtifactsFromBuild>true/false</filterExcludedArtifactsFromBuild>
    <!-- If true build information published to Artifactory will include implicit project as well as build-time dependencies -->
    <recordAllDependencies>true/false</recordAllDependencies>
    <!-- Minimum file size in KB for which the plugin performs checksum deploy optimization. Default: 10. Set to 0 to disable uploading files with checksum that already exists in Artifactory. -->
    <minChecksumDeploySizeKb>10</minChecksumDeploySizeKb>
  </publisher>
  <proxy>
    <host>proxy.jfrog.io</host>
    <port>8888</port>
    <username>proxyUser</username>
    <password>proxyPassword</password>
  </proxy>
</configuration>
```

### Configuration Details

|    Configuration     | Description                                                                                                                                                                                                                                              |
|:--------------------:|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `<deployProperties>` | Specifies properties you can attach to published artifacts. For example: <prope-key>prop-value</prop-key>.                                                                                                                                               |
|   `<artifactory>`    | Specifies whether environment variables are published as part of BuildInfo metadata and which include or exclude patterns are applied when variables are collected                                                                                       |   
|    `<publisher>`     | Defines an Artifactory repository where build artifacts should be published using a combination of a `<contextUrl>` and `<repoKey>/<snapshotRepoKey>`. Build artifacts are deployed if the deploy goal is executed and only after all modules are built. |   
|    `<buildInfo>`     | Updates BuildInfo metadata published together with build artifacts. You can configure whether or not BuildInfo metadata is published using the <publisher> configuration.                                                                                |   
|      `<proxy>`       | Specifies HTTP/S proxy.                                                                                                                                                                                                                                  |   

### Reading Environment Variables and System Properties

Every build server provides its own set of environment variables. You can utilize these variables when configuring the
plugin as shown in the following example:

```xml

<configuration>
  <publisher>
    <contextUrl>{{ARTIFACTORY_CONTEXT_URL|"https://acme.jfrog.io"}}</contextUrl>
  </publisher>
  <buildInfo>
    <buildNumber>{{DRONE_BUILD_NUMBER|TRAVIS_BUILD_NUMBER|CI_BUILD_NUMBER|BUILD_NUMBER|"333"}}</buildNumber>
    <buildUrl>{{DRONE_BUILD_URL|CI_BUILD_URL|BUILD_URL}}</buildUrl>
  </buildInfo>
</configuration>
```

Any plugin configuration value can contain several {{ .. }} expressions. Each expression can contain a single or
multiple environment variables or system properties to be used.
The expression syntax allows you to provide enough variables to accommodate any build server requirements according to
the following rules:

* Each expression can contain several variables, separated by a ' | ' character to be used with a configuration value.
* The last value in a list is the default that will be used if none of the previous variables is available as an
  environment variable or a system property.

For example, for the expression {{V1|V2|"defaultValue"}} the plugin will attempt to locate environment variable V1 ,
then system property V1, then environment variable or system property V2 , and if none of these is available,
"defaultValue" will be used.

If the last value is not a string (as denoted by the quotation marks) and the variable cannot be resolved, null will be
used (For example, for expression {{V1|V2}} where neither V1 nor V2 can be resolved).

### Example

The following project provides a working example of using the plugin:
[Maven Artifactory Plugin Example](https://github.com/JFrog/project-examples/tree/master/artifactory-maven-plugin-example).

## 💻 Contributions

We welcome pull requests from the community. To help us improve this project, please read
our [Contribution](./CONTRIBUTING.md#-guidelines) guide.
