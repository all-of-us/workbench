#!/bin/bash

curl -X GET 'http://localhost:8081/v1/cron/ensureTestUserTosCompliance' \
  --header "X-AppEngine-Cron: true"
