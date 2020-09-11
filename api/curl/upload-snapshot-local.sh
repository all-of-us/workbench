#!/bin/bash

# Call the cron endpoint on localhost to take a reporting snapshot & upload it to BigQuery.
curl -X GET 'http://localhost:8081/v1/cron/uploadReportingSnapshot' \
  --header "X-AppEngine-Cron: true" \
  --header "Authorization: Bearer `gcloud auth print-access-token`" \
   | jq
