#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail

echo "$SA_KEY_JSON" | gcloud auth activate-service-account "$SA_EMAIL" --key-file=-

set -v

# Collect garbage
gcloud app versions list --service=default --project=all-of-us-workbench-test \
  --sort-by=version.createTime --filter='id~pr-[a-z0-9]{7}' --format='value(id)' \
  > deployed-pr-versions.txt
MAX_PR_VERSIONS=20
VERSIONS_TO_DELETE=$(expr $(cat deployed-pr-versions.txt|wc -l) - $MAX_PR_VERSIONS)
if [[ $VERSIONS_TO_DELETE > 0 ]]; then
  echo deleting $VERSIONS_TO_DELETE old versions...
  cat deployed-pr-versions.txt | head -n $VERSIONS_TO_DELETE | \
    xargs gcloud app versions delete --service=default --project=all-of-us-workbench-test
fi

SHORT_HASH="$(git log -n 1 --pretty='format:%C(auto)%h')"
gcloud app deploy --project=all-of-us-workbench-test --version="pr-$SHORT_HASH" --no-promote

# Warm it up:
curl https://"pr-$SHORT_HASH"-dot-all-of-us-workbench-test.uc.r.appspot.com/
