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
        //}
        
        stage('DAST - OWASP ZAP') {
            steps {
                script {
                    // Define a unique volume name to avoid conflicts between builds
                    def zapVolume = "zap-vol-${BUILD_NUMBER}"
                    
                    sh """
                        # 1. Prepare local directory for reports
                        mkdir -p zap-reports
                        chmod 777 zap-reports

                        # 2. GENERATE RULES FILE (Safe Method)
                        # We use printf to insert real Tab characters (\\t)
                        # Rule 10003 = Vulnerable JS Library
                        printf "10003\\tIGNORE\\t(Vulnerable JS Library - Legacy App)\\n" > zap-rules.conf
                        
                        # 3. Create a temporary Docker volume
                        # This satisfies ZAP's requirement for a mounted writeable directory
                        docker volume create ${zapVolume}

                        # 4. Clean up any stuck container
                        docker rm -f ${ZAP_CONTAINER} || true

                        # 5. Start ZAP Container
                        # We mount the dummy volume to /zap/wrk
                        docker run -d --name ${ZAP_CONTAINER} \
                          --network host \
                          -u 0 \
                          -v ${zapVolume}:/zap/wrk:rw \
                          zaproxy/zap-stable \
                          sleep 3000

                        # 6. Copy the generated rules file into the container
                        docker cp zap-rules.conf ${ZAP_CONTAINER}:/zap/zap-rules.conf

                        # 7. Run the Scan
                        # -c points to the rules file we just copied
                        # -r and -J write reports to /zap/wrk (inside the volume)
                        # || true prevents the build from failing if vulnerabilities are found
                        docker exec ${ZAP_CONTAINER} zap-baseline.py \
                          -t ${APP_URL} \
                          -r zap-report.html \
                          -J zap-report.json \
                          -c /zap/zap-rules.conf \
                          -I || true

                        # 8. Extract Reports
                        # Copy files OUT of the container volume into Jenkins workspace
                        docker cp ${ZAP_CONTAINER}:/zap/wrk/zap-report.html ./zap-reports/zap-report.html || true
                        docker cp ${ZAP_CONTAINER}:/zap/wrk/zap-report.json ./zap-reports/zap-report.json || true
                        
                        # 9. Cleanup
                        docker rm -f ${ZAP_CONTAINER} || true
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
