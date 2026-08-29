#!/bin/bash

# Applying to main branch only: Exiting deploy UI and API jobs early if last commit message contains "skip-deploy" string.
COMMIT_MESSAGE=$(git log -1 --pretty=format:"%s")

# Double comma is "Parameter Expansion". It converts string to lowercase letters.
if [[ "${COMMIT_MESSAGE,,}" == *"skip-deploy"* ]]; then
  echo "[skip-deploy] found in commit message. Deploy job will not run."
  circleci-agent step halt
fi
