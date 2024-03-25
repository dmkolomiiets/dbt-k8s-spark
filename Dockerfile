ARG jdkVersion=17
FROM gradle:jre$jdkVersion AS base
RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
RUN chmod +x ./kubectl
RUN mv ./kubectl /usr/local/bin

RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
RUN unzip awscliv2.zip
RUN ./aws/install
RUN curl "https://s3.amazonaws.com/session-manager-downloads/plugin/latest/ubuntu_64bit/session-manager-plugin.deb" -o "session-manager-plugin.deb"
RUN dpkg -i session-manager-plugin.deb

WORKDIR /app
COPY . .
RUN gradle --no-daemon build -x test


ENTRYPOINT ["gradle"]
ENV FEATURES_DIR="" \
    CI=true
CMD ["--project-dir=/app", "--no-daemon", "--console=plain", "--warning-mode=none", "features"]
