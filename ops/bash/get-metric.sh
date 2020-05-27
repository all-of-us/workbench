#!/bin/bash
echo Getting details for $1.

# Get all of the custom Stackdriver Monitoring Metrics in the active project.
# Usage ./get-metric.sh projects/my-project-name/metricDescriptors/custom.googleapis.com/metric_name
# Metric names can be fetched for a project using list-metrics.sh

curl -X GET "https://monitoring.googleapis.com/v3/$1"  \
  --header "Authorization: Bearer $(gcloud auth print-access-token)"
