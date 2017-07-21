#!/bin/bash -e
cd db
source setup_local_vars.sh
cd ..
./generate_appengine_web_xml.sh