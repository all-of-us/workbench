#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

envsubst < src/main/webapp/WEB-INF/appengine-web.xml.template \
  > src/main/webapp/WEB-INF/appengine-web.xml
