pipeline {
    agent any
    
    environment {
        SONARQUBE_URL = 'http://192.168.195.115:9000'
        APP_URL = 'http://192.168.195.115:32424'
        SNYK_TOKEN = credentials('snyk-token')
        DOCKER_REGISTRY = 'tienminhktvn2'
    }
    
    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log --oneline -5'
            }
        }
        
        stage('Secret Scanning - Gitleaks') {
            steps {
                echo '🔍 Scanning for secrets with Gitleaks...'
                sh '''
                    gitleaks detect --source . --verbose --report-path gitleaks-report.json --report-format json || true
                '''
                archiveArtifacts artifacts: 'gitleaks-report.json', allowEmptyArchive: true
            }
        }
        
        stage('Build') {
            steps {
                echo '🔨 Building application...'
                sh 'mvn clean compile -DskipTests'
            }
        }
        
        stage('Unit Tests') {
            steps {
                echo '🧪 Running unit tests...'
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
                echo '🔬 Running SonarQube analysis...'
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn sonar:sonar \
                          -Dsonar.projectKey=spring-petclinic-microservices \
                          -Dsonar.projectName="Spring PetClinic Microservices" \
                          -Dsonar.host.url=${SONARQUBE_URL}
                    '''
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                echo '⏳ Waiting for SonarQube Quality Gate...'
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        
        stage('Dependency Scanning - Snyk') {
            steps {
                echo '🔍 Scanning dependencies with Snyk...'
                sh '''
                    snyk auth ${SNYK_TOKEN}
                    snyk test --all-projects --json > snyk-report.json || true
                    snyk monitor --all-projects || true
                '''
                archiveArtifacts artifacts: 'snyk-report.json', allowEmptyArchive: true
            }
        }
        
        stage('Build Docker Images') {
            steps {
                echo '🐳 Building Docker images...'
                sh 'bash scripts/buildAndPushImages.sh'
            }
        }
        
        stage('Deploy to K8s') {
            steps {
                echo '🚀 Deploying to Kubernetes...'
                sh '''
                    helm upgrade --install spring-petclinic \
                      /home/minh04/spring-petclinic-devsecops-manifests \
                      -n spring-petclinic \
                      --wait --timeout 5m
                '''
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
    }
}