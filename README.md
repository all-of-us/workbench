# workbench

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

* Install JDK 7 on your machine from http://docs.oracle.com/javase/7/docs/webnotes/install/.
* Set your JAVA_HOME environment variable to point at the installation directory.
* Install Google Cloud SDK on your device from https://cloud.google.com/sdk/downloads.
* Run tools/setup_env.sh.

## Running dev appserver

To run AppEngine with workbench, run:

```Shell
./gradlew appengineRun
```

When the console displays "Dev App Server is now running", you can hit your local frontend resources
 under http://localhost:8080/ and your local API server under http://localhost:8081/api/.


## Running in Docker

```
docker build -t workbench .
docker run -p 8080:8080 -v $(pwd):/app --rm -it workbench
```
