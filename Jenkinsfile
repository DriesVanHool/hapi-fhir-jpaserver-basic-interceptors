pipeline {
    agent { label 'master' }  
    environment {
        IMAGE_NAME = "hapi-fhir-basic-auth"
        CONTAINER_NAME = "hapi-fhir"
        DOCKER_PORT = "8085"
    }
    
    triggers {
        githubPush()
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'master', url: 'https://github.com/DriesVanHool/hapi-fhir-jpaserver-basic-interceptors.git'
                echo "Building commit: ${env.GIT_COMMIT}"
            }
        }
            
    stage('Build Maven') {
        steps {
            sh '''
                docker run --rm -v "$(pwd)":/usr/src/app -w /usr/src/app maven:latest mvn clean package -DskipTests
                echo "Maven build completed"
            '''
        }
    }
        
        stage('Build & Tag Image') {
            steps {
                sh """
                docker build --pull --no-cache -t ${IMAGE_NAME}:latest .
                """
                echo "Docker image built: ${IMAGE_NAME}:latest"
            }
        }
        
        stage('Deploy Container') {
            steps {
                sh "docker rm -f ${CONTAINER_NAME} || true"
                sh """
                docker run -d --name ${CONTAINER_NAME} \
                    -p ${DOCKER_PORT}:8080 \
                    --restart unless-stopped \
                    ${IMAGE_NAME}:latest
                """
                echo "Container deployed on port ${DOCKER_PORT}"
            }
        }
        
        stage('Health Check') {
            steps {
                sh '''
                    echo "Waiting for HAPI FHIR to start..."
                    sleep 30
                    
                    for i in {1..6}; do
                        if curl -f http://localhost:${DOCKER_PORT}/fhir/metadata > /dev/null 2>&1; then
                            echo "Health check passed"
                            exit 0
                        fi
                        echo "Attempt $i failed, retrying..."
                        sleep 10
                    done
                    echo "Health check timeout"
                '''
            }
        }
    }
    
    post {
        success {
            echo "Deployment successful! HAPI FHIR is live at http://localhost:${DOCKER_PORT}"
        }
        failure {
            echo "Build or deployment failed. Check logs."
        }
        cleanup {
            sh 'docker image prune -f || true'
        }
    }
}