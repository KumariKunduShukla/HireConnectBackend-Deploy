pipeline {
    agent any

    environment {
        PROJECT_DIR = '/home/ubuntu/hireconnect/microservice-architecture'
        COMPOSE_FILE = 'docker-compose.prod.yml'
        ENV_FILE = '.env'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Code checked out by Jenkins'
            }
        }

        stage('Build All Services') {
            steps {
                sh """
                    cd ${PROJECT_DIR}
                    mvn clean package -DskipTests --file api-gateway/pom.xml
                    mvn clean package -DskipTests --file auth-service/pom.xml
                    mvn clean package -DskipTests --file discovery-server/pom.xml
                    mvn clean package -DskipTests --file job-service/pom.xml
                    mvn clean package -DskipTests --file notification-service/pom.xml
                    mvn clean package -DskipTests --file profile-service/pom.xml
                    mvn clean package -DskipTests --file application-service/pom.xml
                """
            }
        }

        stage('Docker Build') {
            steps {
                sh """
                    cd ${PROJECT_DIR}
                    sudo docker compose -f ${COMPOSE_FILE} --env-file ${ENV_FILE} build
                """
            }
        }

        stage('Deploy') {
            steps {
                sh """
                    cd ${PROJECT_DIR}
                    sudo docker compose -f ${COMPOSE_FILE} --env-file ${ENV_FILE} up -d
                """
            }
        }

        stage('Health Check') {
            steps {
                sleep(time: 15, unit: 'SECONDS')
                sh 'sudo docker ps --filter "status=running" | grep hireconnect'
            }
        }
    }

    post {
        success {
            echo 'Deployment successful!'
        }
        failure {
            echo 'Deployment failed! Check logs above.'
        }
    }
}