# Deployment

This directory contains scripts for deploying the Workbench API/UIs. To deploy
individual services, see [../api/project.rb](../api/project.rb) `deploy-api`
and [../ui/project.rb](../ui/project.rb) `deploy-ui`. Test and staging are
automatically pushed by CircleCI.

- test: pushed by Circle on every main branch merge
- staging: pushed by Circle when someone tags a new release
- stable: manually pushed using a tagged staging release

The scripts in this directory are to be used for manual deployment to stable
(and later, prod). Ideally we'd do this in some cloud service, but none of the
standard solutions have been cleared by security for use; for now we must run
these processes locally.

## Deploy to staging

- Verify that the most recent main commit [passed on Circle](https://circleci.com/gh/all-of-us/workflows/workbench/tree/main)
- Go to the [GitHub releases page](https://github.com/all-of-us/workbench/releases);
  note the latest version (e.g. v0-1-rc1)
- Click "Draft new release"
- Increment the minor version and reset the RC/patch version from the previous
  release (e.g. if the previous were "v0-2-rc5", next would be "v0-3-rc1")
- Wait for the staging release to complete on Circle

## Deploy to stable

- Ensure that Circle staging deployment succeeded.
- Run deployment (automatically takes the current staging deployment):
  ```
  deploy$ ./project.rb deploy --project all-of-us-rw-stable \
    --account deploy@all-of-us-rw-stable.iam.gserviceaccount.com --promote
  ```

# Manual deployment details

Local manual deployment aims to mirror what is used on Circle for test/staging.
Ideally, we'd push an identical binary from staging -> stable, but this is not
currently easy or possible given the tech stack and injection of env specific
files within the build artifacts.

We run within docker for consistent builds across machines, and although
`deploy.rb` will almost certainly be run from a git client, we clone a clean
git client within docker to avoid pollution of local changes or ignored files.
We bootstrap the [circle build image](../ci/Dockerfile.circle_build) using our
own [entrypoint](./bootstrap-docker.sh); this clones the repo (cached) and sets
up volume file permissions for the circleci UID (specified in our Dockerfile).

**Note**: Local changes to deploy.rb or devstart.rb are **not** currently picked
up by the deployment process; it uses the tools code from the commit version
specified for deployment.

# Manual testing

Note: See above for an explanation of why this process is special right now.
This could be improved in the future by supporting builds via dirty clients.

- To test changes without deploying, use the --dry-run flag
- To run a full manual test deployment, use --project all-of-us-workbench-test
  --app-version *username* --no-promote.

To ensure your local changes are picked up during the deploy process, follow
this process:

1. Commit your changes to the deploy scripts
1. Push your branch to GitHub, e.g. 'ch/deploy-fix'
1. Use --dry-run and --git-version to build using your branch:

  ```
  ./project.rb deploy --dry-run --no-promote --project all-of-us-workbench-test \
    --git-version origin/ch/deploy-fix --app-version calbach \
    --account circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com
  ```
