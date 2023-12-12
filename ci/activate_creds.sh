#!/bin/bash -e

# Creates credentials file $1 from two environment variables (see
# below) which combine to decrypt the keys for a service account.
# Does gcloud auth using the result.

# Default values for environment variables
CREDENTIALS_ENV_VAR_NAME=${2:-GCLOUD_CREDENTIALS}
KEY_ENV_VAR_NAME=${3:-GCLOUD_CREDENTIALS_KEY}

if [ ! "${!CREDENTIALS_ENV_VAR_NAME}" ]; then
  echo "No ${CREDENTIALS_ENV_VAR_NAME} env var defined, aborting creds activation."
  exit 1
fi

echo "${!CREDENTIALS_ENV_VAR_NAME}" | \
     openssl enc -d -md sha256 -aes-256-cbc -base64 -A -k "${!KEY_ENV_VAR_NAME}" \
     > $1

gcloud auth activate-service-account --key-file $1

