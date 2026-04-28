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
                    // --- INFRASTRUCTURE ---
                    string(credentialsId: 'AWS_PRIVATE_IP', variable: 'AWS_PRIVATE_IP'),

                    // --- AWS SECRETS ---
                    string(credentialsId: 'AWS_ACCESS_KEY', variable: 'AWS_ACCESS_KEY'),
                    string(credentialsId: 'AWS_SECRET_KEY', variable: 'AWS_SECRET_KEY'),
                    string(credentialsId: 'AWS_REGION', variable: 'AWS_REGION'),
                    string(credentialsId: 'AWS_S3_BUCKET', variable: 'AWS_S3_BUCKET'),

                    // --- OAUTH SECRETS ---
                    string(credentialsId: 'GITHUB_CLIENT_ID', variable: 'GITHUB_CLIENT_ID'),
                    string(credentialsId: 'GITHUB_CLIENT_SECRET', variable: 'GITHUB_CLIENT_SECRET'),
                    string(credentialsId: 'GOOGLE_CLIENT_ID', variable: 'GOOGLE_CLIENT_ID'),
                    string(credentialsId: 'GOOGLE_CLIENT_SECRET', variable: 'GOOGLE_CLIENT_SECRET'),
                    string(credentialsId: 'KAKAO_CLIENT_ID', variable: 'KAKAO_CLIENT_ID'),
                    string(credentialsId: 'KAKAO_CLIENT_SECRET', variable: 'KAKAO_CLIENT_SECRET'),

                    // --- APP SECRETS ---
                    string(credentialsId: 'JWT_SECRET', variable: 'JWT_SECRET'),
                    string(credentialsId: 'MAIL_USERNAME', variable: 'MAIL_USERNAME'),
                    string(credentialsId: 'MAIL_PASSWORD', variable: 'MAIL_PASSWORD'),
                    string(credentialsId: 'REDIS_PASSWORD', variable: 'REDIS_PASSWORD'),
                ]) {
                    sh '''
                       echo "1. Stopping old container..."
                       docker rm -f backend-prod || true

                       echo "2. Booting new container..."
                       docker run -d \\
                         --network host \\
                         -e AWS_ACCESS_KEY="${AWS_ACCESS_KEY}" \\
                         -e AWS_SECRET_KEY="${AWS_SECRET_KEY}" \\
                         -e AWS_REGION="${AWS_REGION}" \\
                         -e AWS_S3_BUCKET="${AWS_S3_BUCKET}" \\
                         -e GITHUB_CLIENT_ID="${GITHUB_CLIENT_ID}" \\
                         -e GITHUB_CLIENT_SECRET="${GITHUB_CLIENT_SECRET}" \\
                         -e GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID}" \\
                         -e GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET}" \\
                         -e KAKAO_CLIENT_ID="${KAKAO_CLIENT_ID}" \\
                         -e KAKAO_CLIENT_SECRET="${KAKAO_CLIENT_SECRET}" \\
                         -e JWT_SECRET="${JWT_SECRET}" \\
                         -e MAIL_USERNAME="${MAIL_USERNAME}" \\
                         -e MAIL_PASSWORD="${MAIL_PASSWORD}" \\
                         -e SPRING_DATA_REDIS_HOST="localhost" \\
                         -e SPRING_DATA_REDIS_PASSWORD="${REDIS_PASSWORD}" \\
                         -e ELASTICSEARCH_HOST="localhost" \\
                         --name backend-prod \\
                         education-backend:latest

                       echo "3. Waiting for Spring Boot to fully start..."

                       # We wait up to 120 seconds (24 * 5) just in case
                       for i in $(seq 1 60); do
                           RESPONSE = $(curl -s -w "\\n%{http_code}" http://127.0.0.1:8090/api/actuator/health || echo "ERROR 000")

                           if echo "$RESPONSE" | grep -q "UP"; then
                               echo "✅ Spring Boot is UP and healthy!"
                               exit 0
                           fi

                           if [ $((i%20)) -eq 0 ]; then
                               echo "Diagnostic Check (Attempt $i):"
                               echo "Full Response: $RESPONSE"
                           fi

                           echo "⏳ Attempt $i: Still booting..."
                           sleep 5
                       done

                       echo "❌ ERROR: Spring Boot failed to start!"
                       docker logs backend-prod --tail 50
                       exit 1
                    '''
                }
            }
        }
    }
}