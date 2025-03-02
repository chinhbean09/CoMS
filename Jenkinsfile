pipeline {
    agent any

    environment {
        USER_PROJECT = "coms-chinhbean"
        CI_COMMIT_SHORT_SHA = ""
        CI_COMMIT_TAG = ""
        CI_PROJECT_NAME = ""
        IMAGE_VERSION = ""
    }

    stages {
        stage('get project information') {
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
                    
                    // Lệnh trích xuất project name (with escaped $)
                    CI_PROJECT_NAME = sh(script: "git remote show origin -n | grep Fetch | awk '{print \$3}' | cut -d':' -f2 | cut -d'/' -f2 | cut -d'.' -f1", returnStdout:true).trim().toLowerCase()
                    def CI_COMMIT_HASH = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                    CI_COMMIT_SHORT_SHA = CI_COMMIT_HASH.take(8)
                    IMAGE_VERSION = "${CI_PROJECT_NAME}:${CI_COMMIT_SHORT_SHA}"
                }
            }
        }
        
        stage('build') {
            steps {
                sh(script: "docker build -t ${IMAGE_VERSION} .", label: "")
            }
        }

        tage('deploy') {
            steps {

                sh(script: "docker compose down", label: "Stop old containers")
                
                sh(script: "BACKEND_IMAGE=${IMAGE_VERSION} docker compose up -d", label: "Deploy new containers" )

            }
        }
    }
    
    post {
        always {
            echo "Pipeline execution completed."
        }
    }
}
