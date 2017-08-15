#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

CIRCLECI="${CIRCLECI:-false}"

if [[ ! "$CIRCLECI" == "true" ]]; then
  cd db
  source vars.env
  cd ..
fi

cat src/main/webapp/WEB-INF/appengine-web.xml.template \
  | envsubst > src/main/webapp/WEB-INF/appengine-web.xml
