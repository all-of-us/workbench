#!/bin/bash -e

./ci/activate_creds.sh ~/gcloud-credentials.key
if [ "$1" == "api" ]
then
  (cd ./api && ./project.rb run-cloud-migrations --project all-of-us-workbench-test \
    --creds_file ~/gcloud-credentials.key)
fi
./tools/deploy.py \
  --target $1 \
  --skip-confirmation \
  --project all-of-us-workbench-test \
  --account circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com
