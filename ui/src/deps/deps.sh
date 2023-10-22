#!/usr/bin/env bash

./project.rb tanagra-dep --env "$1"

yarn run codegen && yarn run build-terra-deps