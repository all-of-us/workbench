#!/bin/bash

# Start hit the reporting snapshot & upload cron endpoint  locally.
curl -X GET 'http://localhost:8081/v1/cron/uploadReportingSnapshot' \
  --header "X-AppEngine-Cron: true" \
  --header "Authorization: Bearer `gcloud auth print-access-token`" \
   | jq
