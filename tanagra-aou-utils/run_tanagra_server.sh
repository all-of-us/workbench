#!/usr/bin/env bash

while getopts ":ad:u:" arg; do
  case "${arg}" in
    a) # Disable authentication.
      disableAuthChecks=1
      ;;
    d) # Drop database if exists.
      dropDbIfExists=1
      ;;
    u) # tanagra underlay to use
      underlay=${OPTARG}
      ;;
    h | *) # Display help.
      usage
      exit 0
      ;;
  esac
done

export TANAGRA_DB_NAME=tanagra_db

export TANAGRA_DB_URI=jdbc:mariadb://${DB_HOST}:${DB_PORT}/${TANAGRA_DB_NAME}
export TANAGRA_DB_INITIALIZE_ON_START=false
export TANAGRA_DB_USERNAME=workbench
export TANAGRA_DB_PASSWORD=wb-notasecret

echo "Using AoU underlays - ${underlay}"
export TANAGRA_UNDERLAY_FILES="${underlay}"

export TANAGRA_FEATURE_ARTIFACT_STORAGE_ENABLED=true
export TANAGRA_AUTH_IAP_GKE_JWT=false

if [[ ${disableAuthChecks} ]]; then
  echo "Disabling AuthN"
  export TANAGRA_AUTH_DISABLE_CHECKS=true
  export TANAGRA_AUTH_BEARER_TOKEN=false
else
  echo "Enabling AuthN"
  export TANAGRA_AUTH_DISABLE_CHECKS=false
  export TANAGRA_AUTH_BEARER_TOKEN=true
fi

# always init mariadb with tanagra db - fix this later
if [[ ${dropDbIfExists} ]]; then
  source init_new_tanagra_db.sh --drop-if-exists &
else
  source init_new_tanagra_db.sh &
fi
echo "Using default application credentials from:"
env | grep GOOGLE_APPLICATION_CREDENTIALS

# run from tanagra folder
cd tanagra
# deploy service
./gradlew -PisMySQL service:bootRun
