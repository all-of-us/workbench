#!/bin/bash -e

# Creates credentials file $1 from two environment variables (see
# below) which combine to decrypt the keys for a service account.
# Does gcloud auth using the result.
if [ ! "${GCLOUD_CREDENTIALS}" ]
then
  echo "No GCLOUD_CREDENTIALS env var defined, aborting creds activation."
  exit 1
fi

echo $GCLOUD_CREDENTIALS | \
     openssl enc -d -md sha256 -aes-256-cbc -base64 -A -k "${GCLOUD_CREDENTIALS_KEY}" \
     > $1

gcloud auth activate-service-account --key-file $1

