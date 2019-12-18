#!/bin/bash

# Log all commands, logs are written to the Leo staging directory during startup
# and can be found via the Leo API's Cluster.stagingBucket.
set -x

# Runs before every Jupyter notebook cluster startup, including both creation
# and resumes. This file is copied to the GCS bucket <PROJECT>-cluster-resources
# and its GCS path is passed in as jupyterStartUserScriptUri during notebook
# cluster creation. This runs after setup_notebook_cluster.sh on creation.

pushd /usr/local/share/wondershaper
wondershaper -a "eth0" -u 16384 # kilobits; 16Mib/s (2MiB/s)

popd
