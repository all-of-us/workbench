#!/bin/bash
# List all the Stackdriver resource groups in the project ID given by $1
curl -X GET "https://monitoring.googleapis.com/v3/projects/$1/groups" \
  --header "Authorization: Bearer $(gcloud auth print-access-token)"

