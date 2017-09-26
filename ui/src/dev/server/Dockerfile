FROM node:alpine

# Java for Swagger client generation
RUN apk --no-cache add openjdk8-jre

# These must be populated on either the build command line or via
# docker-compose
ARG UID
ARG GID

# No sudo in the node alpine image, so explicitly change user
USER 0
RUN mkdir /.config /.npm
RUN chmod a+rwx /.config /.npm
RUN chown -R $UID:$GID /.config /.npm
# and reset user to local user's ID and group ID
USER $UID:$GID
