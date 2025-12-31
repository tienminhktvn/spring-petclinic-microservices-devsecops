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
                          -z "-config pscan.rules(10003).threshold=OFF" \
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

def modifyZapReport() {
    def jsonFile = new File("${WORKSPACE}/zap-reports/zap-report.json")
    
    if (!jsonFile.exists()) {
        echo "ZAP JSON report not found, skipping modification"
        return
    }
    
    try {
        def jsonSlurper = new groovy.json.JsonSlurper()
        def zapReport = jsonSlurper.parse(jsonFile)

        def rulesToRemove = []
        
        zapReport.site.each { site ->
            def modifiedAlerts = []
            
            site.alerts.each { alert ->
                def pluginId = alert.pluginid
                
                if (rulesToRemove.contains(pluginId)) {
                    echo "Removing alert ${pluginId}: ${alert.alert}"
                    return
                }
                
                modifiedAlerts.add(alert)
            }
            
            site.alerts = modifiedAlerts
        }
        
        // Write modified JSON back with pretty print
        def jsonOutput = groovy.json.JsonOutput.toJson(zapReport)
        def prettyJson = groovy.json.JsonOutput.prettyPrint(jsonOutput)
        jsonFile.write(prettyJson)
        
        echo "Successfully modified ZAP report"
        echo "- Downgraded rules: ${rulesToDowngrade}"
        echo "- Removed rules: ${rulesToRemove}"
        
    } catch (Exception e) {
        echo "Error modifying ZAP report: ${e.message}"
        e.printStackTrace()
    }
}