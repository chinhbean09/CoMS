pipeline {
    agent any

    environment {
        CI_COMMIT_SHORT_SHA = ""
        CI_PROJECT_NAME = ""
        IMAGE_VERSION = ""
    }

    stages {
        stage('Project Information') {
            steps {
                script {
                    // Hiển thị thông tin hệ thống
                    sh """
                        echo "Running as: \$(whoami)"
                        echo "Current Directory: \$(pwd)"
                        ls -la
                    """
                    
                    // Lấy tên project từ git remote
                    env.CI_PROJECT_NAME = sh(script: """
                        git remote -v | grep -m1 'origin.*fetch' | awk '{print \$2}' | 
                        sed 's|.*:||; s|/| |g' | awk '{print \$2}' | 
                        sed 's/\\.git//'
                    """, returnStdout: true).trim()

                    // Lấy commit hash ngắn
                    def commitHash = sh(script: "git rev-parse --short=8 HEAD", returnStdout: true).trim()
                    env.CI_COMMIT_SHORT_SHA = commitHash

                    // Tạo version cho image
                    env.IMAGE_VERSION = "${env.CI_PROJECT_NAME}:${env.CI_COMMIT_SHORT_SHA}"
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    echo "Building image version: ${env.IMAGE_VERSION}"
                    sh "docker build -t ${env.IMAGE_VERSION} ."
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline hoàn tất - Kiểm tra trạng thái ở trên"
        }
    }
}
