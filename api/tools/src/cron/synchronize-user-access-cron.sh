#!/bin/bash

# Expire users who have not completed their compliance modules in over a year
curl -X GET 'http://localhost:8081/v1/cron/synchronizeUserAccess' \
  --header "X-AppEngine-Cron: true"