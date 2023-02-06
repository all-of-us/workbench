#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

WD="$(dirname "$0")"
cd "$WD"

TERRA_UI_GIT_HASH=$(< githash.txt)

# Making this a hidden directory avoids having to exclude it from the parent build.
REPODIR=.repo

if [[ -e out ]]; then
  echo Build artifacts directory exists. Delete "$WD"/out to rebuild.
  exit 0
fi

rm -rf $REPODIR

yarn install

# The date specified here must be early-enough to contain the specified hash.
git clone -n --shallow-since='2023-01-01T00:00' \
  https://github.com/DataBiosphere/terra-ui.git \
  $REPODIR

(cd $REPODIR; git checkout --detach $TERRA_UI_GIT_HASH)

# Apply patches. See comments in individual files.

sh patches/removenodeversioncheck.sh $REPODIR/.yarnrc.yml
(cd $REPODIR; yarn install)
bash patches/commentoutbadimportline.sh
sh patches/removereactcomponentfromsvgimports.sh $REPODIR/src/components/CloudProviderIcon.ts
sh patches/removereactcomponentfromsvgimports.sh $REPODIR/src/libs/icon-dict.js

sh patches/adjusttstarget.sh $REPODIR/tsconfig.json

# Provide an empty config. Esbuild replaces the existing one with this.
echo 'export const loadedConfigStore = { current: {} }' > $REPODIR/src/configStoreEmpty.js

(cd $REPODIR; node ../esbuild.js)

sh patches/restorerequire.sh out/Environments.js
