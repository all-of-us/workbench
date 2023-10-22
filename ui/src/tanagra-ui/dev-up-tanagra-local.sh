#!/usr/bin/env bash

while getopts v: flag
do
  case "${flag}" in
    v) version=${OPTARG};;
  esac
done

./project.rb tanagra-dep --env local --version "$version"

#update yarn
yarn

#generate Tanagra
npm install --prefix ../tanagra-aou-utils/tanagra/ui && npm run codegen --prefix ../tanagra-aou-utils/tanagra/ui

#start workbench and tanagra ui
concurrently "yarn run dev-up-local" "yarn run start-tanagra"