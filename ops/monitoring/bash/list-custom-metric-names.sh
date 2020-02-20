#!/bin/bash

./list-custom-metrics.sh | jq  -r '.metricDescriptors[].name'
