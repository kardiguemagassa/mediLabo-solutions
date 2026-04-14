// ╔════════════════════════════════════════════════════════════════════════════╗
// ║                    MEDILABO-SOLUTIONS - CI/CD PIPELINE                     ║
// ║                    OPTIMISÉ V3.0 — PARALLÉLISATION TOTALE                 ║
// ║                    Temps estimé : 25-35 minutes                           ║
// ╚════════════════════════════════════════════════════════════════════════════╝

def config = [
    emailRecipients: "magassakara@gmail.com",
    dockerRegistry:  "localhost:8186",
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
        build:         5,     // ⚡ 10→5 min
        test:          8,     // ⚡ 30→8 min (parallélisé)
        sonarAnalysis: 5,     // ⚡ 25→5 min (parallélisé)
        qualityGate:   2,
        owasp:         10,    // ⚡ 45→10 min
        dockerBuild:   5,     // ⚡ 15→5 min (parallélisé)
        healthCheck:   3,     // ⚡ 5→3 min
        deploy:        5,     // ⚡ 10→5 min
        global:        45     // ⚡ 150→45 min
    ],
    deploy: [
        composeFile:  "docker-compose.yml",
        healthChecks: [
            'gateway':       'http://host.docker.internal:8080/actuator/health',
            'discovery':     'http://host.docker.internal:8761/actuator/health',
            'authorization': 'http://host.docker.internal:9001/actuator/health',
            'notification':  'http://host.docker.internal:8084/actuator/health',
            'patient':       'http://host.docker.internal:8081/actuator/health',
            'notes':         'http://host.docker.internal:8082/actuator/health',
            'assessment':    'http://host.docker.internal:8083/actuator/health',
            'user':          'http://host.docker.internal:8085/actuator/health'
        ],
        rollbackOnFailure: true,
        maxRetries:        12,
        retryInterval:     5
    ]
]

// SERVICES (tous dans une seule liste pour parallélisation)
def backendServices = [
    [name: 'discoveryserverservice',     path: 'backend/discoveryserverservice'],
    [name: 'gatewayserverservice',       path: 'backend/gatewayserverservice'],
    [name: 'authorizationserverservice', path: 'backend/authorizationserverservice'],
    [name: 'notificationservice',        path: 'backend/notificationservice'],
    [name: 'userservice',                path: 'backend/userservice'],
    [name: 'patientservice',             path: 'backend/patientservice'],
    [name: 'notesservice',               path: 'backend/notesservice'],
    [name: 'assessmentservice',          path: 'backend/assessmentservice']
]

def frontend = [name: 'medilabo-frontend', path: 'frontend/mediLabo-solutions-ui']

pipeline {
    agent any

    triggers {
        githubPush()
        pollSCM('H/5 * * * *')
    }

    tools {
        maven  'M3'
        jdk    'JDK-21'
        nodejs 'NodeJS-20'
    }

    environment {
        DOCKER_REGISTRY              = "${config.dockerRegistry}"
        TESTCONTAINERS_RYUK_DISABLED = "true"
        TESTCONTAINERS_HOST_OVERRIDE = "host.docker.internal"
        MAVEN_OPTS = "-Dmaven.repo.local=${WORKSPACE}/.m2/repository -Xmx1024m"
        MAVEN_ARGS = "-T 2C"
        IS_DEPLOYABLE = "${env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'develop'}"
    }

    options {
        timeout(time: config.timeouts.global, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '15', artifactNumToKeepStr: '5'))
        skipDefaultCheckout(true)
        timestamps()
        disableConcurrentBuilds()
    }

    stages {

        // ═══════════════════════════════════════════════════════════════════
        // STAGE 1 — CHECKOUT
        // ═══════════════════════════════════════════════════════════════════
        stage('⚡ Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT_SHA = sh(script: "git rev-parse --short=7 HEAD", returnStdout: true).trim()
                    env.GIT_COMMIT_MSG = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                    env.GIT_AUTHOR = sh(script: "git log -1 --pretty=%an", returnStdout: true).trim()
                    env.SEMVER = sh(script: "git describe --tags --always 2>/dev/null || echo '0.0.0'", returnStdout: true).trim()
                    env.CONTAINER_TAG = "${env.BRANCH_NAME}-${env.GIT_SHORT_SHA}"
                }
                displayBuildInfo(config, backendServices)
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // STAGE 2 — BUILD (PARALLÈLE)
        // ═══════════════════════════════════════════════════════════════════
        stage('🔨 Build') {
            parallel {
                script {
                    def stages = [:]
                    backendServices.each { svc ->
                        stages["${svc.name}"] = {
                            timeout(time: config.timeouts.build, unit: 'MINUTES') {
                                mavenCmd(svc.path, config, "clean compile", "-q")
                            }
                        }
                    }
                    stages
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // STAGE 3 — TEST (PARALLÈLE COMPLET)
        // ═══════════════════════════════════════════════════════════════════
        stage('🧪 Test') {
            parallel {
                script {
                    def stages = [:]
                    backendServices.each { svc ->
                        stages["${svc.name}"] = {
                            timeout(time: config.timeouts.test, unit: 'MINUTES') {
                                mavenCmd(svc.path, config, "test",
                                    "-Dsurefire.useSystemClassLoader=false -Dsurefire.forkCount=1 -T 2C")
                            }
                        }
                    }
                    stages
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // STAGE 4 — COVERAGE (PARALLÈLE)
        // ═══════════════════════════════════════════════════════════════════
        stage('📊 Coverage') {
            when {
                expression { currentBuild.currentResult == 'SUCCESS' }
            }
            parallel {
                script {
                    def stages = [:]
                    backendServices.each { svc ->
                        stages["${svc.name}"] = {
                            mavenCmd(svc.path, config, "jacoco:report", "-q")
                        }
                    }
                    stages
                }
            }
            post {
                always {
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java',
                        exclusionPattern: '**/handler/*.class,**/event/*.class,**/dto/**/*.class,**/domain/**/*.class',
                        minimumInstructionCoverage: '70',
                        minimumBranchCoverage: '60'
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // STAGE 5 — PACKAGE (PARALLÈLE)
        // ═══════════════════════════════════════════════════════════════════
        stage('📦 Package') {
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { return env.IS_DEPLOYABLE == 'true' }
                }
            }
            parallel {
                script {
                    def stages = [:]
                    backendServices.each { svc ->
                        stages["${svc.name}"] = {
                            mavenCmd(svc.path, config, "package", "-DskipTests -q")
                        }
                    }
                    stages
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

        // ═══════════════════════════════════════════════════════════════════
        // STAGE 6 — SONARQUBE (PARALLÈLE)
        // ═══════════════════════════════════════════════════════════════════
        stage('🔍 SonarQube') {
            when {
                allOf {
                    expression { config.sonar.enabled }
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { return env.IS_DEPLOYABLE == 'true' }
                }
            }
            parallel {
                script {
                    def stages = [:]
                    backendServices.each { svc ->
                        stages["${svc.name}"] = {
                            timeout(time: config.timeouts.sonarAnalysis, unit: 'MINUTES') {
                                dir(svc.path) {
                                    withSonarQubeEnv(config.sonar.installationName) {
                                        configFileProvider([configFile(fileId: config.nexus.configFileId, variable: 'MAVEN_SETTINGS')]) {
                                            sh """
                                                mvn sonar:sonar -s \$MAVEN_SETTINGS -B \
                                                    -Dsonar.projectKey=medilabo-${svc.name} \
                                                    -Dsonar.projectName="${svc.name}" \
                                                    -Dsonar.projectVersion=${env.SEMVER} \
                                                    -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                                                    -Dsonar.java.source=21
                                            """
                                        }
                                    }
                                }
                            }
                        }
                    }
                    stages
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // STAGE 7 — QUALITY GATE
        // ═══════════════════════════════════════════════════════════════════
        stage('✅ Quality Gate') {
            when {
                allOf {
                    expression { config.sonar.enabled }
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    anyOf { branch 'main'; branch 'develop'; changeRequest() }
                }
            }
            steps {
                timeout(time: config.timeouts.qualityGate, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status == 'OK') {
                            echo "✅ Quality Gate: PASSED"
                        } else if (qg.status == 'WARN') {
                            echo "⚠️ Quality Gate: WARNING"
                            currentBuild.result = 'UNSTABLE'
                        } else {
                            echo "❌ Quality Gate FAILED: ${qg.status}"
                            currentBuild.result = 'UNSTABLE'
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // STAGE 8 — SECURITY OWASP (PARALLÈLE)
        // ═══════════════════════════════════════════════════════════════════
        stage('🔒 Security') {
            when {
                allOf {
                    expression { currentBuild.currentResult != 'FAILURE' }
                    anyOf { branch 'main'; branch 'develop'; changeRequest() }
                }
            }
            parallel {
                script {
                    def owaspStages = [:]
                    backendServices.each { svc ->
                        owaspStages["OWASP ${svc.name}"] = {
                            runOwaspCheck(config, svc)
                        }
                    }
                    owaspStages
                }
            }
            post {
                always {
                    script {
                        backendServices.each { svc ->
                            archiveOwaspReports(svc)
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // STAGE 9 — FRONTEND
        // ═══════════════════════════════════════════════════════════════════
        stage('🎨 Frontend') {
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { return env.IS_DEPLOYABLE == 'true' }
                }
            }
            steps {
                dir(frontend.path) {
                    sh """
                        npm ci --cache ${WORKSPACE}/.npm-cache
                        npm run build -- --configuration=production
                    """
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // STAGE 10 — RSA KEYS
        // ═══════════════════════════════════════════════════════════════════
        stage('🔐 RSA Keys') {
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { return env.IS_DEPLOYABLE == 'true' }
                }
            }
            steps {
                withCredentials([
                    file(credentialsId: 'medilabo-private-key', variable: 'PRIVATE_KEY'),
                    file(credentialsId: 'medilabo-public-key', variable: 'PUBLIC_KEY')
                ]) {
                    sh '''
                        mkdir -p backend/authorizationserverservice/src/main/resources/keys
                        cp "$PRIVATE_KEY" backend/authorizationserverservice/src/main/resources/keys/private.key
                        cp "$PUBLIC_KEY" backend/authorizationserverservice/src/main/resources/keys/public.key
                        chmod 600 backend/authorizationserverservice/src/main/resources/keys/private.key
                        chmod 644 backend/authorizationserverservice/src/main/resources/keys/public.key
                    '''
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // STAGE 11 — DOCKER BUILD & PUSH (PARALLÈLE COMPLET)
        // ═══════════════════════════════════════════════════════════════════
        stage('🐳 Docker') {
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { return env.IS_DEPLOYABLE == 'true' }
                }
            }
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: config.nexus.credentialsId,
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh "echo \$DOCKER_PASS | docker login ${DOCKER_REGISTRY} -u \$DOCKER_USER --password-stdin"

                        def dockerStages = [:]
                        (backendServices + [frontend]).each { svc ->
                            dockerStages["${svc.name}"] = {
                                timeout(time: config.timeouts.dockerBuild, unit: 'MINUTES') {
                                    dir(svc.path) {
                                        if (fileExists('Dockerfile')) {
                                            sh """
                                                docker build \\
                                                    --label "version=${env.SEMVER}" \\
                                                    -t ${DOCKER_REGISTRY}/medilabo/${svc.name}:${env.CONTAINER_TAG} \\
                                                    -t ${DOCKER_REGISTRY}/medilabo/${svc.name}:latest .
                                                docker push ${DOCKER_REGISTRY}/medilabo/${svc.name}:${env.CONTAINER_TAG}
                                                docker push ${DOCKER_REGISTRY}/medilabo/${svc.name}:latest
                                            """
                                        }
                                    }
                                }
                            }
                        }
                        parallel dockerStages
                        sh "docker logout ${DOCKER_REGISTRY}"
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // STAGE 12 — DEPLOY
        // ═══════════════════════════════════════════════════════════════════
        stage('🚀 Deploy') {
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { return env.IS_DEPLOYABLE == 'true' }
                }
            }
            steps {
                script {
                    def profile = (env.BRANCH_NAME == 'main') ? 'prod' : 'staging'
                    
                    withCredentials([file(credentialsId: 'medilabo-env-staging', variable: 'ENV_FILE')]) {
                        sh 'cp "$ENV_FILE" .env'
                    }
                    
                    withCredentials([usernamePassword(
                        credentialsId: config.nexus.credentialsId,
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh """
                            echo \$DOCKER_PASS | docker login ${DOCKER_REGISTRY} -u \$DOCKER_USER --password-stdin
                            export SPRING_PROFILES_ACTIVE=${profile}
                            export CONTAINER_TAG=${env.CONTAINER_TAG}
                            export DOCKER_REGISTRY=${DOCKER_REGISTRY}
                            
                            docker rm -f \$(docker ps -aq --filter "name=medilabo-") 2>/dev/null || true
                            docker compose -f ${config.deploy.composeFile} up -d --force-recreate --remove-orphans
                            docker logout ${DOCKER_REGISTRY} || true
                        """
                    }
                    echo "✅ Stack deployed — version: ${env.SEMVER}, profile: ${profile}"
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // STAGE 13 — HEALTH CHECK (PARALLÈLE)
        // ═══════════════════════════════════════════════════════════════════
        stage('🏥 Health Check') {
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { return env.IS_DEPLOYABLE == 'true' }
                }
            }
            steps {
                script {
                    timeout(time: config.timeouts.healthCheck, unit: 'MINUTES') {
                        def healthStages = [:]
                        config.deploy.healthChecks.each { serviceName, url ->
                            healthStages["${serviceName}"] = {
                                def healthy = false
                                for (int i = 0; i < config.deploy.maxRetries; i++) {
                                    def status = sh(
                                        script: "curl -sf ${url} -o /dev/null && echo 'UP' || echo 'DOWN'",
                                        returnStdout: true
                                    ).trim()
                                    if (status == 'UP') {
                                        echo "✅ ${serviceName} is UP"
                                        healthy = true
                                        break
                                    }
                                    echo "⏳ Waiting for ${serviceName}... (${i + 1}/${config.deploy.maxRetries})"
                                    sleep(config.deploy.retryInterval)
                                }
                                if (!healthy) {
                                    error("❌ ${serviceName} is DOWN")
                                }
                            }
                        }
                        parallel healthStages
                        echo "✅ ALL services are healthy"
                    }
                }
            }
            post {
                failure {
                    script {
                        if (config.deploy.rollbackOnFailure) {
                            performRollback(config)
                        }
                        sh "docker compose -f ${config.deploy.composeFile} logs --tail=100 || true"
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                sendNotification(config.emailRecipients, 'SUCCESS', "Pipeline terminé — version ${env.SEMVER}", config)
                if (env.BRANCH_NAME == 'main') {
                    withCredentials([usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
                        sh """
                            git config user.email "jenkins@medilabo.com"
                            git config user.name "Jenkins CI"
                            git tag -a "v${env.SEMVER}-build${BUILD_NUMBER}" -m "Release build #${BUILD_NUMBER}" || true
                            git push https://\${GIT_USER}:\${GIT_TOKEN}@github.com/kardiguemagassa/mediLabo-solutions.git "v${env.SEMVER}-build${BUILD_NUMBER}" || true
                        """
                    }
                }
            }
        }
        failure {
            script { sendNotification(config.emailRecipients, 'FAILURE', "Pipeline échoué — version ${env.SEMVER}", config) }
        }
        cleanup {
            script {
                sh "docker image prune -f --filter 'until=24h' 2>/dev/null || true"
                sh "docker volume prune -f 2>/dev/null || true"
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// FONCTIONS UTILITAIRES
// ══════════════════════════════════════════════════════════════════════════════

def mavenCmd(String path, Map config, String goals, String extraArgs = "") {
    dir(path) {
        if (fileExists('pom.xml')) {
            configFileProvider([configFile(fileId: config.nexus.configFileId, variable: 'MAVEN_SETTINGS')]) {
                sh "mvn ${goals} -s \$MAVEN_SETTINGS -B -e ${extraArgs}"
            }
        }
    }
}

def performRollback(Map config) {
    echo "🔄 ROLLBACK — Redéploiement de la version précédente..."
    try {
        if (env.PREVIOUS_TAG && env.PREVIOUS_TAG != 'none') {
            sh """
                export CONTAINER_TAG=${env.PREVIOUS_TAG}
                docker compose -f ${config.deploy.composeFile} up -d --force-recreate --remove-orphans
            """
            echo "✅ Rollback completed — version: ${env.PREVIOUS_TAG}"
        } else {
            sh "docker compose -f ${config.deploy.composeFile} down --remove-orphans || true"
        }
    } catch (Exception e) {
        echo "❌ ROLLBACK FAILED: ${e.getMessage()}"
    }
}

def runOwaspCheck(Map config, Map svc) {
    try {
        dir(svc.path) {
            if (fileExists('pom.xml')) {
                configFileProvider([configFile(fileId: config.nexus.configFileId, variable: 'MAVEN_SETTINGS')]) {
                    timeout(time: config.timeouts.owasp, unit: 'MINUTES') {
                        sh """
                            mvn org.owasp:dependency-check-maven:check -s \$MAVEN_SETTINGS \
                                -DfailBuildOnCVSS=7 \
                                -DsuppressFailureOnError=true \
                                -Dformat=HTML \
                                -DdataDirectory=${WORKSPACE}/.owasp-data \
                                -B -q
                        """
                    }
                }
            }
        }
    } catch (Exception e) {
        echo "⚠️ OWASP error on ${svc.name}: ${e.getMessage()}"
        currentBuild.result = 'UNSTABLE'
    }
}

def archiveOwaspReports(Map svc) {
    def reportPath = "${svc.path}/target/dependency-check-report.html"
    if (fileExists(reportPath)) {
        publishHTML([
            allowMissing: true,
            alwaysLinkToLastBuild: true,
            keepAll: true,
            reportDir: "${svc.path}/target",
            reportFiles: 'dependency-check-report.html',
            reportName: "OWASP - ${svc.name}"
        ])
    }
}

def sendNotification(String recipients, String status, String message, Map config) {
    try {
        def icons = [SUCCESS: '✅', FAILURE: '❌', UNSTABLE: '⚠️']
        def icon = icons[status] ?: '❓'
        def subject = "[MediLabo] ${icon} Build #${BUILD_NUMBER} — ${status} (${env.BRANCH_NAME})"
        def body = """
${icon} ${status} — MediLabo Solutions

📋 DÉTAILS
  Build    : #${BUILD_NUMBER}
  Branch   : ${env.BRANCH_NAME}
  Version  : ${env.SEMVER ?: 'N/A'}
  Commit   : ${env.GIT_SHORT_SHA ?: 'N/A'}
  Author   : ${env.GIT_AUTHOR ?: 'N/A'}
  Message  : ${env.GIT_COMMIT_MSG ?: 'N/A'}
  Durée    : ${currentBuild.durationString ?: 'N/A'}

🔗 LIENS
  Console   : ${env.BUILD_URL}console
  SonarQube : ${config.sonar.url}

📅 ${new Date()}
        """
        mail(to: recipients, subject: subject, body: body, mimeType: 'text/plain')
        echo "📧 Notification sent → ${recipients}"
    } catch (Exception e) {
        echo "⚠️ Notification error: ${e.getMessage()}"
    }
}

def displayBuildInfo(Map config, List services) {
    echo """
╔══════════════════════════════════════════════════════════════════╗
║           🏥 MEDILABO-SOLUTIONS — BUILD #${BUILD_NUMBER}
╠══════════════════════════════════════════════════════════════════╣
║  Branch     : ${env.BRANCH_NAME}
║  Version    : ${env.SEMVER}
║  Commit     : ${env.GIT_SHORT_SHA}
║  Docker Tag : ${env.CONTAINER_TAG}
║  Services   : ${services.size()} backend + 1 frontend
║  Optimisation: PARALLÉLISATION TOTALE ⚡
╚══════════════════════════════════════════════════════════════════╝
    """
}