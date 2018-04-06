#!/bin/bash
# Initializes a Jupyter notebook cluster. This file is copied to the GCS bucket <PROJECT>-scripts
# and its GCS path is passed in as jupyterUserScriptUri during notebook cluster creation.


for v in "2.7" "3.4"; do
  "pip${v}" install --upgrade 'https://github.com/all-of-us/pyclient/archive/pyclient-v1-4.zip#egg=aou_workbench_client&subdirectory=py'
done
