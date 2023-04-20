#!/bin/bash

# Log all commands, logs are written to the Leo staging directory during startup
# and can be found via the Leo API's GetRuntimeResponse.asyncRuntimeFields.stagingBucket.
set -x

# Initializes a Jupyter notebook runtime. This file is hosted from the Workbench
# API server and its path is passed in as jupyterUserScriptUri during notebook
# runtime creation.

# Running these commands with `sudo -E -u jupyter` acts as the Jupyter user,
# which is preferred where possible to ensure that the Jupyter user can access
# any files or directories created below.

# Enable any built-in extensions. Snippets menu is used for AoU-specific code
# snippet insertion, see README.md for more details.
# Note: keep the command line invocation here in-sync with how Leo installs its
# own default extensions: https://github.com/DataBiosphere/terra-docker/blob/master/terra-jupyter-base/scripts/extension/install_jupyter_contrib_nbextensions.sh
sudo -E -u jupyter /opt/conda/bin/jupyter nbextension enable snippets_menu/main

# Enable activity checking on all views, not just notebooks, e.g. to capture terminal or tree interactions.
sudo -E -u jupyter /opt/conda/bin/jupyter nbextension enable activity-checker-extension/main --section=common

# Install on the tree view, which is the only place where this extension applies.
sudo -E -u jupyter /opt/conda/bin/jupyter nbextension enable aou-file-tree-policy-extension/main --section=tree

sudo -E -u jupyter /opt/conda/bin/nbstripout --install --global

# Setup gitignore to avoid accidental checkin of data on AoU. Ideally this would be
# configured on the image, per https://github.com/DataBiosphere/terra-docker/pull/234
# but that's no longer possible as the home directory is mounted at runtime.
ignore_file=/home/jupyter/gitignore_global

cat <<EOF | sudo -E -u jupyter tee ${ignore_file}
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

sudo -E -u jupyter /usr/bin/git config --global core.excludesfile ${ignore_file}

# Initialize a default nextflow config. Don't overwrite in case the user has
# customized their nextflow config file on their PD.
nextflow_config=/home/jupyter/.nextflow/config
if [ ! -f "${nextflow_config}" ]; then
  sudo -E -u jupyter mkdir /home/jupyter/.nextflow
  cat <<EOF | sudo -E -u jupyter tee "${nextflow_config}"
profiles {
  gls {
      process.executor = "google-lifesciences"
      process.container = "gcr.io/google-containers/ubuntu-slim:0.14"
      workDir = "${WORKSPACE_BUCKET}/workflows/nextflow-scratch"
      google.location = "us-central1"
      google.zone = "us-central1-a"
      google.project = "${GOOGLE_PROJECT}"
      google.enableRequesterPaysBuckets = true
      google.lifeSciences.debug = true
      google.lifeSciences.serviceAccountEmail = "${PET_SA_EMAIL}"
      google.lifeSciences.network = "network"
      google.lifeSciences.subnetwork = "subnetwork"
      google.lifeSciences.usePrivateAddress = false
      google.lifeSciences.copyImage = "gcr.io/google.com/cloudsdktool/cloud-sdk:alpine"
      google.lifeSciences.bootDiskSize = "20.GB"
  }
}
EOF
fi

# Initialize a default Cromwell config. Don't overwrite in case the user has
# customized their Cromwell config file on their PD.
cromwell_config=/home/jupyter/cromwell.conf
if [ ! -f "${cromwell_config}" ]; then
  cat <<EOF | sudo -E -u jupyter tee "${cromwell_config}"
include required(classpath("application"))

google {
  application-name = "cromwell"
  auths = [{
    name = "application_default"
    scheme = "application_default"
  }]
}

system {
  call-caching {
     enabled = true
     invalidate-bad-cache-results = true
  }
}

backend {
  default = "PAPIv2-beta"
  providers {

    # Disables the Local backend
    Local.config.root = "/dev/null"

    PAPIv2-beta {
      actor-factory = "cromwell.backend.google.pipelines.v2beta.PipelinesApiLifecycleActorFactory"

      config {
        project = "${GOOGLE_PROJECT}"
        concurrent-job-limit = 10
        root = "${WORKSPACE_BUCKET}/workflows/cromwell-executions"

        virtual-private-cloud {
          network-label-key = "vpc-network-name"
          subnetwork-label-key = "vpc-subnetwork-name"
          auth = "application_default"
        }

        genomics {
          auth = "application_default"
          compute-service-account = "${PET_SA_EMAIL}"
          endpoint-url = "https://lifesciences.googleapis.com/"
          location = "us-central1"
        }

        filesystems {
          gcs {
            auth = "application_default"
          }
        }
      }
    }
  }
}
EOF
fi

mkdir -p $HOME/bucket-folder
gcsfuse --foreground --debug_fuse --debug_fs --debug_gcs --debug_http ${WORKSPACE_BUCKET#*//} $HOME/bucket-folder
