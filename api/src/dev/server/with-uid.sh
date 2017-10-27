#!/bin/sh

if [[ "$UID" == 0 ]]; then
  echo 'UID is not set. Exiting.';
  exit 1;
fi

exec $@
