#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail

echo "$SA_KEY_JSON" | gcloud auth activate-service-account "$SA_EMAIL" --key-file=-

set -xv

gsutil cp gs://all-of-us-workbench-test-credentials/.npmrc .
yarn install
yarn codegen
REACT_APP_ENVIRONMENT=test yarn run build --aot --no-watch --no-progress
