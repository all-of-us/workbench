#!/bin/bash

# List all of the Stackdriver Monitoring Dashboards in the active project.
curl "https://monitoring.googleapis.com/v1/projects/$1/dashboards" \
  --header "Authorization: Bearer $(gcloud auth print-access-token)" \
  --compressed
