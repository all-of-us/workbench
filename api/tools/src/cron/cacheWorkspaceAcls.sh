#!/bin/bash

curl -X GET 'http://localhost:8081/v1/cron/cacheWorkspaceAcls' \
  --header "X-AppEngine-Cron: true"
