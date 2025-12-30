pipeline {
    agent any
    
    environment {
        SONARQUBE_URL = 'http://sonarqube:9000'
        APP_URL = 'http://192.168.195.115:32424'
        SNYK_TOKEN = credentials('snyk-token')
    }
    
    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }
    
    stages {
        stage('Checkout') {
            steps {
                deleteDir()
                checkout scm
            }
        }
        
        stage('Secret Scanning - Gitleaks') {
            steps {
                sh '''
                    gitleaks detect --source . --verbose --report-path gitleaks-report.json --report-format json || true
                '''
                archiveArtifacts artifacts: 'gitleaks-report.json', allowEmptyArchive: true
            }
        }
        
        stage('Build') {
            steps {
                sh 'mvn clean compile -DskipTests'
            }
        }
        
        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('SAST - SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn sonar:sonar \
                          -Dsonar.projectKey=spring-petclinic-microservices \
                          -Dsonar.projectName=SpringPetClinicMicroservices \
                          -Dsonar.host.url=${SONARQUBE_URL}
                    '''
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        
        stage('Dependency Scanning - Snyk') {
            steps {
                sh '''
                    snyk auth ${SNYK_TOKEN}
                    snyk test --all-projects --json > snyk-report.json || true
                    snyk monitor --all-projects || true
                    
                    # Convert JSON report to HTML
                    snyk-to-html -i snyk-report.json -o snyk-report.html || true
                '''
                archiveArtifacts artifacts: 'snyk-report.json, snyk-report.html', allowEmptyArchive: true
                publishHTML(target: [
                    allowMissing: true,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: '.',
                    reportFiles: 'snyk-report.html',
                    reportName: 'Snyk Security Report'
                ])
            }
        }
        
        stage('DAST - OWASP ZAP') {
            steps {
                echo '🔍 Running OWASP ZAP scan...'
                sh '''
                    mkdir -p zap-reports
                    
                    # Wait for application to be ready
                    sleep 30
                    
                    # Run ZAP baseline scan
                    docker run --rm \
                      -v $(pwd)/zap-reports:/zap/wrk:rw \
                      --network host \
                      -t zaproxy/zap-stable zap-baseline.py \
                      -t ${APP_URL} \
                      -r zap-report.html \
                      -J zap-report.json \
                      -I || true
                '''
                archiveArtifacts artifacts: 'zap-reports/*', allowEmptyArchive: true
            }
        }
    }
    
    post {
        always {
            echo '📊 Publishing reports...'
            publishHTML(target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'zap-reports',
                reportFiles: 'zap-report.html',
                reportName: 'ZAP Security Report'
            ])
        }
        success {
            echo '✅ Pipeline completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed!'
        }
        cleanup {
            cleanWs()
        }
    }
}