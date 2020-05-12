#!/bin/bash

# Create a dashboard using the JSON filee given in $1 in the project ID $2
curl -X POST "https://monitoring.googleapis.com/v1/projects/$2/dashboards" \
  -d @$1 \
  --header "Authorization: Bearer $(gcloud auth print-access-token)" \
  --header "Content-Type: application/json"

