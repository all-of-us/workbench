#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

# https://github.com/nodejs/node/discussions/43184?sort=new
cat /etc/ssl/openssl.cnf | grep -v 'providers = provider_sect' > openssl.cnf
sudo cp openssl.cnf /etc/ssl/openssl.cnf

mkdir screenshots

yarn install

export UI_HOSTNAME=pr-"$PR_SITE_NUM"-dot-all-of-us-workbench-test.appspot.com
export PUPPETEER_EXECUTABLE_PATH=/usr/bin/google-chrome
export JEST_SILENT_REPORTER_DOTS=true
export JEST_SILENT_REPORTER_SHOW_PATHS=true

BKT_ROOT=gs://all-of-us-workbench-test.appspot.com/circle-failed-tests
FAILED_TESTS=$(gsutil cat $BKT_ROOT/\*.$CIRCLE_SHA1.txt || echo -n)
gsutil rm $BKT_ROOT/\*.$CIRCLE_SHA1.txt || true

set +e
if [[ -z $FAILED_TESTS ]]; then
  yarn test $FAILED_TESTS \
    --reporters=jest-silent-reporter --reporters=./src/failure-reporter.js
else
  yarn test --reporters=jest-silent-reporter --reporters=./src/failure-reporter.js
fi
TESTS_EXIT_CODE=$?
set -e

if [[ $TESTS_EXIT_CODE -ne 0 ]]; then
  gsutil cp failed-tests.txt $BKT_ROOT/$CIRCLE_BUILD_NUM.$CIRCLE_SHA1.txt || true

  # Collect garbage
  gsutil ls $BKT_ROOT | tail -n 10 > latest.txt
  gsutil ls $BKT_ROOT | grep -v -F -f latest.txt | gsutil rm || true
fi

exit $TESTS_EXIT_CODE
