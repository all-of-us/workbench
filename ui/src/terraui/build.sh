#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

# The date specified here must be early-enough to contain the specified hash.
git clone --shallow-since='2022-11-29T00:00' \
  https://github.com/DataBiosphere/terra-ui.git \
  .repo

(cd .repo; git checkout 6731645eeae3bbdbae08546f5d97ad6b3741fe86)

yarn install

(
  cd .repo;
  ../node_modules/.bin/esbuild --bundle --format=cjs --tsconfig=<(echo '{}') \
    --outdir=../out \
    --external:react --external:lodash/fp \
    src/libs/utils.js
)
