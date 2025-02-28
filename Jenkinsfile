pipeline {
    agent any

    environment {
        CI_COMMIT_SHORT_SHA = ""
        CI_COMMIT_TAG = ""
        CI_PROJECT_NAME = ""
        IMAGE_VERSION = ""
    }

    stages {
        stage('Project Information') {
            steps {
                script {
                    sh """
                        echo "Running as: \$(whoami)"
                        echo "Current Directory: \$(pwd)"
                        echo "Listing files:"
                        ls -la
                        echo "Environment Variables:"
                        env | sort
                    """
                    CI_PROJECT_NAME = sh(script: "git remote show origin -n | grep Fetch | awk -F'[:/]' '{print \$3}' | cut -d'.' -f1", returnStdout:true).trim()
                    echo "CI_PROJECT_NAME: ${CI_PROJECT_NAME}"
                    def CI_COMMIT_HASH = sh(script: "git rev-parse HEAD ", returnStdout: true).trim()
                    echo "CI_COMMIT_HASH: ${CI_COMMIT_HASH}"
                    CI_COMMIT_SHORT_SHA = CI_COMMIT_HASH.take(8)
                    echo "CI_COMMIT_SHORT_SHA: ${CI_COMMIT_SHORT_SHA}"
                    CI_COMMIT_TAG = sh(script:"git log -1 --pretty=%s", returnStdout: true).trim()
                    echo "CI_COMMIT_TAG: ${CI_COMMIT_TAG}"
                    IMAGE_VERSION = "${CI_PROJECT_NAME}:${CI_COMMIT_SHORT_SHA}_${CI_COMMIT_TAG}"
                    echo "IMAGE_VERSION: ${IMAGE_VERSION}"
                }
            }
        }

        stage('build') {
            steps {
                sh (script: """ docker build -t $IMAGE_VERSION . """, label: "")
            
            }
        }
    }

    
    post {
        always {
            echo "Pipeline execution completed."
        }
    }
}
