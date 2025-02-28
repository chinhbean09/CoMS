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
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline execution completed."
        }
    }
}
