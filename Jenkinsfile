pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "education-backend"
        CONTAINER_NAME = "backend-prod"
        HOST_PORT = "8090"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    sh "docker build -t ${DOCKER_IMAGE}:latest ."
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    sh "docker stop ${CONTAINER_NAME} || true"
                    sh "docker rm ${CONTAINER_NAME} || true"

                    sh "docker run -d --name ${CONTAINER_NAME} -p ${HOST_PORT}:8090 ${DOCKER_IMAGE}:latest"
                }
            }
        }

        stage('Cleanup') {
            steps {
                cleanWs()
                sh "docker image prune -f"
            }
        }
    }

    post {
        success {
            echo "Backend Deployed to http://3.34.146.24/:${HOST_PORT}"
        }
    }
}