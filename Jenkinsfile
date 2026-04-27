pipeline {
    agent any
    stages {
        stage('Build Docker Image') {
            steps {
                sh 'docker build -t education-backend:latest .'
            }
        }

        stage('Deploy Backend') {
            steps {
                withCredentials([
                    // AWS
                    string(credentialsId: 'AWS_ACCESS_KEY', variable: 'AWS_ACCESS_KEY'),
                    string(credentialsId: 'AWS_SECRET_KEY', variable: 'AWS_SECRET_KEY'),
                    string(credentialsId: 'AWS_REGION', variable: 'AWS_REGION'),
                    string(credentialsId: 'AWS_S3_BUCKET', variable: 'AWS_S3_BUCKET'),

                    // OAuth
                    string(credentialsId: 'GITHUB_CLIENT_ID', variable: 'GITHUB_CLIENT_ID'),
                    string(credentialsId: 'GITHUB_CLIENT_SECRET', variable: 'GITHUB_CLIENT_SECRET'),
                    string(credentialsId: 'GOOGLE_CLIENT_ID', variable: 'GOOGLE_CLIENT_ID'),
                    string(credentialsId: 'GOOGLE_CLIENT_SECRET', variable: 'GOOGLE_CLIENT_SECRET'),
                    string(credentialsId: 'KAKAO_CLIENT_ID', variable: 'KAKAO_CLIENT_ID'),
                    string(credentialsId: 'KAKAO_CLIENT_SECRET', variable: 'KAKAO_CLIENT_SECRET'),

                    // Security & Mail
                    string(credentialsId: 'JWT_SECRET', variable: 'JWT_SECRET'),
                    string(credentialsId: 'MAIL_USERNAME', variable: 'MAIL_USERNAME'),
                    string(credentialsId: 'MAIL_PASSWORD', variable: 'MAIL_PASSWORD'),
                    string(credentialsId: 'REDIS_PASSWORD', variable: 'REDIS_PASSWORD'),
                ]) {
                    sh '''
                        # Stop and remove the old container
                        docker rm -f backend-prod || true

                        # Boot the container with ports AND all environment variables
                        docker run -d \
                          -p 8090:8090 \
                          -e AWS_ACCESS_KEY="${AWS_ACCESS_KEY}" \
                          -e AWS_SECRET_KEY="${AWS_SECRET_KEY}" \
                          -e AWS_REGION="${AWS_REGION}" \
                          -e AWS_S3_BUCKET="${AWS_S3_BUCKET}" \
                          -e GITHUB_CLIENT_ID="${GITHUB_CLIENT_ID}" \
                          -e GITHUB_CLIENT_SECRET="${GITHUB_CLIENT_SECRET}" \
                          -e GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID}" \
                          -e GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET}" \
                          -e KAKAO_CLIENT_ID="${KAKAO_CLIENT_ID}" \
                          -e KAKAO_CLIENT_SECRET="${KAKAO_CLIENT_SECRET}" \
                          -e JWT_SECRET="${JWT_SECRET}" \
                          -e MAIL_USERNAME="${MAIL_USERNAME}" \
                          -e MAIL_PASSWORD="${MAIL_PASSWORD}" \
                          -e SPRING_DATA_REDIS_HOST="${AWS_PRIVATE_IP}" \
                          -e SPRING_DATA_REDIS_PASSWORD="${REDIS_PASSWORD}" \
                          -e ELASTICSEARCH_HOST="${AWS_PRIVATE_IP}" \
                          --name backend-prod \
                          education-backend:latest
                    '''
                }
            }
        }
    }
}