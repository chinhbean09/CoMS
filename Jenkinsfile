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
                    // Sửa lại cách lấy tên project
                    env.CI_PROJECT_NAME = sh(
                        script: '''git config --get remote.origin.url | 
                                  sed -e 's|.*:||' -e 's|/| |g' | 
                                  awk '{print $2}' | 
                                  sed 's/\\.git//''',
                        returnStdout: true
                    ).trim()

                    // Lấy commit SHA
                    env.CI_COMMIT_SHORT_SHA = sh(
                        script: 'git rev-parse --short=8 HEAD', 
                        returnStdout: true
                    ).trim()
                    
                    // Tạo image version
                    env.IMAGE_VERSION = "${env.CI_PROJECT_NAME}:${env.CI_COMMIT_SHORT_SHA}"
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
