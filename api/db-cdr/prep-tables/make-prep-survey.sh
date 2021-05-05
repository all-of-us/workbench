#!/bin/bash

# Explanation here

set -e

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset

python --version