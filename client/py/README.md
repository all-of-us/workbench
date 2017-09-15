# Python client for All of Us Workbench Jupyter Notebooks

Usage:

  virtualenv venv
  . venv/bin/activate
  # Install HEAD from a branch:
  pip install -e 'git+https://github.com/all-of-us/workbench.git@generated-py-client#egg=aou_workbench_client&subdirectory=client/py'
  # or install a tagged version (which also avoids downloading all history):
  pip install 'https://github.com/all-of-us/workbench/archive/pyclient-v0-0-rc0.zip#egg=aou_workbench_client&subdirectory=client/py'

then

  import aou_workbench_client
  from aou_workbench_client import swagger_client
