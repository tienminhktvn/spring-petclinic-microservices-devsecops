FROM jenkins/jenkins:lts

USER root

RUN apt-get update && apt-get install -y lsb-release

# Docker CLI
RUN curl -fsSL https://get.docker.com -o get-docker.sh
RUN sh get-docker.sh

# Gitleaks
RUN curl -sS -L https://github.com/gitleaks/gitleaks/releases/download/v8.18.2/gitleaks_8.18.2_linux_x64.tar.gz -o gitleaks.tar.gz && \
    tar -xzf gitleaks.tar.gz gitleaks && \
    mv gitleaks /usr/local/bin/ && \
    chmod +x /usr/local/bin/gitleaks && \
    rm gitleaks.tar.gz