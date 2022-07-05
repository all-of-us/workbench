#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail

echo "$SA_KEY_JSON" | gcloud auth activate-service-account "$SA_EMAIL" --key-file=-

set -v

PR_NUM="$(echo "$CIRCLE_PULL_REQUEST" | perl -ne '/(\d+)$/; print $1')"
PR_SITE_NUM="$(expr $PR_NUM % $PR_SITE_COUNT)"
gcloud app deploy --project=all-of-us-workbench-test --version="pr-$PR_SITE_NUM" --no-promote

