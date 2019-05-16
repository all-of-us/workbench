# setup_notebook_cluster.sh

This is the script run on the Leonardo Jupyter server at cluster initialization
time, i.e. the [jupyterUserScriptUri](
https://github.com/DataBiosphere/leonardo/blob/cfdbff2448b9cff73ad658ba028d1feafab01b81/src/main/resources/swagger/api-docs.yaml#L509).

## Local testing

To manually test updates to this script locally:

- Push the script to GCS (username-suffixed) and make it publicly readable:

  ```
  api$ gsutil cp cluster-resources/setup_notebook_cluster.sh "gs://all-of-us-workbench-test-cluster-resources/setup_notebook_cluster-${USER}.sh" &&
    gsutil acl ch -u AllUsers:R "gs://all-of-us-workbench-test-cluster-resources/setup_notebook_cluster-${USER}.sh"
  ```

- (**Disclaimer**: local code change, do not submit) Temporarily update your
  local server config to use your custom script:

  ```
  api$ sed -i "s,setup_notebook_cluster\.sh,setup_notebook_cluster-${USER}.sh," config/config_local.json
  ```

- Restart your dev API server and point a local UI to it
- Open your local Workbench UI: top-right dropdown -> settings -> reset notebook server
- Wait for the notebook cluster to be created.
  - Cluster creation will fail with 500s if the user script is not accessible,
    ensure your script is publicly readable via [cloud console UI](
    https://console.cloud.google.com/storage/browser/all-of-us-workbench-test-cluster-resources?project=all-of-us-workbench-test)
- Revert changes to `config/config_local.json`

## Debugging Script Issues

- Find your existing notebook cluster if any, by authorizing as your
  fake-research-aou.org user [here](
  https://leonardo.dsde-dev.broadinstitute.org/#!/cluster/listClusters).
- Call `listClusters` and take the returned `stagingBucket` value.
- `gcloud auth login USER@fake-research-aou.org`
- `gsutil ls gs://STAGING_BUCKET`
- Dig through the directories until you find the initialization script output
  log, as of 4/3/19 the file was named `dataproc-initialization-script-0_output`


# playground-extension.js

Jupyter UI extension for playground mode. Passed via GCS at cluster creation time.

## Local testing

Tweak the above instructions for testing the user script to push a modified
extension and modify the cluster controller to use it.

# snippets-menu

AoU Clusters enable the [Snippets Menu extension](https://jupyter-contrib-nbextensions.readthedocs.io/en/latest/nbextensions/snippets_menu/readme.html)
with custom AoU-specific code snippets. The snippets live in
https://github.com/all-of-us/workbench-snippets. AoU configures Leo clusters as
follows to enable this:

1. Enable the snippets_menu/main extension in ./setup_notebook_cluster.sh
1. Deploy a Jupyter UI extension to configure the menu with AoU-specific snippets

## Updating Snippets

As code snippets are updated in the source repository, we will want to
periocially update the menus in the Workbench. For now, this process is fairly
manual and can be improved going forwards:

Prerequisite: Must have `jq` installed.

1. In a separate directory, clone the snippets repo and build the menu config:
    https://github.com/all-of-us/workbench-snippets#auto-generation-of-jupyter-snippets-menu-configuration
1. Pull the updated JSON into the Workbench repo

    ```
    export SNIPPETS_JSON="<path to above generated JSON>"
    jq '."sub-menu"[0]' "${SNIPPETS_JSON}" > $(git rev-parse --show-toplevel)/api/cluster-resources/r-snippets-menu.json
    jq '."sub-menu"[1]' "${SNIPPETS_JSON}" > $(git rev-parse --show-toplevel)/api/cluster-resources/py-snippets-menu.json
    ```
1. Commit changes and go through normal pull request process.
1. Wait for a release; note that changes are only visible for clusters started
    after the release.

## Snippets Extension Implementation

The snippets menu extension Javascript is generated dynamically at deploy time
by `./build.rb` under the `./generated` subdirectory and is not checked into
source. This process takes as inputs the templated extension
`snippets-menu.js.template` and the per-language collocated menu JSON files.

# Releasing

Resources are pushed to the appropriate GCS environment as part of our normal
release process.
