#!/bin/bash
echo Getting details for $1.

# Get all of the custom Stackdriver Monitoring Metrics in the active project.
curl -X GET "https://monitoring.googleapis.com/v3/$1"  \
  --header "Authorization: Bearer $(gcloud auth print-access-token)"
