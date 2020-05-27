#!/bin/bash
# filter out the name fields of the custom metrics
./list-custom-metrics.sh $1 | jq  -r '.metricDescriptors[].name'
