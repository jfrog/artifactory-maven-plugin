# Maven Artifactory plugin

The Maven Artifactory integrates in your build to allow you to do the following:
1. Resolve artifacts from Artifactory.
2. Capture the full build information and publish it to Artifactory.
3. Deploy all build Artifacts to Artifactory.

## Using Maven Artifactory plugin
To learn how to use the Maven Artifactory plugin, please visit the [Maven Artifactory plugin user guide](https://www.jfrog.com/confluence/display/JFROG/Maven+Artifactory+Plugin).

## Building the sources
To build the plugin sources, please follow these steps:
1. Clone the code from Git.
2. Install the plugin by running the following Maven command:
```
mvn clean install
```

## Testing the plugin
* If you'd like to run the plugin's tests, install the plugin and run the following command:
```
mvn verify -DskipITs=false
```
The above command run both unit and integration tests.

* In order to remote debug the integration tests, add `debugITs=true` property to the test command and add a break point in your IDE:
```
mvn verify -DskipITs=false -DdebugITs=true
```
After running the above command, you should start a remote debugging session on port 5005 and wait until the code reaches the break point.

## Pull requests

We welcome pull requests from the community.

### Guidelines
If the existing tests do not already cover your changes, please add tests.

## Release Notes
The release notes are available on [Bintray](https://bintray.com/jfrog/jfrog-jars/artifactory-maven-plugin#release).