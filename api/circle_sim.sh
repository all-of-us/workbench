#!/bin/bash

# USAGE
# This script can be used to simulate the Circle CI environment locally for quicker iteration.
# This is fairly limited and skips most of the Circle CI setup, but can help to debug basic
# gradle / environment issues that don't appear locally.
#
# To use this, first modify the script as needed (see comments below). Note that it is currently
# configured primarily for API debugging. Then:
#
# $ docker run -i allofustest/workbench:buildimage-0.0.19 < circle_sim.sh

mkdir -p /home/circleci/workbench
cd /home/circleci/workbench
git clone https://github.com/all-of-us/workbench .

git submodule update --init --recursive
git checkout ch/gradle-5 # YOUR BRANCH NAME HERE

export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.workers.max=2 -Dkotlin.incremental=false -Dkotlin.compiler.execution.strategy=in-process"

# YOUR TEST COMMANDS HERE
cd api
gradle compileGeneratedJava
