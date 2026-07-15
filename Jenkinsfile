pipeline {
    agent any

    environment {
        DOCKER_REGISTRY   = 'cosoriol'
        DOCKER_HUB_CREDS  = credentials('docker-hub-credentials')
        // Credencial "Secret text" en Jenkins con la IP publica del droplet
        DO_SERVER_IP      = credentials('do-server-ip')
        DEPLOY_PATH       = '/root/travelagency'
        IMAGE_TAG         = "${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test Backend') {
            steps {
                dir('backend') {
                    sh 'mvn -B test'
                }
            }
        }

        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    sh '''
                        npm ci
                        npm run build
                    '''
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                sh '''
                    docker build -t ${DOCKER_REGISTRY}/travel-agency-backend:${IMAGE_TAG} -t ${DOCKER_REGISTRY}/travel-agency-backend:latest ./backend
                    docker build -t ${DOCKER_REGISTRY}/travel-agency-frontend:${IMAGE_TAG} -t ${DOCKER_REGISTRY}/travel-agency-frontend:latest ./frontend
                '''
            }
        }

        stage('Push to Docker Hub') {
            steps {
                sh '''
                    echo "${DOCKER_HUB_CREDS_PSW}" | docker login -u "${DOCKER_HUB_CREDS_USR}" --password-stdin
                    docker push ${DOCKER_REGISTRY}/travel-agency-backend:${IMAGE_TAG}
                    docker push ${DOCKER_REGISTRY}/travel-agency-backend:latest
                    docker push ${DOCKER_REGISTRY}/travel-agency-frontend:${IMAGE_TAG}
                    docker push ${DOCKER_REGISTRY}/travel-agency-frontend:latest
                    docker logout
                '''
            }
        }

        stage('Deploy to DigitalOcean') {
            steps {
                // Credencial "SSH Username with private key" (usuario root, clave privada
                // ~/.ssh/digitalocean_rsa). No se toca .env en el servidor: contiene las
                // contraseñas de produccion y no vive en este repo (ver .gitignore).
                sshagent(credentials: ['digitalocean-ssh']) {
                    sh '''
                        scp -o StrictHostKeyChecking=no docker-compose.yml nginx-balancer.conf root@${DO_SERVER_IP}:${DEPLOY_PATH}/

                        ssh -o StrictHostKeyChecking=no root@${DO_SERVER_IP} "
                            cd ${DEPLOY_PATH} &&
                            docker pull ${DOCKER_REGISTRY}/travel-agency-backend:latest &&
                            docker pull ${DOCKER_REGISTRY}/travel-agency-frontend:latest &&
                            docker-compose up -d &&
                            docker-compose ps
                        "
                    '''
                }
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                    sleep 20
                    curl -f http://${DO_SERVER_IP}/health
                    curl -f http://${DO_SERVER_IP}/api/packages/available
                '''
            }
        }
    }

    post {
        success {
            echo "Deploy OK - build ${IMAGE_TAG} vivo en http://${DO_SERVER_IP}"
        }
        failure {
            echo 'Pipeline fallo - revisar logs de la stage que fallo.'
        }
        always {
            sh 'docker logout || true'
        }
    }
}
