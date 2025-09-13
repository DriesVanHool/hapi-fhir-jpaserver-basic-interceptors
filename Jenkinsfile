pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = 'hapi-fhir-basic-auth'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        COMPOSE_FILE = '/docker/compose/docker-compose.yml'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "Building commit: ${env.GIT_COMMIT}"
            }
        }
        
        stage('Build Maven') {
            steps {
                script {
                    sh '''
                        mvn clean package -DskipTests
                        echo "Maven build completed"
                    '''
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    sh '''
                        docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                        docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                        echo "Docker image built: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    '''
                }
            }
        }
        
        stage('Stop Current Container') {
            steps {
                script {
                    sh '''
                        cd /docker/compose
                        docker-compose down hapi-fhir || true
                        echo "Current container stopped"
                    '''
                }
            }
        }
        
        stage('Update Docker Compose') {
            steps {
                script {
                    sh '''
                        # Update the image in docker-compose.yml
                        sed -i "s|image:.*hapi-fhir.*|image: ${DOCKER_IMAGE}:latest|g" ${COMPOSE_FILE}
                        echo "Docker compose file updated"
                    '''
                }
            }
        }
        
        stage('Deploy') {
            steps {
                script {
                    sh '''
                        cd /docker/compose
                        docker-compose up -d hapi-fhir
                        echo "New container deployed"
                    '''
                }
            }
        }
        
        stage('Health Check') {
            steps {
                script {
                    sh '''
                        echo "Waiting for service to start..."
                        sleep 30
                        
                        # Test if the service is responding
                        for i in {1..10}; do
                            if curl -f http://localhost:8085/fhir/metadata; then
                                echo "Health check passed"
                                exit 0
                            fi
                            echo "Attempt $i failed, retrying..."
                            sleep 10
                        done
                        echo "Health check failed"
                        exit 1
                    '''
                }
            }
        }
        
        stage('Cleanup Old Images') {
            steps {
                script {
                    sh '''
                        # Keep only the last 5 builds
                        docker images ${DOCKER_IMAGE} --format "table {{.Tag}}" | grep -v "latest" | grep -v "TAG" | sort -nr | tail -n +6 | xargs -r docker rmi ${DOCKER_IMAGE}: || true
                        echo "Cleanup completed"
                    '''
                }
            }
        }
    }
    
    post {
        success {
            echo 'Pipeline succeeded! HAPI FHIR server deployed successfully.'
        }
        failure {
            echo 'Pipeline failed! Rolling back...'
            script {
                sh '''
                    cd /docker/compose
                    docker-compose down hapi-fhir || true
                    # You could implement rollback logic here
                '''
            }
        }
        always {
            echo 'Cleaning up workspace...'
            cleanWs()
        }
    }
}
