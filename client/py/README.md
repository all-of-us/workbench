# Python client for All of Us Workbench Jupyter Notebooks

Usage:

  virtualenv venv
  . venv/bin/activate
  pip install -e 'git+https://github.com/all-of-us/workbench.git@generated-py-client#egg=aou_workbench_client&subdirectory=client/py'

then

  import aou_workbench_client
  from aou_workbench_client import swagger_client
