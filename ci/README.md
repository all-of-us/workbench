# Utilities for CI

We perform a CI build in CircleCI using a custom build image that incorporates
Java, node, gcloud, and headless browsers. This images is built from
`Dockerfile.circle_build` (see comments in that file for details) and pushed to
GCR at https://us.gcr.io/broad-dsp-gcr-public/workbench/ .
