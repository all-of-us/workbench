# Python client for All of Us Workbench Jupyter Notebooks

The client uses Python 2.7, to match FireCloud Jupyter environments.

## Local testing

### Install

```Shell
virtualenv venv
. venv/bin/activate
# Install a tagged version (preferred; also avoids downloading all history):
export PYCLIENT_VERSION=pyclient-v0-N-rcN  # your version here
pip install 'https://github.com/all-of-us/workbench/archive/${PYCLIENT_VERSION}.zip#egg=aou_workbench_client&subdirectory=client/py'
# Or install HEAD from a branch:
pip install -e 'git+https://github.com/all-of-us/workbench.git@generated-py-client#egg=aou_workbench_client&subdirectory=client/py'
```

### Authenticate

For local development, you can specify a local key file instead of using
application default credentials.

TODO(RW-32) Once available, switch the below to fetching the user's pet service
account key (as will be used in notebooks), instead of the application service
account key.

```Shell
api/project.rb get-service-creds --project all-of-us-workbench-test \
    --account $USER@pmi-ops.org
export GOOGLE_APPLICATION_CREDENTIALS=.../path/to/sa-key.json
```

### Run examples

Authenticate as above, then run [example.py](example.py):

```Shell
workbench/client$ ./project.rb swagger-regen
workbench/client$ py/example.py
```

## Releases

To publish a new version:
*   Regenerate Python Swagger client files.
*   Check them in on an arbitrary working branch (you can reuse
    `generated-py-client` for this).
*   Tag them as `pyclient-vN-N-rcN`, and push the tag.

Also, edit [setup.py](setup.py) and update `version=` (or else pip will not
overwrite old installed versions).

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
