#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

# https://github.com/nodejs/node/discussions/43184?sort=new
cat /etc/ssl/openssl.cnf | grep -v 'providers = provider_sect' > openssl.cnf
sudo cp openssl.cnf /etc/ssl/openssl.cnf

mkdir screenshots

yarn install

export UI_URL_ROOT=https://pr-$PR_SITE_NUM-dot-all-of-us-workbench-test.appspot.com
echo $UI_URL_ROOT
export PUPPETEER_EXECUTABLE_PATH=/usr/bin/google-chrome
export JEST_SILENT_REPORTER_DOTS=true
export JEST_SILENT_REPORTER_SHOW_PATHS=true

BKT_ROOT=gs://all-of-us-workbench-test.appspot.com/circle-failed-tests
FAILED_TESTS=$(gsutil cat $BKT_ROOT/\*.$CIRCLE_SHA1.txt || echo -n)
gsutil rm $BKT_ROOT/\*.$CIRCLE_SHA1.txt || true

PROXY_PORT=8080
export API_PROXY_URL=http://localhost:$PROXY_PORT
export PROXY_MODE=replay-only

(cd ../api-proxy; node startproxy.mjs $PROXY_PORT api-proxy.log.txt &)

set +e
export FAILED_TESTS_LOG=failed-tests.txt
# This should do the right thing. If $FAILED_TESTS is empty, nothing is specified, so Jest runs
# all tests.
yarn test $FAILED_TESTS \
  --reporters=jest-silent-reporter --reporters=./src/failure-reporter.js
TESTS_EXIT_CODE=$?
set -e

killall node

if [[ $TESTS_EXIT_CODE -ne 0 ]]; then
  gsutil cp $FAILED_TESTS_LOG $BKT_ROOT/$CIRCLE_BUILD_NUM.$CIRCLE_SHA1.txt || true

  # Collect garbage
  gsutil ls $BKT_ROOT | tail -n 10 > latest.txt
  gsutil ls $BKT_ROOT | grep -v -F -f latest.txt | gsutil rm -I || true
fi

exit $TESTS_EXIT_CODE
