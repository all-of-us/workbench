#!/bin/bash
set -euo pipefail

cp src/main/webapp/WEB-INF/appengine-web.xml.template src/main/webapp/WEB-INF/appengine-web.xml

for line in $(awk '!/^ *#/ && NF' db/vars.env); do
  IFS='=' read -r var val <<< "$line"

  if [ "$var" == "DB_HOST" ] && [ $OVERWRITE_WORKBENCH_DB_HOST ]; then
    export DB_HOST=localhost
    continue
  fi

  evaluatedString=$(echo $(eval "echo $val"))
  echo "Exporting value $var=$evaluatedString"
  export $var=$evaluatedString

  var="sed -i '' -e 's|\${$var}|$evaluatedString|g' ./src/main/webapp/WEB-INF/appengine-web.xml"
  eval $var

done

echo "Generated App Engine XML"
