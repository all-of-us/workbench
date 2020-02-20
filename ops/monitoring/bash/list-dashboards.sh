#!/bin/bash

# List all of the Stackdriver Monitoring Dashboards in the active project.
curl "https://monitoring.googleapis.com/v1/projects/$(gcloud config get-value project)/dashboards" \
  --header "Authorization: Bearer $(gcloud auth print-access-token)" \
  --compressed
