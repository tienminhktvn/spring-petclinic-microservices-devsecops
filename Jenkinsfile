pipeline {
    agent any
    
    environment {
        SONARQUBE_URL = 'http://sonarqube:9000'
        APP_URL = 'http://192.168.195.115:32424'
        SNYK_TOKEN = credentials('snyk-token')
        ZAP_CONTAINER = 'zap-scan'
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
        
        stage('Code Coverage - JaCoCo') {
            steps {
                sh 'mvn jacoco:report'
            }
            post {
                always {
                    recordCoverage(tools: [[parser: 'JACOCO', pattern: '**/target/site/jacoco/jacoco.xml']])
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
                          -Dsonar.host.url=${SONARQUBE_URL} \
                          -Dsonar.coverage.jacoco.xmlReportPaths=*/target/site/jacoco/jacoco.xml
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
                    snyk-to-html -i snyk-report.json -o snyk-report.html || true
                '''
                archiveArtifacts artifacts: 'snyk-report.json, snyk-report.html', allowEmptyArchive: true
            }
        }
        
        stage('DAST - OWASP ZAP') {
            steps {
                script {
                    def zapVolume = "zap-vol-${BUILD_NUMBER}"
                    
                    sh """
                        mkdir -p zap-reports
                        chmod 777 zap-reports

                        docker volume create ${zapVolume}
                        docker rm -f \${ZAP_CONTAINER} || true

                        docker run -d --name \${ZAP_CONTAINER} \
                          --network host \
                          -u 0 \
                          -v ${zapVolume}:/zap/wrk:rw \
                          zaproxy/zap-stable \
                          sleep 3000

                        docker cp zap-rules.conf \${ZAP_CONTAINER}:/zap/zap-rules.conf

                        docker exec \${ZAP_CONTAINER} zap-baseline.py \
                          -t \${APP_URL} \
                          -r zap-report.html \
                          -J zap-report.json \
                          -c /zap/zap-rules.conf \
                          -I || true

                        docker cp \${ZAP_CONTAINER}:/zap/wrk/zap-report.html ./zap-reports/zap-report.html || true
                        docker cp \${ZAP_CONTAINER}:/zap/wrk/zap-report.json ./zap-reports/zap-report.json || true
                        
                        docker rm -f \${ZAP_CONTAINER} || true
                        docker volume rm ${zapVolume} || true
                    """
                }
                archiveArtifacts artifacts: 'zap-reports/*', allowEmptyArchive: true
            }
        }
    }
    
    post {
        always {
            sh '''
                docker rm -f ${ZAP_CONTAINER} || true
            '''
        }
        cleanup {
            cleanWs()
        }
    }
}