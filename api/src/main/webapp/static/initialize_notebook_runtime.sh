#!/bin/bash

# Log all commands, logs are written to the Leo staging directory during startup
# and can be found via the Leo API's GetRuntimeResponse.asyncRuntimeFields.stagingBucket.
set -x

# Initializes a Jupyter notebook runtime. This file is hosted from the Workbench
# API server and its path is passed in as jupyterUserScriptUri during notebook
# runtime creation.

# Enable any built-in extensions. Snippets menu is used for AoU-specific code
# snippet insertion, see README.md for more details.
# Note: keep the command line invocation here in-sync with how Leo installs its
# own default extensions: https://github.com/DataBiosphere/terra-docker/blob/master/terra-jupyter-base/scripts/extension/install_jupyter_contrib_nbextensions.sh
sudo -E -u jupyter /opt/conda/bin/jupyter nbextension enable snippets_menu/main

# Section represents the jupyter page to which the extension will be applied to
sudo -E -u jupyter /opt/conda/bin/jupyter nbextension enable aou-upload-policy-extension/main --section=tree

# Setup gitignore to avoid accidental checkin of data on AoU. Ideally this would be
# configured on the image, per https://github.com/DataBiosphere/terra-docker/pull/234
# but that's no longer possible as the home directory is mounted at runtime.
ignore_file=/home/jupyter/gitignore_global
cat <<EOF > ${ignore_file}
# By default, all files should be ignored by git.
# We want to be sure to exclude files containing data such as CSVs and images such as PNGs.
*.*
# Now, allow the file types that we do want to track via source control.
!*.ipynb
!*.py
!*.r
!*.R
!*.wdl
!*.sh
# Allow documentation files.
!*.md
!*.rst
!LICENSE*
EOF
chown jupyter:users ${ignore_file}
git config --global core.excludesfile ${ignore_file}
