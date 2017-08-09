# Workbench

[![CircleCI Build Status](https://circleci.com/gh/all-of-us/workbench.svg)](https://circleci.com/gh/all-of-us/workflows/workbench)

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
npm run start [-- --environment test]
```

After webpack finishes the build, you can view your local UI server at
http://localhost:4200/.

By default, this connects to a local API server. Use `--environment=$ENV` to
use an alternate `src/environments/environment.$ENV.ts` file and connect to a
different API server.

#### You can regenerate classes from swagger with

```Shell
npm run codegen
```

#### You can build the UI with

```Shell
npm run build
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
./deploy.py --project PROJECT --account ACCOUNT@pmi-ops.org
```

Example:

```
./deploy.py -p all-of-us-workbench-test -a dan.rodney@pmi-ops.org
```

You will be prompted to confirm the deployment. When it finishes, you will be able to access the
UI under http://PROJECT.appspot.com and the API under http://api.PROJECT.appspot.com.

## git-secrets

### Setup

Download the git-secrets tool.
If you are on a mac, run:
```Shell
  brew install git-secrets
```
If you are on Linux, run:
```Shell
rm -rf git-secrets
git clone https://github.com/awslabs/git-secrets.git
cd git-secrets
sudo make install && sudo chmod o+rx /usr/local/bin/git-secrets
cd ..
rm -rf git-secrets
```
### Running

git-secrets by default runs every time you make a commit. But if you
want to manually scan:
#### The Repository
```Shell
git secrets --scan
```
#### A File(s)
```Shell
git secrets --scan /path/to/file (/other/path/to/file *)
```
#### A Directory (recursively)
```Shell
git secrets --scan -r /path/to/directory
```
