#!/bin/bash

# Log all commands, logs are written to the Leo staging directory during startup
# and can be found via the Leo API's Cluster.stagingBucket.
set -x

# Initializes a Jupyter notebook cluster. This file is copied to the GCS bucket
# <PROJECT>-cluster-resources and its GCS path is passed in as
# jupyterUserScriptUri during notebook cluster creation.

# As of initial Workbench launch, we will not be offering or have a need for
# Spark on notebooks clusters. Disable the kernels to avoid presenting spurious
# kernel options to researchers. See https://github.com/DataBiosphere/leonardo/issues/321.
jupyter kernelspec uninstall -f pyspark2
jupyter kernelspec uninstall -f pyspark3

# reticulate is our preferred access method for the AoU client library - default
# to python3 as our pyclient has better support for python3. Rprofile is executed
# each time the R kernel starts.
echo "Sys.setenv(RETICULATE_PYTHON = '$(which python3)')" >> ~/.Rprofile

# Required for R summarytools to function. Must be installed from the cran
# repository, otherwise will receive install errors. See:
# https://github.com/DataBiosphere/leonardo/issues/813
# TODO: Figure out a less brittle approach here. The value "stretch-cran35" must
# match the repo used by Leo, so this could break on a Leo image change. Either
# push this into the Leo base image, set this repo name as an env var, or let
# users install this themselves via root (https://github.com/DataBiosphere/leonardo/issues/840)
apt-get update
apt-get -t stretch-cran35 install -y --no-install-recommends libmagick++-dev

for v in "2.7" "3"; do
  "pip${v}" install --upgrade 'https://github.com/all-of-us/pyclient/archive/pyclient-v1-17.zip#egg=aou_workbench_client&subdirectory=py'
done
