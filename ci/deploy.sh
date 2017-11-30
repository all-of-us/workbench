#!/bin/bash -e

./ci/activate_creds.sh ~/gcloud-credentials.key

if [ -z "${CIRCLE_TAG}" ]
then
  # On test, CircleCI automatically deploys all commits to master, for testing.
  # These versions need not persist, so give them all the same name.
  VERSION=circle-ci-test
else
  # For a tagged commit, deploy a version matching that tag. We tag some commits
  # on master to tell CircleCI to deploy them as named versions.
  VERSION=${CIRCLE_TAG}
fi
echo "Version: ${VERSION}"

(cd ./ui && ./project.rb deploy-ui --project all-of-us-workbench-test \
  --account circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com \
  --version $VERSION --promote)
