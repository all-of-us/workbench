#!/bin/bash

# List all of the Stackdriver Monitoring Dashboard. First argument is dashboard name, formatted like
# projects/602460048110/dashboards/6849129967503530308
curl "https://monitoring.googleapis.com/v1/$1" \
  --header "Authorization: Bearer $(gcloud auth print-access-token)" \
  --compressed
