#!/bin/bash

curl -X POST "https://monitoring.googleapis.com/v1/projects/$(gcloud config get-value project)/dashboards" \
  -d @$1 \
  --header "Authorization: Bearer $(gcloud auth print-access-token)" \
  --header "Content-Type: application/json"

