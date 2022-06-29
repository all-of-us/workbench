#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

# https://github.com/nodejs/node/discussions/43184?sort=new
cat /etc/ssl/openssl.cnf | grep -v 'providers = provider_sect' > openssl.cnf
sudo cp openssl.cnf /etc/ssl/openssl.cnf

mkdir screenshots

yarn install

export PUPPETEER_EXECUTABLE_PATH=/usr/bin/google-chrome
JEST_SILENT_REPORTER_DOTS=true
JEST_SILENT_REPORTER_SHOW_PATHS=true
yarn test --reporters=jest-silent-reporter
