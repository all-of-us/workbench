#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

TERRA_UI_GIT_HASH=0cfd092effbc6427ee15c8c1f05e48b03d168532

WD="$(dirname "$0")"
cd "$WD"

yarn install

# Making this a hidden directory avoids having to exclude it from the parent build.
REPODIR=.repo

set +e # ignore if repo already exists
# The date specified here must be early-enough to contain the specified hash.
git clone -n --shallow-since='2023-01-01T00:00' \
  https://github.com/DataBiosphere/terra-ui.git \
  $REPODIR
set -e

(cd $REPODIR; git checkout --detach $TERRA_UI_GIT_HASH)

(cd $REPODIR; yarn install)

# Apply patches. See comments in individual files.

bash patches/commentoutbadimportline.sh

sh patches/removereactcomponentfromsvgimports.sh $REPODIR/src/components/CloudProviderIcon.ts
sh patches/removereactcomponentfromsvgimports.sh $REPODIR/src/libs/icon-dict.js

sh patches/adjusttstarget.sh $REPODIR/tsconfig.json

# Provide an empty config. Esbuild replaces the existing one with this.
echo 'export const loadedConfigStore = { current: {} }' > $REPODIR/src/configStoreEmpty.js

(cd $REPODIR; node ../esbuild.js)

sh patches/restorerequire.sh out/Environments.js
