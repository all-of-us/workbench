#! /usr/bin/env python
"""
Swagger generated pip-installable client for the All of Us Workbench project
"""

# Developer's note on the install files:
# * This file (setup.py) provides the main driver and command line tool for
#   installation and packaging a release.  Use it like ` $ python setup.py
#   <command>`
# * `setup.cfg` provides default arguments to the commands put out by setup.py
# * `MANIFEST.in` details what files are and are not includes in a release
#   tarball

import re
from os.path import abspath, dirname, join

from setuptools import setup, find_packages

BASE_DIR = abspath(dirname(__file__))


with open(join(BASE_DIR, 'README.md'), 'r') as readme, \
        open(join(BASE_DIR, 'README.swagger.md')) as swagger_readme:
    readme_contents = readme.read()
    readme_contents += '\n\n'
    readme_contents += swagger_readme.read()

def find_version():
    with open(join(BASE_DIR, 'aou_workbench_client', '__init__.py')) as init:
        text = init.read()
        version = re.search(r"^__version__ = ['\"]([^'\"]*)['\"]", text, re.M)
    if version:
        return version.group(1)
    else:
        raise RuntimeError('No Package Version Found!')

setup(
    name='aou-workbench-client',
    version=find_version(),

    description=__doc__,
    long_description=readme_contents,

    # maintainer
    # maintainer email
    # author
    # author email
    # license

    url='https://github.com/all-of-us/workbench',

    packages=find_packages(include=['aou_workbench_client']),
    include_package_data=True,
    install_requires=[
        'oauth2client',
        # Swagger dependencies
        'certifi >= 14.05.14',
        'six >= 1.10',
        'python_dateutil >= 2.5.3',
        'setuptools >= 21.0.0',
        'urllib3 >= 1.15.1',
    ],
    classifiers=[
        'Development Status :: 3 - Alpha',
        'Intended Audience :: Developers',
        'Programming Language :: Python :: 2',
        'Programming Language :: Python :: 2.6',
        'Programming Language :: Python :: 2.7',

        # Needs a license
        #'License :: OSI Approved :: MIT License',
    ],
)
