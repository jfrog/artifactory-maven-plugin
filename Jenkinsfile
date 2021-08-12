pipeline {

    agent any

    environment {
    		MAVEN_HOME = "/Users/michaelsv/.sdkman/candidates/maven/3.8.1/"
    }

    stages {

        stage ('Clone') {

            steps {
               git branch: "master", url: "https://github.com/sverdlov93/artifactory-maven-plugin"
            }

        }



        stage ('Artifactory configuration') {

            steps {

                rtServer (
                    id: "ARTIFACTORY_SERVER",
                    url: 'https://michaelsv.jfrog.io/artifactory',
		    credentialsId: 'rt-credentials'
                )



                rtMavenDeployer (
                    id: "MAVEN_DEPLOYER",
                    serverId: "ARTIFACTORY_SERVER",
                    .3: 'default-maven-local',
                    snapshotRepo: 'default-maven-local'
                )



                rtMavenResolver (
                    id: "MAVEN_RESOLVER",
                    serverId: "ARTIFACTORY_SERVER",
                    releaseRepo: 'default-maven-virtual',
                    snapshotRepo: 'default-maven-virtual'
                )

            }

        }



        stage ('Exec Maven') {

            steps {

                rtMavenRun (
                    tool: 'maven 3.8.1', // Tool name from Jenkins configuration
                    pom: 'pom.xml',
                    goals: 'clean install DskipITs=true',
                    deployerId: "MAVEN_DEPLOYER",
                    resolverId: "MAVEN_RESOLVER",
                )

            }

        }



        stage ('Publish build info') {

            steps {

                rtPublishBuildInfo (
                    serverId: "ARTIFACTORY_SERVER"
                )

            }

        }

    }

}
