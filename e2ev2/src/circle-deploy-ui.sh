#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail

echo "$SA_KEY_JSON" | gcloud auth activate-service-account "$SA_EMAIL" --key-file=-

set -v

gcloud app versions list --service=default --project=all-of-us-workbench-test \
  --sort-by=version.createTime --filter='id~pr-[a-z0-9]{7}' --format='value(id)' \
  > deployed-pr-versions.txt


SHORT_HASH="$(git log -n 1 --pretty='format:%C(auto)%h')"
gcloud app deploy --project=all-of-us-workbench-test --version="pr-$SHORT_HASH" --no-promote

# Warm it up:
curl https://"pr-$SHORT_HASH"-dot-all-of-us-workbench-test.uc.r.appspot.com/
