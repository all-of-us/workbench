FROM node:alpine

# Java for Swagger client generation
RUN apk --no-cache add openjdk8-jre

RUN mkdir -p /.cache/yarn /.yarn                        \
    && echo '--install.no-lockfile true' > /.yarnrc     \
    && chmod a+rwx /.cache/yarn /.yarn /.yarnrc

ADD with-uid.sh /usr/local/bin

ENTRYPOINT ["with-uid.sh"]
