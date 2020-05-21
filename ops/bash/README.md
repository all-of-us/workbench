# Bash Ops Snippets
This directory has a smattering of bash goodies for working with GCP and Stackdriver Monitoring
especially.

## Prerequisites
* `bash`
* `curl`
* `gcloud`
* `jq`

# Installation
On checkout these will likely not be executable (on Mac/Linux). To fix that, do `chmod +x script.sh`
for each script.

## Auth
You'll need the ability to get an access token for the project you're signed into. Note
that for safety, it's best not to set a default project in `gcloud` if several of them
can be accessed by the same account. To do this you can simply run
```gcloud config unset core/project```. Where a project ID is needed, it can be obtained from
the list in `gcloud projects list`
