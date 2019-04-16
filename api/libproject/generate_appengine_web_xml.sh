#!/bin/bash

for line in `awk '!/^ *#/ && NF' db/vars.env`; do
  IFS='=' read -r var val <<< "$line"

  if [ "$var" == "DB_HOST" ] && [ $OVERWRITE_WORKBENCH_DB_HOST ]; then
    export DB_HOST=localhost
    continue
  fi

  evaluatedString=$(echo $(eval "echo $val"))
  echo "Exporting value $var=$evaluatedString"
  export $var=$evaluatedString
done

set -euo pipefail
IFS=$'\n\t'

cat src/main/webapp/WEB-INF/appengine-web.xml.template \
  | sed "s|\${DB_DRIVER}|${DB_DRIVER}|" \
  | sed "s|\${DB_CONNECTION_STRING}|${DB_CONNECTION_STRING}|" \
  | sed "s|\${WORKBENCH_DB_USER}|${WORKBENCH_DB_USER}|" \
  | sed "s|\${WORKBENCH_DB_PASSWORD}|${WORKBENCH_DB_PASSWORD}|" \
  | sed "s|\${CDR_DB_CONNECTION_STRING}|${CDR_DB_CONNECTION_STRING}|" \
  | sed "s|\${CDR_DB_USER}|${CDR_DB_USER}|" \
  | sed "s|\${CDR_DB_PASSWORD}|${CDR_DB_PASSWORD}|" \
  > src/main/webapp/WEB-INF/appengine-web.xml

echo "Generated App Engine XML:"
cat src/main/webapp/WEB-INF/appengine-web.xml