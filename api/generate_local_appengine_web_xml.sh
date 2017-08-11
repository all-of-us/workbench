#!/bin/bash
set -xeuo pipefail
IFS=$'\n\t'

cd db
source vars.env
cd ..
./generate_appengine_web_xml.sh
