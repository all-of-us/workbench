#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

gsutil cat gs://all-of-us-workbench-test-credentials/dot-npmrc-fontawesome-creds-line.txt \
  >> ~/.npmrc

yarn install
yarn deps test
REACT_APP_ENVIRONMENT=test yarn run build --aot --no-watch --no-progress

