#!/bin/bash
# Get all of the custom Stackdriver Monitoring Metrics in the active project.
readonly CUSTOM_METRIC_PREFIX="metric.type%20%3D%20starts_with%28%22custom.googleapis.com%2F%22%29"
curl -X GET "https://monitoring.googleapis.com/v3/projects/$(gcloud config get-value project)/metricDescriptors?filter=$CUSTOM_METRIC_PREFIX"  \
  --header "Authorization: Bearer $(gcloud auth print-access-token)"
