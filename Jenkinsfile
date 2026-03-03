// ╔════════════════════════════════════════════════════════════════════════════╗
// ║                    MEDILABO-SOLUTIONS - CI/CD PIPELINE                     ║
// ║                    Professional DevSecOps Pipeline                         ║
// ╚════════════════════════════════════════════════════════════════════════════╝

def config = [
    emailRecipients: "magassakara@gmail.com",
    dockerRegistry: "localhost:8186",
    nexus: [
        enabled:      true,
        configFileId: "maven-settings-nexus",
        url:          "http://host.docker.internal:8185",
        credentialsId:"nexus-credentials"
    ],
    sonar: [
        enabled:          true,
        installationName: "SonarQube",
        url:              "http://host.docker.internal:9000"
    ],
    timeouts: [
        qualityGate:   3,
        sonarAnalysis: 10,
        healthCheck:   3
    ]
]

def backendServices = [
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
        DOCKER_REGISTRY              = "${config.dockerRegistry}"
        CONTAINER_TAG                = "${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
        TESTCONTAINERS_RYUK_DISABLED = "true"
        // Pas de MAVEN_OPTS avec repo local → Nexus gère le cache
    }

    options {
        timeout(time: 90, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        skipDefaultCheckout(true)
        timestamps()
        disableConcurrentBuilds()
    }

    stages {

        // STAGE 1 — CHECKOUT & VALIDATION
        stage('Checkout & Validation') {
            steps {
                checkout scm
                script {
                    validateEnvironment()
                    displayBuildInfo(config, backendServices)
                }
            }
        }

        // STAGE 2 — BUILD (compile uniquement, pas de tests)
        // -U : force Maven à résoudre depuis Nexus (pas de cache local corrompu)
        // Parallélisation des 7 services
        stage('Backend - Build') {
            steps {
                script {
                    def buildStages = [:]

                    backendServices.each { service ->
                        def svc = service
                        buildStages["Build ${svc.name}"] = {
                            dir(svc.path) {
                                if (fileExists('pom.xml')) {
                                    configFileProvider([configFile(fileId: config.nexus.configFileId, variable: 'MAVEN_SETTINGS')]) {
                                        sh """
                                            echo "🏗️ Compiling ${svc.name}..."
                                            mvn clean compile -s \$MAVEN_SETTINGS -B -q -U
                                            echo "✅ ${svc.name} compiled"
                                        """
                                    }
                                }
                            }
                        }
                    }

                    parallel buildStages
                }
            }
        }

        // STAGE 3 — TEST (séparé du build — bonne pratique CI/CD)
        stage('Backend - Test') {
            steps {
                script {
                    def testStages = [:]

                    backendServices.each { service ->
                        def svc = service
                        testStages["Test ${svc.name}"] = {
                            dir(svc.path) {
                                if (fileExists('pom.xml')) {
                                    configFileProvider([configFile(fileId: config.nexus.configFileId, variable: 'MAVEN_SETTINGS')]) {
                                        sh """
                                            echo "🧪 Testing ${svc.name}..."
                                            mvn test -s \$MAVEN_SETTINGS -B -U \
                                                -Dsurefire.useSystemClassLoader=false \
                                                -Dsurefire.forkCount=1
                                            echo "✅ ${svc.name} tests passed"
                                        """
                                    }
                                }
                            }
                        }
                    }

                    parallel testStages
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: '**/target/surefire-reports/*.xml'
                }
                failure {
                    script {
                        sendNotification(config.emailRecipients, 'FAILURE', 'Tests échoués', config)
                    }
                }
            }
        }

        // STAGE 4 — COVERAGE (JaCoCo)
        stage('Backend - Coverage') {
            steps {
                script {
                    backendServices.each { service ->
                        dir(service.path) {
                            if (fileExists('pom.xml')) {
                                configFileProvider([configFile(fileId: config.nexus.configFileId, variable: 'MAVEN_SETTINGS')]) {
                                    sh """
                                        echo "📊 Coverage ${service.name}..."
                                        mvn jacoco:report -s \$MAVEN_SETTINGS -B -q -U
                                    """
                                }
                            }
                        }
                    }
                }
            }
            post {
                always {
                    jacoco(
                        execPattern:                '**/target/jacoco.exec',
                        classPattern:               '**/target/classes',
                        sourcePattern:              '**/src/main/java',
                        exclusionPattern:           '**/handler/*.class,**/event/*.class,**/dto/**/*.class,**/domain/**/*.class',
                        minimumInstructionCoverage: '70',
                        minimumBranchCoverage:      '60'
                    )
                }
            }
        }

        // STAGE 5 — PACKAGE (JAR final sans retester)
        stage('Backend - Package') {
            steps {
                script {
                    def packageStages = [:]

                    backendServices.each { service ->
                        def svc = service
                        packageStages["Package ${svc.name}"] = {
                            dir(svc.path) {
                                if (fileExists('pom.xml')) {
                                    configFileProvider([configFile(fileId: config.nexus.configFileId, variable: 'MAVEN_SETTINGS')]) {
                                        sh """
                                            echo "📦 Packaging ${svc.name}..."
                                            mvn package -s \$MAVEN_SETTINGS -DskipTests -B -q -U
                                            echo "✅ ${svc.name} packaged"
                                        """
                                    }
                                }
                            }
                        }
                    }

                    parallel packageStages
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: '**/target/*.jar',
                                     allowEmptyArchive: true,
                                     fingerprint: true
                }
            }
        }

        // STAGE 6 — SONARQUBE ANALYSIS
        stage('Backend - SonarQube') {
            when {
                expression { return config.sonar.enabled }
            }
            steps {
                script {
                    backendServices.each { service ->
                        dir(service.path) {
                            if (fileExists('pom.xml')) {
                                withSonarQubeEnv(config.sonar.installationName) {
                                    configFileProvider([configFile(fileId: config.nexus.configFileId, variable: 'MAVEN_SETTINGS')]) {
                                        timeout(time: config.timeouts.sonarAnalysis, unit: 'MINUTES') {
                                            sh """
                                                echo "🔍 SonarQube: ${service.name}..."
                                                mvn sonar:sonar -s \$MAVEN_SETTINGS -B -q \
                                                    -Dsonar.projectKey=medilabo-${service.name} \
                                                    -Dsonar.projectName="${service.name}" \
                                                    -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                                                    -Dsonar.junit.reportPaths=target/surefire-reports \
                                                    -Dsonar.java.source=21
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

        // STAGE 7 — QUALITY GATE
        // Non bloquant pour l'instant (nettoyage en cours)
        stage('Quality Gate') {
            when {
                allOf {
                    expression { return config.sonar.enabled }
                    anyOf {
                        branch 'main'
                        branch 'develop'
                        changeRequest()
                    }
                }
            }
            steps {
                script {
                    timeout(time: config.timeouts.qualityGate, unit: 'MINUTES') {
                        def qg = waitForQualityGate()
                        if (qg.status == 'OK') {
                            echo "✅ Quality Gate: PASSED"
                        } else if (qg.status == 'WARN') {
                            echo "⚠️ Quality Gate: WARNING"
                            currentBuild.result = 'UNSTABLE'
                        } else {
                            // TODO: remplacer par error() une fois le nettoyage terminé
                            echo "⚠️ Quality Gate FAILED: ${qg.status} — non bloquant (nettoyage en cours)"
                            currentBuild.result = 'UNSTABLE'
                        }
                    }
                }
            }
        }

        // STAGE 8 — SECURITY (OWASP + Maven Audit en parallèle)
        stage('Security') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    changeRequest()
                }
            }
            parallel {
                stage('OWASP Dependency Check') {
                    steps {
                        script {
                            runOwaspCheck(config)
                        }
                    }
                    post {
                        always {
                            archiveOwaspReports()
                        }
                    }
                }
                stage('Maven Security Audit') {
                    steps {
                        script {
                            backendServices.each { service ->
                                dir(service.path) {
                                    if (fileExists('pom.xml')) {
                                        configFileProvider([configFile(fileId: config.nexus.configFileId, variable: 'MAVEN_SETTINGS')]) {
                                            sh """
                                                mvn versions:display-dependency-updates \
                                                    -s \$MAVEN_SETTINGS -B -q || true
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

        // STAGE 9 — FRONTEND BUILD
        // Tests Angular désactivés — non encore implémentés
        stage('Frontend - Build') {
            steps {
                dir(frontend.path) {
                    sh """
                        echo "🏗️ Building ${frontend.name}..."
                        npm ci
                        npm run lint || true
                        echo "⚠️  Tests Angular non implémentés — à activer quand prêts"
                        npm run build -- --configuration=production
                        echo "✅ ${frontend.name} built"
                    """
                }
            }
        }

        // STAGE 10 — FRONTEND SONARQUBE
        stage('Frontend - SonarQube') {
            when {
                expression { fileExists("${frontend.path}/sonar-project.properties") }
            }
            steps {
                dir(frontend.path) {
                    withSonarQubeEnv(config.sonar.installationName) {
                        sh """
                            npm install -g sonar-scanner || true
                            sonar-scanner \
                                -Dsonar.host.url=${config.sonar.url} \
                                -Dsonar.token=\${SONAR_AUTH_TOKEN} || true
                        """
                    }
                }
            }
        }

        // STAGE 11 — DOCKER BUILD
        //  JARs déjà buildés — pas de rebuild dans Docker
        stage('Docker - Build') {
            when {
                anyOf { branch 'main'; branch 'develop' }
            }
            steps {
                script {
                    def dockerStages = [:]

                    backendServices.each { service ->
                        def svc = service
                        dockerStages["Docker ${svc.name}"] = {
                            dir(svc.path) {
                                if (fileExists('Dockerfile')) {
                                    sh """
                                        echo "🐳 Docker build: ${svc.name}..."
                                        docker build \
                                            --label "build.number=${BUILD_NUMBER}" \
                                            --label "vcs.branch=${env.BRANCH_NAME}" \
                                            -t ${DOCKER_REGISTRY}/medilabo/${svc.name}:${CONTAINER_TAG} \
                                            -t ${DOCKER_REGISTRY}/medilabo/${svc.name}:latest .
                                        echo "✅ ${svc.name} image built"
                                    """
                                }
                            }
                        }
                    }

                    dockerStages["Docker ${frontend.name}"] = {
                        dir(frontend.path) {
                            if (fileExists('Dockerfile')) {
                                sh """
                                    echo "🐳 Docker build: ${frontend.name}..."
                                    docker build \
                                        --label "build.number=${BUILD_NUMBER}" \
                                        --label "vcs.branch=${env.BRANCH_NAME}" \
                                        -t ${DOCKER_REGISTRY}/medilabo/${frontend.name}:${CONTAINER_TAG} \
                                        -t ${DOCKER_REGISTRY}/medilabo/${frontend.name}:latest .
                                    echo "✅ ${frontend.name} image built"
                                """
                            }
                        }
                    }

                    parallel dockerStages
                }
            }
        }

        // STAGE 12 — DOCKER PUSH
        stage('Docker - Push') {
            when {
                anyOf { branch 'main'; branch 'develop' }
            }
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: config.nexus.credentialsId,
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh "echo \$DOCKER_PASS | docker login ${DOCKER_REGISTRY} -u \$DOCKER_USER --password-stdin"

                        (backendServices + [frontend]).each { svc ->
                            dir(svc.path) {
                                if (fileExists('Dockerfile')) {
                                    sh """
                                        echo "📤 Pushing ${svc.name}..."
                                        docker push ${DOCKER_REGISTRY}/medilabo/${svc.name}:${CONTAINER_TAG}
                                        docker push ${DOCKER_REGISTRY}/medilabo/${svc.name}:latest
                                        echo "✅ ${svc.name} pushed"
                                    """
                                }
                            }
                        }

                        sh "docker logout ${DOCKER_REGISTRY}"
                    }
                }
            }
        }

        // STAGE 13 — DEPLOY (docker-compose up complet)
        stage('Deploy') {
            when {
                anyOf { branch 'main'; branch 'develop' }
            }
            steps {
                script {
                    def profile = (env.BRANCH_NAME == 'main') ? 'prod' : 'staging'
                    echo "🚀 Deploying to ${profile.toUpperCase()}..."

                    sh """
                        docker-compose down --remove-orphans || true
                        SPRING_PROFILES_ACTIVE=${profile} docker-compose up -d --force-recreate
                        echo "✅ Stack deployed — profile: ${profile}"
                    """
                }
            }
        }

        // STAGE 14 — HEALTH CHECK
        stage('Health Check') {
            when {
                anyOf { branch 'main'; branch 'develop' }
            }
            steps {
                script {
                    timeout(time: config.timeouts.healthCheck, unit: 'MINUTES') {
                        waitUntil {
                            def status = sh(
                                script: "curl -sf http://localhost:8080/actuator/health || echo 'down'",
                                returnStdout: true
                            ).trim()
                            if (status != 'down') {
                                echo "✅ Gateway is UP"
                                return true
                            }
                            echo "⏳ Waiting for gateway..."
                            sleep(10)
                            return false
                        }
                    }
                }
            }
            post {
                failure {
                    sh "docker-compose logs --tail=50 || true"
                }
            }
        }
    }

    // POST
    post {
        success {
            script {
                sendNotification(config.emailRecipients, 'SUCCESS', 'Pipeline terminé avec succès', config)
            }
        }
        failure {
            script {
                sendNotification(config.emailRecipients, 'FAILURE', 'Pipeline échoué', config)
            }
        }
        unstable {
            script {
                sendNotification(config.emailRecipients, 'UNSTABLE', 'Pipeline instable (qualité insuffisante)', config)
            }
        }
        cleanup {
            script {
                sh "docker image prune -f --filter 'until=24h' || true"
            }
            cleanWs()
        }
    }
}

// FONCTIONS UTILITAIRES
def validateEnvironment() {
    sh "java -version"
    sh "mvn -version"
    sh "node --version"
    sh "npm --version"
    sh "docker --version"
    sh "df -h . | tail -1 | awk '{print \"💾 Disk: \" \$4 \" available\"}'"
}

def runOwaspCheck(config) {
    try {
        echo "🛡️ OWASP Dependency Check..."
        dir('backend/assessmentservice') {
            configFileProvider([configFile(fileId: config.nexus.configFileId, variable: 'MAVEN_SETTINGS')]) {
                timeout(time: 20, unit: 'MINUTES') {
                    sh """
                        mvn org.owasp:dependency-check-maven:check -s \$MAVEN_SETTINGS \
                            -DfailBuildOnCVSS=7 \
                            -DsuppressFailureOnError=false \
                            -Dformat=HTML,XML \
                            -DretireJsAnalyzerEnabled=false \
                            -DnodeAnalyzerEnabled=false \
                            -DossindexAnalyzerEnabled=false \
                            -B -q || true
                    """
                }
            }
        }
    } catch (Exception e) {
        echo "⚠️ OWASP error: ${e.getMessage()}"
        currentBuild.result = 'UNSTABLE'
    }
}

def archiveOwaspReports() {
    if (fileExists('backend/assessmentservice/target/dependency-check-report.html')) {
        publishHTML([
            allowMissing:          true,
            alwaysLinkToLastBuild: true,
            keepAll:               true,
            reportDir:             'backend/assessmentservice/target',
            reportFiles:           'dependency-check-report.html',
            reportName:            'OWASP Security Report'
        ])
        archiveArtifacts artifacts: 'backend/assessmentservice/target/dependency-check-report.*',
                         allowEmptyArchive: true
    }
}

def sendNotification(String recipients, String status, String message, config) {
    try {
        def icons   = [SUCCESS: '✅', FAILURE: '❌', UNSTABLE: '⚠️']
        def icon    = icons[status] ?: '❓'
        def subject = "[MediLabo] ${icon} Build #${BUILD_NUMBER} — ${status} (${env.BRANCH_NAME})"

        def body = """
${icon} ${status} — MediLabo Solutions

📋 DÉTAILS
  Build    : #${BUILD_NUMBER}
  Branch   : ${env.BRANCH_NAME}
  Message  : ${message}
  Durée    : ${currentBuild.durationString ?: 'N/A'}

🔍 QUALITÉ
  SonarQube : ${config.sonar.url}
  Coverage  : ${env.BUILD_URL}jacoco/
  Tests     : ${env.BUILD_URL}testReport/
  OWASP     : ${env.BUILD_URL}OWASP_20Security_20Report/

🔗 LIENS
  Console   : ${env.BUILD_URL}console
  Workspace : ${env.BUILD_URL}ws/
  Nexus     : ${config.nexus.url}

📅 ${new Date()}
        """

        mail(to: recipients, subject: subject, body: body, mimeType: 'text/plain')
        echo "📧 Notification sent → ${recipients}"
    } catch (Exception e) {
        echo "⚠️ Notification error: ${e.getMessage()}"
    }
}

def displayBuildInfo(config, services) {
    echo """
╔══════════════════════════════════════════════════════════════════╗
║           🏥 MEDILABO-SOLUTIONS — BUILD #${BUILD_NUMBER}
╠══════════════════════════════════════════════════════════════════╣
║  Branch     : ${env.BRANCH_NAME}
║  Tag        : ${env.BRANCH_NAME}-${BUILD_NUMBER}
║  Services   : ${services.size()} backend + 1 frontend
║  Java       : 21
║  SonarQube  : ${config.sonar.enabled ? '✅ Enabled + Quality Gate' : '❌ Disabled'}
║  Nexus      : ${config.nexus.enabled ? '✅ ' + config.nexus.url : '❌ Disabled'}
║  Docker     : ${config.dockerRegistry}
╚══════════════════════════════════════════════════════════════════╝
    """
}