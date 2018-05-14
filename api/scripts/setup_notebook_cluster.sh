#!/bin/bash
# Initializes a Jupyter notebook cluster. This file is copied to the GCS bucket <PROJECT>-scripts
# and its GCS path is passed in as jupyterUserScriptUri during notebook cluster creation.

# As of initial Workbench launch, we will not be offering or have a need for
# Spark on notebooks clusters. Disable the kernels to avoid presenting spurious
# kernel options to researchers. See https://github.com/DataBiosphere/leonardo/issues/321.
jupyter kernelspec uninstall -f pyspark2
jupyter kernelspec uninstall -f pyspark3

for v in "2.7" "3.4"; do
  "pip${v}" install --upgrade 'https://github.com/all-of-us/pyclient/archive/pyclient-v1-9.zip#egg=aou_workbench_client&subdirectory=py'
done
