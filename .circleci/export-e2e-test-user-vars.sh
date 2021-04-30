#!/bin/bash
set -xv

ENV=${1:-test}

for arg in "$@"
do
  case "$arg" in
    "perf")
       echo "export PERF_WRITER=$PUPPETEER_WRITER_PERF" >> $BASH_ENV
       echo "export PERF_COLLABORATOR=$PUPPETEER_COLLABORATOR_PERF" >> $BASH_ENV
       echo "export USER_NAME=$PUPPETEER_USER_PERF@perf.fake-research-aou.org" >> $BASH_ENV
       ;;
    "staging")
       echo "export STAGING_WRITER=$PUPPETEER_WRITER_STAGING" >> $BASH_ENV
       echo "export STAGING_COLLABORATOR=$PUPPETEER_COLLABORATOR_STAGING" >> $BASH_ENV
       echo "export USER_NAME=$PUPPETEER_USER_STAGING@staging.fake-research-aou.org" >> $BASH_ENV
       ;;
    "test")
       echo "export TEST_WRITER=$PUPPETEER_WRITER_TEST" >> $BASH_ENV
       echo "export TEST_COLLABORATOR=$PUPPETEER_COLLABORATOR_TEST" >> $BASH_ENV
       echo "export USER_NAME=$PUPPETEER_USER_TEST@fake-research-aou.org" >> $BASH_ENV
       ;;
    "local")
       echo "export DEV_WRITER=$PUPPETEER_WRITER_LOCAL" >> $BASH_ENV
       echo "export DEV_COLLABORATOR=$PUPPETEER_COLLABORATOR_LOCAL" >> $BASH_ENV
       echo "export USER_NAME=$PUPPETEER_USER_LOCAL@fake-research-aou.org" >> $BASH_ENV
       ;;
   esac
   echo "export PASSWORD=$PUPPETEER_USER_PASSWORD" >> $BASH_ENV
   source $BASH_ENV
done
