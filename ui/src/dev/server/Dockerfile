FROM node:alpine

# Java for Swagger client generation
RUN apk --no-cache add openjdk8-jre

# We touch yarn's config file here so that we can explicitly make sure
# yarn has permission to read/write to it, even though its empty.
# Without this yarn will complain it can't open its config file.
RUN mkdir -p /.cache/yarn /.yarn \
    && touch /.yarnrc \
    && chmod a+rwx /.cache/yarn /.yarn /.yarnrc

ADD with-uid.sh /usr/local/bin

ENTRYPOINT ["with-uid.sh"]
