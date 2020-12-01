#!/bin/bash

# Start hit the reporting snapshot & upload cron endpoint (configured implementation)
# locally.
curl -X GET 'http://localhost:8081/v1/cron/uploadReportingSnapshot' \
  --header "X-AppEngine-Cron: true"
