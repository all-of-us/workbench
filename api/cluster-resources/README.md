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

# Releasing

Resources will be pushed to the appropriate GCS environment along with our
normal release process.
