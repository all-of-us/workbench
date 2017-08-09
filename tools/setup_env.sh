#!/bin/bash -e
gcloud components install app-engine-java

cd ui
echo Installing the Angular command-line tool, to serve/build the UI.
sudo npm install -g @angular/cli
echo Installing UI project dependencies.
npm install


cd ../tools
./download_swagger_cli.sh
./download_cloud_sql_proxy.sh
