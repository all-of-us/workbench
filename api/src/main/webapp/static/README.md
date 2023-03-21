# Static Files in AppEngine
We host a number of public static files in GAE for runtime creation. Generated files will be copied here at build time and gitnored (e.g. snippets menu), if any.

# initialize_notebook_runtime.sh

This is the script run on the Leonardo Jupyter server at runtime initialization
time, i.e. the [jupyterUserScriptUri](
https://github.com/DataBiosphere/leonardo/blob/cfdbff2448b9cff73ad658ba028d1feafab01b81/src/main/resources/swagger/api-docs.yaml#L509).

## Local testing

To manually test updates to any notebook server assets (initialization script, UI extensions, etc):

- Push a personal API server to test:

  ```
  api$ ./project.rb deploy-api --no-promote --version "${USER}" --project all-of-us-workbench-test
  ```

  **Note**: This deployed API server will ONLY be used for serving your static assets. This approach
  does not interact with any API methods on this deployed server. If you subsequently change your
  static assets being tested, you'll need to rerun this step. This step is necessary because Leo
  cannot make requests to your local workstation to fetch updated static assets.

- (**Disclaimer**: local code change, do not submit) Temporarily update your
  local server config to use your deployed API server for static asset serving:

  ```
  api$ sed -i "s/api-dot-all-of-us-workbench-test.appspot.com/${USER}-dot-\0/" config/config_local.json
  ```

- Run a **local** API server with this config update and point a local UI to it
- Open your local Workbench UI, go to the workspace 'About' tab, and click 'Reset notebook server'.
- Wait for the notebook runtime to be created.
- Revert changes to `config_local.json` before sending a PR

## Debugging Script Issues

- Run `./project.rb list-runtimes --project all-of-us-workbench-test` to find your runtime name
- Run `./project.rb describe-runtime --id <runtime-name>` to print debug information about
  the runtime. This will include command lines for listing out associated log files
- Dig through the directories until you find the initialization script output
  log, as of 4/3/19 the file was named `dataproc-initialization-script-0_output`

## Quick Local Testing of the Leo Jupyter Image

```
docker run -i -t -u 0 --entrypoint "" us.gcr.io/broad-dsp-gcr-public/leonardo-jupyter:prod /bin/bash
```

This can be used to quickly test command lines or reproduce bugs.

# Local Jupyter extension testing

Tweak the above instructions for testing the user script to push a modified
extension and modify the runtime controller to use it.

Alternatively, on a live version of a Leo runtime, use Chrome local overrides to
plug in your locally modified Javascript.

- Follow these instructions to setup local overrides: https://developers.google.com/web/updates/2018/01/devtools#overrides
- Search the scripts tab to find the extension Javascript and save an override
- Find the path to that override on disk
- Copy your local Javascript to this path to push updates
- Reload the browser, ensuring devtools are open and "enable local overrides" is on
