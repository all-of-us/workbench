#!/bin/bash
set -xv

if [[ -z "$CIRCLE_BRANCH" ]]; then
  exit 0
fi

# Applying to master branch only: Exiting Puppeteer job early if certain conditions are met.
if [[ "$CIRCLE_BRANCH" != "master" ]]; then
  exit 0
fi

# On master branch, exiting CI job when all (changed) file names matches the ignore patterns.
# The grep command exits with '0' status when it's successful (match were found).
# While it exits with status '1' when no match was found.
git diff HEAD~1 --quiet --name-only | grep -qvFf .circleci/master-e2e-ignore-patterns.txt
STATUS=$?
if [[ "$STATUS" -eq 1 ]]; then
  echo "non-e2e application code are not changed in last commit on master branch. Exiting job."
  circleci-agent step halt
fi

# if no changes, exit 0. otherwise, exit 1
git diff HEAD~1 --quiet --name-only -- . ':!e2e'
STATUS=$?
if [[ "$STATUS" -eq 0 ]]; then
  echo "non-e2e application code are not changed in last commit on master branch. Exiting job."
  circleci-agent step halt
fi
