#!/bin/bash

# Start hit the reporting snapshot & upload cron endpoint (streaming mode)
# locally.
curl -X GET 'http://localhost:8081/v1/cron/uploadReportingSnapshotStreaming' \
  --header "X-AppEngine-Cron: true"
