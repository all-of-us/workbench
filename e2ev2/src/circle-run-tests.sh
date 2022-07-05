#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

# https://github.com/nodejs/node/discussions/43184?sort=new
cat /etc/ssl/openssl.cnf | grep -v 'providers = provider_sect' > openssl.cnf
sudo cp openssl.cnf /etc/ssl/openssl.cnf

mkdir screenshots

yarn install

PR_NUM="$(echo "$CIRCLE_PULL_REQUEST" | perl -ne '/(\d+)$/; print $1')"
PR_SITE_NUM="$(expr $PR_NUM % $PR_SITE_COUNT)"
export UI_HOSTNAME=pr-"$PR_SITE_NUM"-dot-all-of-us-workbench-test.appspot.com
export PUPPETEER_EXECUTABLE_PATH=/usr/bin/google-chrome
export JEST_SILENT_REPORTER_DOTS=true
export JEST_SILENT_REPORTER_SHOW_PATHS=true
yarn test --reporters=jest-silent-reporter
