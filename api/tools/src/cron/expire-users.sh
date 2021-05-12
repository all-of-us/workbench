#!/bin/bash

# Start hit the reporting snapshot & upload cron endpoint (insert query mode)
# locally.
curl -X GET 'http://localhost:8001/v1/cron/expireNonCompliantUsers' \
  --header "X-AppEngine-Cron: true"