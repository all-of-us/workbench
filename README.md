# workbench

## Getting the code

* Run "git clone https://https://github.com/all-of-us/workbench"

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

* Download Gradle 4.0 from https://services.gradle.org/distributions/gradle-4.0-bin.zip

* Unzip it and add bin/gradle to the path.

* Run tools/setup_env.sh.

## Running dev appserver

To run AppEngine with workbench, run:

```Shell
gradle appengineRun
```

When the console displays "Dev App Server is now running", you can hit your local server at http://localhost:8080/.
