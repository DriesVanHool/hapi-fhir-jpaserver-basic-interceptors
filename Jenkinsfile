pipeline {
    agent any
    
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
            }
        }
        
        stage('Build & Tag Image') {
            steps {
                sh """
                docker build --pull --no-cache -t ${IMAGE_NAME}:latest .
                """
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
            }
        }
    }
    
    post {
        success {
            echo "Deployment successful! HAPI FHIR is live at http://localhost:${DOCKER_PORT}"
        }
        failure {
            echo "Build failed. Check logs."
        }
        cleanup {
            sh 'docker image prune -f || true'
        }
    }
}