#!/bin/bash

set +e
set -u

#
# Note: if the project does not exist, it'll fail with something like ->
# ERROR: Some requests did not succeed:
# - Project {PROJECT_ID} is not found and cannot be used for API calls

# The script should continue to run when it gets an error

ENV=$1

docker run --rm -v $HOME:/root broadinstitute/dsde-toolbox mysql-connect.sh -p firecloud -a rawls -e $ENV \
  "select NAME from BILLING_PROJECT where creation_status=\"Ready\" UNION select GOOGLE_PROJECT as NAME from WORKSPACE" > projects.tsv

while read PROJECT
do
	echo "enabling Google Life Sciences API for $PROJECT"
	gcloud services enable lifesciences.googleapis.com --project=$PROJECT
done < projects.tsv

set -e
