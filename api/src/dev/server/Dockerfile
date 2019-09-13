FROM ubuntu:18.04

#
# Development requirements
#

RUN apt-get update && \
    apt-get install --no-install-recommends -y \
     openjdk-8-jdk=8u222-b10-1ubuntu1~18.04.1 \
     curl=7.58.0-2ubuntu3.7 \
     python=2.7.15~rc1-1 \
     gcc=4:7.4.0-1ubuntu2.3 \
     python-dev=2.7.15~rc1-1 \
     python-setuptools=39.0.1-2 \
     bash=4.4.18-2ubuntu1.2 \
     libc6-i386=2.27-3ubuntu1 \
     openssh-client=1:7.6p1-4ubuntu0.3 \
     git=1:2.17.1-1ubuntu0.4 \
     gettext=0.19.8.1-6ubuntu0.3 \
     mysql-server=5.7.27-0ubuntu0.18.04.1 \
     mysql-client=5.7.27-0ubuntu0.18.04.1 \
     ruby=1:2.5.1 \
     ruby-dev=1:2.5.1 \
     ruby-json=2.1.0+dfsg-2 \
     make=4.1-9.1ubuntu1 \
     unzip=6.0-21ubuntu1 \
     wait-for-it=0.0~git20170723-1 \
     && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV CLOUD_SDK_VERSION 260.0.0

ENV PATH /google-cloud-sdk/bin:$PATH

RUN curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
    tar xzf google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
    rm google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
    ln -s /lib /lib64 && \
    gcloud config set core/disable_usage_reporting true && \
    gcloud config set component_manager/disable_update_check true && \
    gcloud config set metrics/environment github_docker_image

#
# Other development tools
#

RUN gcloud components install app-engine-java

RUN curl https://dl.google.com/cloudsql/cloud_sql_proxy.linux.amd64 \
  > /usr/local/bin/cloud_sql_proxy && chmod +x /usr/local/bin/cloud_sql_proxy

# Create a gradle cache directory as a volume that can be read/written by any
# container (including containers running as any user -- hence the a+rwx)
RUN mkdir /.gradle && chmod a+rwx -R /.gradle
VOLUME /.gradle
ENV GRADLE_USER_HOME /.gradle

# It never makes sense for Gradle to run a daemon within a docker container.
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"

RUN curl https://services.gradle.org/distributions/gradle-4.3.1-bin.zip -L > /tmp/gradle.zip
WORKDIR /tmp
RUN unzip gradle.zip && rm gradle.zip \
  && mv gradle-* /gradle
ENV PATH="$PATH:/gradle/bin"
WORKDIR /

RUN gem install --no-document io-console

RUN gem install --no-document googleauth

RUN curl -O https://bin.equinox.io/c/htRtQZagtfg/rainforest-cli-stable-linux-amd64.tgz && \
  tar -xvf rainforest-cli-stable-linux-amd64.tgz && rm rainforest-cli-stable-linux-amd64.tgz && \
  mv rainforest /usr/local/bin

COPY with-mysql-login.sh /usr/local/bin
COPY with-uid.sh /usr/local/bin

ENTRYPOINT ["with-uid.sh"]
