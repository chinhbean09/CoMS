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

        stage('Debug Git Info') {
            steps {
                script {
                    // In ra toàn bộ output của lệnh "git remote show origin -n"
                    def remoteOutput = sh(script: "git remote show origin -n", returnStdout: true).trim()
                    echo "Output of 'git remote show origin -n':\n${remoteOutput}"
                    
                    // In ra kết quả của lệnh grep 'Fetch'
                    def fetchOutput = sh(script: "git remote show origin -n | grep Fetch", returnStdout: true).trim()
                    echo "Output of 'grep Fetch':\n${fetchOutput}"
                    
                    // Trích xuất tên project từ output
                    def projectName = sh(script: "git remote show origin -n | grep Fetch | cut -d'/' -f5 | cut -d'.' -f1", returnStdout: true).trim()
                    echo "Extracted project name: ${projectName}"
                    
                    // Lấy commit hash
                    def commitHash = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                    echo "Commit Hash: ${commitHash}"
                    
                    // Lấy 8 ký tự đầu tiên của commit hash
                    def commitShortSha = commitHash.take(8)
                    echo "Commit Short SHA: ${commitShortSha}"
                    
                    // Lấy tag của commit (nếu có)
                    def commitTag = sh(script: "git describe --tags --exact-match ${commitHash}", returnStdout: true).trim()
                    echo "Commit Tag: ${commitTag}"
                    
                    // Xây dựng IMAGE_VERSION
                    def imageVersion = "${projectName}:${commitShortSha}_${commitTag}"
                    echo "IMAGE_VERSION: ${imageVersion}"
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
