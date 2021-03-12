#!/bin/bash
set -xv

# PR branch only
if [[ ! -z $CIRCLE_PULL_REQUEST ]]; then
  # Count total number of changed files that are not e2e and circleci
  CHANGES=$(git diff --name-only $(git merge-base origin/master ${CIRCLE_BRANCH}) -- . ':!e2e' ':!.circleci' | wc -l | xargs)
  # If count is zero, update WORKBENCH_ENV to "test"
  if [[ "$CHANGES" -eq 0 ]]; then
    echo "Set WORKBENCH_ENV=test"
    echo "export WORKBENCH_ENV=test" >> $BASH_ENV
    echo "export USER_NAME=${PUPPETEER_USER_TEST}@fake-research-aou.org" >> $BASH_ENV
    source $BASH_ENV
  fi
fi
exit 0
