pipeline {
	agent any
	environment {
		
	  // The M2_HOME environment variable should be set to the local maven installation path.
	  M2_HOME = "/Users/michaelsv/.sdkman/candidates/maven/3.8.1/"

	  JFROG_CLI_BUILD_NAME = "${JOB_NAME}"
	  JFROG_CLI_BUILD_NUMBER = "${BUILD_NUMBER}"
	  RT_USERNAME="sverdlov93@gmail.com"
	  // Sets the CI server build URL in the build-info.
	  JFROG_CLI_BUILD_URL = "http://127.0.0.1:8080/myjenkins/job/MyMaven/${BUILD_NUMBER}/console"
	  
	}
	stages {
		stage ('Clone') {
			steps {
				// If cloning the code requires credentials, follow these steps:
				// 1. Uncomment the ending of the below 'git' step.
				// 2. Create the 'git_cred_id' credentials as described here - https://www.jenkins.io/doc/book/using/using-credentials/
				git branch: "master", url: "https://github.com/sverdlov93/artifactory-maven-plugin" //, credentialsId: 'git_cred_id'
			}
		}
   
		stage ('Config') {
			steps {
				// Download JFrog CLI.
				sh 'curl -fL https://getcli.jfrog.io | sh && chmod +x jfrog'

				// Configure JFrog CLI 
				withCredentials([string(credentialsId: 'rt-password', variable: 'RT_PASSWORD')]) {
					sh '''./jfrog c add ci-setup-cmd --url https://michaelsv.jfrog.io/ --user ${RT_USERNAME} --password ${RT_PASSWORD}
					./jfrog rt mvn-config --server-id-resolve ci-setup-cmd --repo-resolve-releases default-maven-virtual --repo-resolve-snapshots default-maven-virtual
					'''
				}
			}
		}
   
		stage ('Build') {
			steps {
				dir('artifactory-maven-plugin') {
					sh '''./jfrog rt mvn clean install'''
				}
			}
		}
	}
	   
	post {
		success {
			script {
				env.JFROG_BUILD_STATUS="PASS"
			}
		}
		 
		failure {
			script {
				env.JFROG_BUILD_STATUS="FAIL"
			}
		}
		 
		cleanup {
			// Collect and store environment variables in the build-info
			sh './jfrog rt bce'
			// Collect and store VCS details in the build-info
			sh './jfrog rt bag'
			// Publish the build-info to Artifactory
			sh './jfrog rt bp'
			sh './jfrog c remove ci-setup-cmd --quiet'
		}
	}
  }
