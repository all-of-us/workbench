#!/bin/bash -e
gcloud components install app-engine-java

cd ui
echo Installing the Angular command-line tool, to serve/build the UI.
sudo npm install -g @angular/cli
echo Installing UI project dependencies.
npm install


cd ../tools

echo Installing git-secrets
echo See README for setup instructions if this fails
if which brew
then
  brew install git-secrets
else
  git clone https://github.com/awslabs/git-secrets.git
  cd git-secrets
  make install
  cd ..
  rm -rf git-secrets
fi


./download_swagger_cli.sh
./download_cloud_sql_proxy.sh
