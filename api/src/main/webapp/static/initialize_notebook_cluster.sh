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

# Enable any built-in extensions. Snippets menu is used for AoU-specific code
# snippet insertion, see README.md for more details.
jupyter nbextension enable snippets_menu/main

# Section represents the jupyter page to which the extension will be applied to
jupyter nbextension enable aou-upload-policy-extension/main --section=tree

