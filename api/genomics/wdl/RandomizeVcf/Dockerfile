FROM openjdk:8-jdk
ENV CLOUD_SDK_VERSION 260.0.0
RUN cd && \
  wget https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz -O gcloud.tgz && \
  tar -xf gcloud.tgz && \
  ./google-cloud-sdk/install.sh  --quiet && \
  ~/google-cloud-sdk/bin/gcloud components install app-engine-java && \
  rm -rf gcloud.tgz
RUN apt-get update
RUN apt-get install -y gettext ruby default-mysql-client python-pip wait-for-it
RUN pip install --upgrade pip pylint
ENV PATH=/home/circleci/node/bin:/home/circleci/google-cloud-sdk/bin:$PATH
RUN curl https://dl.google.com/cloudsql/cloud_sql_proxy.linux.amd64 > /tmp/cloud_sql_proxy \
  && mv /tmp/cloud_sql_proxy /usr/local/bin && chmod +x /usr/local/bin/cloud_sql_proxy
# It never makes sense for Gradle to run a daemon within a docker container.
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"
# We're not yet ready to upgrade to gradle 5 (the new default for apt-get).
ENV GRADLE_VERSION 4.10.3
RUN wget "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O /tmp/gradle.zip \
  && unzip /tmp/gradle.zip -d /opt/gradle
ENV PATH="/opt/gradle/gradle-${GRADLE_VERSION}/bin:${PATH}"
# Force a lower concurrent-ruby version, as we only have Ruby 2.3.
RUN gem install jira-ruby
RUN git clone https://github.com/all-of-us/workbench && cd workbench && git submodule update -f --init --recursive
RUN cd workbench/api && ./gradlew -p genomics build
