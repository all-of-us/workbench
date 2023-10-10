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
openapi-generator-cli generate -i ../tanagra-aou-utils/tanagra/service/src/main/resources/api/service_openapi.yaml -g typescript-fetch  --additional-properties=typescriptThreePlus=true -o src/tanagra-generated

#start Tanagra
npm install --prefix ../tanagra-aou-utils/tanagra/ui && npm run codegen --prefix ../tanagra-aou-utils/tanagra/ui && BROWSER='none' REACT_APP_POST_MESSAGE_ORIGIN=http://localhost:4200 npm start --prefix ../tanagra-aou-utils/tanagra/ui

#start workbench ui
yarn run dev-up-local