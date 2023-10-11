#!/usr/bin/env bash

while getopts v: flag
do
  case "${flag}" in
    v) version=${OPTARG};;
  esac
done

cd ../tanagra-aou-utils
if [ ! -d "tanagra" ]; then
  echo "Cloning Tanagra repo"
  git clone https://github.com/DataBiosphere/tanagra.git
fi

cd tanagra
if [ -z "$version" ]; then
  git checkout main
else
  git checkout tags/"$version"
fi

cd ../../ui

#update yarn
yarn

#generate openapi
rm -rf src/tanagra-generated && openapi-generator-cli generate -i ../tanagra-aou-utils/tanagra/service/src/main/resources/api/service_openapi.yaml -g typescript-fetch  --additional-properties=typescriptThreePlus=true -o src/tanagra-generated

#generate Tanagra
npm install --prefix ../tanagra-aou-utils/tanagra/ui && npm run codegen --prefix ../tanagra-aou-utils/tanagra/ui

#start workbench and tanagra ui
concurrently "yarn run dev-up-local" "yarn run start-tanagra"