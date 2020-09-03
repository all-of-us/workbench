#!/bin/bash

# Log all commands, logs are written to the Leo staging directory during startup
# and can be found via the Leo API's GetRuntimeResponse.asyncRuntimeFields.stagingBucket.
set -x

# Runs before every Jupyter notebook runtime startup, including both creation
# and resumes. This file is served as a static asset from the Workbench API
# server and its path is passed in as jupyterStartUserScriptUri during notebook
# runtime creation. This runs after initialize_notebook_runtime.sh on creation.

pushd /usr/local/share/wondershaper
wondershaper -a "eth0" -u 16384 # kilobits; 16Mib/s (2MiB/s)

popd
