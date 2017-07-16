#!/bin/bash

# Run this script to upload service-account keys for gcloud on Circle CI.
# Arg: [path to gcloud service account key JSON file]
# Requires CI_TOKEN environment variable containing a valid Circle CI token.

set -e

function make_circle_envvar {
    curl -s -X DELETE \
        https://circleci.com/api/v1.1/project/$1/$2/$3/envvar/$4?circle-token=$CI_TOKEN

    curl -s -X POST  --header "Content-Type: application/json" \
        https://circleci.com/api/v1.1/project/$1/$2/$3/envvar?circle-token=$CI_TOKEN \
        -d "{\"name\": \"$4\", \"value\": \"$5\"}"
}

GCLOUD_CREDENTIALS_KEY=$(openssl rand -base64 32)
GCLOUD_CREDENTIALS=$(openssl enc -md sha256 -aes-256-cbc -in $1 -base64 -A  -k $GCLOUD_CREDENTIALS_KEY)

VCS_TYPE=github
CI_USERNAME=all-of-us
CI_PROJECT=workbench

echo "----- Environment vars to set in Circle CI Admin UI:"

echo "GCLOUD_CREDENTIALS=$GCLOUD_CREDENTIALS"
echo "GCLOUD_CREDENTIALS_KEY=$GCLOUD_CREDENTIALS_KEY"

make_circle_envvar $VCS_TYPE $CI_USERNAME $CI_PROJECT GCLOUD_CREDENTIALS $GCLOUD_CREDENTIALS
make_circle_envvar $VCS_TYPE $CI_USERNAME $CI_PROJECT GCLOUD_CREDENTIALS_KEY $GCLOUD_CREDENTIALS_KEY

echo "----- Created vars:"
curl https://circleci.com/api/v1.1/project/$VCS_TYPE/$CI_USERNAME/$CI_PROJECT/envvar?circle-token=$CI_TOKEN

echo "----- Decryption command:"
echo 'echo $GCLOUD_CREDENTIALS | openssl enc -d -md sha256 -aes-256-cbc -base64 -A -k $GCLOUD_CREDENTIALS_KEY'
