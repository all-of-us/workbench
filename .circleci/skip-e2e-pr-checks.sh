#!/bin/bash
set -xv

# Applying to PR branch only: Exiting Puppeteer job early if certain conditions are met.

if [[ ${CIRCLE_PULL_REQUEST} ]]; then
  echo "CIRCLE_PULL_REQUEST: ${CIRCLE_PULL_REQUEST}"
  regexp="[[:digit:]]\+$"
  PR_NUMBER=$(echo $CIRCLE_PULL_REQUEST | grep -o $regexp)
  echo "PR_NUMBER: ${PR_NUMBER}"
else
  exit 0
fi

# Get the commit message. Exiting job early if the commit message contains 'skip e2e' string.
COMMIT_MESSAGE=$(git log -1 --pretty=format:"%s")

# Double comma is "Parameter Expansion". It converts string to lowercase letters
if [[ "${COMMIT_MESSAGE,,}" == *"skip e2e"* ]]; then
  echo "Skip e2e text found in commit message"
  circleci-agent step halt
else
  echo "not found skip e2e text"
fi

# Exiting on PR branch when all (changed) file names matched ignore pattern.
# The grep command exits with '0' status when it's successful (match were found). While it exits with status '1' when no match was found.
git diff --name-only $(git merge-base origin/master ${CIRCLE_BRANCH}) | grep -qvFf .circleci/e2e-job-ignore-patterns.txt
STATUS=$?
if [[ "$STATUS" -eq 1 ]]; then
  echo "Workbench application code were not changed. Exiting job."
  circleci-agent step halt
fi
