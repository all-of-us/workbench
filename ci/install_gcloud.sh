#!/bin/bash -e

# Install gcloud

cd
wget https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-162.0.1-linux-x86_64.tar.gz -O gcloud.tgz
tar -xf gcloud.tgz
./google-cloud-sdk/install.sh  --quiet
echo "export PATH=~/google-cloud-sdk/bin:$PATH" > ~/.bashrc
