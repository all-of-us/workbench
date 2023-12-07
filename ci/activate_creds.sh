#!/bin/bash -e

# Creates credentials file $1 from two environment variables (see
# below) which combine to decrypt the keys for a service account.
# Does gcloud auth using the result.
if [ ! "${SERVICE_ACCOUNT_CREDENTIALS_DEPLOY_STABLE_ENV}" ]
then
  echo "No SERVICE_ACCOUNT_CREDENTIALS_DEPLOY_STABLE_ENV env var defined, aborting creds activation."
  exit 1
fi

echo $SERVICE_ACCOUNT_CREDENTIALS_DEPLOY_STABLE_ENV | \
     openssl enc -d -md sha256 -aes-256-cbc -base64 -A -k "${SERVICE_ACCOUNT_CREDENTIALS_DEPLOY_STABLE_ENV_KEY}" \
     > $1

gcloud auth activate-service-account --key-file $1

