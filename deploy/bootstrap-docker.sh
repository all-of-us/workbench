#!/bin/bash
set -e

if [[ -z "${WORKBENCH_VERSION}" ]]; then
  echo "missing required env var WORKBENCH_VERSION" 1>&2
  exit 1
fi

if [[ ! -d ~/workbench/.git ]]; then
  git clone https://github.com/all-of-us/workbench ~/workbench
fi
cd ~/workbench

# Get all tags; by default only tags from active remote branches are fetched.
# In the case of a cherry pick, the original branch may not exist or may have
# already been deleted.
git fetch --tags

# Drop any untracked/ignored files which may have carried over, to ensure a clean build.
git clean -fdx

# To test deploy scripts push changes to a branch and use it as
# git checkout "<your branch name>"
git checkout "${WORKBENCH_VERSION}"
git submodule update -f --init --recursive
git status

exec "$@"
