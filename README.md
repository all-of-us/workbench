# Workbench

## Getting the code

* Run "git clone https://github.com/all-of-us/workbench"

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

* Install JDK 7 on your machine from http://docs.oracle.com/javase/7/docs/webnotes/install/.
* Set your JAVA_HOME environment variable to point at the installation directory.

### UI

Direct your editor to write swap files outside the source tree, so Webpack
does not reload when they're updated.
[Example for vim](https://github.com/angular/angular-cli/issues/4593).

### General

* Install Google Cloud SDK on your device from https://cloud.google.com/sdk/downloads.
* Run tools/setup_env.sh.

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
./deploy.sh --project PROJECT --account ACCOUNT@pmi-ops.org
```

Example:

```
./deploy.sh --project all-of-us-workbench-test --account dan.rodney@pmi-ops.org
```

You will be prompted to confirm the deployment. When it finishes, you will be able to access the
UI under http://PROJECT.appspot.com and the API under http://api.PROJECT.appspot.com.

