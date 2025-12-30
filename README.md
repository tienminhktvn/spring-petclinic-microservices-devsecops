# DevSecOps Implementation Guide

## Overview

This guide covers implementing DevSecOps practices for the Spring PetClinic Microservices project:

1. **SAST** - SonarQube for static code analysis
2. **Dependency Scanning** - Snyk for vulnerability detection
3. **DAST** - OWASP ZAP for dynamic application security testing
4. **Secret Scanning** - Gitleaks with Git hooks

---

## Quick Start: Setup Jenkins Pipeline

### Step 1: Create Jenkins Pipeline Job

1. **Login to Jenkins**

2. **Create New Pipeline Job**
   ```
   New Item → Enter name: "spring-petclinic-devsecops" → Pipeline → OK
   ```

3. **Configure Pipeline from SCM**
   ```
   Pipeline section:
   - Definition: Pipeline script from SCM
   - SCM: Git
   - Repository URL: https://github.com/tienminhktvn/spring-petclinic-microservices-devsecops.git
   - Branch Specifier: */main
   - Script Path: Jenkinsfile
   ```

4. **Save and Build**
   - Click "Save"
   - Click "Build Now" to run the pipeline

### Step 2: Required Jenkins Configuration

Before running the pipeline, ensure you have:

1. **Jenkins Tools Configured** (`Manage Jenkins → Tools`):
   - Maven: Name = `Maven-3.9`
   - JDK: Name = `JDK-17`

2. **Jenkins Credentials** (`Manage Jenkins → Credentials`):
   - `snyk-token` - Snyk API token (Secret text)
   - `sonarqube-token` - SonarQube token (Secret text)

3. **SonarQube Server** (`Manage Jenkins → System → SonarQube servers`):
   - Name: `SonarQube`
   - URL: `http://192.168.195.115:9000`

---

## Part 1: SonarQube Integration (SAST)

### Step 1.1: Configure SonarQube Server

1. **Login to SonarQube** at `http://192.168.195.115:9000`
   - Default credentials: `admin/admin`
   - Change password on first login

2. **Generate Authentication Token**
   ```
   My Account → Security → Generate Tokens
   Name: jenkins-token
   Type: Global Analysis Token
   ```
   > ⚠️ Save this token! You'll need it for Jenkins.

3. **Create Project in SonarQube**
   ```
   Projects → Create Project → Manually
   Project Key: spring-petclinic-microservices
   Display Name: Spring PetClinic Microservices
   ```

### Step 1.2: Configure Jenkins for SonarQube

1. **Install Jenkins Plugins**
   ```
   Manage Jenkins → Plugins → Available plugins
   ```
   Install:
   - SonarQube Scanner
   - Pipeline Maven Integration
   - Pipeline Utility Steps

2. **Add SonarQube Server in Jenkins**
   ```
   Manage Jenkins → System → SonarQube servers
   ```
   - Name: `SonarQube`
   - Server URL: `http://<SonarQube URL>:9000`
   - Server authentication token: Add credentials (Secret text) with your SonarQube token

3. **Configure SonarQube Scanner**
   ```
   Manage Jenkins → Tools → SonarQube Scanner installations
   ```
   - Name: `SonarScanner`
   - Install automatically: ✅

### Step 1.3: Configure SonarQube Webhook to Jenkins

This step is **required** for the Quality Gate to work properly. Without the webhook, Jenkins cannot receive the Quality Gate status from SonarQube.

1. **Login to SonarQube** at `http://<SONARQUBE_URL>:9000`

2. **Navigate to Webhooks**
   ```
   Administration → Configuration → Webhooks
   ```

3. **Create a new Webhook**
   - Click **Create**
   - Name: `Jenkins`
   - URL: `http://<JENKINS_URL>:8080/sonarqube-webhook/`
   - Secret: (leave empty or add a secret if needed)
   - Click **Create**

   > **Note**: Replace `<JENKINS_URL>` with your Jenkins server IP/hostname.
   > Example: `http://192.168.195.115:8080/sonarqube-webhook/`

4. **Verify Webhook**
   - After running a pipeline, check the webhook delivery status
   - Go to `Administration → Configuration → Webhooks → Jenkins → Recent Deliveries`
   - Status should show ✅ (green checkmark)

### Step 1.4: Create sonar-project.properties

Create this file in your project root:

```properties
# sonar-project.properties
sonar.projectKey=spring-petclinic-microservices
sonar.projectName=Spring PetClinic Microservices
sonar.projectVersion=1.0

# Source directories
sonar.sources=spring-petclinic-api-gateway/src/main/java,\
  spring-petclinic-customers-service/src/main/java,\
  spring-petclinic-vets-service/src/main/java,\
  spring-petclinic-visits-service/src/main/java,\
  spring-petclinic-config-server/src/main/java

# Test directories
sonar.tests=spring-petclinic-api-gateway/src/test/java,\
  spring-petclinic-customers-service/src/test/java,\
  spring-petclinic-vets-service/src/test/java,\
  spring-petclinic-visits-service/src/test/java

# Java version
sonar.java.source=17

# Encoding
sonar.sourceEncoding=UTF-8

# Exclusions
sonar.exclusions=**/target/**,**/node_modules/**
```

---

## Part 2: Snyk Integration (Dependency Scanning)

### Step 2.1: Setup Snyk Account & CLI

1. **Create Snyk Account**
   - Go to https://snyk.io/
   - Sign up (free tier available)
   - Get your API token from Account Settings

2. **Install Snyk CLI on Jenkins Agent**
   ```bash
   curl -Lo snyk https://static.snyk.io/cli/latest/snyk-linux
   chmod +x snyk
   mv snyk /usr/local/bin/
   ```

3. **Install snyk-to-html (for HTML Reports)**
   ```bash
   # Download the snyk-to-html binary
   curl -Lo snyk-to-html https://github.com/snyk/snyk-to-html/releases/latest/download/snyk-to-html-linux
   chmod +x snyk-to-html
   mv snyk-to-html /usr/local/bin/
   
   # Verify installation
   snyk-to-html --version
   ```
   > **Note**: This tool converts Snyk JSON output to readable HTML reports.

4. **Authenticate Snyk**
   ```bash
   snyk auth <YOUR_SNYK_TOKEN>
   ```

### Step 2.2: Add Snyk Credentials to Jenkins

```
Manage Jenkins → Credentials → System → Global credentials
```
- Kind: Secret text
- Secret: Your Snyk API token
- ID: `snyk-token`
- Description: Snyk API Token

### Step 2.3: Test Snyk Locally

```bash
cd /home/minh04/spring-petclinic-microservices-devsecops

# Test for vulnerabilities
snyk test --all-projects

# Generate HTML report
snyk test --all-projects --json | snyk-to-html -o snyk-report.html
```

---

## Part 3: OWASP ZAP Integration (DAST)

### Step 3.1: Install OWASP ZAP

```bash
# Option 1: Docker (Recommended for CI/CD)
docker pull zaproxy/zap-stable

# Option 2: Download and install
wget https://github.com/zaproxy/zaproxy/releases/download/v2.15.0/ZAP_2_15_0_unix.sh
chmod +x ZAP_2_15_0_unix.sh
./ZAP_2_15_0_unix.sh
```

### Step 3.2: ZAP Scan Scripts

Create `scripts/zap-scan.sh`:

```bash
#!/bin/bash

TARGET_URL="${1:-http://192.168.195.115:32424}"
REPORT_DIR="./zap-reports"
REPORT_NAME="zap-report-$(date +%Y%m%d-%H%M%S)"

mkdir -p $REPORT_DIR

echo "🔍 Starting OWASP ZAP Baseline Scan..."
echo "Target: $TARGET_URL"

# Run ZAP Baseline Scan (passive scan - quick)
docker run --rm -v $(pwd)/$REPORT_DIR:/zap/wrk:rw \
  -t zaproxy/zap-stable zap-baseline.py \
  -t $TARGET_URL \
  -r ${REPORT_NAME}.html \
  -J ${REPORT_NAME}.json \
  -I

echo "✅ Baseline scan complete!"
echo "Reports saved to: $REPORT_DIR/"

# For more thorough testing, use full scan (takes longer):
# docker run --rm -v $(pwd)/$REPORT_DIR:/zap/wrk:rw \
#   -t zaproxy/zap-stable zap-full-scan.py \
#   -t $TARGET_URL \
#   -r ${REPORT_NAME}-full.html
```

### Step 3.3: ZAP API Scan for REST APIs

Create `scripts/zap-api-scan.sh`:

```bash
#!/bin/bash

TARGET_URL="${1:-http://192.168.195.115:32424}"
REPORT_DIR="./zap-reports"

mkdir -p $REPORT_DIR

echo "🔍 Starting OWASP ZAP API Scan..."

# Scan specific API endpoints
ENDPOINTS=(
  "/api/customer/owners"
  "/api/vet/vets"
  "/api/visit/pets/visits?petId=1"
  "/api/gateway/owners/1"
)

for endpoint in "${ENDPOINTS[@]}"; do
  echo "Scanning: ${TARGET_URL}${endpoint}"
  curl -s "${TARGET_URL}${endpoint}" > /dev/null
done

# Run ZAP with API scan
docker run --rm -v $(pwd)/$REPORT_DIR:/zap/wrk:rw \
  --network host \
  -t zaproxy/zap-stable zap-baseline.py \
  -t $TARGET_URL \
  -r zap-api-report.html \
  -J zap-api-report.json \
  -I

echo "✅ API scan complete!"
```

---

## Part 4: Gitleaks (Secret Scanning)

### Step 4.1: Install Gitleaks

```bash
# Download binary
wget https://github.com/gitleaks/gitleaks/releases/download/v8.18.4/gitleaks_8.18.4_linux_x64.tar.gz
tar -xzf gitleaks_8.18.4_linux_x64.tar.gz
sudo mv gitleaks /usr/local/bin/

# Verify installation
gitleaks version
```

### Step 4.2: Create Gitleaks Configuration

Create `.gitleaks.toml` in project root:

```toml
# .gitleaks.toml
title = "Gitleaks Configuration for Spring PetClinic"

[allowlist]
description = "Global allowlist"
paths = [
    '''\.git/''',
    '''target/''',
    '''node_modules/''',
    '''\.idea/''',
]

[[rules]]
id = "aws-access-key"
description = "AWS Access Key"
regex = '''AKIA[0-9A-Z]{16}'''
tags = ["aws", "credentials"]

[[rules]]
id = "aws-secret-key"
description = "AWS Secret Key"
regex = '''(?i)aws_secret_access_key\s*=\s*['\"]?([A-Za-z0-9/+=]{40})['\"]?'''
tags = ["aws", "credentials"]

[[rules]]
id = "generic-api-key"
description = "Generic API Key"
regex = '''(?i)(api[_-]?key|apikey)\s*[:=]\s*['\"]?([a-zA-Z0-9]{32,})['\"]?'''
tags = ["api", "key"]

[[rules]]
id = "password-in-url"
description = "Password in URL"
regex = '''[a-zA-Z]{3,10}://[^/\s:@]{3,20}:[^/\s:@]{3,20}@.{1,100}'''
tags = ["password", "url"]

[[rules]]
id = "private-key"
description = "Private Key"
regex = '''-----BEGIN (RSA|DSA|EC|OPENSSH|PGP) PRIVATE KEY-----'''
tags = ["private", "key"]
```

### Step 4.3: Setup Pre-commit Hook

Create `.git/hooks/pre-commit`:

```bash
#!/bin/bash

echo "🔍 Running Gitleaks pre-commit scan..."

# Run gitleaks on staged changes
gitleaks protect --staged --verbose --redact

if [ $? -ne 0 ]; then
    echo ""
    echo "❌ COMMIT BLOCKED: Secrets detected!"
    echo "Please remove the secrets before committing."
    echo ""
    echo "To see details, run: gitleaks detect --verbose"
    exit 1
fi

echo "✅ No secrets detected. Proceeding with commit."
exit 0
```

Make it executable:
```bash
chmod +x .git/hooks/pre-commit
```

### Step 4.4: Test Gitleaks

```bash
# Scan entire repository
gitleaks detect --source . --verbose

# Scan staged files only
gitleaks protect --staged --verbose

# Generate report
gitleaks detect --source . --report-path gitleaks-report.json --report-format json
```

---

## Part 5: Complete Jenkins Pipeline

### Jenkinsfile

Create `Jenkinsfile` in project root:

```groovy
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
                          -Dsonar.projectName=SpringPetClinicMicroservices \
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
            echo '🧹 Cleaning up resources...'
            cleanWs()
        }
    }
}
```

---

## Part 6: Testing & Validation

### Test 1: SonarQube Quality Gate

```bash
# Run SonarQube analysis locally
mvn sonar:sonar \
  -Dsonar.projectKey=spring-petclinic-microservices \
  -Dsonar.host.url=http://192.168.195.115:9000 \
  -Dsonar.login=<YOUR_SONAR_TOKEN>
```

**Expected Result:**
- Check SonarQube dashboard for code smells, bugs, vulnerabilities
- Quality Gate should PASS

### Test 2: Snyk Dependency Scan

```bash
# Scan for vulnerabilities
snyk test --all-projects

# Monitor project continuously
snyk monitor --all-projects
```

**Expected Result:**
- List of vulnerable dependencies
- Suggested fixes/upgrades

### Test 3: OWASP ZAP Scan

```bash
# Run baseline scan
bash scripts/zap-scan.sh http://192.168.195.115:32424
```

**Expected Result:**
- HTML/JSON reports in `zap-reports/`
- List of potential vulnerabilities (Low/Medium/High)

### Test 4: Gitleaks Hook

```bash
# Test with a fake secret
echo "aws_secret_access_key=AKIAIOSFODNN7EXAMPLE1234567890abcdef" > test-secret.txt
git add test-secret.txt
git commit -m "test secret detection"
```

**Expected Result:**
- Commit should be BLOCKED
- Error message showing detected secret

---

## Deliverables Checklist

| Item | File/Location |
|------|---------------|
| ✅ Jenkinsfile | `Jenkinsfile` |
| ✅ SonarQube properties | `sonar-project.properties` |
| ✅ Snyk config | Jenkins credentials |
| ✅ ZAP scan script | `scripts/zap-scan.sh` |
| ✅ Gitleaks config | `.gitleaks.toml` |
| ✅ Pre-commit hook | `.git/hooks/pre-commit` |
| 📄 Snyk Report | `snyk-report.json` |
| 📄 ZAP Report | `zap-reports/zap-report.html` |
| 📄 Gitleaks Report | `gitleaks-report.json` |

---

## Quick Commands Reference

```bash
# SonarQube scan
mvn sonar:sonar -Dsonar.host.url=http://<SonarQube URL>:9000

# Snyk scan
snyk test --all-projects

# Gitleaks scan
gitleaks detect --source . --verbose

# ZAP scan
docker run --rm -v $(pwd)/zap-reports:/zap/wrk:rw \
  --network host \
  zaproxy/zap-stable zap-baseline.py \
  -t http://192.168.195.115:32424 \
  -r report.html -I
```

---

## Troubleshooting

### SonarQube Issues
- **Connection refused**: Check if SonarQube is running on port 9000
- **Authentication failed**: Verify token in Jenkins credentials

### Snyk Issues
- **API token invalid**: Re-authenticate with `snyk auth`
- **No projects found**: Run from directory with `pom.xml`

### ZAP Issues
- **Target unreachable**: Ensure app is deployed and accessible
- **Docker network**: Use `--network host` for local targets

### Gitleaks Issues
- **Hook not running**: Check `chmod +x .git/hooks/pre-commit`
- **False positives**: Update `.gitleaks.toml` allowlist
