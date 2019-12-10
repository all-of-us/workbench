#!/bin/bash

# Commands to load data and configs in a local Workbench environment.
#
# This script is meant to be run within the api-scripts Docker service in order to update
# a developer's local Workbench environment. This runs as a part of the main dev-env setup script,
# and should be re-run whenever a developer updates one of the relevant config files.
#
# These commands should be kept in sync with the associated deployment commands, which can be
# found under the "deploy" command in api/libproject/devstart.rb .

./gradlew --daemon updateCdrVersions -PappArgs="['/w/api/config/cdr_versions_local.json',false]"
./gradlew --daemon loadConfig -Pconfig_key=main -Pconfig_file=config/config_local.json
./gradlew --daemon loadConfig -Pconfig_key=cdrBigQuerySchema -Pconfig_file=config/cdm/cdm_5_2.json
./gradlew --daemon loadConfig -Pconfig_key=featuredWorkspaces -Pconfig_file=config/featured_workspaces_local.json
./gradlew --daemon loadDataDictionary -PappArgs=false
