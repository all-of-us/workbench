#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

WD="$(dirname $0)"
cd "$WD"

# Making this a hidden directory avoids having to exclude it from the parent build.
REPODIR=.repo

set +e # ignore if repo already exists
# The date specified here must be early-enough to contain the specified hash.
git clone -n --shallow-since='2022-11-29T00:00' \
  https://github.com/DataBiosphere/terra-ui.git \
  $REPODIR
set -e

(cd $REPODIR; git checkout --detach 6731645eeae3bbdbae08546f5d97ad6b3741fe86)

yarn install

(
  cd $REPODIR;
  ../node_modules/.bin/esbuild --bundle --format=cjs --tsconfig=<(echo '{}') \
    --outdir=../out \
    --external:react --external:lodash/fp \
    src/libs/utils.js
)
