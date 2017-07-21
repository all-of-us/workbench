#!/bin/bash -e
cat src/main/webapp/WEB-INF/appengine-web.xml.template | envsubst > src/main/webapp/WEB-INF/appengine-web.xml
