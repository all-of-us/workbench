#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

# https://github.com/nodejs/node/discussions/43184?sort=new
cat /etc/ssl/openssl.cnf | grep -v 'providers = provider_sect' > openssl.cnf
sudo cp openssl.cnf /etc/ssl/openssl.cnf

mkdir screenshots

yarn install

# Override hostname to be an authorized origin for Google Sign-In.
SHORT_HASH="$(git log -n 1 --pretty='format:%C(auto)%h')"
PR_SITE_IP="$(ping -c 1 pr-"$SHORT_HASH"-dot-all-of-us-workbench-test.appspot.com \
  | head -n 1 | perl -ne '/[(](.*?)[)]/ && print $1')"
cp /etc/hosts hosts
echo "$PR_SITE_IP" all-of-us-workbench-test.appspot.com >> hosts
sudo mv hosts /etc/hosts

export PUPPETEER_EXECUTABLE_PATH=/usr/bin/google-chrome
export JEST_SILENT_REPORTER_DOTS=true
export JEST_SILENT_REPORTER_SHOW_PATHS=true
yarn test --reporters=jest-silent-reporter
