#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

gsutil cp gs://all-of-us-workbench-test-credentials/.npmrc .
echo "@terra-ui-packages:registry=https://us-central1-npm.pkg.dev/dsp-artifact-registry/terra-ui-packages" >> .npmrc

yarn install
yarn deps
REACT_APP_ENVIRONMENT=test yarn run build --aot --no-watch --no-progress

