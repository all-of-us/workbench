#!/bin/bash -e

./ci/install_gcloud.sh && source ~/.bashrc
./ci/activate_creds.sh ~/gcloud-credentials.key

gcloud components install app-engine-java

./tools/deploy.py \
  --target $1 \
  --skip-confirmation \
  --project all-of-us-workbench-test \
  --account circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com

