#!/usr/bin/env bash

while getopts ":v:b:" flag; do
  case "${flag}" in
    v) # Deploy using tag
      version=${OPTARG}
      ;;
    b) # Deploy using branch
      branch=${OPTARG}
      ;;
    *) # Display help
      usage
      exit 0
      ;;
  esac
done

if [[ -n "${version}" && -n "${branch}" ]]; then
  echo "Please only provide version or branch as an arg"
  exit 0
fi

if [[ -n "${version}" ]]; then
  ./project.rb tanagra-dep --env local --version "$version"
elif [[ -n "${branch}" ]] ; then
  ./project.rb tanagra-dep --env local --branch "$branch"
else
  ./project.rb tanagra-dep --env local
fi

#update yarn
yarn

#generate Tanagra
npm install --prefix ../tanagra-aou-utils/tanagra/ui && npm run codegen --prefix ../tanagra-aou-utils/tanagra/ui

#start workbench and tanagra ui
concurrently "yarn run dev-up-local" "yarn run start-tanagra"