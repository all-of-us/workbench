#!/bin/bash
set -e

# This script makes CircleCI exit a non pull-request job.

if [[ ${CIRCLE_PULL_REQUEST} ]]; then
  echo "CIRCLE_PULL_REQUEST: ${CIRCLE_PULL_REQUEST}"
  regexp="[[:digit:]]\+$"
  PR_NUMBER=`echo $CIRCLE_PULL_REQUEST | grep -o $regexp`
  echo "PR_NUMBER: ${PR_NUMBER}"
fi

# puppeteer-e2e-local-* jobs to skip running if this is not a pull request.
if [[ -z "${CIRCLE_PULL_REQUEST}" ]] || ([[ ${CIRCLE_BRANCH} != "" ]] && [[ ${CIRCLE_BRANCH} == "master" ]]); then
	echo "Not a pull request, stop job now."
	circleci step halt
fi
