#!/bin/bash -e

./ci/activate_creds.sh ~/gcloud-credentials.key
./tools/deploy.py \
  --target $1 \
  --skip-confirmation \
  --project all-of-us-workbench-test \
  --account circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com
