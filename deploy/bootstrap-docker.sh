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
git fetch
git checkout "${WORKBENCH_VERSION}"
git submodule update --init --recursive
# Drop any ignored files which may have carried over, to ensure a clean build.
git clean -fX
git status

exec "$@"
