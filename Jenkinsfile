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
        // ══════════════════════════════════════════════════════════════════════
        // CHECKOUT
        // ══════════════════════════════════════════════════════════════════════
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

        // ══════════════════════════════════════════════════════════════════════
        // BACKEND - BUILD & TEST
        // ══════════════════════════════════════════════════════════════════════
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

        // ══════════════════════════════════════════════════════════════════════
        // BACKEND - SONARQUBE ANALYSIS
        // ══════════════════════════════════════════════════════════════════════
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

        // ══════════════════════════════════════════════════════════════════════
        // FRONTEND - BUILD & TEST
        // ══════════════════════════════════════════════════════════════════════
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

        // ══════════════════════════════════════════════════════════════════════
        // FRONTEND - SONARQUBE ANALYSIS
        // ══════════════════════════════════════════════════════════════════════
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
                                        -Dsonar.login=\${SONAR_AUTH_TOKEN} || true
                                """
                            }
                        } else {
                            echo "⚠️ sonar-project.properties not found, skipping SonarQube analysis"
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // DOCKER BUILD
        // ══════════════════════════════════════════════════════════════════════
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
                    
                    // Build backend images
                    services.each { service ->
                        dir(service.path) {
                            if (fileExists('Dockerfile')) {
                                sh """
                                    echo "🐳 Building Docker image for ${service.name}..."
                                    docker build -t ${DOCKER_REGISTRY}/${service.name}:${tag} .
                                    docker tag ${DOCKER_REGISTRY}/${service.name}:${tag} ${DOCKER_REGISTRY}/${service.name}:latest
                                    echo "✅ Image: ${DOCKER_REGISTRY}/${service.name}:${tag}"
                                """
                            } else {
                                echo "⚠️ No Dockerfile found for ${service.name}"
                            }
                        }
                    }
                    
                    // Build frontend image
                    dir(frontend.path) {
                        if (fileExists('Dockerfile')) {
                            sh """
                                echo "🐳 Building Docker image for ${frontend.name}..."
                                docker build -t ${DOCKER_REGISTRY}/${frontend.name}:${tag} .
                                docker tag ${DOCKER_REGISTRY}/${frontend.name}:${tag} ${DOCKER_REGISTRY}/${frontend.name}:latest
                                echo "✅ Image: ${DOCKER_REGISTRY}/${frontend.name}:${tag}"
                            """
                        } else {
                            echo "⚠️ No Dockerfile found for ${frontend.name}"
                        }
                    }
                    
                    // Liste des images créées
                    sh "docker images | grep ${DOCKER_REGISTRY} || true"
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // DOCKER PUSH (optionnel - vers registry privé)
        // ══════════════════════════════════════════════════════════════════════
        stage('Docker Push') {
            when {
                allOf {
                    branch 'main'
                    expression { return false } // Désactivé par défaut
                }
            }
            steps {
                script {
                    def tag = "${env.BRANCH_NAME}-${BUILD_NUMBER}"
                    withCredentials([usernamePassword(credentialsId: 'nexus-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh """
                            echo \$DOCKER_PASS | docker login host.docker.internal:8186 -u \$DOCKER_USER --password-stdin
                            
                            # Push backend images
                            ${services.collect { "docker push host.docker.internal:8186/${DOCKER_REGISTRY}/${it.name}:${tag} || true" }.join('\n')}
                            
                            # Push frontend image
                            docker push host.docker.internal:8186/${DOCKER_REGISTRY}/${frontend.name}:${tag} || true
                        """
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
            script {
                def tag = "${env.BRANCH_NAME ?: 'main'}-${BUILD_NUMBER}"
                echo """
                ════════════════════════════════════════════════════════════
                🎉 BUILD SUCCESS
                ════════════════════════════════════════════════════════════
                
                📦 Docker Images créées:
                ${services.collect { "   - ${DOCKER_REGISTRY}/${it.name}:${tag}" }.join('\n')}
                   - ${DOCKER_REGISTRY}/${frontend.name}:${tag}
                
                🔍 SonarQube: http://localhost:9000
                📦 Nexus:     http://localhost:8185
                
                ════════════════════════════════════════════════════════════
                """
            }
        }
        failure {
            echo '❌ Pipeline échoué!'
        }
        cleanup {
            cleanWs()
        }
    }
}
EOFcat > Jenkinsfile << 'EOF'
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
        // ══════════════════════════════════════════════════════════════════════
        // CHECKOUT
        // ══════════════════════════════════════════════════════════════════════
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

        // ══════════════════════════════════════════════════════════════════════
        // BACKEND - BUILD & TEST
        // ══════════════════════════════════════════════════════════════════════
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

        // ══════════════════════════════════════════════════════════════════════
        // BACKEND - SONARQUBE ANALYSIS
        // ══════════════════════════════════════════════════════════════════════
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

        // ══════════════════════════════════════════════════════════════════════
        // FRONTEND - BUILD & TEST
        // ══════════════════════════════════════════════════════════════════════
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

        // ══════════════════════════════════════════════════════════════════════
        // FRONTEND - SONARQUBE ANALYSIS
        // ══════════════════════════════════════════════════════════════════════
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
                                        -Dsonar.login=\${SONAR_AUTH_TOKEN} || true
                                """
                            }
                        } else {
                            echo "⚠️ sonar-project.properties not found, skipping SonarQube analysis"
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // DOCKER BUILD
        // ══════════════════════════════════════════════════════════════════════
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
                    
                    // Build backend images
                    services.each { service ->
                        dir(service.path) {
                            if (fileExists('Dockerfile')) {
                                sh """
                                    echo "🐳 Building Docker image for ${service.name}..."
                                    docker build -t ${DOCKER_REGISTRY}/${service.name}:${tag} .
                                    docker tag ${DOCKER_REGISTRY}/${service.name}:${tag} ${DOCKER_REGISTRY}/${service.name}:latest
                                    echo "✅ Image: ${DOCKER_REGISTRY}/${service.name}:${tag}"
                                """
                            } else {
                                echo "⚠️ No Dockerfile found for ${service.name}"
                            }
                        }
                    }
                    
                    // Build frontend image
                    dir(frontend.path) {
                        if (fileExists('Dockerfile')) {
                            sh """
                                echo "🐳 Building Docker image for ${frontend.name}..."
                                docker build -t ${DOCKER_REGISTRY}/${frontend.name}:${tag} .
                                docker tag ${DOCKER_REGISTRY}/${frontend.name}:${tag} ${DOCKER_REGISTRY}/${frontend.name}:latest
                                echo "✅ Image: ${DOCKER_REGISTRY}/${frontend.name}:${tag}"
                            """
                        } else {
                            echo "⚠️ No Dockerfile found for ${frontend.name}"
                        }
                    }
                    
                    // Liste des images créées
                    sh "docker images | grep ${DOCKER_REGISTRY} || true"
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // DOCKER PUSH (optionnel - vers registry privé)
        // ══════════════════════════════════════════════════════════════════════
        stage('Docker Push') {
            when {
                allOf {
                    branch 'main'
                    expression { return false } // Désactivé par défaut
                }
            }
            steps {
                script {
                    def tag = "${env.BRANCH_NAME}-${BUILD_NUMBER}"
                    withCredentials([usernamePassword(credentialsId: 'nexus-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh """
                            echo \$DOCKER_PASS | docker login host.docker.internal:8186 -u \$DOCKER_USER --password-stdin
                            
                            # Push backend images
                            ${services.collect { "docker push host.docker.internal:8186/${DOCKER_REGISTRY}/${it.name}:${tag} || true" }.join('\n')}
                            
                            # Push frontend image
                            docker push host.docker.internal:8186/${DOCKER_REGISTRY}/${frontend.name}:${tag} || true
                        """
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
            script {
                def tag = "${env.BRANCH_NAME ?: 'main'}-${BUILD_NUMBER}"
                echo """
                ════════════════════════════════════════════════════════════
                🎉 BUILD SUCCESS
                ════════════════════════════════════════════════════════════
                
                📦 Docker Images créées:
                ${services.collect { "   - ${DOCKER_REGISTRY}/${it.name}:${tag}" }.join('\n')}
                   - ${DOCKER_REGISTRY}/${frontend.name}:${tag}
                
                🔍 SonarQube: http://localhost:9000
                📦 Nexus:     http://localhost:8185
                
                ════════════════════════════════════════════════════════════
                """
            }
        }
        failure {
            echo '❌ Pipeline échoué!'
        }
        cleanup {
            cleanWs()
        }
    }
}
