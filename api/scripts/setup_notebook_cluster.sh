#!/bin/bash
# Initializes a Jupyter notebook cluster. This file is copied to the GCS bucket <PROJECT>-scripts
# and its GCS path is passed in as jupyterUserScriptUri during notebook cluster creation.

pip install --upgrade 'https://github.com/all-of-us/pyclient/archive/pyclient-v1-1-rc4.zip#egg=aou_workbench_client&subdirectory=py'