#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

yarn install
yarn deps test
REACT_APP_ENVIRONMENT=test yarn run build --aot --no-watch --no-progress

