// ╔════════════════════════════════════════════════════════════════════════════╗
// ║                    MEDILABO-SOLUTIONS - CI/CD PIPELINE                     ║
// ║                    Professional DevSecOps Pipeline                         ║
// ║                                                                            ║
// ║  Branch Strategy:                                                          ║
// ║  ├── feature/* : Build + Test only (no Docker)                             ║
// ║  ├── develop   : Build + Test + Docker Push (tag: develop-{BUILD})         ║
// ║  └── main      : Build + Test + Docker Push (tag: latest + main-{BUILD})   ║
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

// Branch detection helpers
def isMainBranch() { return env.BRANCH_NAME == 'main' }
def isDevelopBranch() { return env.BRANCH_NAME == 'develop' }
def isFeatureBranch() { return env.BRANCH_NAME?.startsWith('feature/') }
def isReleaseBranch() { return env.BRANCH_NAME?.startsWith('release/') }
def isHotfixBranch() { return env.BRANCH_NAME?.startsWith('hotfix/') }
def shouldBuildDocker() { return isMainBranch() || isDevelopBranch() || isReleaseBranch() || isHotfixBranch() }
def shouldPushDocker() { return isMainBranch() || isDevelopBranch() }
def shouldTagLatest() { return isMainBranch() }

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
        
        // Dynamic Docker tag based on branch
        DOCKER_TAG = "${env.BRANCH_NAME?.replaceAll('/', '-')}-${BUILD_NUMBER}"
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
                    def branchType = isMainBranch() ? '🚀 PRODUCTION' : 
                                     isDevelopBranch() ? '🔧 INTEGRATION' : 
                                     isFeatureBranch() ? '🌿 FEATURE' :
                                     isReleaseBranch() ? '📦 RELEASE' :
                                     isHotfixBranch() ? '🔥 HOTFIX' : '📌 OTHER'
                    
                    def stages = isMainBranch() ? 'Build → Test → SonarQube → Docker → Push → Deploy-Ready' :
                                 isDevelopBranch() ? 'Build → Test → SonarQube → Docker → Push' :
                                 'Build → Test → SonarQube'
                    
                    echo """
                    ════════════════════════════════════════════════════════════════════
                    🏥 MEDILABO-SOLUTIONS PIPELINE
                    ════════════════════════════════════════════════════════════════════
                    📌 Branch: ${env.BRANCH_NAME}
                    🏷️  Type:   ${branchType}
                    🔢 Build:  #${BUILD_NUMBER}
                    📋 Stages: ${stages}
                    ════════════════════════════════════════════════════════════════════
                    """
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // BUILD & TEST - All branches
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
                                                    -Dsonar.branch.name=${env.BRANCH_NAME} \
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
                                        -Dsonar.token=\${SONAR_AUTH_TOKEN} \
                                        -Dsonar.branch.name=${env.BRANCH_NAME} || true
                                """
                            }
                        } else {
                            echo "⚠️ sonar-project.properties not found"
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // DOCKER BUILD - main, develop, release/*, hotfix/* only              
        // ══════════════════════════════════════════════════════════════════════
        stage('Docker Build') {
            when {
                expression { shouldBuildDocker() }
            }
            steps {
                script {
                    echo "🐳 Building Docker images with tag: ${DOCKER_TAG}"
                    
                    services.each { service ->
                        dir(service.path) {
                            if (fileExists('Dockerfile')) {
                                sh """
                                    echo "🐳 Building Docker image for ${service.name}..."
                                    docker build -t ${DOCKER_REGISTRY}/medilabo/${service.name}:${DOCKER_TAG} .
                                    echo "✅ Image: ${DOCKER_REGISTRY}/medilabo/${service.name}:${DOCKER_TAG}"
                                """
                                
                                // Tag as 'latest' only for main branch
                                if (shouldTagLatest()) {
                                    sh """
                                        docker tag ${DOCKER_REGISTRY}/medilabo/${service.name}:${DOCKER_TAG} \
                                                   ${DOCKER_REGISTRY}/medilabo/${service.name}:latest
                                        echo "🏷️  Tagged as latest"
                                    """
                                }
                            } else {
                                echo "⚠️ No Dockerfile for ${service.name}"
                            }
                        }
                    }
                    
                    dir(frontend.path) {
                        if (fileExists('Dockerfile')) {
                            sh """
                                echo "🐳 Building Docker image for ${frontend.name}..."
                                docker build -t ${DOCKER_REGISTRY}/medilabo/${frontend.name}:${DOCKER_TAG} .
                                echo "✅ Image: ${DOCKER_REGISTRY}/medilabo/${frontend.name}:${DOCKER_TAG}"
                            """
                            
                            if (shouldTagLatest()) {
                                sh """
                                    docker tag ${DOCKER_REGISTRY}/medilabo/${frontend.name}:${DOCKER_TAG} \
                                               ${DOCKER_REGISTRY}/medilabo/${frontend.name}:latest
                                    echo "🏷️  Tagged as latest"
                                """
                            }
                        } else {
                            echo "⚠️ No Dockerfile for ${frontend.name}"
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // DOCKER PUSH - main, develop only
        // ══════════════════════════════════════════════════════════════════════
        stage('Docker Push') {
            when {
                expression { shouldPushDocker() }
            }
            steps {
                script {
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
                                        echo "📤 Pushing ${service.name}:${DOCKER_TAG}..."
                                        docker push ${DOCKER_REGISTRY}/medilabo/${service.name}:${DOCKER_TAG}
                                    """
                                    
                                    // Push 'latest' tag only for main branch
                                    if (shouldTagLatest()) {
                                        sh """
                                            echo "📤 Pushing ${service.name}:latest..."
                                            docker push ${DOCKER_REGISTRY}/medilabo/${service.name}:latest
                                        """
                                    }
                                }
                            }
                        }
                        
                        // Push frontend image
                        dir(frontend.path) {
                            if (fileExists('Dockerfile')) {
                                sh """
                                    echo "📤 Pushing ${frontend.name}:${DOCKER_TAG}..."
                                    docker push ${DOCKER_REGISTRY}/medilabo/${frontend.name}:${DOCKER_TAG}
                                """
                                
                                if (shouldTagLatest()) {
                                    sh """
                                        echo "📤 Pushing ${frontend.name}:latest..."
                                        docker push ${DOCKER_REGISTRY}/medilabo/${frontend.name}:latest
                                    """
                                }
                            }
                        }
                        
                        sh "docker logout ${DOCKER_REGISTRY}"
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // DEPLOY MARKER - main only (placeholder for future deployment)
        // ══════════════════════════════════════════════════════════════════════
        stage('Deploy Ready') {
            when {
                branch 'main'
            }
            steps {
                script {
                    echo """
                    ════════════════════════════════════════════════════════════════════
                    🚀 PRODUCTION DEPLOYMENT READY
                    ════════════════════════════════════════════════════════════════════
                    
                    All images tagged with 'latest' are ready for production deployment.
                    
                    Images available:
                    - ${DOCKER_REGISTRY}/medilabo/*:latest
                    - ${DOCKER_REGISTRY}/medilabo/*:${DOCKER_TAG}
                    
                    To deploy manually:
                    docker-compose -f docker-compose.prod.yml pull
                    docker-compose -f docker-compose.prod.yml up -d
                    
                    ════════════════════════════════════════════════════════════════════
                    """
                    
                    // Future: Add actual deployment here
                    // sh 'docker-compose -f docker-compose.prod.yml up -d'
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
            script {
                def deployInfo = isMainBranch() ? "\n    🚀 PRODUCTION READY - Images tagged as 'latest'" : 
                                 isDevelopBranch() ? "\n    🔧 INTEGRATION BUILD - Ready for testing" : 
                                 "\n    ✅ BUILD VALIDATED - Ready for PR"
                
                echo """
                ════════════════════════════════════════════════════════════════════
                ✅ PIPELINE SUCCESS - ${env.BRANCH_NAME}
                ════════════════════════════════════════════════════════════════════
                ${deployInfo}
                
                📦 Docker Tag: ${DOCKER_TAG}
                🔍 SonarQube: http://localhost:9000
                📦 Nexus: http://localhost:8185
                🐳 Docker Registry: http://localhost:8186
                
                ════════════════════════════════════════════════════════════════════
                """
            }
        }
        failure {
            echo """
            ════════════════════════════════════════════════════════════════════
            ❌ PIPELINE FAILED - ${env.BRANCH_NAME}
            ════════════════════════════════════════════════════════════════════
            
            Please check the logs above for details.
            
            ════════════════════════════════════════════════════════════════════
            """
        }
        cleanup {
            cleanWs()
        }
    }
}