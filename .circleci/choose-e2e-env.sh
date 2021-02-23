#!/bin/bash
set -xv

if [[ ! -z $CIRCLE_PULL_REQUEST ]]; then
  # Count total number of files changed
  CHANGED_COUNT=$(git diff --name-only $(git merge-base origin/master ${CIRCLE_BRANCH}) | wc -l | xargs)

  # Count total number of e2e files
  E2E_CHANGED_COUNT=$(git diff --name-only $(git merge-base origin/master ${CIRCLE_BRANCH}) -- e2e | wc -l | xargs)

  # If e2e files are the only files that have changed, update WORKBENCH_ENV to "test"
  if [ $E2E_CHANGED_COUNT -gt 0 ] && [ $E2E_CHANGED_COUNT -eq $CHANGED_COUNT ]; then
    echo "export WORKBENCH_ENV=test" >> $BASH_ENV
    echo "export USER_NAME=${PUPPETEER_USER_TEST}@fake-research-aou.org" >> $BASH_ENV
    source $BASH_ENV
  fi
fi
exit 0
