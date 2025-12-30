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
                    
                    # Convert JSON report to HTML
                    snyk-to-html -i snyk-report.json -o snyk-report.html || true
                '''
                archiveArtifacts artifacts: 'snyk-report.json, snyk-report.html', allowEmptyArchive: true
            }
        }
        
        stage('DAST - OWASP ZAP') {
            steps {
                sh '''
                    mkdir -p zap-reports
                    chmod 777 zap-reports
                    
                    # Run ZAP baseline scan
                    docker run --name zap-scan \
                      -v /zap/wrk \
                      -u 0 \
                      --network host \
                      -t zaproxy/zap-stable zap-baseline.py \
                      -t ${APP_URL} \
                      -r zap-report.html \
                      -J zap-report.json \
                      -I || true

                    docker cp zap-scan:/zap/wrk/zap-report.html ./zap-reports/zap-report.html
                    docker cp zap-scan:/zap/wrk/zap-report.json ./zap-reports/zap-report.json

                    docker rm zap-scan
                '''
                archiveArtifacts artifacts: 'zap-reports/*', allowEmptyArchive: true
            }
        }
    }
    
    post {
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