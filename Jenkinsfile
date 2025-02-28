pipeline {
    agent any

    environment {
        CI_COMMIT_SHORT_SHA = ''
        CI_PROJECT_NAME = ''
        IMAGE_VERSION = ''
    }

    stages {
        stage('Project Information') {
            steps {
                script {
                    // Hiển thị thông tin debug
                    sh """
                        echo "DEBUG - Danh sách remote:"
                        git remote -v
                        echo "DEBUG - URL của remote origin:"
                        git config --get remote.origin.url
                    """
                    
                    // Lấy tên project chính xác
                    def projectName = sh(
                        script: 'basename -s .git $(git config --get remote.origin.url)',
                        returnStdout: true
                    ).trim()
                    env.CI_PROJECT_NAME = projectName

                    // Lấy commit SHA ngắn
                    def commitHash = sh(
                        script: 'git rev-parse --short=8 HEAD', 
                        returnStdout: true
                    ).trim()
                    env.CI_COMMIT_SHORT_SHA = commitHash
                    
                    // Tạo version image
                    env.IMAGE_VERSION = "${env.CI_PROJECT_NAME}:${env.CI_COMMIT_SHORT_SHA}"
                    
                    // Verify các giá trị
                    echo "VERIFY - Project Name: ${env.CI_PROJECT_NAME}"
                    echo "VERIFY - Commit SHA: ${env.CI_COMMIT_SHORT_SHA}"
                    echo "VERIFY - Image Version: ${env.IMAGE_VERSION}"
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh "docker build -t ${env.IMAGE_VERSION} ."
                }
            }
        }
    }
}
