#!/bin/bash

# Pull the projectId field off the main projects list
gcloud projects list --format=json | jq '.[].projectId'
