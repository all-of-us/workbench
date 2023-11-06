#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

TERRA_UI_GIT_HASH=4333c7b94d6ce10a6fe079361e98c2b6cc71f83a

WD="$(dirname "$0")"
cd "$WD"

# Making this a hidden directory avoids having to exclude it from the parent build.
REPODIR=.repo

if [[ -e $REPODIR ]]; then
  gitlogexitcode=$(cd $REPODIR; git log $TERRA_UI_GIT_HASH..$TERRA_UI_GIT_HASH>/dev/null; echo $?)
  if [[ $gitlogexitcode == 0 ]]; then
    echo Repo exists and is current. Delete "$WD"/$REPODIR to rebuild.
    exit 0
  else
    echo Repo is out of date. Delete "$WD"/$REPODIR to rebuild.
    exit 1
  fi
fi

yarn install

# The date specified here must be early-enough to contain the specified hash.
git clone -n --shallow-since='2023-05-01T00:00' \
  https://github.com/DataBiosphere/terra-ui.git \
  $REPODIR

(cd $REPODIR; git checkout --detach $TERRA_UI_GIT_HASH)

# Apply patches. See comments in individual files.

sh patches/removenodeversioncheck.sh $REPODIR/.yarnrc.yml
sh patches/rm-tests.sh

# the change in rm-tests causes yarn.lock to change - i.e. not immutable
(cd $REPODIR; YARN_ENABLE_IMMUTABLE_INSTALLS=false yarn install)

bash patches/commentoutbadimportline.sh
sh patches/removereactcomponentfromsvgimports.sh $REPODIR/src/components/CloudProviderIcon.ts
sh patches/removereactcomponentfromsvgimports.sh $REPODIR/src/libs/icon-dict.js

sh patches/adjusttstarget.sh $REPODIR/tsconfig.json

# Provide an empty config. Esbuild replaces the existing one with this.
echo 'export const loadedConfigStore = { current: {} }' > $REPODIR/src/configStoreEmpty.js

(cd $REPODIR; node ../esbuild.js)

sh patches/restorerequire.sh out/Environments.js
