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
                    // In ra một số thông tin cơ bản
                    sh """
                        echo "Running as: \$(whoami)"
                        echo "Current Directory: \$(pwd)"
                        echo "Listing files:"
                        ls -la
                        echo "Environment Variables:"
                        env | sort
                    """
                    
                    // Lệnh trích xuất project name
                    CI_PROJECT_NAME = sh(script: "git remote show origin -n | grep Fetch | awk '{print \\$3}' | cut -d':' -f2 | sed 's/\\.git\\$//'", returnStdout:true).trim()
                    
                    def CI_COMMIT_HASH = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                    CI_COMMIT_SHORT_SHA = CI_COMMIT_HASH.take(8)
                    CI_COMMIT_TAG = sh(script:"git log -1 --pretty=%s", returnStdout: true).trim()
                    IMAGE_VERSION = "${CI_PROJECT_NAME}:${CI_COMMIT_SHORT_SHA}_${CI_COMMIT_TAG}"
                }
            }
        }
        
        stage('build') {
            steps {
                sh(script: "docker build -t ${IMAGE_VERSION} .", label: "")
            }
        }
    }
    
    post {
        always {
            echo "Pipeline execution completed."
        }
    }
}
