# workbench

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
