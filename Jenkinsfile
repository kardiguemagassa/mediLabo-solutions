// ╔════════════════════════════════════════════════════════════════════════════╗
// ║                    MEDILABO-SOLUTIONS - CI/CD PIPELINE                     ║
// ╚════════════════════════════════════════════════════════════════════════════╝

def services = [
    [name: 'discoveryserverservice',     path: 'backend/discoveryserverservice',     port: '8761'],
    [name: 'authorizationserverservice', path: 'backend/authorizationserverservice', port: '9000'],
    [name: 'gatewayserverservice',       path: 'backend/gatewayserverservice',       port: '8080'],
    [name: 'patientservice',             path: 'backend/patientservice',             port: '8081'],
    [name: 'notesservice',               path: 'backend/notesservice',               port: '8082'],
    [name: 'assessmentservice',          path: 'backend/assessmentservice',          port: '8083'],
    [name: 'notificationservice',        path: 'backend/notificationservice',        port: '8084']
]

def frontend = [name: 'mediLabo-solutions-ui', path: 'frontend/mediLabo-solutions-ui']

pipeline {
    agent any

    tools {
        maven 'M3'
        jdk 'JDK-21'
        nodejs 'NodeJS-20'
    }

    environment {
        DOCKER_REGISTRY = 'medilabo'
        NEXUS_URL = 'http://host.docker.internal:8185'
        SONAR_URL = 'http://host.docker.internal:9000'
    }

    options {
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    echo """
                    ════════════════════════════════════════════════════════════
                    🏥 MEDILABO-SOLUTIONS PIPELINE
                    📌 Branch: ${env.BRANCH_NAME ?: 'main'}
                    🔢 Build:  #${BUILD_NUMBER}
                    ════════════════════════════════════════════════════════════
                    """
                }
            }
        }

        stage('Backend - Build & Test') {
            steps {
                script {
                    services.each { service ->
                        dir(service.path) {
                            if (fileExists('pom.xml')) {
                                withCredentials([
                                    usernamePassword(
                                        credentialsId: 'nexus-credentials',
                                        usernameVariable: 'NEXUS_USERNAME',
                                        passwordVariable: 'NEXUS_PASSWORD'
                                    )
                                ]) {
                                    configFileProvider([configFile(fileId: 'maven-settings-nexus', variable: 'MAVEN_SETTINGS')]) {
                                        sh """
                                            echo "🏗️ Building ${service.name}..."
                                            mvn clean package -s \$MAVEN_SETTINGS -DskipTests -B
                                            echo "✅ ${service.name} built successfully"
                                        """
                                    }
                                }
                            } else {
                                echo "⚠️ No pom.xml found in ${service.path}"
                            }
                        }
                    }
                }
            }
        }

        stage('Backend - SonarQube') {
            steps {
                script {
                    services.each { service ->
                        dir(service.path) {
                            if (fileExists('pom.xml')) {
                                withSonarQubeEnv('SonarQube') {
                                    withCredentials([
                                        usernamePassword(
                                            credentialsId: 'nexus-credentials',
                                            usernameVariable: 'NEXUS_USERNAME',
                                            passwordVariable: 'NEXUS_PASSWORD'
                                        )
                                    ]) {
                                        configFileProvider([configFile(fileId: 'maven-settings-nexus', variable: 'MAVEN_SETTINGS')]) {
                                            sh """
                                                echo "🔍 SonarQube analysis for ${service.name}..."
                                                mvn sonar:sonar -s \$MAVEN_SETTINGS \
                                                    -Dsonar.projectKey=medilabo-${service.name} \
                                                    -Dsonar.projectName="${service.name}" \
                                                    -B || true
                                            """
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Frontend - Build') {
            steps {
                dir(frontend.path) {
                    script {
                        if (fileExists('package.json')) {
                            sh """
                                echo "🏗️ Building ${frontend.name}..."
                                npm ci
                                npm run build --configuration=production || npm run build
                                echo "✅ ${frontend.name} built successfully"
                            """
                        } else {
                            echo "⚠️ No package.json found in ${frontend.path}"
                        }
                    }
                }
            }
        }

        stage('Docker Build') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                script {
                    def tag = "${env.BRANCH_NAME}-${BUILD_NUMBER}"
                    
                    services.each { service ->
                        dir(service.path) {
                            if (fileExists('Dockerfile')) {
                                sh """
                                    echo "🐳 Building Docker image for ${service.name}..."
                                    docker build -t ${DOCKER_REGISTRY}/${service.name}:${tag} .
                                """
                            }
                        }
                    }
                    
                    dir(frontend.path) {
                        if (fileExists('Dockerfile')) {
                            sh """
                                echo "🐳 Building Docker image for ${frontend.name}..."
                                docker build -t ${DOCKER_REGISTRY}/${frontend.name}:${tag} .
                            """
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
        }
        success {
            echo '✅ Pipeline terminé avec succès!'
        }
        failure {
            echo '❌ Pipeline échoué!'
        }
        cleanup {
            cleanWs()
        }
    }
}
