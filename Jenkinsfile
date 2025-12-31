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

        // stage('Build') {
        //     steps {
        //         sh 'mvn clean compile -DskipTests'
        //     }
        // }
        
        // stage('Unit Tests') {
        //     steps {
        //         sh 'mvn test'
        //     }
        //     post {
        //         always {
        //             junit '**/target/surefire-reports/*.xml'
        //         }
        //     }
        // }
        
        // stage('Code Coverage - JaCoCo') {
        //     steps {
        //         sh 'mvn jacoco:report'
        //     }
        //     post {
        //         always {
        //             recordCoverage(tools: [[parser: 'JACOCO', pattern: '**/target/site/jacoco/jacoco.xml']])
        //         }
        //     }
        // }
        
        // stage('SAST - SonarQube Analysis') {
        //     steps {
        //         withSonarQubeEnv('SonarQube') {
        //             sh '''
        //                 mvn sonar:sonar \
        //                   -Dsonar.projectKey=spring-petclinic-microservices \
        //                   -Dsonar.projectName=SpringPetClinicMicroservices \
        //                   -Dsonar.host.url=${SONARQUBE_URL} \
        //                   -Dsonar.coverage.jacoco.xmlReportPaths=*/target/site/jacoco/jacoco.xml
        //             '''
        //         }
        //     }
        // }
        
        // stage('Quality Gate') {
        //     steps {
        //         timeout(time: 5, unit: 'MINUTES') {
        //             waitForQualityGate abortPipeline: true
        //         }
        //     }
        // }
        
        // stage('Dependency Scanning - Snyk') {
        //     steps {
        //         sh '''
        //             snyk auth ${SNYK_TOKEN}
        //             snyk test --all-projects --json > snyk-report.json || true
                    
        //             # Convert JSON report to HTML
        //             snyk-to-html -i snyk-report.json -o snyk-report.html || true
        //         '''
        //         archiveArtifacts artifacts: 'snyk-report.json, snyk-report.html', allowEmptyArchive: true
        //     }
        // }
        
        stage('DAST - OWASP ZAP') {
            steps {
                script {
                    def zapVolume = "zap-vol-${BUILD_NUMBER}"
                    
                    sh """
                        # 1. Prepare directory
                        mkdir -p zap-reports
                        chmod 777 zap-reports

                        # 2. GENERATE RULES (For the OTHER warnings)
                        # We do NOT include 10003 here. We kill it with the -z flag below.
                        printf "10027\\tIGNORE\\t(Suspicious Comments)\\n" > zap-rules.conf
                        printf "10049\\tIGNORE\\t(Cacheable Content)\\n" >> zap-rules.conf
                        printf "10055\\tIGNORE\\t(CSP Issues)\\n" >> zap-rules.conf
                        printf "10109\\tIGNORE\\t(Modern Web App)\\n" >> zap-rules.conf
                        printf "10110\\tIGNORE\\t(Dangerous JS Functions)\\n" >> zap-rules.conf
                        printf "90004\\tIGNORE\\t(Spectre Site Isolation)\\n" >> zap-rules.conf

                        # 3. Create Volume
                        docker volume create ${zapVolume}
                        docker rm -f \${ZAP_CONTAINER} || true

                        # 4. Start ZAP
                        docker run -d --name \${ZAP_CONTAINER} \
                          --network host \
                          -u 0 \
                          -v ${zapVolume}:/zap/wrk:rw \
                          zaproxy/zap-stable \
                          sleep 3000

                        # 5. Copy Rules
                        docker cp zap-rules.conf \${ZAP_CONTAINER}:/zap/zap-rules.conf

                        # 6. Run Scan (THE FIX IS HERE)
                        # We removed the backslashes. 
                        # This sets the Passive Scan Threshold for Rule 10003 to OFF.
                        docker exec \${ZAP_CONTAINER} zap-baseline.py \
                          -t \${APP_URL} \
                          -r zap-report.html \
                          -J zap-report.json \
                          -c /zap/zap-rules.conf \
                          -z "-config pscan.rules(10003).threshold=OFF" \
                          -I || true

                        # 7. Extract Reports
                        docker cp \${ZAP_CONTAINER}:/zap/wrk/zap-report.html ./zap-reports/zap-report.html || true
                        docker cp \${ZAP_CONTAINER}:/zap/wrk/zap-report.json ./zap-reports/zap-report.json || true
                        
                        # 8. Cleanup
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
