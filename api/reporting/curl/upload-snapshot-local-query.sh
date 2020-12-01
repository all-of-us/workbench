#!/bin/bash

# Start hit the reporting snapshot & upload cron endpoint (insert query mode)
# locally.
curl -X GET 'http://localhost:8081/v1/cron/uploadReportingSnapshotQuery' \
  --header "X-AppEngine-Cron: true"
