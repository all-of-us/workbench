#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

export REACT_APP_ENVIRONMENT="$1"
shift

gsutil cp gs://all-of-us-workbench-test-credentials/.npmrc .
yarn install
yarn deps
yarn run build --aot --no-watch --no-progress

