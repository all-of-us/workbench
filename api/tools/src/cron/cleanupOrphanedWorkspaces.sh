#!/bin/bash

curl -X GET 'http://localhost:8081/v1/cron/cleanupOrphanedWorkspaces' \
  --header "X-AppEngine-Cron: true"
