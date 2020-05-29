#!/usr/bin/env bash


curl -X GET "https://api-dot-all-of-us-workbench-test.appspot.com/v1/admin/workspaces/$1/audit?after=0&limit=50" \
   --header "Authorization: Bearer `gcloud auth print-access-token`" \
   | jq

