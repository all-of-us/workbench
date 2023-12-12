#!/usr/bin/env bash

set -e

envs="local, test, staging, stable, preprod, prod"

if [[ -z ${1} || $envs != *"$1"* ]]; then
  echo "Please provide an environment arg (ex: yarn run deps local)"
  echo "Valid args are: $envs"
  exit 1
fi

yarn install

./project.rb tanagra-dep --env "$1"

yarn run codegen && yarn run build-terra-deps