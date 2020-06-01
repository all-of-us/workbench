#!/bin/bash

# Get the IAM roles for project $1 with filter $2. Usage:
# ./list-service-account-roles.sh my-project-id user:that.user@domain.biz
gcloud projects get-iam-policy $1  \
--flatten="bindings[].members" \
--format='table(bindings.role)' \
--filter="bindings.members:$2"
