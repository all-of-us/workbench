#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

cat src/main/webapp/WEB-INF/appengine-web.xml.template \
  | envsubst > src/main/webapp/WEB-INF/appengine-web.xml
