pipeline {

	agent any

	environment {
		M2_HOME = "/Users/michaelsv/.sdkman/candidates/maven/3.8.1/"
	}

	
	stages {
		
		stage ("Clone") {
			steps {
				
				git branch: "master",
				url: "https://github.com/sverdlov93/artifactory-maven-plugin"
			}
		}

		stage ("Artifactory configuration") {
			steps {
				
				rtServer (
					id: "ci-setup-cmd",
					url: "https://michaelsv.jfrog.io/artifactory/",
					credentialsId: 'rt-credentials'
				)
				rtMavenDeployer (
					id: "MAVEN_DEPLOYER",
					serverId: "ci-setup-cmd",
					releaseRepo: "default-maven-local",
					snapshotRepo: "default-maven-local"	
				)
				rtMavenResolver (
					id: "MAVEN_RESOLVER",
					serverId: "ci-setup-cmd",
					releaseRepo: "default-maven-virtual",
					snapshotRepo: "default-maven-virtual"
				)
			}
		}

		stage ("Exec Maven") {
			steps {
				
				rtMavenRun (
					//tool: 'maven 3.8.1', // Tool name from Jenkins configuration
					pom: 'pom.xml',
					goals: 'clean install -DskipTests',
					resolverId: "MAVEN_RESOLVER",
					deployerId: "MAVEN_DEPLOYER",
					// useWrapper: true, (Set to true if you'd like the build to use the Maven Wrapper.)
					// opts: '-Xms1024m -Xmx4096m', (Optional - Maven options)
					// buildName: 'my-build-name', (If the build name and build number are not set here, the current job name and number will be used:)
					// buildNumber: '17',
					// project: 'my-project-key' (Optional - Only if this build is associated with a project in Artifactory, set the project key as follows.)
				)
			}
		}

		stage ("Config build info") {
			steps {
				
				rtBuildInfo (
					captureEnv: true,
					includeEnvPatterns: ["*"]
				)
			}
		}

		stage ("Publish build info") {
			steps {
				
				rtPublishBuildInfo (
					serverId: "ci-setup-cmd"
				)
			}
		}

	}
}
