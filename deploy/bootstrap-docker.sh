#!/bin/bash
set -e

if [[ -z "${WORKBENCH_VERSION}" ]]; then
  echo "missing required env var WORKBENCH_VERSION" 1>&2
  exit 1
fi

# Coerce some of the volume permissions to be available to our docker user
# "circleci" (group "circleci"). Use group for this creds file as the calling
# script will want to maintain ownership to delete it afterwards.
sudo chgrp circleci /creds/sa-key.json
sudo chmod g+r /creds/sa-key.json
sudo chown -R circleci /.gradle

if [[ ! -d ~/workbench/.git ]]; then
  sudo git clone https://github.com/all-of-us/workbench ~/workbench
  sudo chown -R circleci ~/workbench
fi
cd ~/workbench

# Get all tags; by default only tags from active remote branches are fetched.
# In the case of a cherry pick, the original branch may not exist or may have
# already been deleted.
git fetch --tags

# Drop any untracked/ignored files which may have carried over, to ensure a clean build.
git clean -fdx

git checkout "${WORKBENCH_VERSION}"
git submodule update --init --recursive
git status

exec "$@"
