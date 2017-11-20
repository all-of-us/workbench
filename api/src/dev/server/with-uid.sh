#!/bin/sh

if [[ "$(whoami 2>/dev/null)" == 'root' ]]; then
  >&2 echo 'This container should not be run as the root user. Exiting.';
  exit 1;
fi

exec $@
