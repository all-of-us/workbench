#!/usr/bin/env bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

gcloud app deploy --project=all-of-us-workbench-test --version="pr-$PR_SITE_NUM" --no-promote

