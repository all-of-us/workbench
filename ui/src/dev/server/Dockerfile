FROM node:alpine

# We set the UID to the host's UID to ensure written files get proper permissions. The default UID
# for node is 1000, which conflicts with some of our host machines.
RUN apk --no-cache add shadow
RUN groupmod -g 666 node \
  && usermod -u 666 -g 666 node

#
# https://github.com/GoogleCloudPlatform/cloud-sdk-docker/blob/master/alpine/Dockerfile
#

# TODO: update this to a later version
ENV CLOUD_SDK_VERSION 165.0.0

ENV PATH /google-cloud-sdk/bin:$PATH

RUN apk --no-cache add \
        curl \
        python \
        py-crcmod \
        bash \
        libc6-compat \
        openssh-client \
        git \
    && curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
    tar xzf google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
    rm google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
    ln -s /lib /lib64 && \
    gcloud config set core/disable_usage_reporting true && \
    gcloud config set component_manager/disable_update_check true && \
    gcloud config set metrics/environment github_docker_image

# Java for Swagger client generation
RUN apk --no-cache add openjdk8-jre

# We touch yarn's config file here so that we can explicitly make sure
# yarn has permission to read/write to it, even though its empty.
# Without this yarn will complain it can't open its config file.
RUN mkdir -p /.cache/yarn /.yarn \
    && touch /.yarnrc \
    && chmod a+rwx /.cache/yarn /.yarn /.yarnrc

RUN apk --no-cache add ruby ruby-json ruby-io-console

ADD with-uid.sh /usr/local/bin

ENTRYPOINT ["with-uid.sh"]
