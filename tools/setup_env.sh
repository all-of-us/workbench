#!/bin/bash -e
gcloud components install app-engine-java

cd ui
echo Installing the Angular command-line tool, to serve/build the UI.
sudo npm install -g @angular/cli
echo Installing UI project dependencies.
npm install

cd ../tools
wget https://oss.sonatype.org/content/repositories/snapshots/io/swagger/swagger-codegen-cli/2.3.0-SNAPSHOT/swagger-codegen-cli-2.3.0-20170716.142514-29.jar -O swagger-codegen-cli.jar
