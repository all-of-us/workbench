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
by `./build.rb` into the GAE static resources folder. This process takes as
inputs the templated extension `snippets-menu.js.template` and the per-language
collocated menu JSON files.

## Local testing

See instructions for local extension testing above.

To test the menu contents JSON alone:

- Click the "Jupyter" logo from a Workbench notebook.
- Click the "Nbextensions" tab
- Click on "Snippets Menu"
- Check "Include custom menu content parsed from JSON string below"
- Paste menu JSON into text field

# Releasing

Resources are pushed as static assets on the API GAE server as part of our
normal release process.
