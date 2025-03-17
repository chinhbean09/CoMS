pipeline {
    agent any

    environment {
        CI_COMMIT_SHORT_SHA = ""
        CI_PROJECT_NAME = ""
        IMAGE_VERSION = ""
    }

    parameters {
        booleanParam(name: 'CLEAN_VOLUMES', defaultValue: false, description: 'Xóa volume khi down?')
    }

    stages {
        stage('get project information') {
            steps {
                script {
                    CI_PROJECT_NAME = sh(script: "git remote show origin -n | grep Fetch | awk '{print \$3}' | cut -d':' -f2 | cut -d'/' -f2 | cut -d'.' -f1", returnStdout:true).trim().toLowerCase()
                    def CI_COMMIT_HASH = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                    CI_COMMIT_SHORT_SHA = CI_COMMIT_HASH.take(8)
                }
            }
        }

        stage('build') {
            steps {
                script {
                    withCredentials([string(credentialsId: 'docker-hub-repo', variable: 'DOCKER_HUB_REPO')]) {
                        IMAGE_VERSION = "${DOCKER_HUB_REPO}:${CI_COMMIT_SHORT_SHA}"
                        sh "docker build -t ${IMAGE_VERSION} ."
                    }
                }
            }
        }

        stage('push to Docker Hub') {
            steps {
                script {
                    withCredentials([
                        string(credentialsId: 'docker-hub-repo', variable: 'DOCKER_HUB_REPO'),
                        usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')
                    ]) {
                        sh "echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin"
                        sh "docker push ${DOCKER_HUB_REPO}:${CI_COMMIT_SHORT_SHA}"
                        sh "docker logout"
                    }
                }
            }
        }

        stage('deploy') {
            steps {
                script {
                    withCredentials([string(credentialsId: 'docker-hub-repo', variable: 'DOCKER_HUB_REPO')]) {
                        IMAGE_VERSION = "${DOCKER_HUB_REPO}:${CI_COMMIT_SHORT_SHA}"
                        def downCommand = "docker compose down"
                            if (params.CLEAN_VOLUMES) {
                                downCommand += " -v"
                            }
                        sh "export BACKEND_IMAGE=${IMAGE_VERSION} && ${downCommand}"
                        sh "export BACKEND_IMAGE=${IMAGE_VERSION} && docker compose up -d"
                    }
                }
            }
        }

        stage('clean old images') {
            steps {
                script {
                    withCredentials([string(credentialsId: 'docker-hub-repo', variable: 'DOCKER_HUB_REPO')]) {
                        def images = sh(script: "docker images --format '{{.Repository}}:{{.Tag}}' | grep '^${DOCKER_HUB_REPO}:'", returnStdout: true).trim().split('\n')
                        def oldImages = images.findAll { it != "${DOCKER_HUB_REPO}:${CI_COMMIT_SHORT_SHA}" }
                        oldImages.each { image ->
                            sh "docker rmi ${image} || true"
                        }
                    }
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
