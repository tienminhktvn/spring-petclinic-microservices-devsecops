FROM jenkins/jenkins:lts

USER root

RUN apt-get update && apt-get install -y \
    lsb-release \
    curl \
    tar \
    && rm -rf /var/lib/apt/lists/*

# Docker CLI
RUN curl -fsSLo /usr/share/keyrings/docker-archive-keyring.asc \
  https://download.docker.com/linux/debian/gpg
RUN echo "deb [arch=$(dpkg --print-architecture) \
  signed-by=/usr/share/keyrings/docker-archive-keyring.asc] \
  https://download.docker.com/linux/debian \
  $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list
RUN apt-get update && apt-get install -y docker-ce-cli && rm -rf /var/lib/apt/lists/*

# Gitleaks
RUN curl -sS -L https://github.com/gitleaks/gitleaks/releases/download/v8.18.2/gitleaks_8.18.2_linux_x64.tar.gz -o gitleaks.tar.gz && \
    tar -xzf gitleaks.tar.gz gitleaks && \
    mv gitleaks /usr/local/bin/ && \
    chmod +x /usr/local/bin/gitleaks && \
    rm gitleaks.tar.gz

# Snyk CLI
RUN curl -Lo snyk https://static.snyk.io/cli/latest/snyk-linux && \
    chmod +x snyk && \
    mv snyk /usr/local/bin/

# Snyk to HTML CLI
RUN curl -Lo snyk-to-html https://github.com/snyk/snyk-to-html/releases/latest/download/snyk-to-html-linux  && \
    chmod +x snyk-to-html && \
    mv snyk-to-html /usr/local/bin/