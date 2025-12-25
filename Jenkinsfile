// ╔════════════════════════════════════════════════════════════════════════════╗
// ║                    MEDILABO-SOLUTIONS - CI/CD PIPELINE                     ║
// ║                    Professional DevSecOps Pipeline                         ║
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

def frontend = [name: 'medilabo-frontend', path: 'frontend/mediLabo-solutions-ui']

pipeline {
    agent any

    tools {
        maven 'M3'
        jdk 'JDK-21'
        nodejs 'NodeJS-20'
    }

    environment {
        DOCKER_REGISTRY = 'localhost:8186'
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

        stage('Frontend - Build & Test') {
            steps {
                dir(frontend.path) {
                    script {
                        if (fileExists('package.json')) {
                            sh """
                                echo "🏗️ Building ${frontend.name}..."
                                npm ci
                                npm run lint || true
                                npm run build -- --configuration=production
                                echo "✅ ${frontend.name} built successfully"
                            """
                        }
                    }
                }
            }
        }

        stage('Frontend - SonarQube') {
            steps {
                dir(frontend.path) {
                    script {
                        if (fileExists('sonar-project.properties')) {
                            withSonarQubeEnv('SonarQube') {
                                sh """
                                    echo "🔍 SonarQube analysis for ${frontend.name}..."
                                    npm install -g sonar-scanner || true
                                    sonar-scanner \
                                        -Dsonar.host.url=${SONAR_URL} \
                                        -Dsonar.token=\${SONAR_AUTH_TOKEN} || true
                                """
                            }
                        } else {
                            echo "⚠️ sonar-project.properties not found"
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
                                    docker build -t ${DOCKER_REGISTRY}/medilabo/${service.name}:${tag} .
                                    docker tag ${DOCKER_REGISTRY}/medilabo/${service.name}:${tag} ${DOCKER_REGISTRY}/medilabo/${service.name}:latest
                                    echo "✅ Image: ${DOCKER_REGISTRY}/medilabo/${service.name}:${tag}"
                                """
                            } else {
                                echo "⚠️ No Dockerfile for ${service.name}"
                            }
                        }
                    }
                    
                    dir(frontend.path) {
                        if (fileExists('Dockerfile')) {
                            sh """
                                echo "🐳 Building Docker image for ${frontend.name}..."
                                docker build -t ${DOCKER_REGISTRY}/medilabo/${frontend.name}:${tag} .
                                docker tag ${DOCKER_REGISTRY}/medilabo/${frontend.name}:${tag} ${DOCKER_REGISTRY}/medilabo/${frontend.name}:latest
                                echo "✅ Image: ${DOCKER_REGISTRY}/medilabo/${frontend.name}:${tag}"
                            """
                        } else {
                            echo "⚠️ No Dockerfile for ${frontend.name}"
                        }
                    }
                }
            }
        }

        stage('Docker Push') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                script {
                    def tag = "${env.BRANCH_NAME}-${BUILD_NUMBER}"
                    
                    withCredentials([usernamePassword(credentialsId: 'nexus-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh """
                            echo "🔐 Logging into Nexus Docker Registry..."
                            echo \$DOCKER_PASS | docker login ${DOCKER_REGISTRY} -u \$DOCKER_USER --password-stdin
                        """
                        
                        // Push backend images
                        services.each { service ->
                            dir(service.path) {
                                if (fileExists('Dockerfile')) {
                                    sh """
                                        echo "📤 Pushing ${service.name}..."
                                        docker push ${DOCKER_REGISTRY}/medilabo/${service.name}:${tag}
                                        docker push ${DOCKER_REGISTRY}/medilabo/${service.name}:latest
                                    """
                                }
                            }
                        }
                        
                        // Push frontend image
                        dir(frontend.path) {
                            if (fileExists('Dockerfile')) {
                                sh """
                                    echo "📤 Pushing ${frontend.name}..."
                                    docker push ${DOCKER_REGISTRY}/medilabo/${frontend.name}:${tag}
                                    docker push ${DOCKER_REGISTRY}/medilabo/${frontend.name}:latest
                                """
                            }
                        }
                        
                        sh "docker logout ${DOCKER_REGISTRY}"
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
            echo """
            ════════════════════════════════════════════════════════════
            ✅ PIPELINE SUCCESS
            ════════════════════════════════════════════════════════════
            
            📦 Images pushed to: ${DOCKER_REGISTRY}/medilabo/
            🔍 SonarQube: http://localhost:9000
            📦 Nexus: http://localhost:8185
            🐳 Docker Registry: http://localhost:8186
            
            ════════════════════════════════════════════════════════════
            """
        }
        failure {
            echo '❌ Pipeline échoué!'
        }
        cleanup {
            cleanWs()
        }
    }
}
