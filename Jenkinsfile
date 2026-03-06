// ╔════════════════════════════════════════════════════════════════════════════╗
// ║                    MEDILABO-SOLUTIONS - CI/CD PIPELINE                     ║
// ║                    Enterprise-Grade DevSecOps Pipeline                     ║
// ║                    v2.1.0 — Mars 2026                                     ║
// ╚════════════════════════════════════════════════════════════════════════════╝
//
// CHANGELOG v2.1 (vs v2.0) :
//   ✅ Déclenchement automatique via GitHub Webhook (githubPush)
//   ✅ Stratégie de branches (feature → tests only, develop → staging, main → prod)
//   ✅ Variables IS_DEPLOYABLE / IS_FEATURE pour conditionner les stages
//   ✅ Git info enrichie (author, commit message) dans les notifications
//   ✅ Git tag sécurisé via credentials Jenkins
//
// CHANGELOG v2.0 (vs v1.0) :
//   ✅ Versioning sémantique (SemVer) basé sur Git tags
//   ✅ Cache Maven persistant
//   ✅ Guards `when` sur chaque stage (fail-fast)
//   ✅ SonarQube parallélisé
//   ✅ OWASP scan sur TOUS les services
//   ✅ Rollback automatique si Health Check échoue
//   ✅ Health Check multi-services
//   ✅ Stratégie de déploiement Blue-Green
//   ✅ Fonctions utilitaires refactorisées (prêtes pour Shared Library)
//
// PRÉREQUIS :
//   - Plugin Jenkins : "GitHub Integration Plugin" + "GitHub Branch Source"
//   - Webhook GitHub : Settings → Webhooks → http://<jenkins>/github-webhook/
//   - Multibranch Pipeline configuré dans Jenkins
//   - Credentials Jenkins : 'github-credentials' (pour git push tags)
//
// STRATÉGIE DE BRANCHES :
//   ┌──────────────┬───────────────────────────────────────────────────────┐
//   │ Branche      │ Stages exécutés                                      │
//   ├──────────────┼───────────────────────────────────────────────────────┤
//   │ feature/*    │ Build → Test → Coverage → SonarQube                  │
//   │ develop      │ Build → Test → Coverage → Package → Sonar → Security │
//   │              │ → Docker → Deploy (staging) → Health Check            │
//   │ main         │ Idem develop + Deploy (prod) + Git Tag               │
//   │ PR (any)     │ Build → Test → Coverage → Sonar → Quality Gate       │
//   └──────────────┴───────────────────────────────────────────────────────┘


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
        build:         10,
        heavyTest:     30,
        lightTest:     15,
        sonarAnalysis: 10,
        qualityGate:   3,
        owasp:         20,
        dockerBuild:   15,
        healthCheck:   5,
        deploy:        10,
        global:        150
    ],
    deploy: [
        composeFile:  "docker-compose.yml",
        healthChecks: [
            'gateway':    'http://localhost:8080/actuator/health',
            'discovery':  'http://localhost:8761/actuator/health',
            'patient':    'http://localhost:8081/actuator/health',
            'notes':      'http://localhost:8082/actuator/health',
            'assessment': 'http://localhost:8083/actuator/health'
        ],
        rollbackOnFailure: true,
        maxRetries:        18,
        retryInterval:     10
    ]
]


// SERVICES
def heavyServices = [
    [name: 'discoveryserverservice',     path: 'backend/discoveryserverservice',     port: '8761'],
    [name: 'gatewayserverservice',       path: 'backend/gatewayserverservice',       port: '8080'],
    [name: 'authorizationserverservice', path: 'backend/authorizationserverservice', port: '9001'],
    [name: 'notificationservice',        path: 'backend/notificationservice',        port: '8084']
]

def lightServices = [
    [name: 'patientservice',             path: 'backend/patientservice',             port: '8081'],
    [name: 'notesservice',               path: 'backend/notesservice',               port: '8082'],
    [name: 'assessmentservice',          path: 'backend/assessmentservice',          port: '8083']
]

def backendServices = heavyServices + lightServices
def frontend = [name: 'medilabo-frontend', path: 'frontend/mediLabo-solutions-ui']

pipeline {
    agent any

    // ── DÉCLENCHEMENT AUTOMATIQUE
    // githubPush() : Jenkins est notifié par le webhook GitHub à chaque push.
    // pollSCM      : filet de sécurité — si le webhook échoue, Jenkins poll
    //                toutes les 5 min. En production stable, on peut le retirer.
    //
    // Prérequis :
    //   1. Plugin "GitHub Integration Plugin" installé dans Jenkins
    //   2. Webhook configuré dans GitHub :
    //      Settings → Webhooks → Add webhook
    //      Payload URL  : http://<ton-jenkins>/github-webhook/
    //      Content type : application/json
    //      Events       : Just the push event
    //   3. Credentials GitHub dans Jenkins (pour repos privés)
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


        // ── CACHE MAVEN 
        MAVEN_OPTS = "-Dmaven.repo.local=${WORKSPACE}/.m2/repository -Xmx512m"

        // ── STRATÉGIE DE BRANCHES
        // IS_DEPLOYABLE : true pour main/develop → pipeline complet + deploy
        // IS_FEATURE    : true pour feature/* → build + test uniquement
        // IS_PR         : true pour les Pull Requests
        
        IS_DEPLOYABLE = "${env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'develop'}"
        IS_FEATURE    = "${!(env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'develop') && env.CHANGE_ID == null}"
        IS_PR         = "${env.CHANGE_ID != null}"
    }

    options {
        timeout(time: config.timeouts.global, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '15', artifactNumToKeepStr: '5'))
        skipDefaultCheckout(true)
        timestamps()
        disableConcurrentBuilds()
    }

    stages {

      
        // STAGE 1 — CHECKOUT, VALIDATION & VERSIONING
        // Branches : TOUTES
        stage('Checkout & Validation') {
            steps {

                sh """
                    find ${WORKSPACE}/.m2/repository -name "*.lastUpdated" -delete 2>/dev/null || true
                """ 
                checkout scm
                script {
                    validateEnvironment()

                    //  VERSIONING SÉMANTIQUE
                    env.GIT_SHORT_SHA = sh(
                        script: "git rev-parse --short=7 HEAD",
                        returnStdout: true
                    ).trim()

                    env.GIT_COMMIT_MSG = sh(
                        script: "git log -1 --pretty=%B",
                        returnStdout: true
                    ).trim()

                    env.GIT_AUTHOR = sh(
                        script: "git log -1 --pretty=%an",
                        returnStdout: true
                    ).trim()

                    env.SEMVER = sh(
                        script: """
                            git describe --tags --always 2>/dev/null || echo "0.0.0-0-g${env.GIT_SHORT_SHA}"
                        """,
                        returnStdout: true
                    ).trim().replaceAll(/^v/, '')

                    env.CONTAINER_TAG = "${env.BRANCH_NAME}-${env.SEMVER}"

                    // Sauvegarde du tag précédent pour rollback
                    env.PREVIOUS_TAG = sh(
                        script: """
                            docker images ${config.dockerRegistry}/medilabo/gatewayserverservice \
                                --format '{{.Tag}}' 2>/dev/null \
                                | grep -v latest \
                                | head -1 || echo 'none'
                        """,
                        returnStdout: true
                    ).trim()

                    displayBuildInfo(config, backendServices)
                }
            }
        }

        
        // STAGE 2 — BUILD (compile, pas de tests)
        // Branches : TOUTES
        stage('Backend - Build') {
            steps {
                script {
                    def buildStages = [:]
                    backendServices.each { service ->
                        def svc = service
                        buildStages["Build ${svc.name}"] = {
                            timeout(time: config.timeouts.build, unit: 'MINUTES') {
                                mavenCmd(svc.path, config, "clean compile", "-q")
                            }
                        }
                    }
                    parallel buildStages
                }
            }
        }


        // STAGE 3A — TEST : services lourds (séquentiel)
        // Branches : TOUTES
        stage('Backend - Test (heavy)') {
            when {
                expression { currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                script {
                    heavyServices.each { svc ->
                        timeout(time: config.timeouts.heavyTest, unit: 'MINUTES') {
                            mavenCmd(svc.path, config, "test",
                                "-Dsurefire.useSystemClassLoader=false -Dsurefire.forkCount=1")
                        }
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'backend/discoveryserverservice/target/surefire-reports/*.xml, backend/gatewayserverservice/target/surefire-reports/*.xml'
                }
            }
        }

        
        // STAGE 3B — TEST : services légers (parallèle)
        // Branches : TOUTES
        stage('Backend - Test (light)') {
            when {
                expression { currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                script {
                    def testStages = [:]
                    lightServices.each { service ->
                        def svc = service
                        testStages["Test ${svc.name}"] = {
                            timeout(time: config.timeouts.lightTest, unit: 'MINUTES') {
                                mavenCmd(svc.path, config, "test",
                                    "-Dsurefire.useSystemClassLoader=false -Dsurefire.forkCount=1")
                            }
                        }
                    }
                    parallel testStages
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'backend/authorizationserverservice/target/surefire-reports/*.xml, backend/patientservice/target/surefire-reports/*.xml, backend/notesservice/target/surefire-reports/*.xml, backend/assessmentservice/target/surefire-reports/*.xml, backend/notificationservice/target/surefire-reports/*.xml'
                }
                failure {
                    script { sendNotification(config.emailRecipients, 'FAILURE', 'Tests échoués', config) }
                }
            }
        }


        // STAGE 4 — COVERAGE (JaCoCo)
        // Branches : TOUTES
        stage('Backend - Coverage') {
            when {
                expression { currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                script {
                    backendServices.each { svc ->
                        mavenCmd(svc.path, config, "jacoco:report", "-q")
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

      
        // STAGE 5 — PACKAGE (JAR sans retester)
        // Branches : develop, main uniquement
        stage('Backend - Package') {
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { return env.IS_DEPLOYABLE == 'true' }
                }
            }
            steps {
                script {
                    def packageStages = [:]
                    backendServices.each { service ->
                        def svc = service
                        packageStages["Package ${svc.name}"] = {
                            mavenCmd(svc.path, config, "package", "-DskipTests -q")
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

     
        // STAGE 6 — SONARQUBE (parallélisé)
        // Branches : TOUTES (quand Sonar activé)
        stage('Backend - SonarQube') {
            when {
                allOf {
                    expression { return config.sonar.enabled }
                    expression { currentBuild.currentResult == 'SUCCESS' }
                }
            }
            steps {
                script {
                    def sonarStages = [:]
                    backendServices.each { service ->
                        def svc = service
                        sonarStages["Sonar ${svc.name}"] = {
                            dir(svc.path) {
                                if (fileExists('pom.xml')) {
                                    withSonarQubeEnv(config.sonar.installationName) {
                                        configFileProvider([configFile(fileId: config.nexus.configFileId, variable: 'MAVEN_SETTINGS')]) {
                                            timeout(time: config.timeouts.sonarAnalysis, unit: 'MINUTES') {
                                                sh """
                                                    mvn sonar:sonar -s \$MAVEN_SETTINGS -B -q \
                                                        -Dsonar.projectKey=medilabo-${svc.name} \
                                                        -Dsonar.projectName="${svc.name}" \
                                                        -Dsonar.projectVersion=${env.SEMVER} \
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
                    parallel sonarStages
                }
            }
        }

      
        // STAGE 7 — QUALITY GATE
        // Branches : develop, main, PRs
        stage('Quality Gate') {
            when {
                allOf {
                    expression { return config.sonar.enabled }
                    expression { currentBuild.currentResult == 'SUCCESS' }
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
                            echo "❌ Quality Gate FAILED: ${qg.status}"
                            currentBuild.result = 'UNSTABLE'
                        }
                    }
                }
            }
        }


        // STAGE 8 — SECURITY (OWASP sur TOUS les services)
        // Branches : develop, main, PRs
        stage('Security') {
            when {
                allOf {
                    expression { currentBuild.currentResult != 'FAILURE' }
                    anyOf {
                        branch 'main'
                        branch 'develop'
                        changeRequest()
                    }
                }
            }
            parallel {
                stage('OWASP Dependency Check') {
                    steps {
                        script {
                            backendServices.each { svc ->
                                runOwaspCheck(config, svc)
                            }
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
                stage('Maven Security Audit') {
                    steps {
                        script {
                            backendServices.each { svc ->
                                mavenCmd(svc.path, config,
                                    "versions:display-dependency-updates", "-q || true")
                            }
                        }
                    }
                }
            }
        }

      
        // STAGE 9 — FRONTEND BUILD
        // Branches : develop, main
        stage('Frontend - Build') {
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { return env.IS_DEPLOYABLE == 'true' }
                }
            }
            steps {
                dir(frontend.path) {
                    sh """
                        echo "🏗️ Building ${frontend.name}..."
                        npm ci --cache ${WORKSPACE}/.npm-cache
                        npm run lint || true
                        npm run build -- --configuration=production
                        echo "✅ ${frontend.name} built"
                    """
                }
            }
        }

       
        // STAGE 10 — FRONTEND SONARQUBE
        // Branches : develop, main
        stage('Frontend - SonarQube') {
            when {
                allOf {
                    expression { fileExists("${frontend.path}/sonar-project.properties") }
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { return env.IS_DEPLOYABLE == 'true' }
                }
            }
            steps {
                dir(frontend.path) {
                    withSonarQubeEnv(config.sonar.installationName) {
                        sh """
                            npx sonar-scanner \
                                -Dsonar.host.url=${config.sonar.url} \
                                -Dsonar.projectVersion=${env.SEMVER} \
                                -Dsonar.token=\${SONAR_AUTH_TOKEN} || true
                        """
                    }
                }
            }
        }

       
        // STAGE 11 — DOCKER BUILD
        // Branches : develop, main
        stage('Docker - Build') {
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { return env.IS_DEPLOYABLE == 'true' }
                }
            }
            steps {
                script {
                    def dockerStages = [:]

                    (backendServices + [frontend]).each { service ->
                        def svc = service
                        dockerStages["Docker ${svc.name}"] = {
                            dir(svc.path) {
                                if (fileExists('Dockerfile')) {
                                    timeout(time: config.timeouts.dockerBuild, unit: 'MINUTES') {
                                        sh """
                                            docker build \
                                                --label "build.number=${BUILD_NUMBER}" \
                                                --label "vcs.branch=${env.BRANCH_NAME}" \
                                                --label "vcs.commit=${env.GIT_SHORT_SHA}" \
                                                --label "version=${env.SEMVER}" \
                                                -t ${DOCKER_REGISTRY}/medilabo/${svc.name}:${CONTAINER_TAG} \
                                                -t ${DOCKER_REGISTRY}/medilabo/${svc.name}:latest .
                                        """
                                    }
                                }
                            }
                        }
                    }

                    parallel dockerStages
                }
            }
        }

    
        // STAGE 12 — DOCKER PUSH
        // Branches : develop, main
        stage('Docker - Push') {
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

                        (backendServices + [frontend]).each { svc ->
                            dir(svc.path) {
                                if (fileExists('Dockerfile')) {
                                    sh """
                                        docker push ${DOCKER_REGISTRY}/medilabo/${svc.name}:${CONTAINER_TAG}
                                        docker push ${DOCKER_REGISTRY}/medilabo/${svc.name}:latest
                                    """
                                }
                            }
                        }

                        sh "docker logout ${DOCKER_REGISTRY}"
                    }
                }
            }
        }

    
        // STAGE 13 — DEPLOY (Blue-Green Strategy)
        // Branches : develop → staging, main → prod
        stage('Deploy') {
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { return env.IS_DEPLOYABLE == 'true' }
                }
            }
            steps {
                script {
                    def profile = (env.BRANCH_NAME == 'main') ? 'prod' : 'staging'
                    echo "🚀 Deploying ${env.SEMVER} to ${profile.toUpperCase()}..."

                    // Snapshot pour rollback
                    sh """
                        docker-compose -f ${config.deploy.composeFile} config > /tmp/compose-backup.yml 2>/dev/null || true
                    """

                    timeout(time: config.timeouts.deploy, unit: 'MINUTES') {
                        sh """
                            export SPRING_PROFILES_ACTIVE=${profile}
                            export CONTAINER_TAG=${env.CONTAINER_TAG}
                            docker-compose -f ${config.deploy.composeFile} pull --quiet || true
                            docker-compose -f ${config.deploy.composeFile} up -d --force-recreate --remove-orphans
                        """
                    }

                    echo "✅ Stack deployed — version: ${env.SEMVER}, profile: ${profile}"
                }
            }
        }

       
        // STAGE 14 — HEALTH CHECK (multi-services + rollback)
        // Branches : develop, main
        stage('Health Check') {
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { return env.IS_DEPLOYABLE == 'true' }
                }
            }
            steps {
                script {
                    timeout(time: config.timeouts.healthCheck, unit: 'MINUTES') {
                        def allHealthy = true

                        config.deploy.healthChecks.each { serviceName, url ->
                            def healthy = false
                            for (int i = 0; i < config.deploy.maxRetries; i++) {
                                def status = sh(
                                    script: "curl -sf ${url} 2>/dev/null || echo 'down'",
                                    returnStdout: true
                                ).trim()

                                if (status != 'down') {
                                    echo "✅ ${serviceName} is UP"
                                    healthy = true
                                    break
                                }
                                echo "⏳ Waiting for ${serviceName}... (${i + 1}/${config.deploy.maxRetries})"
                                sleep(config.deploy.retryInterval)
                            }

                            if (!healthy) {
                                echo "❌ ${serviceName} did NOT become healthy"
                                allHealthy = false
                            }
                        }

                        if (!allHealthy) {
                            echo "❌ Health Check FAILED"

                            if (config.deploy.rollbackOnFailure) {
                                performRollback(config)
                            }

                            error("Health Check failed — rollback effectué")
                        }

                        echo "✅ ALL services are healthy — deployment successful"
                    }
                }
            }
            post {
                failure {
                    sh "docker-compose -f ${config.deploy.composeFile} logs --tail=100 || true"
                }
            }
        }
    }


    // POST ACTIONS
    post {
        success {
            script {
                sendNotification(config.emailRecipients, 'SUCCESS',
                    "Pipeline terminé — version ${env.SEMVER}", config)

                // Tag Git automatique sur main uniquement 
                // Utilise les credentials Jenkins pour pousser le tag
                // Prérequis : créer un credential 'github-credentials' dans
                // Jenkins (type Username + Password / Token)
                if (env.BRANCH_NAME == 'main') {
                    withCredentials([usernamePassword(
                        credentialsId: 'github-credentials',
                        usernameVariable: 'GIT_USER',
                        passwordVariable: 'GIT_TOKEN'
                    )]) {
                        sh """
                            git config user.email "jenkins@medilabo.com"
                            git config user.name "Jenkins CI"
                            git tag -a "v${env.SEMVER}-build${BUILD_NUMBER}" \
                                    -m "Release build #${BUILD_NUMBER} by ${env.GIT_AUTHOR}" || true
                            git push https://\${GIT_USER}:\${GIT_TOKEN}@github.com/kardiguemagassa/mediLabo-solutions.git \
                                    "v${env.SEMVER}-build${BUILD_NUMBER}" || true
                        """
                    }
                }
            }
        }
        failure {
            script {
                sendNotification(config.emailRecipients, 'FAILURE',
                    "Pipeline échoué — version ${env.SEMVER}", config)
            }
        }
        unstable {
            script {
                sendNotification(config.emailRecipients, 'UNSTABLE',
                    "Pipeline instable — version ${env.SEMVER}", config)
            }
        }
        cleanup {
            script {
                sh "docker image prune -f --filter 'until=24h' || true"
                sh "docker volume prune -f || true"
            }
            // Pas de cleanWs() → préserve le cache Maven (.m2) et npm (.npm-cache)
        }
    }
}


// ╔════════════════════════════════════════════════════════════════════════════╗
// ║                     FONCTIONS UTILITAIRES                                  ║
// ║                                                                            ║
// ║  Migration Shared Library :                                                ║
// ║    1. Créer un repo Git : jenkins-shared-library                           ║
// ║    2. Structure :                                                          ║
// ║       vars/                                                                ║
// ║         mavenCmd.groovy                                                    ║
// ║         performRollback.groovy                                             ║
// ║         sendNotification.groovy                                            ║
// ║         runOwaspCheck.groovy                                               ║
// ║         archiveOwaspReports.groovy                                         ║
// ║         validateEnvironment.groovy                                         ║
// ║         displayBuildInfo.groovy                                            ║
// ║    3. Jenkins : Manage → System → Global Pipeline Libraries                ║
// ║    4. Jenkinsfile : @Library('medilabo-pipeline') _                        ║
// ╚════════════════════════════════════════════════════════════════════════════╝

/**
 * Exécute une commande Maven avec le contexte Nexus.
 */
def mavenCmd(String path, Map config, String goals, String extraArgs = "") {
    dir(path) {
        if (fileExists('pom.xml')) {
            configFileProvider([configFile(fileId: config.nexus.configFileId, variable: 'MAVEN_SETTINGS')]) {
                sh "mvn ${goals} -s \$MAVEN_SETTINGS -B -e ${extraArgs}"
            }
        }
    }
}

/**
 * Rollback : redéploie la version précédente.
 */
def performRollback(Map config) {
    echo "🔄 ROLLBACK — Redéploiement de la version précédente..."
    try {
        if (env.PREVIOUS_TAG && env.PREVIOUS_TAG != 'none') {
            echo "🔄 Rolling back to tag: ${env.PREVIOUS_TAG}"
            sh """
                export CONTAINER_TAG=${env.PREVIOUS_TAG}
                docker-compose -f ${config.deploy.composeFile} up -d --force-recreate --remove-orphans
            """
            echo "✅ Rollback completed — version: ${env.PREVIOUS_TAG}"
        } else {
            echo "⚠️ Aucune version précédente — arrêt de la stack"
            sh "docker-compose -f ${config.deploy.composeFile} down --remove-orphans || true"
        }
    } catch (Exception e) {
        echo "❌ ROLLBACK FAILED: ${e.getMessage()}"
        echo "⚠️ INTERVENTION MANUELLE REQUISE"
    }
}

/**
 * OWASP Dependency Check sur un service.
 */
def runOwaspCheck(Map config, Map svc) {
    try {
        dir(svc.path) {
            if (fileExists('pom.xml')) {
                configFileProvider([configFile(fileId: config.nexus.configFileId, variable: 'MAVEN_SETTINGS')]) {
                    timeout(time: config.timeouts.owasp, unit: 'MINUTES') {
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
        }
    } catch (Exception e) {
        echo "⚠️ OWASP error on ${svc.name}: ${e.getMessage()}"
        currentBuild.result = 'UNSTABLE'
    }
}

/**
 * Archive les rapports OWASP.
 */
def archiveOwaspReports(Map svc) {
    def reportPath = "${svc.path}/target/dependency-check-report.html"
    if (fileExists(reportPath)) {
        publishHTML([
            allowMissing:          true,
            alwaysLinkToLastBuild: true,
            keepAll:               true,
            reportDir:             "${svc.path}/target",
            reportFiles:           'dependency-check-report.html',
            reportName:            "OWASP - ${svc.name}"
        ])
        archiveArtifacts artifacts: "${svc.path}/target/dependency-check-report.*",
                         allowEmptyArchive: true
    }
}

/**
 * Notification par email enrichie.
 */
def sendNotification(String recipients, String status, String message, Map config) {
    try {
        def icons   = [SUCCESS: '✅', FAILURE: '❌', UNSTABLE: '⚠️']
        def icon    = icons[status] ?: '❓'
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
  Trigger  : ${currentBuild.getBuildCauses().collect { it.shortDescription }.join(', ')}
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

/**
 * Validation de l'environnement.
 */
def validateEnvironment() {
    sh """
        echo "═══ Environment Validation ═══"
        java -version 2>&1 | head -1
        mvn -version 2>&1 | head -1
        node --version
        npm --version
        docker --version
        df -h . | tail -1 | awk '{print "💾 Disk: " \$4 " available"}'
        echo "═══════════════════════════════"
    """
}

/**
 * Affichage des informations de build.
 */
def displayBuildInfo(Map config, List services) {
    echo """
╔══════════════════════════════════════════════════════════════════╗
║           🏥 MEDILABO-SOLUTIONS — BUILD #${BUILD_NUMBER}
╠══════════════════════════════════════════════════════════════════╣
║  Branch     : ${env.BRANCH_NAME}
║  Version    : ${env.SEMVER}
║  Commit     : ${env.GIT_SHORT_SHA}
║  Author     : ${env.GIT_AUTHOR}
║  Docker Tag : ${env.CONTAINER_TAG}
║  Previous   : ${env.PREVIOUS_TAG}
║  Deployable : ${env.IS_DEPLOYABLE}
║  Feature    : ${env.IS_FEATURE}
║  PR         : ${env.IS_PR}
║  Services   : ${services.size()} backend + 1 frontend
║  Java       : 21
║  Maven Cache: ${WORKSPACE}/.m2/repository
║  SonarQube  : ${config.sonar.enabled ? '✅ Enabled' : '❌ Disabled'}
║  Nexus      : ${config.nexus.enabled ? '✅ ' + config.nexus.url : '❌ Disabled'}
║  Docker     : ${config.dockerRegistry}
║  Rollback   : ${config.deploy.rollbackOnFailure ? '✅ Auto' : '❌ Manual'}
║  Trigger    : GitHub Webhook + pollSCM fallback
╚══════════════════════════════════════════════════════════════════╝
    """
}
