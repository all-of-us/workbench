# Workbench

[![CircleCI Build Status](https://circleci.com/gh/all-of-us/workbench.svg)](https://circleci.com/gh/all-of-us/workbench)

You can use a simple image URL like this to see the status of your project’s default branch: https://circleci.com/gh/:owner/:repo.png?circle-token=9cd8d79a126cdcffb0c18239b62fa7068b491942

## Getting the code

* Run `git clone https://github.com/all-of-us/workbench`

To make changes, do:

* git checkout master
* git pull
* git checkout -b USERNAME/BRANCH_NAME
* (make changes and git add / commit them)
* git push -u origin USERNAME/BRANCH_NAME

And make a pull request in your browser at
https://github.com/all-of-us/workbench based on your upload.

After responding to changes, merge in GitHub.

## Setup

### API

* Install JDK 8 on your machine from http://www.oracle.com/technetwork/java/javase/downloads/index.html.
* Set your JAVA_HOME environment variable to point at the installation directory.

### UI

* Direct your editor to write swap files outside the source tree, so Webpack
does not reload when they're updated.
[Example for vim](https://github.com/angular/angular-cli/issues/4593).
* Install npm. (For Google workstations, see go/nodejs.)

### General

* Install Google Cloud SDK on your device from https://cloud.google.com/sdk/downloads.
* If not selected initially, add the AppEngine component with `gcloud components install app-engine-java`.
* Run tools/setup_env.sh. (This installs angular-cli globally. You may also get it from `ui/node_modules` after
running `npm install`.)

## Running the Dev Servers

### API: dev AppEngine appserver

From the `api/` directory:

```Shell
./gradlew appengineRun
```

When the console displays "Dev App Server is now running", you can hit your
local API server under http://localhost:8081/api/.

### UI

From the `ui/` subdirectory:

```Shell
ng serve
```

After webpack finishes the build, you can view your local UI server at
http://localhost:4200/.

## Running in Docker

```
cd api
docker build -t workbench-api .
docker run -p 8081:8081 -v --rm -it workbench-api
```

```
cd ui
docker build -t workbench-ui .
docker run -p 4200:4200 -v --rm -it workbench-ui
```

## Deploying

To deploy your local code to a given AppEngine project, run:

```
./deploy.py --project PROJECT --account ACCOUNT@pmi-ops.org
```

Example:

```
./deploy.py -p all-of-us-workbench-test -a dan.rodney@pmi-ops.org
```

You will be prompted to confirm the deployment. When it finishes, you will be able to access the
UI under http://PROJECT.appspot.com and the API under http://api.PROJECT.appspot.com.

