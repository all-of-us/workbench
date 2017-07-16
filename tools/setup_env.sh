#!/bin/bash -e
gcloud components install app-engine-java

cd ui
echo Installing the Angular command-line tool, to serve/build the UI.
sudo npm install -g @angular/cli
echo Installing UI project dependencies.
npm install

cd ../tools
wget https://repo1.maven.org/maven2/io/swagger/swagger-codegen-cli/2.2.3/swagger-codegen-cli-2.2.3.jar -O swagger-codegen-cli.jar
