#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

gsutil cp gs://all-of-us-workbench-test-credentials/.npmrc .
yarn install
yarn deps
CI=false REACT_APP_ENVIRONMENT=test yarn run build --aot --no-watch --no-progress

