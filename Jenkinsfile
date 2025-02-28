pipeline {
    agent any

    environment {
        CI_PROJECT_NAME = ''
        CI_COMMIT_SHORT_SHA = ''
        IMAGE_VERSION = ''
    }

    stages {
        stage('Project Information') {
            steps {
                script {
                    // Lấy tên project chuẩn xác cho mọi loại URL
                    env.CI_PROJECT_NAME = sh(script: '''
                        git config --get remote.origin.url | 
                        sed -e "s/:/\\//g" -e "s/.*\\///" -e "s/\\.git//"
                    ''', returnStdout: true).trim()

                    // Lấy commit SHA ngắn
                    env.CI_COMMIT_SHORT_SHA = sh(
                        script: 'git rev-parse --short=8 HEAD',
                        returnStdout: true
                    ).trim()

                    // Tạo version image
                    env.IMAGE_VERSION = "${env.CI_PROJECT_NAME}:${env.CI_COMMIT_SHORT_SHA}"

                    // Debug output
                    echo "Project: ${env.CI_PROJECT_NAME}"
                    echo "Commit SHA: ${env.CI_COMMIT_SHORT_SHA}"
                    echo "Image Version: ${env.IMAGE_VERSION}"
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
