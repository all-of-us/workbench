# Image used for deployment. The Circle image is not used here as it does not
# have an ARM build (M1 chip requires emulation).
# Keep synchronized with ci/Dockerfile.circle_build
FROM openjdk:11

RUN apt-get update \
  && apt-get install --no-install-recommends -yq \
  gettext \
  ruby \
  default-mysql-client \
  python3 \
  python3-pip \
  nodejs \
  npm \
  wait-for-it \
  zip \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/*

RUN npm install --global yarn n
# Upgrade to Node 14, above installs 10.
RUN n 14

ENV CLOUD_SDK_VERSION 392.0.0

WORKDIR /root
RUN wget https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz -O gcloud.tgz && \
  tar -xf gcloud.tgz && \
  ./google-cloud-sdk/install.sh  --quiet && \
  ./google-cloud-sdk/bin/gcloud components install app-engine-java && \
  rm -rf gcloud.tgz

ENV PATH=~/node/bin:~/google-cloud-sdk/bin:$PATH

RUN wget https://dl.google.com/cloudsql/cloud_sql_proxy.linux.amd64 -O /tmp/cloud_sql_proxy \
  && mv /tmp/cloud_sql_proxy /usr/local/bin && chmod +x /usr/local/bin/cloud_sql_proxy

# It never makes sense for Gradle to run a daemon within a docker container.
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"

ENV GRADLE_VERSION 7.6.1

RUN wget "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O /tmp/gradle.zip \
  && unzip /tmp/gradle.zip -d /opt/gradle
ENV PATH="/opt/gradle/gradle-${GRADLE_VERSION}/bin:${PATH}"

# Force a lower concurrent-ruby version, as we only have Ruby 2.3.
RUN gem install activesupport -v 6.1.4.6
RUN gem install jira-ruby
RUN gem install execjs


chmod +x deploy/libporject/some.js