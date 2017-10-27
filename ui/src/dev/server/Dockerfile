FROM node:alpine

# Java for Swagger client generation
RUN apk --no-cache add openjdk8-jre

RUN mkdir /.config &&  mkdir /.npm && chmod a+rwx /.config /.npm

ADD with-uid.sh /usr/local/bin

ENTRYPOINT ["with-uid.sh"]
