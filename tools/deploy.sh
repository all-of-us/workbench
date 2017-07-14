#!/bin/bash -e
# Deploys the workbench app to an environment. (In future, will support tagged versions and will
# perform configuration updates and schema migrations.)

while true; do
  case "$1" in
    --account) ACCOUNT=$2; shift 2;;
    --project) PROJECT=$2; shift 2;;
    -- ) shift; break ;;
     * ) break ;;
  esac
done

function usage {
  echo "Usage: deploy.sh --project all-of-us-workbench-test --account $USER@pmi-ops.org"
  exit 1
}

if [ -z "${PROJECT}" ]
then
  echo "Missing required --project flag."
  usage
fi

if [ -z "${ACCOUNT}" ]
then
  echo "Missing required --account flag."
  usage
fi

BOLD=$(tput bold)
NONE=$(tput sgr0)

echo "Deploying local code to project: ${BOLD}$PROJECT${NONE}"
read -p "Are you sure? (Y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]
then
  echo "Exiting."
  exit 1
fi

gcloud auth login $ACCOUNT
gcloud config set project $PROJECT

echo "${BOLD}Deploying API...${NONE}"
./gradlew :api:appengineDeploy

echo "${BOLD}Deploying UI...${NONE}"
cd ui
ng build  # Change this to `ng build --environment=prod` to minify.
gcloud app deploy
