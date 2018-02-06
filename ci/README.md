# Utilities for CI

We perform a CI build in CircleCI using a custom build image that incorporates
Java, node, gcloud, and headless browsers. This images is built from
`Dockerfile.circle_build` (see comments in that file for details) and pushed to
Docker Hub at https://hub.docker.com/r/allofustest/workbench/ .
