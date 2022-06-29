#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail

echo "$SA_KEY_JSON" | gcloud auth activate-service-account "$SA_EMAIL" --key-file=-

set -xv

SHORT_HASH="$(git log -n 1 --pretty='format:%C(auto)%h')"
gcloud app deploy --project=all-of-us-workbench-test --version="$SHORT_HASH" --no-promote
