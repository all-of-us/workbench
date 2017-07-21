#!/bin/bash -e

./ci/activate_creds.sh ~/gcloud-credentials.key
# Environment variables set in CI will populate stuff in here
./api/generate_appengine_web_xml.sh
./tools/deploy.py \
  --target $1 \
  --skip-confirmation \
  --project all-of-us-workbench-test \
  --account circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com
