# Python client for All of Us Workbench Jupyter Notebooks

Usage for local testing:

```Shell
virtualenv venv
. venv/bin/activate
# Install HEAD from a branch:
pip install -e 'git+https://github.com/all-of-us/workbench.git@generated-py-client#egg=aou_workbench_client&subdirectory=client/py'
# or install a tagged version (which also avoids downloading all history):
pip install 'https://github.com/all-of-us/workbench/archive/pyclient-v0-1-rc1.zip#egg=aou_workbench_client&subdirectory=client/py'
```

then

```Python
import aou_workbench_client
from aou_workbench_client import swagger_client
```

To publish a new version:
*   Regenerate Python Swagger client files.
*   Check them in on an arbitrary working branch (you can reuse
    `generated-py-client` for this).
*   Tag them as `pyclient-vN-N-rcN`, and push the tag.

```Shell
PYCLIENT_VERSION=pyclient-v0-N-rcN  # your version here
git checkout master
git pull
git checkout generated-py-client
git reset --hard master
git cherry-pick 2d1b2db97560cae505326ac2b157eaba2945829d  # remove gitignores
client/project.rb swagger-regen
git add client/py  # includes swagger_client, generated docs and requirements
git commit -a -m "Add auto-generated Python Swagger client ${PYCLIENT_VERSION}."
git tag ${PYCLIENT_VERSION}
git push --tags
```
