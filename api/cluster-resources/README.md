# initialize_notebook_cluster.sh

This is the script run on the Leonardo Jupyter server at cluster initialization
time, i.e. the [jupyterUserScriptUri](
https://github.com/DataBiosphere/leonardo/blob/cfdbff2448b9cff73ad658ba028d1feafab01b81/src/main/resources/swagger/api-docs.yaml#L509).

## Local testing

To manually test updates to this script locally:

- Push the script to GCS (username-suffixed):

  ```
  api$ gsutil cp cluster-resources/initialize_notebook_cluster.sh "gs://all-of-us-workbench-test-cluster-resources/initialize_notebook_cluster-${USER}.sh"
  ```

- (**Disclaimer**: local code change, do not submit) Temporarily update your
  local server config to use your custom script:

  ```
  api$ sed -i "s,initialize_notebook_cluster\.sh,initialize_notebook_cluster-${USER}.sh," src/main/java/org/pmiops/workbench/notebooks/LeonardoNotebooksClientImpl.java
  ```

- Ensure the change is picked up by your API server and point a local UI to it
- Open your local Workbench UI, go to the workspace 'About' tab, and click 'Reset notebook server'.
- Wait for the notebook cluster to be created.
  - Cluster creation will fail with 500s if the user script is not accessible to your pet SA,
    this should be granted by the registered tier group on the bucket ACL [cloud console UI](
    https://console.cloud.google.com/storage/browser/all-of-us-workbench-test-cluster-resources?project=all-of-us-workbench-test)
- Revert changes to `LeonardoNotebooksClientImpl.java`

## Debugging Script Issues

- Find your existing notebook cluster if any, by authorizing as your
  fake-research-aou.org user [here](
  https://leonardo.dsde-dev.broadinstitute.org/#!/cluster/listClusters).
- Call `listClusters` and take the returned `stagingBucket` value.
- `gcloud auth login USER@fake-research-aou.org`
- `gsutil ls gs://STAGING_BUCKET`
- Dig through the directories until you find the initialization script output
  log, as of 4/3/19 the file was named `dataproc-initialization-script-0_output`

## Quick Local Testing of the Leo Jupyter Image

```
docker run -i -t -u 0 --entrypoint "" us.gcr.io/broad-dsp-gcr-public/leonardo-jupyter:prod /bin/bash
```

This can be used to quickly test command lines or reproduce bugs.

# Local Jupyter extension testing

Tweak the above instructions for testing the user script to push a modified
extension and modify the cluster controller to use it.

Alternatively, on a live version of a Leo cluster, use Chrome local overrides to
plug in your locally modified Javascript.

- Follow these instructions to setup local overrides: https://developers.google.com/web/updates/2018/01/devtools#overrides
- Search the scripts tab to find the extension Javascript and save an override
- Find the path to that override on disk
- Copy your local Javascript to this path to push updates
- Reload the browser, ensuring devtools are open and "enable local overrides" is on

# Snippets Menu

AoU Clusters enable the [Snippets Menu extension](https://jupyter-contrib-nbextensions.readthedocs.io/en/latest/nbextensions/snippets_menu/readme.html)
with custom AoU-specific code snippets. The snippets live in
https://github.com/all-of-us/workbench-snippets. AoU configures Leo clusters as
follows to enable this:

1. Enable the snippets_menu/main extension in ./initialize_notebook_cluster.sh
1. Deploy a Jupyter UI extension to configure the menu with AoU-specific snippets

## Updating Snippets

As code snippets are updated in the source repository, we will want to
periocially update the menus in the Workbench. For now, this process is fairly
manual and can be improved going forwards (see RW-2665):

Prerequisite: Must have `jq` installed (for pretty printing).

1. In a separate directory, clone the snippets repo and build the menu config:
    https://github.com/all-of-us/workbench-snippets/blob/master/CONTRIBUTING.md#auto-generation-of-jupyter-snippets-menu-configuration
1. Run the following from the `workbench/api/cluster-resources` directory to pull in the updated JSON

    ```
    ./import_json_from_snippets_repo.sh <path to workbench-snippets repo>
    ```
1. Commit changes and go through normal pull request process.
1. Wait for a release; note that changes are only visible for clusters started
    after the release.

## Snippets Extension Implementation

The snippets menu extension Javascript is generated dynamically at deploy time
by `./build.rb` under the `./generated` subdirectory and is not checked into
source. This process takes as inputs the templated extension
`snippets-menu.js.template` and the per-language collocated menu JSON files.

## Local testing

See instructions for local extension testing above.

To test the menu contents JSON alone:

- Click the "Jupyter" logo from a Workbench notebook.
- Click the "Nbextensions" tab
- Click on "Snippets Menu"
- Check "Include custom menu content parsed from JSON string below"
- Paste menu JSON into text field

# Releasing

Resources are pushed to the appropriate GCS environment as part of our normal
release process.
